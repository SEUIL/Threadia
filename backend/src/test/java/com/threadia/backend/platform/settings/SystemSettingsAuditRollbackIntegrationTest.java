package com.threadia.backend.platform.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.platform.audit.AdminActionAuditRepository;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.repository.PlatformAdminRepository;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:system-settings-rollback;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
class SystemSettingsAuditRollbackIntegrationTest {
	@Autowired private SystemSettingsService service;
	@Autowired private SystemSettingRepository settingRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private PlatformAdminRepository platformAdminRepository;
	@Autowired private ObjectMapper objectMapper;
	@MockitoBean private PlatformAdminAccessService platformAdminAccessService;
	@MockitoBean private AdminActionAuditRepository auditRepository;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void auditPersistenceFailureRollsBackSettingReplacement() throws Exception {
		LocalDateTime now = LocalDateTime.of(2026, 7, 16, 0, 0);
		User actor = userRepository.save(new User("admin@example.com", "hash", "admin", null, UserStatus.ACTIVE, now, now));
		PlatformAdmin admin = platformAdminRepository.save(new PlatformAdmin(actor, PlatformAdminRole.SUPER_ADMIN, now));
		SystemSetting setting = settingRepository.save(new SystemSetting(SystemSettingKey.SUPPORT_CENTER_STATUS, objectMapper.readTree("{\"status\":\"OPEN\",\"message\":\"정상\"}"), 1, now, now));
		when(platformAdminAccessService.requireAdmin("token")).thenReturn(new PlatformAdminAuthority(actor, admin));
		when(auditRepository.save(any())).thenThrow(new IllegalStateException("audit write failed"));

		assertThatThrownBy(() -> service.replace("token", SystemSettingKey.SUPPORT_CENTER_STATUS,
				new ReplaceSystemSettingRequest(objectMapper.readTree("{\"status\":\"DELAYED\",\"message\":\"지연\"}"), setting.getVersion(), "운영 상태 변경")))
				.isInstanceOf(IllegalStateException.class).hasMessage("audit write failed");
		assertThat(settingRepository.findById(setting.getId())).get().extracting(SystemSetting::getValue)
				.isEqualTo(objectMapper.readTree("{\"status\":\"OPEN\",\"message\":\"정상\"}"));
	}
}
