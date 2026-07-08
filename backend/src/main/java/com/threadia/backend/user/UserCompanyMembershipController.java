package com.threadia.backend.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserCompanyMembershipController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final UserCompanyMembershipService userCompanyMembershipService;

	public UserCompanyMembershipController(UserCompanyMembershipService userCompanyMembershipService) {
		this.userCompanyMembershipService = userCompanyMembershipService;
	}

	@GetMapping("/company-memberships")
	public ResponseEntity<ApiResponse<UserCompanyMembershipsData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		String accessToken = extractAccessToken(authorizationHeader);
		UserCompanyMembershipsData data = userCompanyMembershipService.list(accessToken);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
