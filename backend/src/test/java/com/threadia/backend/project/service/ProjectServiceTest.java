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
import com.threadia.backend.project.dto.ProjectsData;
import com.threadia.backend.project.exception.ProjectConflictException;
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
