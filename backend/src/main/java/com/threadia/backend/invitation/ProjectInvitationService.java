package com.threadia.backend.invitation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.EmailNormalizer;
import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.common.exception.UserNotFoundException;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenService;
import com.threadia.backend.invitation.domain.Invitation;
import com.threadia.backend.invitation.domain.InvitationScopeType;
import com.threadia.backend.invitation.domain.InvitationStatus;
import com.threadia.backend.invitation.dto.InvitationActionData;
import com.threadia.backend.invitation.dto.InvitationResponse;
import com.threadia.backend.invitation.dto.ProjectInvitableMemberData;
import com.threadia.backend.invitation.dto.ProjectInvitationData;
import com.threadia.backend.invitation.dto.ProjectInvitationRequest;
import com.threadia.backend.invitation.dto.ProjectInvitationsData;
import com.threadia.backend.invitation.exception.InvitationConflictException;
import com.threadia.backend.invitation.exception.InvitationNotFoundException;
import com.threadia.backend.invitation.repository.InvitationRepository;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@Service
public class ProjectInvitationService {

	private static final ZoneOffset UTC = ZoneOffset.UTC;
	private static final int EXPIRY_DAYS = 7;

	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final CompanyMemberRepository companyMemberRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final InvitationRepository invitationRepository;
	private final Clock clock;

	@Autowired
	public ProjectInvitationService(
			JwtTokenService jwtTokenService,
			UserRepository userRepository,
			CompanyMemberRepository companyMemberRepository,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			InvitationRepository invitationRepository) {
		this(
				jwtTokenService,
				userRepository,
				companyMemberRepository,
				projectRepository,
				projectMemberRepository,
				invitationRepository,
				Clock.systemUTC());
	}

	ProjectInvitationService(
			JwtTokenService jwtTokenService,
			UserRepository userRepository,
			CompanyMemberRepository companyMemberRepository,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			InvitationRepository invitationRepository,
			Clock clock) {
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
		this.companyMemberRepository = companyMemberRepository;
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.invitationRepository = invitationRepository;
		this.clock = clock;
	}

	@Transactional
	public ProjectInvitationData create(String accessToken, Long projectId, ProjectInvitationRequest request) {
		User requester = activeRequester(accessToken);
		Project project = projectForUpdate(projectId);
		requireProjectManage(project, requester.getId());
		if (requester.getId().equals(request.inviteeUserId())) {
			throw InvitationConflictException.selfInvitation();
		}
		User invitee = activeInvitee(request.inviteeUserId());
		assertActiveCompanyMember(project, invitee.getId());
		if (projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(
				projectId, invitee.getId(), ProjectMemberStatus.ACTIVE)) {
			throw InvitationConflictException.alreadyProjectMember();
		}
		LocalDateTime now = now();
		assertNoPendingInvitation(projectId, invitee.getId(), now);
		Invitation invitation = invitationRepository.save(new Invitation(
				project.getCompany(),
				projectId,
				requester,
				invitee,
				InvitationScopeType.PROJECT,
				request.role().name(),
				InvitationStatus.PENDING,
				now.plusDays(EXPIRY_DAYS),
				now,
				now));
		return new ProjectInvitationData(toResponse(invitation));
	}

	@Transactional
	public ProjectInvitationsData list(String accessToken, Long projectId) {
		User requester = activeRequester(accessToken);
		Project project = projectForUpdate(projectId);
		requireProjectManage(project, requester.getId());
		List<InvitationResponse> invitations = invitationRepository
				.findByProjectIdAndScopeTypeOrderByCreatedAtDesc(projectId, InvitationScopeType.PROJECT)
				.stream()
				.map(this::expirePendingIfNeeded)
				.map(ProjectInvitationService::toResponse)
				.toList();
		return new ProjectInvitationsData(invitations);
	}

