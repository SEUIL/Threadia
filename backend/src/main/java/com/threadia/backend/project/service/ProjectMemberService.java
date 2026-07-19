package com.threadia.backend.project.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.dto.ProjectMemberData;
import com.threadia.backend.project.dto.ProjectMemberLockPreview;
import com.threadia.backend.project.dto.ProjectMemberRemoveData;
import com.threadia.backend.project.dto.ProjectMemberResponse;
import com.threadia.backend.project.dto.ProjectMemberUpdateRequest;
import com.threadia.backend.project.dto.ProjectMembersData;
import com.threadia.backend.project.exception.ProjectMemberConflictException;
import com.threadia.backend.project.exception.ProjectMemberNotFoundException;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@Service
public class ProjectMemberService {

	private final AuthContextService authContextService;
	private final CompanyMembershipService companyMembershipService;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final Clock clock;

	@Autowired
	public ProjectMemberService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository) {
		this(authContextService, companyMembershipService, projectRepository, projectMemberRepository, Clock.systemUTC());
	}

	ProjectMemberService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			Clock clock) {
		this.authContextService = authContextService;
		this.companyMembershipService = companyMembershipService;
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public ProjectMembersData list(
			String accessToken,
			Long projectId,
			ProjectMemberRole role,
			ProjectMemberStatus status,
			String q) {
		AuthContext authContext = activeAuthContext(accessToken);
		Project project = project(projectId);
		requireProjectView(project, authContext);
		return new ProjectMembersData(projectMemberRepository.findByProjectIdWithFiltersOrderByIdAsc(
					projectId,
					role,
					status,
					normalizeQ(q))
				.stream()
				.map(ProjectMemberResponse::from)
				.toList());
	}

	@Transactional
	public ProjectMemberData update(String accessToken, Long projectMemberId, ProjectMemberUpdateRequest request) {
		AuthContext authContext = activeAuthContext(accessToken);
		ProjectMemberLockPreview preview = projectMemberLockPreview(projectMemberId);
		boolean targetActiveCompanyMember = lockTargetActiveCompanyMemberIfNeeded(preview, request.role(), request.status());
		List<ProjectMember> lockedProjectMembers = projectMemberWithActiveAdminsForUpdate(preview.projectId(), projectMemberId);
		ProjectMember projectMember = lockedProjectMember(lockedProjectMembers, projectMemberId);
		projectForUpdate(projectMember.getProject().getId());
		requireProjectManage(projectMember.getProject(), authContext);
		validateLastActiveProjectAdmin(projectMember, request.role(), request.status(), lockedProjectMembers);
		validateTargetActiveCompanyMember(request.role(), request.status(), targetActiveCompanyMember);
		projectMember.setRole(request.role());
		projectMember.setStatus(request.status());
		projectMember.setUpdatedAt(now());
		return new ProjectMemberData(ProjectMemberResponse.from(projectMember));
	}

	@Transactional
	public ProjectMemberRemoveData remove(String accessToken, Long projectMemberId) {
		AuthContext authContext = activeAuthContext(accessToken);
		ProjectMemberLockPreview preview = projectMemberLockPreview(projectMemberId);
		List<ProjectMember> lockedProjectMembers = projectMemberWithActiveAdminsForUpdate(preview.projectId(), projectMemberId);
		ProjectMember projectMember = lockedProjectMember(lockedProjectMembers, projectMemberId);
		projectForUpdate(projectMember.getProject().getId());
		requireProjectManage(projectMember.getProject(), authContext);
		validateLastActiveProjectAdmin(projectMember, ProjectMemberRole.MEMBER, ProjectMemberStatus.REMOVED, lockedProjectMembers);
		projectMember.setStatus(ProjectMemberStatus.REMOVED);
		projectMember.setUpdatedAt(now());
		return new ProjectMemberRemoveData(true);
	}

	private static String normalizeQ(String q) {
		if (q == null || q.trim().isEmpty()) {
			return null;
		}
		return q.trim().toLowerCase(Locale.ROOT);
	}

	private void requireProjectView(Project project, AuthContext authContext) {
		Long companyId = project.getCompany().getId();
		if (companyMembershipService.hasActiveRole(companyId, authContext, CompanyMemberRole.COMPANY_ADMIN)) {
			return;
		}
		companyMembershipService.requireActiveMember(companyId, authContext);
		activeProjectMember(project, authContext);
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
		return projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), authContext.userId())
				.filter(projectMember -> projectMember.getStatus() == ProjectMemberStatus.ACTIVE)
				.orElseThrow(() -> new ForbiddenException("프로젝트 접근 권한이 없습니다."));
	}

	private void validateLastActiveProjectAdmin(
			ProjectMember projectMember,
			ProjectMemberRole requestedRole,
			ProjectMemberStatus requestedStatus,
			List<ProjectMember> lockedProjectMembers) {
		if (!isActiveProjectAdmin(projectMember) || isActiveProjectAdmin(requestedRole, requestedStatus)) {
			return;
		}
		long activeAdminCount = lockedProjectMembers.stream()
				.filter(ProjectMemberService::isActiveProjectAdmin)
				.count();
		if (activeAdminCount <= 1) {
			throw ProjectMemberConflictException.lastActiveProjectAdmin();
		}
	}

	private void validateTargetActiveCompanyMember(
			ProjectMemberRole requestedRole,
			ProjectMemberStatus requestedStatus,
			boolean targetActiveCompanyMember) {
		if (!requiresTargetActiveCompanyMember(requestedRole, requestedStatus)) {
			return;
		}
		if (!targetActiveCompanyMember) {
			throw ProjectMemberConflictException.targetUserNotActiveCompanyMember();
		}
	}

	private boolean lockTargetActiveCompanyMemberIfNeeded(
			ProjectMemberLockPreview preview,
			ProjectMemberRole requestedRole,
			ProjectMemberStatus requestedStatus) {
		if (!requiresTargetActiveCompanyMember(requestedRole, requestedStatus)) {
			return true;
		}
		return companyMembershipService.isActiveMemberForUpdate(preview.companyId(), preview.userId());
	}

	private static boolean requiresTargetActiveCompanyMember(
			ProjectMemberRole requestedRole,
			ProjectMemberStatus requestedStatus) {
		return requestedStatus == ProjectMemberStatus.ACTIVE || requestedRole == ProjectMemberRole.PROJECT_ADMIN;
	}

	private static boolean isActiveProjectAdmin(ProjectMember projectMember) {
		return isActiveProjectAdmin(projectMember.getRole(), projectMember.getStatus());
	}

	private static boolean isActiveProjectAdmin(ProjectMemberRole role, ProjectMemberStatus status) {
		return role == ProjectMemberRole.PROJECT_ADMIN && status == ProjectMemberStatus.ACTIVE;
	}

	private AuthContext activeAuthContext(String accessToken) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		return authContext;
	}

	private Project project(Long projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new ProjectNotFoundException(projectId));
	}

	private ProjectMemberLockPreview projectMemberLockPreview(Long projectMemberId) {
		return projectMemberRepository.findLockPreviewById(projectMemberId)
				.orElseThrow(() -> new ProjectMemberNotFoundException(projectMemberId));
	}

	private void projectForUpdate(Long projectId) {
		projectRepository.findByIdForUpdate(projectId)
				.orElseThrow(() -> new ProjectNotFoundException(projectId));
	}

	private List<ProjectMember> projectMemberWithActiveAdminsForUpdate(Long projectId, Long projectMemberId) {
		return projectMemberRepository.findTargetAndActiveProjectAdminsByProjectIdForUpdate(
				projectId,
				projectMemberId,
				ProjectMemberRole.PROJECT_ADMIN,
				ProjectMemberStatus.ACTIVE);
	}

	private static ProjectMember lockedProjectMember(List<ProjectMember> projectMembers, Long projectMemberId) {
		return projectMembers.stream()
				.filter(projectMember -> projectMember.getId().equals(projectMemberId))
				.findFirst()
				.orElseThrow(() -> new ProjectMemberNotFoundException(projectMemberId));
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime();
	}
}
