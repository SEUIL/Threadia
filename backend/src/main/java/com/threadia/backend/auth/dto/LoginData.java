package com.threadia.backend.auth.dto;

import java.util.List;

import com.threadia.backend.common.auth.UserResponse;

// POST /api/v1/auth/login 성공 응답의 data. refreshToken은 여기 안 담고 Set-Cookie로만 전달한다.
public record LoginData(
		String accessToken,
		String tokenType, // "Bearer"
		long expiresIn, // 1800 (30분)
		UserResponse user,
		List<Object> companies // Sprint 1은 항상 빈 배열
) {

	// tokenType/companies는 Sprint 1 고정값이라 매 호출부에서 반복하지 않도록 여기서 채운다.
	// expiresIn은 JwtTokenService가 실제로 발급에 사용한 만료초를 그대로 받아 값이 어긋나지 않게 한다.
	public static LoginData of(String accessToken, long expiresIn, UserResponse user) {
		return new LoginData(accessToken, "Bearer", expiresIn, user, List.of());
	}
}
