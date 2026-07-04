package com.threadia.backend.config.token;

// refresh token은 JWT가 아니라 random raw 값 + DB에는 해시만 저장하는 방식이다(원문 저장 금지).
public interface RefreshTokenGenerator {

	// 쿠키에 내려줄 원문 refresh token.
	String generateRawToken();

	// DB(token_hash)에 저장할 값.
	String hash(String rawToken);
}
