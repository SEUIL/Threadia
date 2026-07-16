package com.threadia.backend.supportcase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.platform.audit.AdminActionAuditRepository;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.supportcase.domain.SupportCaseType;
import com.threadia.backend.supportcase.dto.SupportCaseCreateRequest;
import com.threadia.backend.supportcase.repository.SupportCaseRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:support-case-rollback;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
class SupportCaseAuditRollbackIntegrationTest {

	@Autowired
	private SupportCaseService supportCaseService;

	@Autowired
	private SupportCaseRepository supportCaseRepository;

	@Autowired
	private UserRepository userRepository;

	@MockitoBean
	private AuthContextService authContextService;

	@MockitoBean
	private AdminActionAuditRepository auditRepository;

	@MockitoBean
	private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void auditPersistenceFailureRollsBackCaseCreation() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 15, 7, 0);
		User reporter = userRepository.save(new User(
				"rollback@example.com",
				"hashed",
				"reporter",
				null,
				UserStatus.ACTIVE,
				now,
				now));
		AuthContext context = new AuthContext(reporter.getId(), reporter.getEmail());
		when(authContextService.fromAccessToken("token")).thenReturn(context);
		when(authContextService.requireActiveUser(context)).thenReturn(reporter);
		when(auditRepository.save(any())).thenThrow(new IllegalStateException("audit write failed"));
		long beforeCount = supportCaseRepository.count();

		assertThatThrownBy(() -> supportCaseService.create("token", new SupportCaseCreateRequest(
				SupportCaseType.INQUIRY,
				"Rollback test",
				"Case must roll back",
				null,
				null,
				"Verify atomic audit")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("audit write failed");

		assertThat(supportCaseRepository.count()).isEqualTo(beforeCount);
	}
}
