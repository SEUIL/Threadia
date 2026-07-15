package com.threadia.backend.invitation;

import org.springframework.stereotype.Service;

import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenService;
import com.threadia.backend.invitation.domain.Invitation;
import com.threadia.backend.invitation.domain.InvitationScopeType;
import com.threadia.backend.invitation.dto.InvitationActionData;
import com.threadia.backend.invitation.exception.InvitationNotFoundException;
import com.threadia.backend.invitation.repository.InvitationRepository;

@Service
public class InvitationActionService {

	private final InvitationRepository invitationRepository;
	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final CompanyInvitationService companyInvitationService;
	private final ProjectInvitationService projectInvitationService;

	public InvitationActionService(
			InvitationRepository invitationRepository,
			JwtTokenService jwtTokenService,
			UserRepository userRepository,
			CompanyInvitationService companyInvitationService,
			ProjectInvitationService projectInvitationService) {
		this.invitationRepository = invitationRepository;
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
		this.companyInvitationService = companyInvitationService;
		this.projectInvitationService = projectInvitationService;
	}

	public InvitationActionData accept(String accessToken, Long invitationId) {
		validateActiveRequester(accessToken);
		return scopeFor(invitationId) == InvitationScopeType.COMPANY
				? companyInvitationService.accept(accessToken, invitationId)
				: projectInvitationService.accept(accessToken, invitationId);
	}

	public InvitationActionData decline(String accessToken, Long invitationId) {
		validateActiveRequester(accessToken);
		return scopeFor(invitationId) == InvitationScopeType.COMPANY
				? companyInvitationService.decline(accessToken, invitationId)
				: projectInvitationService.decline(accessToken, invitationId);
	}

	public InvitationActionData cancel(String accessToken, Long invitationId) {
		validateActiveRequester(accessToken);
		return scopeFor(invitationId) == InvitationScopeType.COMPANY
				? companyInvitationService.cancel(accessToken, invitationId)
				: projectInvitationService.cancel(accessToken, invitationId);
	}

	private InvitationScopeType scopeFor(Long invitationId) {
		Invitation invitation = invitationRepository.findById(invitationId)
				.orElseThrow(() -> new InvitationNotFoundException(invitationId));
		return invitation.getScopeType();
	}

	private void validateActiveRequester(String accessToken) {
		AuthenticatedUser authUser = jwtTokenService.parseAccessToken(accessToken);
		userRepository.findById(authUser.userId())
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new AuthenticationFailedException("인증에 실패했습니다."));
	}
}
