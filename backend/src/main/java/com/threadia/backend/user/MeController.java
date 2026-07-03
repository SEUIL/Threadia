package com.threadia.backend.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

/**
 * GET /api/v1/auth/me의 HTTP 진입점.
 * URL은 /auth 하위지만, 응답 로직은 auth가 아니라 user 도메인이 담당한다(백엔드-구조.md v1.1 확정).
 *
 * <p>흐름: Authorization 헤더에서 Bearer token 추출 -> MeService.getMe()로 검증/조회 위임
 * -> 성공 시 ApiResponse(4필드 user)로 응답, 실패 시 GlobalExceptionHandler가 401로 변환.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class MeController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final MeService meService;

	public MeController(MeService meService) {
		this.meService = meService;
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<MeData>> me(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		String accessToken = extractAccessToken(authorizationHeader);
		MeData data = meService.getMe(accessToken);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	// "Authorization: Bearer ..." 헤더 파싱은 JwtTokenService가 아니라 여기(controller helper)에서 처리한다(합의안 기준).
	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
