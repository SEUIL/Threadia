package com.threadia.backend.invitation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

@Service
public class ReceivedInvitationService {

	private static final ZoneOffset UTC = ZoneOffset.UTC;

	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final InvitationRepository invitationRepository;
	private final Clock clock;

	@Autowired
	public ReceivedInvitationService(
			JwtTokenService jwtTokenService,
			UserRepository userRepository,
			InvitationRepository invitationRepository) {
		this(jwtTokenService, userRepository, invitationRepository, Clock.systemUTC());
	}

	ReceivedInvitationService(
			JwtTokenService jwtTokenService,
			UserRepository userRepository,
			InvitationRepository invitationRepository,
			Clock clock) {
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
		this.invitationRepository = invitationRepository;
		this.clock = clock;
	}

	@Transactional
	public CompanyInvitationsData listMine(String accessToken) {
		User requester = activeRequester(accessToken);
		List<InvitationResponse> invitations = invitationRepository.findByInvitee_IdOrderByCreatedAtDesc(requester.getId())
				.stream()
				.map(this::expirePendingIfNeeded)
				.map(ReceivedInvitationService::toResponse)
				.toList();
		return new CompanyInvitationsData(invitations);
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

	private static InvitationResponse toResponse(Invitation invitation) {
		return new InvitationResponse(
				invitation.getId(), invitation.getScopeType().name(), invitation.getCompany().getId(),
				invitation.getCompany().getName(), invitation.getProjectId(), invitation.getInviter().getId(),
				invitation.getInviter().getNickname(), invitation.getInvitee().getId(), invitation.getInvitee().getEmail(),
				invitation.getInvitee().getNickname(), invitation.getRole(), invitation.getStatus().name(),
				invitation.getExpiresAt().atOffset(UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
	}
}
