package com.threadia.backend.invitation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.invitation.dto.CompanyInvitationData;
import com.threadia.backend.invitation.dto.CompanyInvitationRequest;
import com.threadia.backend.invitation.dto.CompanyInvitationsData;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/invitations")
public class CompanyInvitationController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final CompanyInvitationService companyInvitationService;

	public CompanyInvitationController(CompanyInvitationService companyInvitationService) {
		this.companyInvitationService = companyInvitationService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<CompanyInvitationData>> create(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId,
			@Valid @RequestBody CompanyInvitationRequest request) {
		if (request == null) {
			throw new HttpMessageNotReadableException("요청 body가 비어있습니다.");
		}
		String accessToken = extractAccessToken(authorizationHeader);
		CompanyInvitationData data = companyInvitationService.create(accessToken, companyId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<CompanyInvitationsData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId) {
		String accessToken = extractAccessToken(authorizationHeader);
		CompanyInvitationsData data = companyInvitationService.list(accessToken, companyId);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
