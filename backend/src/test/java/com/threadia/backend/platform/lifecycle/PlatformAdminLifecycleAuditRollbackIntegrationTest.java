package com.threadia.backend.platform.lifecycle;

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
import com.threadia.backend.platform.audit.AdminActionAuditRepository;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminBootstrapLock;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.domain.PlatformAdminStatus;
import com.threadia.backend.platform.repository.PlatformAdminBootstrapLockRepository;
import com.threadia.backend.platform.repository.PlatformAdminRepository;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:platform-admin-lifecycle-rollback;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901",
		"threadia.audit.cursor-hmac-secret=01234567890123456789012345678901"
})
class PlatformAdminLifecycleAuditRollbackIntegrationTest {
	@Autowired private PlatformAdminLifecycleService lifecycleService;
	@Autowired private UserRepository userRepository;
	@Autowired private PlatformAdminRepository adminRepository;
	@Autowired private PlatformAdminBootstrapLockRepository lockRepository;
	@MockitoBean private PlatformAdminAccessService accessService;
	@MockitoBean private AdminActionAuditRepository auditRepository;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void auditPersistenceFailureRollsBackSoftRevoke() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 18, 0, 0);
		lockRepository.save(new PlatformAdminBootstrapLock(1));
		User actor = userRepository.save(new User("admin@example.com", "hash", "admin", null, UserStatus.ACTIVE, now, now));
		PlatformAdmin admin = adminRepository.save(new PlatformAdmin(actor, PlatformAdminRole.SUPER_ADMIN, now));
		User targetUser = userRepository.save(new User("target@example.com", "hash", "target", null, UserStatus.ACTIVE, now, now));
		PlatformAdmin target = adminRepository.save(new PlatformAdmin(targetUser, PlatformAdminRole.SUPER_ADMIN, now));
		when(accessService.requireSuperAdminFresh("token")).thenReturn(new PlatformAdminAuthority(actor, admin));
		when(auditRepository.save(any())).thenThrow(new IllegalStateException("audit write failed"));

		assertThatThrownBy(() -> lifecycleService.revoke("token", target.getId(), new PlatformAdminLifecycleRequest("권한 회수")))
				.isInstanceOf(IllegalStateException.class).hasMessage("audit write failed");
		assertThat(adminRepository.findById(target.getId())).get().extracting(PlatformAdmin::getStatus).isEqualTo(PlatformAdminStatus.ACTIVE);
	}
}
