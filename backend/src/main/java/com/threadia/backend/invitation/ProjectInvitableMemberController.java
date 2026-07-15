package com.threadia.backend.invitation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.invitation.dto.ProjectInvitableMemberData;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/invitable-members")
public class ProjectInvitableMemberController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final ProjectInvitationService projectInvitationService;

	public ProjectInvitableMemberController(ProjectInvitationService projectInvitationService) {
		this.projectInvitationService = projectInvitationService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<ProjectInvitableMemberData>> findInvitableMember(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectId,
			@RequestParam String email) {
		ProjectInvitableMemberData data = projectInvitationService.findInvitableMember(
				extractAccessToken(authorizationHeader), projectId, email);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
