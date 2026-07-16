package com.threadia.backend.platform.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.Duration;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.hibernate.exception.LockAcquisitionException;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.platform.audit.AdminAction;
import com.threadia.backend.platform.audit.AdminActionAuditRepository;
import com.threadia.backend.platform.audit.AuditTargetType;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.repository.PlatformAdminRepository;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:system-settings-persistence;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
class SystemSettingsPersistenceIntegrationTest {
	private static final Duration CONCURRENCY_TIMEOUT = Duration.ofSeconds(10);
	@Autowired private SystemSettingsService service;
	@Autowired private SystemSettingRepository settingRepository;
	@Autowired private AdminActionAuditRepository auditRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private PlatformAdminRepository platformAdminRepository;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private PlatformTransactionManager transactionManager;
	@Autowired private JdbcTemplate jdbcTemplate;
	@MockitoBean private PlatformAdminAccessService platformAdminAccessService;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;

	private User actor;
	private PlatformAdmin admin;
	private SystemSetting setting;

	@BeforeEach
	void setUp() throws Exception {
		auditRepository.deleteAll();
		settingRepository.deleteAll();
		platformAdminRepository.deleteAll();
		userRepository.deleteAll();
		LocalDateTime now = LocalDateTime.of(2026, 7, 16, 0, 0);
		actor = userRepository.save(new User("admin@example.com", "hash", "admin", null, UserStatus.ACTIVE, now, now));
		admin = platformAdminRepository.save(new PlatformAdmin(actor, PlatformAdminRole.SUPER_ADMIN, now));
		setting = settingRepository.save(new SystemSetting(SystemSettingKey.SUPPORT_CENTER_STATUS, json("{\"status\":\"OPEN\",\"message\":\"before private message\"}"), 1, now, now));
		when(platformAdminAccessService.requireAdmin("token")).thenReturn(new PlatformAdminAuthority(actor, admin));
	}

	@Test
	void commitsSettingAndAuditWithPersistedVersionEvidenceAndRedaction() throws Exception {
		SystemSettingResponse response = service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS,
				new ReplaceSystemSettingRequest(json("{\"status\":\"DELAYED\",\"message\":\"after private message\"}"), setting.getVersion(), "운영 상태 변경"));

		SystemSetting persisted = settingRepository.findById(setting.getId()).orElseThrow();
		assertThat(response.version()).isEqualTo(2L);
		assertThat(persisted.getVersion()).isEqualTo(2L);
		assertThat(persisted.getValue().get("message").asText()).isEqualTo("after private message");
		assertThat(auditRepository.findAll()).singleElement().satisfies(audit -> {
			assertThat(audit.getAction()).isEqualTo(AdminAction.SYSTEM_SETTING_REPLACED);
			assertThat(audit.getTargetType()).isEqualTo(AuditTargetType.SYSTEM_SETTING);
			assertThat(audit.getTargetId()).isEqualTo(setting.getId());
			assertThat(audit.getBeforeState()).doesNotContain("before private message").contains("[REDACTED]");
			assertThat(audit.getAfterState()).doesNotContain("after private message").contains("[REDACTED]");
			assertThat(snapshot(audit.getBeforeState()).path("version").asLong()).isEqualTo(1L);
			assertThat(snapshot(audit.getAfterState()).path("version").asLong()).isEqualTo(2L);
			assertThat(snapshot(audit.getAfterState()).path("settingKey").asText()).isEqualTo("SUPPORT_CENTER_STATUS");
		});
	}

	@Test
	void onePersistedReplacementMakesSecondStaleExpectedVersionConflictWithoutSecondAudit() throws Exception {
		service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS,
				new ReplaceSystemSettingRequest(json("{\"status\":\"DELAYED\",\"message\":\"first\"}"), 1L, "첫 번째 변경"));

		assertThatThrownBy(() -> service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS,
				new ReplaceSystemSettingRequest(json("{\"status\":\"MAINTENANCE\",\"message\":\"stale\"}"), 1L, "두 번째 변경")))
				.isInstanceOf(SystemSettingConflictException.class)
				.extracting("code")
				.isEqualTo("SYSTEM_SETTING_VERSION_CONFLICT");
		assertThat(settingRepository.findById(setting.getId())).get().extracting(SystemSetting::getVersion).isEqualTo(2L);
		assertThat(auditRepository.findAll()).hasSize(1);
	}

	@Test
	void pessimisticWriteLockTimesOutInIndependentTransaction() throws Exception {
		CountDownLatch firstHasLock = new CountDownLatch(1);
		CountDownLatch releaseFirstLock = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<?> first = executor.submit(() -> transactionTemplate().executeWithoutResult(status -> {
				settingRepository.findByKeyForUpdate(SystemSettingKey.SUPPORT_CENTER_STATUS).orElseThrow();
				firstHasLock.countDown();
				await(releaseFirstLock);
			}));
			await(firstHasLock);

			Future<Throwable> second = executor.submit(() -> {
				try {
					return transactionTemplate().execute(status -> {
						jdbcTemplate.execute("SET LOCK_TIMEOUT 100");
						try {
							settingRepository.findByKeyForUpdate(SystemSettingKey.SUPPORT_CENTER_STATUS).orElseThrow();
							return null;
						} catch (Throwable throwable) {
							throw rethrow(throwable);
						}
					});
				} catch (Throwable throwable) {
					return throwable;
				}
			});
			Throwable lockFailure = second.get(CONCURRENCY_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
			assertThat(isLockFailure(lockFailure)).isTrue();
		} finally {
			releaseFirstLock.countDown();
			executor.shutdownNow();
			executor.awaitTermination(CONCURRENCY_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
		}
	}

	private JsonNode snapshot(String value) {
		try {
			return objectMapper.readTree(value);
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}

	private JsonNode json(String value) throws Exception { return objectMapper.readTree(value); }

	private TransactionTemplate transactionTemplate() {
		return new TransactionTemplate(transactionManager);
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(CONCURRENCY_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
				throw new IllegalStateException("system settings concurrency latch 대기 시간이 초과되었습니다.");
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("system settings concurrency test가 중단되었습니다.", exception);
		}
	}

	private static boolean isLockFailure(Throwable throwable) {
		for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
			if (cause instanceof CannotAcquireLockException
					|| cause instanceof PessimisticLockingFailureException
					|| cause instanceof LockAcquisitionException
					|| cause instanceof PessimisticLockException
					|| cause instanceof LockTimeoutException) {
				return true;
			}
			if (cause instanceof SQLException sqlException
					&& "HYT00".equals(sqlException.getSQLState())
					&& sqlException.getErrorCode() == 50200) {
				return true;
			}
		}
		return false;
	}

	private static RuntimeException rethrow(Throwable throwable) {
		if (throwable instanceof RuntimeException runtimeException) {
			return runtimeException;
		}
		if (throwable instanceof Error error) {
			throw error;
		}
		return new IllegalStateException(throwable);
	}
}
