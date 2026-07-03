package com.threadia.backend.config.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// signup/login/me 전체가 의존하는 서명/검증 로직이라 만료, 변조, 형식 오류를 분리해서 검증한다.
class JwtTokenServiceImplTest {

	private static final String SECRET = "01234567890123456789012345678901";

	@Test
	void 발급한_토큰을_파싱하면_userId와_email이_그대로_복원된다() {
		JwtTokenServiceImpl service = new JwtTokenServiceImpl(SECRET, 1800);

		String token = service.issueAccessToken(1L, "user@example.com");
		AuthenticatedUser authUser = service.parseAccessToken(token);

		assertThat(authUser.userId()).isEqualTo(1L);
		assertThat(authUser.email()).isEqualTo("user@example.com");
	}

	@Test
	void 만료된_토큰은_JwtAuthenticationException을_던진다() {
		JwtTokenServiceImpl service = new JwtTokenServiceImpl(SECRET, -1);

		String expiredToken = service.issueAccessToken(1L, "user@example.com");

		assertThatThrownBy(() -> service.parseAccessToken(expiredToken))
				.isInstanceOf(JwtAuthenticationException.class)
				.hasMessageContaining("만료");
	}

	@Test
	void 변조된_토큰은_JwtAuthenticationException을_던진다() {
		JwtTokenServiceImpl service = new JwtTokenServiceImpl(SECRET, 1800);

		String token = service.issueAccessToken(1L, "user@example.com");
		String tampered = token.substring(0, token.length() - 1)
				+ (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');

		assertThatThrownBy(() -> service.parseAccessToken(tampered))
				.isInstanceOf(JwtAuthenticationException.class);
	}

	@Test
	void 형식이_아닌_문자열은_JwtAuthenticationException을_던진다() {
		JwtTokenServiceImpl service = new JwtTokenServiceImpl(SECRET, 1800);

		assertThatThrownBy(() -> service.parseAccessToken("not-a-jwt"))
				.isInstanceOf(JwtAuthenticationException.class);
	}

	@Test
	void secret이_비어있으면_생성_시점에_IllegalStateException을_던진다() {
		assertThatThrownBy(() -> new JwtTokenServiceImpl("", 1800))
				.isInstanceOf(IllegalStateException.class);
	}

	/**
	 * AuthService.login이 응답 expiresIn을 getAccessTokenExpirationSeconds()에서 가져오도록 리팩터링한
	 * 근거가 되는 불변조건: "그 getter가 반환하는 값 = 실제 서명에 쓰인 (exp - iat)"을 직접 검증한다.
	 * mock으로 getter를 stub하는 방식은 이 관계를 전혀 검증하지 못하므로, 여기서는 실제 토큰의 claim을
	 * 파싱해서 대조한다.
	 */
	@Test
	void getAccessTokenExpirationSeconds가_실제_토큰의_exp와_iat_차이와_일치한다() {
		long expirationSeconds = 900L;
		JwtTokenServiceImpl service = new JwtTokenServiceImpl(SECRET, expirationSeconds);

		String token = service.issueAccessToken(1L, "user@example.com");

		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
		Date issuedAt = claims.getIssuedAt();
		Date expiration = claims.getExpiration();
		long actualLifetimeSeconds = (expiration.getTime() - issuedAt.getTime()) / 1000;

		assertThat(service.getAccessTokenExpirationSeconds()).isEqualTo(expirationSeconds);
		assertThat(actualLifetimeSeconds).isEqualTo(expirationSeconds);
	}
}
