package com.threadia.backend.platform.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.platform.audit.AdminAction;
import com.threadia.backend.platform.audit.AdminActionAuditService;
import com.threadia.backend.platform.audit.AuditTargetType;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;
import com.threadia.backend.supportcase.exception.SupportCaseValidationException;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;

@ExtendWith(MockitoExtension.class)
class SystemSettingsServiceTest {
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC);
	@Mock private PlatformAdminAccessService accessService;
	@Mock private SystemSettingRepository settingRepository;
	@Mock private AdminActionAuditService auditService;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private SystemSettingsService service;
	private User actor;
	private PlatformAdmin superAdmin;
	private SystemSetting setting;

	@BeforeEach
	void setUp() throws Exception {
		service = new SystemSettingsService(accessService, settingRepository, new SystemSettingCatalogValidator(), auditService, CLOCK);
		actor = user(1L);
		superAdmin = new PlatformAdmin(actor, PlatformAdminRole.SUPER_ADMIN, LocalDateTime.now(CLOCK));
		setField(superAdmin, "id", 2L);
		setting = new SystemSetting(SystemSettingKey.SUPPORT_CENTER_STATUS, json("{\"status\":\"OPEN\",\"message\":\"정상 운영\"}"), 1, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK));
		setField(setting, "id", 10L);
		setField(setting, "version", 1L);
	}

	@Test
	void superAdminReplacesWholeValueAndRecordsBeforeAfterAudit() throws Exception {
		when(accessService.requireAdmin("token")).thenReturn(new PlatformAdminAuthority(actor, superAdmin));
		when(settingRepository.findByKeyForUpdate(SystemSettingKey.SUPPORT_CENTER_STATUS)).thenReturn(Optional.of(setting));
		doAnswer(invocation -> { setField(setting, "version", 2L); return null; }).when(settingRepository).flush();
		List<Map<?, ?>> capturedSnapshots = capturedAuditSnapshots();

		service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS, new ReplaceSystemSettingRequest(json("{\"status\":\"DELAYED\",\"message\":\"응답 지연\"}"), 1L, "  운영 상태 변경  "));

		assertThat(setting.getValue().get("status").asText()).isEqualTo("DELAYED");
		verify(auditService).recordPlatformMutation(eq(null), eq(actor), eq(superAdmin), eq(AdminAction.SYSTEM_SETTING_REPLACED), eq(AuditTargetType.SYSTEM_SETTING), eq(10L), eq("운영 상태 변경"), any(), any());
		assertThat(capturedSnapshots.get(0).get("settingKey")).isEqualTo("SUPPORT_CENTER_STATUS");
		assertThat(capturedSnapshots.get(0).get("valueSchemaVersion")).isEqualTo(1);
		assertThat(capturedSnapshots.get(0).get("version")).isEqualTo(1L);
		assertThat(capturedSnapshots.get(1).get("version")).isEqualTo(2L);
		assertThat(capturedSnapshots.get(0).get("value").toString()).doesNotContain("정상 운영").contains("[REDACTED]");
		assertThat(capturedSnapshots.get(1).get("value").toString()).doesNotContain("응답 지연").contains("[REDACTED]");
		verify(settingRepository).flush();
	}

	@Test
	void platformAdminReadsOneSettingButNonAdminCannotReadIt() {
		PlatformAdmin platformAdmin = new PlatformAdmin(actor, PlatformAdminRole.PLATFORM_ADMIN, LocalDateTime.now(CLOCK));
		when(accessService.requireAdmin("platform-token")).thenReturn(new PlatformAdminAuthority(actor, platformAdmin));
		when(settingRepository.findByKey(SystemSettingKey.SUPPORT_CENTER_STATUS)).thenReturn(Optional.of(setting));

		assertThat(service.get("platform-token", SystemSettingKey.SUPPORT_CENTER_STATUS).key()).isEqualTo(SystemSettingKey.SUPPORT_CENTER_STATUS);
		when(accessService.requireAdmin("regular-token")).thenThrow(new ForbiddenException("플랫폼 관리자 권한이 필요합니다."));
		assertThatThrownBy(() -> service.get("regular-token", SystemSettingKey.SUPPORT_CENTER_STATUS)).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void redactsTemplateAndAnnouncementTextInAuditSnapshots() throws Exception {
		SystemSetting templates = setting(SystemSettingKey.REPLY_TEMPLATES, "{\"templates\":[{\"code\":\"DEFAULT\",\"title\":\"기본\",\"body\":\"비공개 답변 원문\"}]}", 11L);
		SystemSetting banner = setting(SystemSettingKey.ANNOUNCEMENT_BANNER, "{\"enabled\":true,\"message\":\"비공개 점검 안내\"}", 12L);
		when(accessService.requireAdmin("token")).thenReturn(new PlatformAdminAuthority(actor, superAdmin));
		when(settingRepository.findByKeyForUpdate(SystemSettingKey.REPLY_TEMPLATES)).thenReturn(Optional.of(templates));
		when(settingRepository.findByKeyForUpdate(SystemSettingKey.ANNOUNCEMENT_BANNER)).thenReturn(Optional.of(banner));
		List<Map<?, ?>> capturedSnapshots = capturedAuditSnapshots();

		service.replace("token", SystemSettingKey.REPLY_TEMPLATES, new ReplaceSystemSettingRequest(json("{\"templates\":[{\"code\":\"DEFAULT\",\"title\":\"기본\",\"body\":\"새 비공개 답변 원문\"}]}"), 1L, "템플릿 변경"));
		service.replace("token", SystemSettingKey.ANNOUNCEMENT_BANNER, new ReplaceSystemSettingRequest(json("{\"enabled\":true,\"message\":\"새 비공개 점검 안내\"}"), 1L, "배너 변경"));

		verify(auditService, org.mockito.Mockito.times(2)).recordPlatformMutation(eq(null), eq(actor), eq(superAdmin), eq(AdminAction.SYSTEM_SETTING_REPLACED), eq(AuditTargetType.SYSTEM_SETTING), any(), any(), any(), any());
		String serializedSnapshots = capturedSnapshots.toString();
		assertThat(serializedSnapshots).doesNotContain("비공개 답변 원문", "새 비공개 답변 원문", "비공개 점검 안내", "새 비공개 점검 안내").contains("[REDACTED]");
	}

	@Test
	void platformAdminCanReadButCannotReplace() throws Exception {
		PlatformAdmin platformAdmin = new PlatformAdmin(actor, PlatformAdminRole.PLATFORM_ADMIN, LocalDateTime.now(CLOCK));
		when(accessService.requireAdmin("token")).thenReturn(new PlatformAdminAuthority(actor, platformAdmin));
		assertThatThrownBy(() -> service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS, new ReplaceSystemSettingRequest(json("{\"status\":\"DELAYED\",\"message\":\"응답 지연\"}"), 1L, "운영 상태 변경")))
				.isInstanceOf(ForbiddenException.class);
		verifyNoInteractions(settingRepository, auditService);
	}

	@Test
	void distinguishesStaleVersionAndNoChangeWithoutAudit() throws Exception {
		when(accessService.requireAdmin("token")).thenReturn(new PlatformAdminAuthority(actor, superAdmin));
		when(settingRepository.findByKeyForUpdate(SystemSettingKey.SUPPORT_CENTER_STATUS)).thenReturn(Optional.of(setting));
		assertThatThrownBy(() -> service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS, new ReplaceSystemSettingRequest(json("{\"status\":\"DELAYED\",\"message\":\"응답 지연\"}"), 2L, "운영 상태 변경")))
				.isInstanceOf(SystemSettingConflictException.class).extracting("code").isEqualTo("SYSTEM_SETTING_VERSION_CONFLICT");
		assertThatThrownBy(() -> service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS, new ReplaceSystemSettingRequest(setting.getValue(), 1L, "운영 상태 변경")))
				.isInstanceOf(SystemSettingConflictException.class).extracting("code").isEqualTo("SYSTEM_SETTING_NO_CHANGE");
		verifyNoInteractions(auditService);
	}

	@ParameterizedTest
	@MethodSource("lockContentionExceptions")
	void convertsRepositoryLockContentionToVersionConflict(RuntimeException lockFailure) throws Exception {
		when(accessService.requireAdmin("token")).thenReturn(new PlatformAdminAuthority(actor, superAdmin));
		when(settingRepository.findByKeyForUpdate(SystemSettingKey.SUPPORT_CENTER_STATUS)).thenThrow(lockFailure);

		assertThatThrownBy(() -> service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS,
				new ReplaceSystemSettingRequest(json("{\"status\":\"DELAYED\",\"message\":\"응답 지연\"}"), 1L, "운영 상태 변경")))
				.isInstanceOf(SystemSettingConflictException.class)
				.extracting("code")
				.isEqualTo("SYSTEM_SETTING_VERSION_CONFLICT");
		verifyNoInteractions(auditService);
	}

	@Test
	void validatesReasonAfterTrimUsingThreeToFiveHundredCharacterBoundary() throws Exception {
		when(accessService.requireAdmin("token")).thenReturn(new PlatformAdminAuthority(actor, superAdmin));
		when(settingRepository.findByKeyForUpdate(SystemSettingKey.SUPPORT_CENTER_STATUS)).thenReturn(Optional.of(setting));
		String exactlyFiveHundred = "r".repeat(500);
		service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS,
				new ReplaceSystemSettingRequest(json("{\"status\":\"DELAYED\",\"message\":\"응답 지연\"}"), 1L, " " + exactlyFiveHundred + " "));
		assertThatThrownBy(() -> service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS,
				new ReplaceSystemSettingRequest(json("{\"status\":\"MAINTENANCE\",\"message\":\"점검\"}"), 1L, "  x  ")))
				.isInstanceOf(SupportCaseValidationException.class);
		assertThatThrownBy(() -> service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS,
				new ReplaceSystemSettingRequest(json("{\"status\":\"MAINTENANCE\",\"message\":\"점검\"}"), 1L, "  xy  ")))
				.isInstanceOf(SupportCaseValidationException.class);
		assertThatThrownBy(() -> service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS,
				new ReplaceSystemSettingRequest(json("{\"status\":\"MAINTENANCE\",\"message\":\"점검\"}"), 1L, "r".repeat(501))))
				.isInstanceOf(SupportCaseValidationException.class);
	}

	private User user(Long id) throws Exception { User user = new User("admin@example.com", "hash", "admin", null, UserStatus.ACTIVE, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK)); setField(user, "id", id); return user; }
	private SystemSetting setting(SystemSettingKey key, String value, Long id) throws Exception { SystemSetting result = new SystemSetting(key, json(value), 1, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK)); setField(result, "id", id); setField(result, "version", 1L); return result; }
	private List<Map<?, ?>> capturedAuditSnapshots() {
		List<Map<?, ?>> snapshots = new ArrayList<>();
		doAnswer(invocation -> {
			snapshots.add(snapshotArgument(invocation.getArgument(7)));
			snapshots.add(snapshotArgument(invocation.getArgument(8)));
			return null;
		}).when(auditService).recordPlatformMutation(any(), any(), any(), any(), any(), any(), any(), any(), any());
		return snapshots;
	}

	private static Map<?, ?> snapshotArgument(Object value) {
		return Map.copyOf((Map<?, ?>) value);
	}
	private static Stream<RuntimeException> lockContentionExceptions() {
		return Stream.of(
				new LockTimeoutException("lock timeout"),
				new PessimisticLockException("pessimistic lock"),
				new PessimisticLockingFailureException("pessimistic locking failure"));
	}
	private com.fasterxml.jackson.databind.JsonNode json(String value) throws Exception { return objectMapper.readTree(value); }
	private static void setField(Object target, String fieldName, Object value) throws Exception { Field field = target.getClass().getDeclaredField(fieldName); field.setAccessible(true); field.set(target, value); }
}
