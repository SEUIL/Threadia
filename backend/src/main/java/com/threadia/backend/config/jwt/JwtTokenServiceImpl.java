package com.threadia.backend.config.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// jjwt 기반 HMAC(HS) 서명/검증 구현체. secret은 THREADIA_JWT_SECRET 환경변수로만 주입한다(코드/문서에 노출 금지).
@Component
public class JwtTokenServiceImpl implements JwtTokenService {

	private static final String CLAIM_EMAIL = "email";

	private final SecretKey key;
	private final long accessTokenExpirationSeconds;

	public JwtTokenServiceImpl(
			@Value("${threadia.jwt.secret:}") String secret,
			@Value("${threadia.jwt.access-token-expiration-seconds:1800}") long accessTokenExpirationSeconds) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"threadia.jwt.secret이 설정되지 않았습니다. THREADIA_JWT_SECRET 환경변수를 확인하세요.");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
	}

	// subject=userId, claim.email=email, UTC 기준 iat/exp를 담아 서명한다.
	@Override
	public String issueAccessToken(Long userId, String email) {
		Instant now = Instant.now(Clock.system(ZoneOffset.UTC));
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim(CLAIM_EMAIL, email)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(accessTokenExpirationSeconds)))
				.signWith(key)
				.compact();
	}

	// 서명 검증 + 만료 확인 후 claim을 AuthenticatedUser로 변환한다. 실패 유형별로 원인을 구분해 예외 메시지에 남긴다.
	@Override
	public AuthenticatedUser parseAccessToken(String accessToken) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(accessToken)
					.getPayload();
			Long userId = Long.valueOf(claims.getSubject());
			String email = claims.get(CLAIM_EMAIL, String.class);
			return new AuthenticatedUser(userId, email);
		} catch (ExpiredJwtException e) {
			throw new JwtAuthenticationException("access token이 만료되었습니다.", e);
		} catch (JwtException | IllegalArgumentException e) {
			throw new JwtAuthenticationException("유효하지 않은 access token입니다.", e);
		}
	}

	@Override
	public long getAccessTokenExpirationSeconds() {
		return accessTokenExpirationSeconds;
	}
}
