package com.threadia.backend.project.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.exception.CompanyNotFoundException;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.dto.ProjectArchiveData;
import com.threadia.backend.project.dto.ProjectArchiveRequest;
import com.threadia.backend.project.dto.ProjectCreateData;
import com.threadia.backend.project.dto.ProjectCreateRequest;
import com.threadia.backend.project.dto.ProjectData;
import com.threadia.backend.project.dto.ProjectDetailData;
import com.threadia.backend.project.dto.ProjectDashboardData;
import com.threadia.backend.project.dto.ProjectMemberSummaryResponse;
import com.threadia.backend.project.dto.ProjectResponse;
import com.threadia.backend.project.dto.ProjectUpdateRequest;
import com.threadia.backend.project.dto.ProjectViewerData;
import com.threadia.backend.project.dto.ProjectsData;
import com.threadia.backend.project.exception.ProjectConflictException;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.exception.ProjectValidationException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectDashboardRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@Service
public class ProjectService {

	private final AuthContextService authContextService;
	private final CompanyMembershipService companyMembershipService;
	private final CompanyRepository companyRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectDashboardRepository projectDashboardRepository;
	private final Clock clock;

	@Autowired
	public ProjectService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			CompanyRepository companyRepository,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			ProjectDashboardRepository projectDashboardRepository) {
		this(authContextService, companyMembershipService, companyRepository, projectRepository, projectMemberRepository, projectDashboardRepository, Clock.systemUTC());
	}

	ProjectService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			CompanyRepository companyRepository,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			ProjectDashboardRepository projectDashboardRepository,
			Clock clock) {
		this.authContextService = authContextService;
		this.companyMembershipService = companyMembershipService;
		this.companyRepository = companyRepository;
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.projectDashboardRepository = projectDashboardRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public ProjectsData list(String accessToken, Long companyId) {
		AuthContext authContext = activeAuthContext(accessToken);
		company(companyId);
		boolean companyAdmin = companyMembershipService.hasActiveRole(companyId, authContext, CompanyMemberRole.COMPANY_ADMIN);
		if (!companyAdmin) {
			companyMembershipService.requireActiveMember(companyId, authContext);
		}
		return new ProjectsData((companyAdmin
				? projectRepository.findByCompany_IdOrderByIdAsc(companyId)
				: projectRepository.findActiveMemberProjectsByCompanyIdAndUserIdOrderByIdAsc(companyId, authContext.userId()))
				.stream()
				.map(ProjectResponse::from)
				.toList());
	}

	@Transactional
	public ProjectCreateData create(String accessToken, Long companyId, ProjectCreateRequest request) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		User creator = authContextService.requireActiveUser(authContext);
		Company company = companyForUpdate(companyId);
		companyMembershipService.requireCompanyAdminForUpdate(companyId, authContext);
		validateCreateRequest(request);
		String projectKey = request.projectKey().trim();
		if (projectRepository.existsByCompany_IdAndProjectKey(companyId, projectKey)) {
			throw ProjectConflictException.duplicateProjectKey();
		}
		LocalDateTime now = now();
		Project project = projectRepository.save(new Project(
				company,
				request.name().trim(),
				projectKey,
				request.description(),
				request.status() == null ? ProjectStatus.PLANNING : request.status(),
				request.startDate(),
				request.endDate(),
				creator,
				null,
				null,
				null,
				now,
				now));
		ProjectMember projectMember = projectMemberRepository.save(new ProjectMember(
				project,
				creator,
				ProjectMemberRole.PROJECT_ADMIN,
				ProjectMemberStatus.ACTIVE,
				now,
				now,
				now));
		return new ProjectCreateData(ProjectResponse.from(project), ProjectMemberSummaryResponse.from(projectMember));
	}

	@Transactional(readOnly = true)
	public ProjectDetailData detail(String accessToken, Long projectId) {
		AuthContext authContext = activeAuthContext(accessToken);
		Project project = project(projectId);
		ProjectViewerData viewer = projectViewer(project, authContext);
		return new ProjectDetailData(ProjectResponse.from(project), viewer);
	}

	@Transactional(readOnly = true)
	public ProjectDashboardData dashboard(String accessToken, Long projectId) {
		AuthContext authContext = activeAuthContext(accessToken);
		Project project = project(projectId);
		projectViewer(project, authContext);
		return projectDashboardRepository.load(projectId);
	}

	@Transactional
	public ProjectData update(String accessToken, Long projectId, ProjectUpdateRequest request) {
		AuthContext authContext = activeAuthContext(accessToken);
		Project project = project(projectId);
		requireProjectManage(project, authContext);
		if (project.getStatus() == ProjectStatus.ARCHIVED) {
			throw ProjectConflictException.archivedProjectCannotBeUpdated();
		}
		validateUpdateRequest(request);
		LocalDateTime now = now();
		project.setName(request.name().trim());
		project.setDescription(request.description());
		project.setStatus(request.status());
		project.setStartDate(request.startDate());
		project.setEndDate(request.endDate());
		project.setUpdatedAt(now);
		return new ProjectData(ProjectResponse.from(project));
	}

	@Transactional
	public ProjectArchiveData archive(String accessToken, Long projectId, ProjectArchiveRequest request) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		User actor = authContextService.requireActiveUser(authContext);
		Project project = project(projectId);
		requireProjectManage(project, authContext);
		if (project.getStatus() == ProjectStatus.ARCHIVED) {
			throw ProjectConflictException.alreadyArchived();
		}
		LocalDateTime now = now();
		project.setStatus(ProjectStatus.ARCHIVED);
		project.setArchivedAt(now);
		project.setArchivedBy(actor);
		project.setArchiveReason(request.reason());
		project.setUpdatedAt(now);
		return new ProjectArchiveData(project.getId(), project.getStatus().name());
	}

	private void validateCreateRequest(ProjectCreateRequest request) {
		if (request.status() == ProjectStatus.ARCHIVED) {
			throw ProjectValidationException.archivedStatusNotAllowed();
		}
		if (request.startDate() != null && request.endDate() != null && request.startDate().isAfter(request.endDate())) {
			throw ProjectValidationException.invalidDateRange();
		}
	}

	private void validateUpdateRequest(ProjectUpdateRequest request) {
		if (request.status() == ProjectStatus.ARCHIVED) {
			throw ProjectValidationException.archivedUpdateStatusNotAllowed();
		}
		if (request.startDate() != null && request.endDate() != null && request.startDate().isAfter(request.endDate())) {
			throw ProjectValidationException.invalidDateRange();
		}
	}

	private ProjectViewerData projectViewer(Project project, AuthContext authContext) {
		Long companyId = project.getCompany().getId();
		CompanyMember companyMember = activeCompanyMember(companyId, authContext);
		java.util.Optional<ProjectMember> projectMember = activeProjectMemberOptional(project, authContext);
		if (companyMember.getRole() != CompanyMemberRole.COMPANY_ADMIN && projectMember.isEmpty()) {
			throw new ForbiddenException("프로젝트 접근 권한이 없습니다.");
		}
		return ProjectViewerData.from(
				companyMember.getRole(),
				projectMember.map(ProjectMember::getRole).orElse(null));
	}

	private void requireProjectManage(Project project, AuthContext authContext) {
		Long companyId = project.getCompany().getId();
		if (companyMembershipService.hasActiveRole(companyId, authContext, CompanyMemberRole.COMPANY_ADMIN)) {
			return;
		}
		companyMembershipService.requireActiveMember(companyId, authContext);
		ProjectMember projectMember = activeProjectMember(project, authContext);
		if (projectMember.getRole() != ProjectMemberRole.PROJECT_ADMIN) {
			throw new ForbiddenException("프로젝트 관리자 권한이 필요합니다.");
		}
	}

	private ProjectMember activeProjectMember(Project project, AuthContext authContext) {
		return activeProjectMemberOptional(project, authContext)
				.orElseThrow(() -> new ForbiddenException("프로젝트 접근 권한이 없습니다."));
	}

	private java.util.Optional<ProjectMember> activeProjectMemberOptional(Project project, AuthContext authContext) {
		return projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), authContext.userId())
				.filter(projectMember -> projectMember.getStatus() == ProjectMemberStatus.ACTIVE);
	}

	private CompanyMember activeCompanyMember(Long companyId, AuthContext authContext) {
		return companyMembershipService.findMembership(companyId, authContext)
				.filter(companyMember -> companyMember.getStatus() == CompanyMemberStatus.ACTIVE)
				.orElseThrow(() -> new ForbiddenException("회사 접근 권한이 없습니다."));
	}

	private AuthContext activeAuthContext(String accessToken) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		return authContext;
	}

	private Company company(Long companyId) {
		return companyRepository.findById(companyId)
				.orElseThrow(() -> new CompanyNotFoundException(companyId));
	}

	private Company companyForUpdate(Long companyId) {
		return companyRepository.findByIdForUpdate(companyId)
				.orElseThrow(() -> new CompanyNotFoundException(companyId));
	}

	private Project project(Long projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new ProjectNotFoundException(projectId));
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime();
	}
}
