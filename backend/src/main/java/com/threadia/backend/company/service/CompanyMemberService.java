package com.threadia.backend.company.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.dto.CompanyMemberData;
import com.threadia.backend.company.dto.CompanyMemberRemoveData;
import com.threadia.backend.company.dto.CompanyMemberResponse;
import com.threadia.backend.company.dto.CompanyMemberUpdateRequest;
import com.threadia.backend.company.dto.CompanyMembersData;
import com.threadia.backend.company.exception.CompanyMemberConflictException;
import com.threadia.backend.company.exception.CompanyMemberNotFoundException;
import com.threadia.backend.company.exception.CompanyNotFoundException;
import com.threadia.backend.company.exception.DepartmentNotFoundException;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.repository.DepartmentRepository;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.exception.ProjectMemberConflictException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@Service
public class CompanyMemberService {

	private final AuthContextService authContextService;
	private final CompanyAccessService companyAccessService;
	private final CompanyRepository companyRepository;
	private final CompanyMemberRepository companyMemberRepository;
	private final DepartmentRepository departmentRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectRepository projectRepository;
	private final Clock clock;

	@Autowired
	public CompanyMemberService(
			AuthContextService authContextService,
			CompanyAccessService companyAccessService,
			CompanyRepository companyRepository,
			CompanyMemberRepository companyMemberRepository,
			DepartmentRepository departmentRepository,
			ProjectMemberRepository projectMemberRepository,
			ProjectRepository projectRepository) {
		this(
				authContextService,
				companyAccessService,
				companyRepository,
				companyMemberRepository,
				departmentRepository,
				projectMemberRepository,
				projectRepository,
				Clock.systemUTC());
	}

	CompanyMemberService(
			AuthContextService authContextService,
			CompanyAccessService companyAccessService,
			CompanyRepository companyRepository,
			CompanyMemberRepository companyMemberRepository,
			DepartmentRepository departmentRepository,
			ProjectMemberRepository projectMemberRepository,
			ProjectRepository projectRepository,
			Clock clock) {
		this.authContextService = authContextService;
		this.companyAccessService = companyAccessService;
		this.companyRepository = companyRepository;
		this.companyMemberRepository = companyMemberRepository;
		this.departmentRepository = departmentRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.projectRepository = projectRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public CompanyMembersData list(
			String accessToken,
			Long companyId,
			CompanyMemberStatus status,
			Long departmentId,
			String q) {
		AuthContext authContext = activeAuthContext(accessToken);
		company(companyId);
		companyAccessService.requireCompanyAdmin(companyId, authContext);
		String normalizedQ = normalizeQ(q);
		return new CompanyMembersData(companyMemberRepository.findByCompanyIdWithFiltersOrderByIdAsc(
					companyId,
					status,
					departmentId,
					normalizedQ)
				.stream()
				.map(CompanyMemberResponse::from)
				.toList());
	}

	private static String normalizeQ(String q) {
		if (q == null || q.trim().isEmpty()) {
			return null;
		}
		return q.trim().toLowerCase(Locale.ROOT);
	}

	@Transactional
	public CompanyMemberData update(String accessToken, Long companyId, Long companyMemberId, CompanyMemberUpdateRequest request) {
		AuthContext authContext = activeAuthContext(accessToken);
		companyForUpdate(companyId);
		companyAccessService.requireCompanyAdmin(companyId, authContext);
		CompanyMember companyMember = companyMemberForUpdate(companyMemberId, companyId);
		validateDepartment(companyId, request.departmentId());
		validateRemovalRequiresDelete(request.status());
		validateLeftMemberReactivation(companyMember, request.status());
		validateLastActiveCompanyAdmin(companyMember, request.role(), request.status());
		companyMember.setRole(request.role());
		companyMember.setStatus(request.status());
		companyMember.setDepartmentId(request.departmentId());
		companyMember.setUpdatedAt(now());
		return new CompanyMemberData(CompanyMemberResponse.from(companyMember));
	}


	@Transactional
	public CompanyMemberRemoveData remove(String accessToken, Long companyId, Long companyMemberId) {
		AuthContext authContext = activeAuthContext(accessToken);
		companyForUpdate(companyId);
		companyAccessService.requireCompanyAdmin(companyId, authContext);
		CompanyMember companyMember = companyMemberForUpdate(companyMemberId, companyId);
		validateSelfRemoval(companyMember, authContext);
		validateAlreadyLeft(companyMember);
		validateLastActiveCompanyAdmin(companyMember, CompanyMemberRole.MEMBER, CompanyMemberStatus.LEFT);
		List<ProjectMember> lockedProjectMembers = lockAffectedProjectMembers(companyId, companyMember.getUser().getId());
		validateNoLastActiveProjectAdmin(companyMember.getUser().getId(), lockedProjectMembers);
		lockAffectedProjects(affectedProjectIds(companyMember.getUser().getId(), lockedProjectMembers));
		LocalDateTime now = now();
		companyMember.setStatus(CompanyMemberStatus.LEFT);
		companyMember.setDepartmentId(null);
		companyMember.setUpdatedAt(now);
		int removedProjectMemberCount = projectMemberRepository.removeActiveMembershipsByCompanyIdAndUserId(
				companyId,
				companyMember.getUser().getId(),
				ProjectMemberStatus.ACTIVE,
				ProjectMemberStatus.REMOVED,
				now);
		return new CompanyMemberRemoveData(true, removedProjectMemberCount);
	}

	private AuthContext activeAuthContext(String accessToken) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		return authContext;
	}

