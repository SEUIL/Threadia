package com.threadia.backend.auth;

import java.util.List;

import com.threadia.backend.common.UserResponse;

public record LoginData(
		String accessToken,
		String tokenType, // "Bearer"
		long expiresIn, // 1800 (30분)
		UserResponse user,
		List<Object> companies // Sprint 1은 항상 빈 배열
) {

	public static LoginData of(String accessToken, UserResponse user) {
		return new LoginData(accessToken, "Bearer", 1800L, user, List.of());
	}
}
