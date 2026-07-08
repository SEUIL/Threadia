package com.threadia.backend.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.dto.ProjectCreateData;
import com.threadia.backend.project.dto.ProjectCreateRequest;
import com.threadia.backend.project.dto.ProjectArchiveData;
import com.threadia.backend.project.dto.ProjectArchiveRequest;
import com.threadia.backend.project.dto.ProjectData;
import com.threadia.backend.project.dto.ProjectsData;
import com.threadia.backend.project.dto.ProjectUpdateRequest;
import com.threadia.backend.project.exception.ProjectConflictException;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.exception.ProjectValidationException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-07T00:00:00Z"), ZoneOffset.UTC);

	@Mock
	private AuthContextService authContextService;

	@Mock
	private CompanyMembershipService companyMembershipService;

	@Mock
	private CompanyRepository companyRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private ProjectMemberRepository projectMemberRepository;

	private ProjectService projectService;

	@BeforeEach
	void setUp() {
		projectService = new ProjectService(
				authContextService,
				companyMembershipService,
				companyRepository,
				projectRepository,
				projectMemberRepository,
				FIXED_CLOCK);
	}

	@Test
	void listReturnsAllCompanyProjectsForCompanyAdmin() {
		AuthContext authContext = authContext();
		User admin = user(1L);
		Company company = company(10L, admin);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
		when(projectRepository.findByCompany_IdOrderByIdAsc(10L))
				.thenReturn(List.of(project(20L, company, admin, "Threadia MVP", "THD")));

		ProjectsData data = projectService.list("admin-token", 10L);

		InOrder inOrder = inOrder(authContextService, companyMembershipService, companyRepository, projectRepository);
		inOrder.verify(authContextService).fromAccessToken("admin-token");
		inOrder.verify(authContextService).requireActiveUser(authContext);
		inOrder.verify(companyMembershipService).hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN);
		inOrder.verify(companyRepository).findById(10L);
		inOrder.verify(projectRepository).findByCompany_IdOrderByIdAsc(10L);
		assertThat(data.projects()).extracting("id").containsExactly(20L);
	}

	@Test
	void listReturnsJoinedProjectsOnlyForActiveNonAdminMember() {
		AuthContext authContext = authContext();
		User member = user(1L);
		Company company = company(10L, member);
		when(authContextService.fromAccessToken("member-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(member);
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
		when(projectRepository.findActiveMemberProjectsByCompanyIdAndUserIdOrderByIdAsc(10L, 1L))
				.thenReturn(List.of(project(21L, company, member, "Joined Project", "JND")));

		ProjectsData data = projectService.list("member-token", 10L);

		verify(companyMembershipService).requireActiveMember(10L, authContext);
		verify(projectRepository).findActiveMemberProjectsByCompanyIdAndUserIdOrderByIdAsc(10L, 1L);
		assertThat(data.projects()).extracting("id").containsExactly(21L);
	}

	@Test
	void createCreatesProjectAndCreatorProjectAdminMembershipForCompanyAdmin() {
		AuthContext authContext = authContext();
		User admin = user(1L);
		Company company = company(10L, admin);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
		when(projectRepository.existsByCompany_IdAndProjectKey(10L, "THD")).thenReturn(false);
		when(projectRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
			Project project = invocation.getArgument(0);
			setId(project, 20L);
			return project;
		});
		when(projectMemberRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
			ProjectMember projectMember = invocation.getArgument(0);
			setId(projectMember, 200L);
			return projectMember;
		});

		ProjectCreateData data = projectService.create(
				"admin-token",
				10L,
				new ProjectCreateRequest(
						"Threadia MVP",
						"THD",
						"Threadia MVP 개발 프로젝트",
						ProjectStatus.PLANNING,
						LocalDate.parse("2026-06-17"),
						LocalDate.parse("2026-07-31")));

		verify(companyMembershipService).requireCompanyAdmin(10L, authContext);
		ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
		verify(projectRepository).save(projectCaptor.capture());
		assertThat(projectCaptor.getValue().getCompany()).isSameAs(company);
		assertThat(projectCaptor.getValue().getProjectKey()).isEqualTo("THD");
		ArgumentCaptor<ProjectMember> projectMemberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
		verify(projectMemberRepository).save(projectMemberCaptor.capture());
		assertThat(projectMemberCaptor.getValue().getRole()).isEqualTo(ProjectMemberRole.PROJECT_ADMIN);
		assertThat(projectMemberCaptor.getValue().getStatus()).isEqualTo(ProjectMemberStatus.ACTIVE);
		assertThat(data.project().id()).isEqualTo(20L);
		assertThat(data.projectMember().id()).isEqualTo(200L);
	}

	@Test
	void createRejectsNonCompanyAdmin() {
		AuthContext authContext = authContext();
		User member = user(1L);
		when(authContextService.fromAccessToken("member-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(member);
		org.mockito.Mockito.doThrow(new ForbiddenException("회사 관리자 권한이 필요합니다."))
				.when(companyMembershipService).requireCompanyAdmin(10L, authContext);

		assertThatThrownBy(() -> projectService.create(
				"member-token",
				10L,
				new ProjectCreateRequest("Threadia MVP", "THD", null, null, null, null)))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void createRejectsDuplicateProjectKey() {
		AuthContext authContext = authContext();
		User admin = user(1L);
		Company company = company(10L, admin);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
		when(projectRepository.existsByCompany_IdAndProjectKey(10L, "THD")).thenReturn(true);

		assertThatThrownBy(() -> projectService.create(
				"admin-token",
				10L,
				new ProjectCreateRequest("Threadia MVP", "THD", null, null, null, null)))
				.isInstanceOf(ProjectConflictException.class)
				.hasMessage("이미 사용 중인 프로젝트 키입니다.");
		verify(projectRepository, never()).save(org.mockito.ArgumentMatchers.any());
		verify(projectMemberRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void createRejectsArchivedStatus() {
		AuthContext authContext = authContext();
		User admin = user(1L);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);

		assertThatThrownBy(() -> projectService.create(
				"admin-token",
				10L,
				new ProjectCreateRequest("Threadia MVP", "THD", null, ProjectStatus.ARCHIVED, null, null)))
				.isInstanceOf(ProjectValidationException.class)
				.hasMessage("프로젝트 생성 시 ARCHIVED 상태는 사용할 수 없습니다.");
		verify(projectRepository, never()).save(org.mockito.ArgumentMatchers.any());
		verify(projectMemberRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void createRejectsEndDateBeforeStartDate() {
		AuthContext authContext = authContext();
		User admin = user(1L);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);

		assertThatThrownBy(() -> projectService.create(
				"admin-token",
				10L,
				new ProjectCreateRequest(
						"Threadia MVP",
						"THD",
						null,
						ProjectStatus.PLANNING,
						LocalDate.parse("2026-07-31"),
						LocalDate.parse("2026-06-17"))))
				.isInstanceOf(ProjectValidationException.class)
				.hasMessage("프로젝트 종료일은 시작일보다 빠를 수 없습니다.");
		verify(projectRepository, never()).save(org.mockito.ArgumentMatchers.any());
		verify(projectMemberRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void detailReturnsProjectForCompanyAdmin() {
		AuthContext authContext = authContext();
		User admin = user(1L);
		Company company = company(10L, admin);
		Project project = project(20L, company, admin, "Threadia MVP", "THD");
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		ProjectData data = projectService.detail("admin-token", 20L);

		assertThat(data.project().id()).isEqualTo(20L);
		verify(projectMemberRepository, never()).findByProject_IdAndUser_Id(20L, 1L);
	}

	@Test
	void detailReturnsProjectForActiveProjectMember() {
		AuthContext authContext = authContext();
		User member = user(1L);
		Company company = company(10L, member);
		Project project = project(20L, company, member, "Threadia MVP", "THD");
		when(authContextService.fromAccessToken("member-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(member);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L))
				.thenReturn(Optional.of(projectMember(project, member, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)));

		ProjectData data = projectService.detail("member-token", 20L);

		verify(companyMembershipService).requireActiveMember(10L, authContext);
		assertThat(data.project().id()).isEqualTo(20L);
	}

	@Test
	void detailRejectsCompanyMemberWithoutProjectMembership() {
		AuthContext authContext = authContext();
		User member = user(1L);
		Company company = company(10L, member);
		Project project = project(20L, company, member, "Threadia MVP", "THD");
		when(authContextService.fromAccessToken("member-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(member);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> projectService.detail("member-token", 20L))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("프로젝트 접근 권한이 없습니다.");
	}

	@Test
	void detailRejectsMissingProject() {
		AuthContext authContext = authContext();
		User member = user(1L);
		when(authContextService.fromAccessToken("member-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(member);
		when(projectRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> projectService.detail("member-token", 404L))
				.isInstanceOf(ProjectNotFoundException.class);
	}

	@Test
	void updateUpdatesProjectForProjectAdmin() {
		AuthContext authContext = authContext();
		User projectAdmin = user(1L);
		Company company = company(10L, projectAdmin);
		Project project = project(20L, company, projectAdmin, "Threadia MVP", "THD");
		when(authContextService.fromAccessToken("project-admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(projectAdmin);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L))
				.thenReturn(Optional.of(projectMember(project, projectAdmin, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE)));

		ProjectData data = projectService.update(
				"project-admin-token",
				20L,
				new ProjectUpdateRequest(
						"Threadia MVP Sprint 1",
						"Sprint 1 범위로 조정된 프로젝트",
						ProjectStatus.ACTIVE,
						LocalDate.parse("2026-06-17"),
						LocalDate.parse("2026-07-31")));

		assertThat(data.project().name()).isEqualTo("Threadia MVP Sprint 1");
		assertThat(data.project().projectKey()).isEqualTo("THD");
		assertThat(project.getDescription()).isEqualTo("Sprint 1 범위로 조정된 프로젝트");
		assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
		assertThat(project.getUpdatedAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
	}

	@Test
	void updateUpdatesProjectForCompanyAdmin() {
		AuthContext authContext = authContext();
		User companyAdmin = user(1L);
		Company company = company(10L, companyAdmin);
		Project project = project(20L, company, companyAdmin, "Threadia MVP", "THD");
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(companyAdmin);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		ProjectData data = projectService.update(
				"admin-token",
				20L,
				new ProjectUpdateRequest(
						"Threadia MVP Sprint 1",
						"Sprint 1 범위로 조정된 프로젝트",
						ProjectStatus.ACTIVE,
						null,
						null));

		assertThat(data.project().name()).isEqualTo("Threadia MVP Sprint 1");
		assertThat(project.getDescription()).isEqualTo("Sprint 1 범위로 조정된 프로젝트");
		verify(projectMemberRepository, never()).findByProject_IdAndUser_Id(20L, 1L);
	}

	@Test
	void updateRejectsRegularProjectMember() {
		AuthContext authContext = authContext();
		User member = user(1L);
		Company company = company(10L, member);
		Project project = project(20L, company, member, "Threadia MVP", "THD");
		when(authContextService.fromAccessToken("member-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(member);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L))
				.thenReturn(Optional.of(projectMember(project, member, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)));

		assertThatThrownBy(() -> projectService.update(
				"member-token",
				20L,
				new ProjectUpdateRequest("Threadia MVP", null, ProjectStatus.ACTIVE, null, null)))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("프로젝트 관리자 권한이 필요합니다.");
	}

	@Test
	void updateRejectsArchivedStatus() {
		AuthContext authContext = authContext();
		User companyAdmin = user(1L);
		Company company = company(10L, companyAdmin);
		Project project = project(20L, company, companyAdmin, "Threadia MVP", "THD");
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(companyAdmin);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		assertThatThrownBy(() -> projectService.update(
				"admin-token",
				20L,
				new ProjectUpdateRequest("Threadia MVP", null, ProjectStatus.ARCHIVED, null, null)))
				.isInstanceOf(ProjectValidationException.class)
				.hasMessage("프로젝트 수정 시 ARCHIVED 상태는 사용할 수 없습니다.");
	}

	@Test
	void updateRejectsEndDateBeforeStartDate() {
		AuthContext authContext = authContext();
		User companyAdmin = user(1L);
		Company company = company(10L, companyAdmin);
		Project project = project(20L, company, companyAdmin, "Threadia MVP", "THD");
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(companyAdmin);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		assertThatThrownBy(() -> projectService.update(
				"admin-token",
				20L,
				new ProjectUpdateRequest(
						"Threadia MVP",
						null,
						ProjectStatus.ACTIVE,
						LocalDate.parse("2026-07-31"),
						LocalDate.parse("2026-06-17"))))
				.isInstanceOf(ProjectValidationException.class)
				.hasMessage("프로젝트 종료일은 시작일보다 빠를 수 없습니다.");
	}

	@Test
	void updateRejectsArchivedProject() {
		AuthContext authContext = authContext();
		User companyAdmin = user(1L);
		Company company = company(10L, companyAdmin);
		Project project = project(20L, company, companyAdmin, "Threadia MVP", "THD");
		project.setStatus(ProjectStatus.ARCHIVED);
		project.setArchivedAt(LocalDateTime.now(FIXED_CLOCK));
		project.setArchivedBy(companyAdmin);
		project.setArchiveReason("MVP 종료 후 보관");
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(companyAdmin);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		assertThatThrownBy(() -> projectService.update(
				"admin-token",
				20L,
				new ProjectUpdateRequest("Threadia MVP", null, ProjectStatus.ACTIVE, null, null)))
				.isInstanceOf(ProjectConflictException.class)
				.hasMessage("archive된 프로젝트는 수정할 수 없습니다.");
	}

	@Test
	void archiveArchivesProjectForCompanyAdmin() {
		AuthContext authContext = authContext();
		User companyAdmin = user(1L);
		Company company = company(10L, companyAdmin);
		Project project = project(20L, company, companyAdmin, "Threadia MVP", "THD");
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(companyAdmin);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		ProjectArchiveData data = projectService.archive("admin-token", 20L, new ProjectArchiveRequest("MVP 종료 후 보관"));

		assertThat(data.projectId()).isEqualTo(20L);
		assertThat(data.status()).isEqualTo("ARCHIVED");
		assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
		assertThat(project.getArchivedAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
		assertThat(project.getArchivedBy()).isSameAs(companyAdmin);
		assertThat(project.getArchiveReason()).isEqualTo("MVP 종료 후 보관");
		assertThat(project.getUpdatedAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
	}

	@Test
	void archiveArchivesProjectForProjectAdmin() {
		AuthContext authContext = authContext();
		User projectAdmin = user(1L);
		Company company = company(10L, projectAdmin);
		Project project = project(20L, company, projectAdmin, "Threadia MVP", "THD");
		when(authContextService.fromAccessToken("project-admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(projectAdmin);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L))
				.thenReturn(Optional.of(projectMember(project, projectAdmin, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE)));

		ProjectArchiveData data = projectService.archive("project-admin-token", 20L, new ProjectArchiveRequest("MVP 종료 후 보관"));

		verify(companyMembershipService).requireActiveMember(10L, authContext);
		assertThat(data.status()).isEqualTo("ARCHIVED");
		assertThat(project.getArchivedBy()).isSameAs(projectAdmin);
	}

	@Test
	void archiveRejectsRegularProjectMember() {
		AuthContext authContext = authContext();
		User member = user(1L);
		Company company = company(10L, member);
		Project project = project(20L, company, member, "Threadia MVP", "THD");
		when(authContextService.fromAccessToken("member-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(member);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L))
				.thenReturn(Optional.of(projectMember(project, member, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)));

		assertThatThrownBy(() -> projectService.archive("member-token", 20L, new ProjectArchiveRequest("MVP 종료 후 보관")))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("프로젝트 관리자 권한이 필요합니다.");
	}

	@Test
	void archiveRejectsMissingProject() {
		AuthContext authContext = authContext();
		User companyAdmin = user(1L);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(companyAdmin);
		when(projectRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> projectService.archive("admin-token", 404L, new ProjectArchiveRequest("MVP 종료 후 보관")))
				.isInstanceOf(ProjectNotFoundException.class);
	}

	@Test
	void archiveRejectsAlreadyArchivedProject() {
		AuthContext authContext = authContext();
		User companyAdmin = user(1L);
		Company company = company(10L, companyAdmin);
		Project project = project(20L, company, companyAdmin, "Threadia MVP", "THD");
		project.setStatus(ProjectStatus.ARCHIVED);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(companyAdmin);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, authContext, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		assertThatThrownBy(() -> projectService.archive("admin-token", 20L, new ProjectArchiveRequest("MVP 종료 후 보관")))
				.isInstanceOf(ProjectConflictException.class)
				.hasMessage("이미 archive된 프로젝트입니다.");
	}

	private static AuthContext authContext() {
		return new AuthContext(1L, "admin@example.com");
	}

	private static User user(Long id) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		User user = new User("admin@example.com", "hashed", "admin", null, UserStatus.ACTIVE, now, now);
		setId(user, id);
		return user;
	}

	private static Company company(Long id, User createdBy) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		Company company = new Company("Threadia Inc.", "Admin", "workspace", "IT", null, "ACTIVE", createdBy, now, now);
		setId(company, id);
		return company;
	}

	private static Project project(Long id, Company company, User createdBy, String name, String projectKey) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		Project project = new Project(
				company,
				name,
				projectKey,
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

	private static ProjectMember projectMember(Project project, User user, ProjectMemberRole role, ProjectMemberStatus status) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		return new ProjectMember(project, user, role, status, now, now, now);
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