	private void validateDepartment(Long companyId, Long departmentId) {
		if (departmentId != null && !departmentRepository.existsByIdAndCompany_Id(departmentId, companyId)) {
			throw new DepartmentNotFoundException(departmentId);
		}
	}

	private static void validateLeftMemberReactivation(CompanyMember companyMember, CompanyMemberStatus requestedStatus) {
		if (companyMember.getStatus() == CompanyMemberStatus.LEFT && requestedStatus == CompanyMemberStatus.ACTIVE) {
			throw CompanyMemberConflictException.leftMemberReactivation();
		}
	}

	private static void validateRemovalRequiresDelete(CompanyMemberStatus requestedStatus) {
		if (requestedStatus == CompanyMemberStatus.LEFT) {
			throw CompanyMemberConflictException.removalRequiresDelete();
		}
	}

	private static void validateSelfRemoval(CompanyMember companyMember, AuthContext authContext) {
		if (companyMember.getUser().getId().equals(authContext.userId())) {
			throw CompanyMemberConflictException.selfRemoval();
		}
	}

	private static void validateAlreadyLeft(CompanyMember companyMember) {
		if (companyMember.getStatus() == CompanyMemberStatus.LEFT) {
			throw CompanyMemberConflictException.alreadyLeft();
		}
	}

	private List<ProjectMember> lockAffectedProjectMembers(Long companyId, Long userId) {
		return projectMemberRepository.findActiveTargetMembershipsAndActiveProjectAdminsInTargetProjectsForUpdate(
				companyId,
				userId,
				ProjectMemberRole.PROJECT_ADMIN,
				ProjectMemberStatus.ACTIVE);
	}

	private void lockAffectedProjects(List<Long> affectedProjectIds) {
		if (!affectedProjectIds.isEmpty()) {
			projectRepository.findAllByIdInForUpdateOrderByIdAsc(affectedProjectIds);
		}
	}

	private static List<Long> affectedProjectIds(Long userId, List<ProjectMember> lockedProjectMembers) {
		List<Long> projectIds = new ArrayList<>();
		for (ProjectMember projectMember : lockedProjectMembers) {
			if (!projectMember.getUser().getId().equals(userId)) {
				continue;
			}
			Long projectId = projectMember.getProject().getId();
			if (!projectIds.contains(projectId)) {
				projectIds.add(projectId);
			}
		}
		projectIds.sort(Long::compareTo);
		return projectIds;
	}

	private static void validateNoLastActiveProjectAdmin(Long userId, List<ProjectMember> lockedProjectMembers) {
		Map<Long, Long> activeAdminCountsByProjectId = new HashMap<>();
		for (ProjectMember projectMember : lockedProjectMembers) {
			if (isActiveProjectAdmin(projectMember)) {
				activeAdminCountsByProjectId.merge(projectMember.getProject().getId(), 1L, Long::sum);
			}
		}
		for (ProjectMember projectMember : lockedProjectMembers) {
			if (!projectMember.getUser().getId().equals(userId) || !isActiveProjectAdmin(projectMember)) {
				continue;
			}
			if (activeAdminCountsByProjectId.getOrDefault(projectMember.getProject().getId(), 0L) <= 1L) {
				throw ProjectMemberConflictException.lastActiveProjectAdmin();
			}
		}
	}

	private static boolean isActiveProjectAdmin(ProjectMember projectMember) {
		return projectMember.getRole() == ProjectMemberRole.PROJECT_ADMIN
				&& projectMember.getStatus() == ProjectMemberStatus.ACTIVE;
	}

	private void validateLastActiveCompanyAdmin(
			CompanyMember companyMember,
			CompanyMemberRole requestedRole,
			CompanyMemberStatus requestedStatus) {
		if (!isActiveCompanyAdmin(companyMember) || isActiveCompanyAdmin(requestedRole, requestedStatus)) {
			return;
		}
		long activeAdminCount = companyMemberRepository.countByCompany_IdAndRoleAndStatus(
				companyMember.getCompany().getId(),
				CompanyMemberRole.COMPANY_ADMIN,
				CompanyMemberStatus.ACTIVE);
		if (activeAdminCount <= 1) {
			throw CompanyMemberConflictException.lastActiveCompanyAdmin();
		}
	}

	private static boolean isActiveCompanyAdmin(CompanyMember companyMember) {
		return isActiveCompanyAdmin(companyMember.getRole(), companyMember.getStatus());
	}

	private static boolean isActiveCompanyAdmin(CompanyMemberRole role, CompanyMemberStatus status) {
		return role == CompanyMemberRole.COMPANY_ADMIN && status == CompanyMemberStatus.ACTIVE;
	}

	private void company(Long companyId) {
		companyRepository.findById(companyId)
				.orElseThrow(() -> new CompanyNotFoundException(companyId));
	}

	private void companyForUpdate(Long companyId) {
		companyRepository.findByIdForUpdate(companyId)
				.orElseThrow(() -> new CompanyNotFoundException(companyId));
	}

	private CompanyMember companyMemberForUpdate(Long companyMemberId, Long companyId) {
		return companyMemberRepository.findByIdAndCompanyIdForUpdate(companyMemberId, companyId)
				.orElseThrow(() -> new CompanyMemberNotFoundException(companyMemberId));
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime();
	}
}
