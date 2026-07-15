package com.threadia.backend.invitation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenService;
import com.threadia.backend.invitation.domain.Invitation;
import com.threadia.backend.invitation.domain.InvitationStatus;
import com.threadia.backend.invitation.dto.CompanyInvitationsData;
import com.threadia.backend.invitation.dto.InvitationResponse;
import com.threadia.backend.invitation.repository.InvitationRepository;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.repository.ProjectRepository;

@Service
public class ReceivedInvitationService {

	private static final ZoneOffset UTC = ZoneOffset.UTC;

	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final InvitationRepository invitationRepository;
	private final ProjectRepository projectRepository;
	private final Clock clock;

	@Autowired
	public ReceivedInvitationService(
			JwtTokenService jwtTokenService,
			UserRepository userRepository,
			InvitationRepository invitationRepository,
			ProjectRepository projectRepository) {
		this(jwtTokenService, userRepository, invitationRepository, projectRepository, Clock.systemUTC());
	}

	ReceivedInvitationService(
			JwtTokenService jwtTokenService,
			UserRepository userRepository,
			InvitationRepository invitationRepository,
			ProjectRepository projectRepository,
			Clock clock) {
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
		this.invitationRepository = invitationRepository;
		this.projectRepository = projectRepository;
		this.clock = clock;
	}

	@Transactional
	public CompanyInvitationsData listMine(String accessToken) {
		User requester = activeRequester(accessToken);
		List<Invitation> found = invitationRepository.findByInvitee_IdOrderByCreatedAtDesc(requester.getId())
				.stream()
				.map(this::expirePendingIfNeeded)
				.toList();
		Map<Long, String> projectNames = projectNamesOf(found);
		List<InvitationResponse> invitations = found.stream()
				.map(invitation -> toResponse(invitation, projectNameOf(invitation, projectNames)))
				.toList();
		return new CompanyInvitationsData(invitations);
	}

	/**
	 * COMPANY scope 초대는 projectId가 null이다. Map 구현에 따라 get(null)이 NPE를
	 * 던지므로(조회할 프로젝트가 없어 Map.of()를 돌려준 경우가 그렇다) 먼저 걸러낸다.
	 * 이걸 빠뜨리면 회사 초대만 받은 사용자의 목록 조회가 통째로 500이 된다.
	 */
	private static String projectNameOf(Invitation invitation, Map<Long, String> projectNames) {
		Long projectId = invitation.getProjectId();
		return projectId == null ? null : projectNames.get(projectId);
	}

	/**
	 * PROJECT scope 초대의 projectId → 프로젝트 이름. 초대마다 개별 조회하면 N+1이라
	 * id를 모아 한 번에 가져오고, PROJECT 초대가 없으면 아예 조회하지 않는다.
	 */
	private Map<Long, String> projectNamesOf(List<Invitation> invitations) {
		List<Long> projectIds = invitations.stream()
				.map(Invitation::getProjectId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
		if (projectIds.isEmpty()) {
			return Map.of();
		}
		return projectRepository.findAllById(projectIds).stream()
				.collect(Collectors.toMap(Project::getId, Project::getName));
	}

	private User activeRequester(String accessToken) {
		AuthenticatedUser authUser = jwtTokenService.parseAccessToken(accessToken);
		return userRepository.findById(authUser.userId())
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new AuthenticationFailedException("인증에 실패했습니다."));
	}

	private Invitation expirePendingIfNeeded(Invitation invitation) {
		if (invitation.getStatus() == InvitationStatus.PENDING && !invitation.getExpiresAt().isAfter(now())) {
			invitation.expire(now());
		}
		return invitation;
	}

	private LocalDateTime now() {
		return LocalDateTime.ofInstant(clock.instant(), UTC);
	}

	private static InvitationResponse toResponse(Invitation invitation, String projectName) {
		return new InvitationResponse(
				invitation.getId(), invitation.getScopeType().name(), invitation.getCompany().getId(),
				invitation.getCompany().getName(), invitation.getProjectId(), projectName, invitation.getInviter().getId(),
				invitation.getInviter().getNickname(), invitation.getInvitee().getId(), invitation.getInvitee().getEmail(),
				invitation.getInvitee().getNickname(), invitation.getRole(), invitation.getStatus().name(),
				invitation.getExpiresAt().atOffset(UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
	}
}
