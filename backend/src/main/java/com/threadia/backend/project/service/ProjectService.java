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
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.exception.CompanyNotFoundException;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.dto.ProjectCreateData;
import com.threadia.backend.project.dto.ProjectCreateRequest;
import com.threadia.backend.project.dto.ProjectMemberSummaryResponse;
import com.threadia.backend.project.dto.ProjectResponse;
import com.threadia.backend.project.dto.ProjectsData;
import com.threadia.backend.project.exception.ProjectConflictException;
import com.threadia.backend.project.exception.ProjectValidationException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@Service
public class ProjectService {

	private final AuthContextService authContextService;
	private final CompanyMembershipService companyMembershipService;
	private final CompanyRepository companyRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final Clock clock;

	@Autowired
	public ProjectService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			CompanyRepository companyRepository,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository) {
		this(authContextService, companyMembershipService, companyRepository, projectRepository, projectMemberRepository, Clock.systemUTC());
	}

	ProjectService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			CompanyRepository companyRepository,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			Clock clock) {
		this.authContextService = authContextService;
		this.companyMembershipService = companyMembershipService;
		this.companyRepository = companyRepository;
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public ProjectsData list(String accessToken, Long companyId) {
		AuthContext authContext = activeAuthContext(accessToken);
		boolean companyAdmin = companyMembershipService.hasActiveRole(companyId, authContext, CompanyMemberRole.COMPANY_ADMIN);
		if (!companyAdmin) {
			companyMembershipService.requireActiveMember(companyId, authContext);
		}
		company(companyId);
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
		companyMembershipService.requireCompanyAdmin(companyId, authContext);
		validateCreateRequest(request);
		Company company = company(companyId);
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

	private void validateCreateRequest(ProjectCreateRequest request) {
		if (request.status() == ProjectStatus.ARCHIVED) {
			throw ProjectValidationException.archivedStatusNotAllowed();
		}
		if (request.startDate() != null && request.endDate() != null && request.startDate().isAfter(request.endDate())) {
			throw ProjectValidationException.invalidDateRange();
		}
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

	private LocalDateTime now() {
		return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime();
	}
}
