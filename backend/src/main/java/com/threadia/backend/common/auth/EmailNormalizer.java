package com.threadia.backend.common.auth;

// signup, login, password reset, OAuth 계정 연결 등 모든 진입점에서 공통으로 사용한다.
public final class EmailNormalizer {

	private EmailNormalizer() {
	}

	// trim + lowercase. 저장/조회/비교 전 항상 이 값을 기준으로 한다.
	public static String normalize(String rawEmail) {
		if (rawEmail == null) {
			return null;
		}
		return rawEmail.trim().toLowerCase();
	}
}
