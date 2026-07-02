package com.threadia.backend.common;

// signup, login, password reset, OAuth 계정 연결 등 모든 진입점에서 공통으로 사용한다.
public final class EmailNormalizer {

	private EmailNormalizer() {
	}

	public static String normalize(String rawEmail) {
		if (rawEmail == null) {
			return null;
		}
		return rawEmail.trim().toLowerCase();
	}
}
