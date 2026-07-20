package com.threadia.backend.platform.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.platform.audit.AdminAction;
import com.threadia.backend.platform.audit.AdminActionAuditRepository;
import com.threadia.backend.platform.audit.AuditTargetType;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.repository.PlatformAdminRepository;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:company-lifecycle-persist;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop", "threadia.jwt.secret=01234567890123456789012345678901"})
@Transactional
class PlatformCompanyLifecyclePersistenceIntegrationTest {
	@Autowired private PlatformCompanyService service;
	@Autowired private CompanyRepository companyRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private PlatformAdminRepository platformAdminRepository;
	@Autowired private AdminActionAuditRepository auditRepository;
	@MockitoBean private PlatformAdminAccessService platformAdminAccessService;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void persistsCompanyLifecycleAuditWithoutSupportCase() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 16, 2, 0);
		User actor = userRepository.save(new User("admin@example.com", "hash", "admin", null, UserStatus.ACTIVE, now, now));
		PlatformAdmin admin = platformAdminRepository.save(new PlatformAdmin(actor, PlatformAdminRole.SUPER_ADMIN, now));
		Company company = companyRepository.save(new Company("Threadia", "대표", null, "IT", null, CompanyStatus.ACTIVE, actor, now, now));
		when(platformAdminAccessService.requireAdmin("token")).thenReturn(new PlatformAdminAuthority(actor, admin));

		service.updateStatus("token", company.getId(), new CompanyStatusUpdateRequest(CompanyStatus.INACTIVE, "  policy violation  "));

		assertThat(companyRepository.findById(company.getId())).get().extracting(Company::getStatus).isEqualTo(CompanyStatus.INACTIVE);
		assertThat(auditRepository.findAll()).singleElement().satisfies(audit -> {
			assertThat(audit.getSupportCase()).isNull(); assertThat(audit.getAction()).isEqualTo(AdminAction.COMPANY_STATUS_CHANGED); assertThat(audit.getTargetType()).isEqualTo(AuditTargetType.COMPANY); assertThat(audit.getTargetId()).isEqualTo(company.getId()); assertThat(audit.getActor().getId()).isEqualTo(actor.getId()); assertThat(audit.getActorPlatformAdmin().getRole()).isEqualTo(PlatformAdminRole.SUPER_ADMIN); assertThat(audit.getReason()).isEqualTo("policy violation"); assertThat(audit.getBeforeState()).contains("ACTIVE"); assertThat(audit.getAfterState()).contains("INACTIVE");
		});
	}

	@Test
	void findsLiteralWildcardQueryForRequestedStatusWithDescendingIdCursorBoundary() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 16, 2, 0);
		User creator = userRepository.save(new User("repository-admin@example.com", "hash", "admin", null, UserStatus.ACTIVE, now, now));
		Company activeOlder = companyRepository.save(new Company("literal %_\\ older", "대표", null, "IT", null, CompanyStatus.ACTIVE, creator, now, now));
		Company activeNewer = companyRepository.save(new Company("literal %_\\ newer", "대표", null, "IT", null, CompanyStatus.ACTIVE, creator, now, now));
		companyRepository.save(new Company("literal %_\\ inactive", "대표", null, "IT", null, CompanyStatus.INACTIVE, creator, now, now));
		companyRepository.save(new Company("literal wildcard only", "대표", null, "IT", null, CompanyStatus.ACTIVE, creator, now, now));

		assertThat(companyRepository.findPlatformCompanyPage(CompanyStatus.ACTIVE, "\\%\\_\\\\", null, PageRequest.of(0, 10)))
				.extracting(Company::getId).containsExactly(activeNewer.getId(), activeOlder.getId());
		assertThat(companyRepository.findPlatformCompanyPage(CompanyStatus.ACTIVE, "\\%\\_\\\\", activeNewer.getId(), PageRequest.of(0, 10)))
				.extracting(Company::getId).containsExactly(activeOlder.getId());
	}
}
