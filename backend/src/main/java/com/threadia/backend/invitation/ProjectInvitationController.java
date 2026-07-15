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
import com.threadia.backend.invitation.dto.ProjectInvitationData;
import com.threadia.backend.invitation.dto.ProjectInvitationRequest;
import com.threadia.backend.invitation.dto.ProjectInvitationsData;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/invitations")
public class ProjectInvitationController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final ProjectInvitationService projectInvitationService;

	public ProjectInvitationController(ProjectInvitationService projectInvitationService) {
		this.projectInvitationService = projectInvitationService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ProjectInvitationData>> create(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectId,
			@Valid @RequestBody ProjectInvitationRequest request) {
		if (request == null) {
			throw new HttpMessageNotReadableException("요청 body가 비어있습니다.");
		}
		ProjectInvitationData data = projectInvitationService.create(extractAccessToken(authorizationHeader), projectId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<ProjectInvitationsData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectId) {
		ProjectInvitationsData data = projectInvitationService.list(extractAccessToken(authorizationHeader), projectId);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
