package com.threadia.backend.platform.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
	void commitsSettingAndAuditWithPersistedVersionEvidenceWithoutSettingValueSnapshot() throws Exception {
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
			assertThat(audit.getBeforeState()).doesNotContain("before private message", "[REDACTED]", "\"value\"");
			assertThat(audit.getAfterState()).doesNotContain("after private message", "[REDACTED]", "\"value\"");
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
	void pessimisticWriteLockContentionReturnsVersionConflictThroughReplace() throws Exception {
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
					service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS,
							new ReplaceSystemSettingRequest(json("{\"status\":\"DELAYED\",\"message\":\"contended\"}"), setting.getVersion(), "동시 변경"));
					return null;
				} catch (Throwable throwable) {
					return throwable;
				}
			});
			Throwable lockFailure = second.get(CONCURRENCY_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
			assertThat(lockFailure)
					.isInstanceOfSatisfying(SystemSettingConflictException.class,
							conflict -> assertThat(conflict.getCode()).isEqualTo("SYSTEM_SETTING_VERSION_CONFLICT"));
			releaseFirstLock.countDown();
			first.get(CONCURRENCY_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
			SystemSetting persisted = settingRepository.findById(setting.getId()).orElseThrow();
			assertThat(persisted.getVersion()).isEqualTo(1L);
			assertThat(persisted.getValue().path("status").asText()).isEqualTo("OPEN");
			assertThat(persisted.getValue().path("message").asText()).isEqualTo("before private message");
			assertThat(auditRepository.findAll()).isEmpty();
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

}