	@Transactional
	public ProjectInvitableMemberData findInvitableMember(String accessToken, Long projectId, String rawEmail) {
		User requester = activeRequester(accessToken);
		Project project = projectForUpdate(projectId);
		requireProjectManage(project, requester.getId());
		String email = EmailNormalizer.normalize(rawEmail);
		User invitee = activeInviteeByEmail(email);
		if (requester.getId().equals(invitee.getId())) {
			throw InvitationConflictException.selfInvitation();
		}
		assertActiveCompanyMember(project, invitee.getId());
		if (projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(
				projectId, invitee.getId(), ProjectMemberStatus.ACTIVE)) {
			throw InvitationConflictException.alreadyProjectMember();
		}
		assertNoPendingInvitation(projectId, invitee.getId(), now());
		return new ProjectInvitableMemberData(UserResponse.from(invitee));
	}

	@Transactional(noRollbackFor = InvitationConflictException.class)
	public InvitationActionData accept(String accessToken, Long invitationId) {
		User requester = activeRequester(accessToken);
		Invitation invitation = projectInvitationForUpdate(invitationId);
		assertInvitee(invitation, requester.getId());
		assertPending(invitation);
		LocalDateTime now = now();
		expirePendingIfNeeded(invitation, now);
		assertPending(invitation);
		Optional<ProjectMember> existingMember = projectMemberRepository.findByProject_IdAndUser_IdForUpdate(
				invitation.getProjectId(), requester.getId());
		Project project = projectForUpdate(invitation.getProjectId());
		assertActiveCompanyMember(project, requester.getId());
		if (existingMember.filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE).isPresent()) {
			throw InvitationConflictException.alreadyProjectMember();
		}
		ProjectMemberRole role = ProjectMemberRole.valueOf(invitation.getRole());
		boolean membershipCreated = existingMember.isEmpty();
		if (membershipCreated) {
			projectMemberRepository.save(new ProjectMember(
					project,
					requester,
					role,
					ProjectMemberStatus.ACTIVE,
					now,
					now,
					now));
		} else {
			ProjectMember member = existingMember.get();
			member.setRole(role);
			member.setStatus(ProjectMemberStatus.ACTIVE);
			member.setJoinedAt(now);
			member.setUpdatedAt(now);
		}
		invitation.accept(now);
		return new InvitationActionData(invitation.getId(), invitation.getStatus().name(), membershipCreated);
	}

	@Transactional(noRollbackFor = InvitationConflictException.class)
	public InvitationActionData decline(String accessToken, Long invitationId) {
		User requester = activeRequester(accessToken);
		Invitation invitation = projectInvitationForUpdate(invitationId);
		assertInvitee(invitation, requester.getId());
		assertPending(invitation);
		LocalDateTime now = now();
		expirePendingIfNeeded(invitation, now);
		assertPending(invitation);
		invitation.decline(now);
		return new InvitationActionData(invitation.getId(), invitation.getStatus().name(), false);
	}

	@Transactional(noRollbackFor = InvitationConflictException.class)
	public InvitationActionData cancel(String accessToken, Long invitationId) {
		User requester = activeRequester(accessToken);
		Invitation invitation = projectInvitationForUpdate(invitationId);
		Project project = projectForUpdate(invitation.getProjectId());
		requireProjectManage(project, requester.getId());
		assertPending(invitation);
		LocalDateTime now = now();
		expirePendingIfNeeded(invitation, now);
		assertPending(invitation);
		invitation.cancel(now);
		return new InvitationActionData(invitation.getId(), invitation.getStatus().name(), false);
	}

	private User activeRequester(String accessToken) {
		AuthenticatedUser authUser = jwtTokenService.parseAccessToken(accessToken);
		return userRepository.findById(authUser.userId())
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new AuthenticationFailedException("인증에 실패했습니다."));
	}

	private User activeInvitee(Long userId) {
		User invitee = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(String.valueOf(userId)));
		if (invitee.getStatus() != UserStatus.ACTIVE) {
			throw InvitationConflictException.targetUserNotActiveCompanyMember();
		}
		return invitee;
	}

	private User activeInviteeByEmail(String email) {
		User invitee = userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException(email));
		if (invitee.getStatus() != UserStatus.ACTIVE) {
			throw InvitationConflictException.targetUserNotActiveCompanyMember();
		}
		return invitee;
	}

	private Project projectForUpdate(Long projectId) {
		return projectRepository.findByIdForUpdate(projectId)
				.orElseThrow(() -> new ProjectNotFoundException(projectId));
	}

	private Invitation projectInvitationForUpdate(Long invitationId) {
		Invitation invitation = invitationRepository.findByIdForUpdate(invitationId)
				.orElseThrow(() -> new InvitationNotFoundException(invitationId));
		if (invitation.getScopeType() != InvitationScopeType.PROJECT) {
			throw new ForbiddenException("프로젝트 초대만 처리할 수 있습니다.");
		}
		return invitation;
	}

	private void requireProjectManage(Project project, Long requesterUserId) {
		Long companyId = project.getCompany().getId();
		if (!companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(
				companyId, requesterUserId, CompanyMemberStatus.ACTIVE)) {
			throw new ForbiddenException("프로젝트 관리자 권한이 필요합니다.");
		}
		if (companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(
				companyId,
				requesterUserId,
				CompanyMemberRole.COMPANY_ADMIN,
				CompanyMemberStatus.ACTIVE)) {
			return;
		}
		boolean projectAdmin = projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), requesterUserId)
						.filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE)
						.filter(member -> member.getRole() == ProjectMemberRole.PROJECT_ADMIN)
						.isPresent();
		if (!projectAdmin) {
			throw new ForbiddenException("프로젝트 관리자 권한이 필요합니다.");
		}
	}

	private void assertActiveCompanyMember(Project project, Long userId) {
		if (!companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(
				project.getCompany().getId(), userId, CompanyMemberStatus.ACTIVE)) {
			throw InvitationConflictException.targetUserNotActiveCompanyMember();
		}
	}

	private void assertNoPendingInvitation(Long projectId, Long inviteeUserId, LocalDateTime now) {
		boolean pending = invitationRepository
				.findAllByProjectIdAndInvitee_IdAndScopeTypeAndStatus(
						projectId, inviteeUserId, InvitationScopeType.PROJECT, InvitationStatus.PENDING)
				.stream()
				.map(invitation -> expirePendingIfNeeded(invitation, now))
				.anyMatch(invitation -> invitation.getStatus() == InvitationStatus.PENDING);
		if (pending) {
			throw InvitationConflictException.alreadyPending();
		}
	}

	private void assertInvitee(Invitation invitation, Long requesterUserId) {
		if (!invitation.getInvitee().getId().equals(requesterUserId)) {
			throw new ForbiddenException("초대 대상자만 처리할 수 있습니다.");
		}
	}

	private void assertPending(Invitation invitation) {
		if (invitation.getStatus() == InvitationStatus.EXPIRED) {
			throw InvitationConflictException.expired();
		}
		if (invitation.getStatus() == InvitationStatus.CANCELED) {
			throw InvitationConflictException.canceled();
		}
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw InvitationConflictException.invalidStatus();
		}
	}

	private Invitation expirePendingIfNeeded(Invitation invitation) {
		return expirePendingIfNeeded(invitation, now());
	}

	private Invitation expirePendingIfNeeded(Invitation invitation, LocalDateTime now) {
		if (invitation.getStatus() == InvitationStatus.PENDING && !invitation.getExpiresAt().isAfter(now)) {
			invitation.expire(now);
		}
		return invitation;
	}

	private LocalDateTime now() {
		return LocalDateTime.ofInstant(clock.instant(), UTC);
	}

	private static InvitationResponse toResponse(Invitation invitation) {
		return new InvitationResponse(
				invitation.getId(),
				invitation.getScopeType().name(),
				invitation.getCompany().getId(),
				invitation.getCompany().getName(),
				invitation.getProjectId(),
				invitation.getInviter().getId(),
				invitation.getInviter().getNickname(),
				invitation.getInvitee().getId(),
				invitation.getInvitee().getEmail(),
				invitation.getInvitee().getNickname(),
				invitation.getRole(),
				invitation.getStatus().name(),
				invitation.getExpiresAt().atOffset(UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
	}
}
