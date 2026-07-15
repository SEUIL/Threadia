package com.threadia.backend.invitation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.invitation.dto.CompanyInvitationsData;
import com.threadia.backend.invitation.dto.InvitationActionData;

@RestController
@RequestMapping("/api/v1")
public class InvitationController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final ReceivedInvitationService receivedInvitationService;
	private final InvitationActionService invitationActionService;

	public InvitationController(
			ReceivedInvitationService receivedInvitationService,
			InvitationActionService invitationActionService) {
		this.receivedInvitationService = receivedInvitationService;
		this.invitationActionService = invitationActionService;
	}

	@GetMapping("/users/me/invitations")
	public ResponseEntity<ApiResponse<CompanyInvitationsData>> listMine(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		String accessToken = extractAccessToken(authorizationHeader);
		return ResponseEntity.ok(ApiResponse.of(receivedInvitationService.listMine(accessToken)));
	}

	@PostMapping("/invitations/{invitationId}/accept")
	public ResponseEntity<ApiResponse<InvitationActionData>> accept(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long invitationId) {
		String accessToken = extractAccessToken(authorizationHeader);
		return ResponseEntity.ok(ApiResponse.of(invitationActionService.accept(accessToken, invitationId)));
	}

	@PostMapping("/invitations/{invitationId}/decline")
	public ResponseEntity<ApiResponse<InvitationActionData>> decline(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long invitationId) {
		String accessToken = extractAccessToken(authorizationHeader);
		return ResponseEntity.ok(ApiResponse.of(invitationActionService.decline(accessToken, invitationId)));
	}

	@PostMapping("/invitations/{invitationId}/cancel")
	public ResponseEntity<ApiResponse<InvitationActionData>> cancel(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long invitationId) {
		String accessToken = extractAccessToken(authorizationHeader);
		return ResponseEntity.ok(ApiResponse.of(invitationActionService.cancel(accessToken, invitationId)));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
