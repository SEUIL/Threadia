package com.threadia.backend.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.auth.dto.LoginData;
import com.threadia.backend.auth.dto.LoginRequest;
import com.threadia.backend.auth.dto.LogoutData;
import com.threadia.backend.auth.dto.SignupData;
import com.threadia.backend.auth.dto.SignupRequest;
import com.threadia.backend.auth.service.AuthService;
import com.threadia.backend.auth.service.LoginResult;
import com.threadia.backend.auth.service.LogoutResult;
import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

import jakarta.validation.Valid;

/**
 * 이메일 Auth API의 HTTP 진입점(signup/login/logout).
 *
 * <p>전체 흐름:
 * <pre>
 * 요청 JSON
 *   -> DTO(SignupRequest/LoginRequest)로 파싱 + validation (@Valid, compact constructor의 email 정규화)
 *   -> AuthService가 Repository/PasswordEncoder/JWT/refresh token/쿠키를 조합해 실제 처리
 *   -> 실패 시 서비스가 예외를 던지고 GlobalExceptionHandler(common.exception)가 401/409/400 응답으로 변환
 *   -> 성공 시 ApiResponse로 감싼 JSON(+login/logout은 Set-Cookie 헤더)을 응답
 * </pre>
 *
 * logout은 access token과 refreshToken cookie를 함께 검증한 뒤 refresh token을 revoke하고 clear cookie를 응답한다.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	// 이메일 회원가입. 성공 시 201 + 가입된 user 정보만 반환(토큰 미발급).
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignupData>> signup(@Valid @RequestBody SignupRequest request) {
		SignupData data = authService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
	}

	// 이메일 로그인. 성공 시 200 + accessToken(body) + refreshToken(Set-Cookie, HttpOnly)을 함께 응답.
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginData>> login(@Valid @RequestBody LoginRequest request) {
		LoginResult result = authService.login(request);
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
				.body(ApiResponse.of(result.data()));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<LogoutData>> logout(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@CookieValue(value = "refreshToken", required = false) String refreshToken) {
		String accessToken = extractAccessToken(authorizationHeader);
		LogoutResult result = authService.logout(accessToken, refreshToken);
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, result.clearCookie().toString())
				.body(ApiResponse.of(result.data()));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization header is missing or invalid.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
