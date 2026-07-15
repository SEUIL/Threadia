package com.threadia.backend.auth.dto;

// POST /api/v1/auth/logout 성공 응답의 data. (logout 엔드포인트 자체는 별도 구현 예정)
public record LogoutData(boolean success) {

	public static LogoutData ofSuccess() {
		return new LogoutData(true);
	}
}
