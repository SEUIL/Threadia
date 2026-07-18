package com.threadia.backend.platform.company;

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
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.platform.audit.AdminActionAuditRepository;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:company-status-rollback;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
class PlatformCompanyAuditRollbackIntegrationTest {
	@Autowired private PlatformCompanyService service;
	@Autowired private CompanyRepository companyRepository;
	@Autowired private UserRepository userRepository;
	@MockitoBean private PlatformAdminAccessService platformAdminAccessService;
	@MockitoBean private AdminActionAuditRepository auditRepository;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void auditPersistenceFailureRollsBackCompanyStatus() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 16, 0, 0);
		User actor = userRepository.save(new User("admin@example.com", "hash", "admin", null, UserStatus.ACTIVE, now, now));
		Company company = companyRepository.save(new Company("Threadia", "대표", null, "IT", null, CompanyStatus.ACTIVE, actor, now, now));
		PlatformAdmin admin = new PlatformAdmin(actor, PlatformAdminRole.SUPER_ADMIN, now);
		when(platformAdminAccessService.requireAdmin("token")).thenReturn(new PlatformAdminAuthority(actor, admin));
		when(auditRepository.save(any())).thenThrow(new IllegalStateException("audit write failed"));

		assertThatThrownBy(() -> service.updateStatus("token", company.getId(), new CompanyStatusUpdateRequest(CompanyStatus.INACTIVE, "policy violation")))
				.isInstanceOf(IllegalStateException.class).hasMessage("audit write failed");
		assertThat(companyRepository.findById(company.getId())).get().extracting(Company::getStatus).isEqualTo(CompanyStatus.ACTIVE);
	}
}
