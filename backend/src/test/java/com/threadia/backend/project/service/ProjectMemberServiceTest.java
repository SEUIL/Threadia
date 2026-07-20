package com.threadia.backend.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.dto.ProjectMemberData;
import com.threadia.backend.project.dto.ProjectMemberLockPreview;
import com.threadia.backend.project.dto.ProjectMemberRemoveData;
import com.threadia.backend.project.dto.ProjectMemberUpdateRequest;
import com.threadia.backend.project.dto.ProjectMembersData;
import com.threadia.backend.project.exception.ProjectMemberConflictException;
import com.threadia.backend.project.exception.ProjectMemberNotFoundException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-07T00:00:00Z"), ZoneOffset.UTC);

	@Mock
	private AuthContextService authContextService;

	@Mock
	private CompanyMembershipService companyMembershipService;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private ProjectMemberRepository projectMemberRepository;

	private ProjectMemberService projectMemberService;

	@BeforeEach
	void setUp() {
		projectMemberService = new ProjectMemberService(
				authContextService,
				companyMembershipService,
				projectRepository,
				projectMemberRepository,
				FIXED_CLOCK);
	}

	@Test
	void listReturnsMembersForCompanyAdminWithFiltersAndNormalizedQ() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		Project project = project(20L, company, admin);
		ProjectMember member = projectMember(200L, project, user(2L, "member@example.com"), ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
		when(projectMemberRepository.findByProjectIdWithFiltersOrderByIdAsc(
				20L,
				ProjectMemberRole.MEMBER,
				ProjectMemberStatus.ACTIVE,
				"member@example"))
				.thenReturn(List.of(member));
		when(companyMembershipService.findActiveRolesByCompanyIdAndUserIds(10L, List.of(2L)))
				.thenReturn(Map.of(2L, CompanyMemberRole.COMPANY_ADMIN));

		ProjectMembersData data = projectMemberService.list(
				"admin-token",
				20L,
				ProjectMemberRole.MEMBER,
				ProjectMemberStatus.ACTIVE,
				"  Member@Example  ");

		assertThat(data.projectMembers()).extracting("id").containsExactly(200L);
		assertThat(data.projectMembers().get(0).companyRole()).isEqualTo("COMPANY_ADMIN");
		assertThat(data.projectMembers().get(0).user().email()).isEqualTo("member@example.com");
		verify(projectMemberRepository, never()).findByProject_IdAndUser_Id(20L, 1L);
	}

	@Test
	void listAllowsActiveProjectMemberWhenRequesterIsNotCompanyAdmin() {
		AuthContext authContext = authContext();
		User user = user(1L, "member@example.com");
		Company company = company(10L, user);
		Project project = project(20L, company, user);
		ProjectMember requester = projectMember(200L, project, user, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("member-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(user);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.of(requester));
		when(projectMemberRepository.findByProjectIdWithFiltersOrderByIdAsc(20L, null, null, null))
				.thenReturn(List.of(requester));
		when(companyMembershipService.findActiveRolesByCompanyIdAndUserIds(10L, List.of(1L)))
				.thenReturn(Map.of(1L, CompanyMemberRole.MEMBER));

		ProjectMembersData data = projectMemberService.list("member-token", 20L, null, null, "   ");

		verify(companyMembershipService).requireActiveMember(10L, authContext);
		assertThat(data.projectMembers()).hasSize(1);
		assertThat(data.projectMembers().get(0).companyRole()).isEqualTo("MEMBER");
	}

	@Test
	void listRejectsCompanyMemberWithoutActiveProjectMembership() {
		AuthContext authContext = authContext();
		User user = user(1L, "member@example.com");
		Company company = company(10L, user);
		Project project = project(20L, company, user);
		when(authContextService.fromAccessToken("member-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(user);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> projectMemberService.list("member-token", 20L, null, null, null))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("프로젝트 접근 권한이 없습니다.");
	}

	@Test
	void updateAllowsProjectAdminAndUpdatesRoleStatusAndUpdatedAt() {
		AuthContext authContext = authContext();
		User projectAdmin = user(1L, "admin@example.com");
		User targetUser = user(2L, "member@example.com");
		Company company = company(10L, projectAdmin);
		Project project = project(20L, company, projectAdmin);
		ProjectMember requester = projectMember(200L, project, projectAdmin, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE);
		ProjectMember target = projectMember(201L, project, targetUser, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("project-admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(projectAdmin);
		when(projectMemberRepository.findLockPreviewById(201L)).thenReturn(Optional.of(projectMemberLockPreview(target)));
		when(projectMemberRepository.findTargetAndActiveProjectAdminsByProjectIdForUpdate(
				20L,
				201L,
				ProjectMemberRole.PROJECT_ADMIN,
				ProjectMemberStatus.ACTIVE))
				.thenReturn(List.of(requester, target));
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.of(requester));
		when(companyMembershipService.isActiveMemberForUpdate(10L, 2L)).thenReturn(true);
		when(companyMembershipService.findActiveRolesByCompanyIdAndUserIds(10L, List.of(2L)))
				.thenReturn(Map.of(2L, CompanyMemberRole.MEMBER));

		ProjectMemberData data = projectMemberService.update(
				"project-admin-token",
				201L,
				new ProjectMemberUpdateRequest(ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE));

		verify(companyMembershipService).requireActiveMember(10L, authContext);
		assertThat(target.getRole()).isEqualTo(ProjectMemberRole.PROJECT_ADMIN);
		assertThat(target.getStatus()).isEqualTo(ProjectMemberStatus.ACTIVE);
		assertThat(target.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 7, 0, 0));
		assertThat(data.projectMember().role()).isEqualTo("PROJECT_ADMIN");
		assertThat(data.projectMember().companyRole()).isEqualTo("MEMBER");
	}

	@Test
	void updateAllowsCompanyAdminWithoutProjectMemberLookup() {
		AuthContext authContext = authContext();
		User companyAdmin = user(1L, "admin@example.com");
		Company company = company(10L, companyAdmin);
		Project project = project(20L, company, companyAdmin);
		ProjectMember target = projectMember(201L, project, user(2L, "member@example.com"), ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(companyAdmin);
		when(projectMemberRepository.findLockPreviewById(201L)).thenReturn(Optional.of(projectMemberLockPreview(target)));
		when(projectMemberRepository.findTargetAndActiveProjectAdminsByProjectIdForUpdate(
				20L,
				201L,
				ProjectMemberRole.PROJECT_ADMIN,
				ProjectMemberStatus.ACTIVE))
				.thenReturn(List.of(target));
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
		when(companyMembershipService.isActiveMemberForUpdate(10L, 2L)).thenReturn(true);
		when(companyMembershipService.findActiveRolesByCompanyIdAndUserIds(10L, List.of(2L)))
				.thenReturn(Map.of(2L, CompanyMemberRole.COMPANY_ADMIN));

		ProjectMemberData data = projectMemberService.update(
				"admin-token",
				201L,
				new ProjectMemberUpdateRequest(ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE));

		assertThat(data.projectMember().companyRole()).isEqualTo("COMPANY_ADMIN");
		verify(projectMemberRepository, never()).findByProject_IdAndUser_Id(20L, 1L);
	}

	@Test
	void updateThrowsConflictWhenActivatingTargetWithoutActiveCompanyMembership() {
		AuthContext authContext = authContext();
		User companyAdmin = user(1L, "admin@example.com");
		User targetUser = user(2L, "member@example.com");
		Company company = company(10L, companyAdmin);
		Project project = project(20L, company, companyAdmin);
		ProjectMember target = projectMember(201L, project, targetUser, ProjectMemberRole.MEMBER, ProjectMemberStatus.REMOVED);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(companyAdmin);
		when(projectMemberRepository.findLockPreviewById(201L)).thenReturn(Optional.of(projectMemberLockPreview(target)));
		when(projectMemberRepository.findTargetAndActiveProjectAdminsByProjectIdForUpdate(
				20L,
				201L,
				ProjectMemberRole.PROJECT_ADMIN,
				ProjectMemberStatus.ACTIVE))
				.thenReturn(List.of(target));
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
		when(companyMembershipService.isActiveMemberForUpdate(10L, 2L)).thenReturn(false);

		assertThatThrownBy(() -> projectMemberService.update(
				"admin-token",
				201L,
				new ProjectMemberUpdateRequest(ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)))
				.isInstanceOf(ProjectMemberConflictException.class)
				.hasMessage("대상 사용자가 활성 회사 구성원이 아닙니다.");
		assertThat(target.getStatus()).isEqualTo(ProjectMemberStatus.REMOVED);
	}

	@Test
	void updateThrowsConflictWhenPromotingTargetWithoutActiveCompanyMembership() {
		AuthContext authContext = authContext();
		User companyAdmin = user(1L, "admin@example.com");
		User targetUser = user(2L, "member@example.com");
		Company company = company(10L, companyAdmin);
		Project project = project(20L, company, companyAdmin);
		ProjectMember target = projectMember(201L, project, targetUser, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(companyAdmin);
		when(projectMemberRepository.findLockPreviewById(201L)).thenReturn(Optional.of(projectMemberLockPreview(target)));
		when(projectMemberRepository.findTargetAndActiveProjectAdminsByProjectIdForUpdate(
				20L,
				201L,
				ProjectMemberRole.PROJECT_ADMIN,
				ProjectMemberStatus.ACTIVE))
				.thenReturn(List.of(target));
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
		when(companyMembershipService.isActiveMemberForUpdate(10L, 2L)).thenReturn(false);

		assertThatThrownBy(() -> projectMemberService.update(
				"admin-token",
				201L,
				new ProjectMemberUpdateRequest(ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE)))
				.isInstanceOf(ProjectMemberConflictException.class)
				.hasMessage("대상 사용자가 활성 회사 구성원이 아닙니다.");
		assertThat(target.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
	}

	@Test
	void updateThrowsNotFoundWhenProjectMemberDoesNotExist() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(projectMemberRepository.findLockPreviewById(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> projectMemberService.update(
				"admin-token",
				999L,
				new ProjectMemberUpdateRequest(ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)))
				.isInstanceOf(ProjectMemberNotFoundException.class);
	}

	@Test
	void updateThrowsConflictWhenDemotingLastActiveProjectAdmin() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		Project project = project(20L, company, admin);
		ProjectMember target = projectMember(201L, project, admin, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(projectMemberRepository.findLockPreviewById(201L)).thenReturn(Optional.of(projectMemberLockPreview(target)));
		when(companyMembershipService.isActiveMemberForUpdate(10L, 1L)).thenReturn(true);
		when(projectMemberRepository.findTargetAndActiveProjectAdminsByProjectIdForUpdate(
				20L,
				201L,
				ProjectMemberRole.PROJECT_ADMIN,
				ProjectMemberStatus.ACTIVE))
				.thenReturn(List.of(target));
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		assertThatThrownBy(() -> projectMemberService.update(
				"admin-token",
				201L,
				new ProjectMemberUpdateRequest(ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)))
				.isInstanceOf(ProjectMemberConflictException.class);
		assertThat(target.getRole()).isEqualTo(ProjectMemberRole.PROJECT_ADMIN);
		assertThat(target.getStatus()).isEqualTo(ProjectMemberStatus.ACTIVE);
	}

	@Test
	void updateAllowsAdminChangeWhenAnotherActiveProjectAdminExists() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		Project project = project(20L, company, admin);
		ProjectMember target = projectMember(201L, project, admin, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE);
		ProjectMember otherAdmin = projectMember(202L, project, user(2L, "other@example.com"), ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(projectMemberRepository.findLockPreviewById(201L)).thenReturn(Optional.of(projectMemberLockPreview(target)));
		when(projectMemberRepository.findTargetAndActiveProjectAdminsByProjectIdForUpdate(
				20L,
				201L,
				ProjectMemberRole.PROJECT_ADMIN,
				ProjectMemberStatus.ACTIVE))
				.thenReturn(List.of(target, otherAdmin));
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
		when(companyMembershipService.findActiveRolesByCompanyIdAndUserIds(10L, List.of(1L)))
				.thenReturn(Map.of(1L, CompanyMemberRole.COMPANY_ADMIN));

		ProjectMemberData data = projectMemberService.update(
				"admin-token",
				201L,
				new ProjectMemberUpdateRequest(ProjectMemberRole.MEMBER, ProjectMemberStatus.REMOVED));

		assertThat(target.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
		assertThat(target.getStatus()).isEqualTo(ProjectMemberStatus.REMOVED);
		assertThat(data.projectMember().status()).isEqualTo("REMOVED");
		assertThat(data.projectMember().companyRole()).isEqualTo("COMPANY_ADMIN");
	}

	@Test
	void removeSoftRemovesProjectMember() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		Project project = project(20L, company, admin);
		ProjectMember target = projectMember(201L, project, user(2L, "member@example.com"), ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(projectMemberRepository.findLockPreviewById(201L)).thenReturn(Optional.of(projectMemberLockPreview(target)));
		when(projectMemberRepository.findTargetAndActiveProjectAdminsByProjectIdForUpdate(
				20L,
				201L,
				ProjectMemberRole.PROJECT_ADMIN,
				ProjectMemberStatus.ACTIVE))
				.thenReturn(List.of(target));
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		ProjectMemberRemoveData data = projectMemberService.remove("admin-token", 201L);

		assertThat(data.removed()).isTrue();
		assertThat(target.getStatus()).isEqualTo(ProjectMemberStatus.REMOVED);
		assertThat(target.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 7, 0, 0));
	}

	@Test
	void removeThrowsConflictWhenRemovingLastActiveProjectAdmin() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		Project project = project(20L, company, admin);
		ProjectMember target = projectMember(201L, project, admin, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(projectMemberRepository.findLockPreviewById(201L)).thenReturn(Optional.of(projectMemberLockPreview(target)));
		when(projectMemberRepository.findTargetAndActiveProjectAdminsByProjectIdForUpdate(
				20L,
				201L,
				ProjectMemberRole.PROJECT_ADMIN,
				ProjectMemberStatus.ACTIVE))
				.thenReturn(List.of(target));
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		assertThatThrownBy(() -> projectMemberService.remove("admin-token", 201L))
				.isInstanceOf(ProjectMemberConflictException.class);
		assertThat(target.getStatus()).isEqualTo(ProjectMemberStatus.ACTIVE);
	}

	private static AuthContext authContext() {
		return new AuthContext(1L, "admin@example.com");
	}

	private static User user(Long id, String email) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		User user = new User(email, "hashed", "user" + id, null, UserStatus.ACTIVE, now, now);
		setId(user, id);
		return user;
	}

	private static Company company(Long id, User createdBy) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		Company company = new Company("Threadia Inc.", "Admin", "workspace", "IT", null, com.threadia.backend.company.domain.CompanyStatus.ACTIVE, createdBy, now, now);
		setId(company, id);
		return company;
	}

	private static Project project(Long id, Company company, User createdBy) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		Project project = new Project(
				company,
				"Threadia MVP",
				"THD",
				"Threadia MVP 개발 프로젝트",
				ProjectStatus.PLANNING,
				LocalDate.parse("2026-06-17"),
				LocalDate.parse("2026-07-31"),
				createdBy,
				null,
				null,
				null,
				now,
				now);
		setId(project, id);
		return project;
	}

	private static ProjectMember projectMember(
			Long id,
			Project project,
			User user,
			ProjectMemberRole role,
			ProjectMemberStatus status) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK).minusDays(1);
		ProjectMember projectMember = new ProjectMember(project, user, role, status, now, now, now);
		setId(projectMember, id);
		return projectMember;
	}

	private static ProjectMemberLockPreview projectMemberLockPreview(ProjectMember projectMember) {
		return new ProjectMemberLockPreview(
				projectMember.getProject().getId(),
				projectMember.getProject().getCompany().getId(),
				projectMember.getUser().getId());
	}

	private static void setId(Object entity, Long id) {
		try {
			Field field = entity.getClass().getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
