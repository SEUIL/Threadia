package com.threadia.backend.platform.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.repository.PlatformAdminRepository;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;
import com.threadia.backend.project.service.ProjectService;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:platform-membership-auth;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
@Transactional
class PlatformMembershipAuthorizationIntegrationTest {

	@Autowired
	private PlatformAdminAccessService platformAdminAccessService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private CompanyMemberRepository companyMemberRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;
	@Autowired private ProjectService projectService;
	@Autowired private PlatformAdminRepository platformAdminRepository;

	@MockitoBean
	private AuthContextService authContextService;

	@MockitoBean
	private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void companyAndProjectAdminMembershipsDoNotGrantPlatformAuthority() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 15, 8, 0);
		User companyAdmin = userRepository.save(user("company-admin@example.com", now));
		User projectAdmin = userRepository.save(user("project-admin@example.com", now));
		Company company = companyRepository.save(new Company("Membership Boundary Co",
				"Company Admin",
				null,
				"IT",
				null, com.threadia.backend.company.domain.CompanyStatus.ACTIVE, companyAdmin,
				now,
				now));
		companyMemberRepository.save(new CompanyMember(
				company,
				companyAdmin,
				null,
				CompanyMemberRole.COMPANY_ADMIN,
				CompanyMemberStatus.ACTIVE,
				now,
				now,
				now));
		companyMemberRepository.save(new CompanyMember(
				company,
				projectAdmin,
				null,
				CompanyMemberRole.MEMBER,
				CompanyMemberStatus.ACTIVE,
				now,
				now,
				now));
		Project project = projectRepository.save(new Project(
				company,
				"Membership Boundary Project",
				"MBP",
				null,
				ProjectStatus.ACTIVE,
				null,
				null,
				companyAdmin,
				null,
				null,
				null,
				now,
				now));
		projectMemberRepository.save(new ProjectMember(
				project,
				projectAdmin,
				ProjectMemberRole.PROJECT_ADMIN,
				ProjectMemberStatus.ACTIVE,
				now,
				now,
				now));
		AuthContext companyContext = new AuthContext(companyAdmin.getId(), companyAdmin.getEmail());
		AuthContext projectContext = new AuthContext(projectAdmin.getId(), projectAdmin.getEmail());
		when(authContextService.fromAccessToken("company-token")).thenReturn(companyContext);
		when(authContextService.requireActiveUser(companyContext)).thenReturn(companyAdmin);
		when(authContextService.fromAccessToken("project-token")).thenReturn(projectContext);
		when(authContextService.requireActiveUser(projectContext)).thenReturn(projectAdmin);

		assertThatThrownBy(() -> platformAdminAccessService.requireAdmin("company-token"))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> platformAdminAccessService.requireAdmin("project-token"))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void inactiveCompanyBlocksProjectDetailDespitePersistedActiveMemberships() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 16, 1, 0);
		User member = userRepository.save(user("member@example.com", now));
		Company company = companyRepository.save(new Company("Inactive Co", "Admin", null, "IT", null, CompanyStatus.INACTIVE, member, now, now));
		companyMemberRepository.save(new CompanyMember(company, member, null, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, now, now, now));
		Project project = projectRepository.save(new Project(company, "Blocked", "BLK", null, ProjectStatus.ACTIVE, null, null, member, null, null, null, now, now));
		projectMemberRepository.save(new ProjectMember(project, member, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE, now, now, now));
		AuthContext context = new AuthContext(member.getId(), member.getEmail());
		when(authContextService.fromAccessToken("member-token")).thenReturn(context);
		when(authContextService.requireActiveUser(context)).thenReturn(member);

		assertThatThrownBy(() -> projectService.detail("member-token", project.getId())).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void platformAdminWithoutWorkspaceMembershipCannotReadProjectDetail() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 16, 4, 0);
		User owner = userRepository.save(user("owner@example.com", now));
		User platformUser = userRepository.save(user("platform@example.com", now));
		platformAdminRepository.save(new PlatformAdmin(platformUser, PlatformAdminRole.PLATFORM_ADMIN, now));
		Company company = companyRepository.save(new Company("Workspace", "Owner", null, "IT", null, CompanyStatus.ACTIVE, owner, now, now));
		Project project = projectRepository.save(new Project(company, "Private", "PVT", null, ProjectStatus.ACTIVE, null, null, owner, null, null, null, now, now));
		AuthContext context = new AuthContext(platformUser.getId(), platformUser.getEmail());
		when(authContextService.fromAccessToken("platform-token")).thenReturn(context);
		when(authContextService.requireActiveUser(context)).thenReturn(platformUser);

		assertThatThrownBy(() -> projectService.detail("platform-token", project.getId())).isInstanceOf(ForbiddenException.class);
	}

	private static User user(String email, LocalDateTime now) {
		return new User(email, "hashed", email, null, UserStatus.ACTIVE, now, now);
	}
}
