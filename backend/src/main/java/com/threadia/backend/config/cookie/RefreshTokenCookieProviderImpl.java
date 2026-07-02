package com.threadia.backend.config.cookie;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieProviderImpl implements RefreshTokenCookieProvider {

	private static final String COOKIE_NAME = "refreshToken";

	// 운영(HTTPS)에서는 THREADIA_COOKIE_SECURE=true로 Secure 속성을 켠다. 로컬(HTTP)은 기본 false.
	private final boolean secure;

	// 기본 14일(1209600초, Sprint 1 확정값). AuthService가 RefreshToken entity의 expiresAt 계산에도
	// getMaxAge()로 이 값을 그대로 가져다 써서 쿠키/DB 만료가 서로 어긋나지 않게 한다.
	private final Duration maxAge;

	public RefreshTokenCookieProviderImpl(
			@Value("${threadia.cookie.secure:false}") boolean secure,
			@Value("${threadia.refresh-token.expiration-days:14}") long expirationDays) {
		this.secure = secure;
		this.maxAge = Duration.ofDays(expirationDays);
	}

	// 프론트엔드-구조.md 5.4절 확정값(Path=/) 그대로 적용.
	@Override
	public ResponseCookie create(String rawRefreshToken) {
		return ResponseCookie.from(COOKIE_NAME, rawRefreshToken)
				.httpOnly(true)
				.secure(secure)
				.sameSite("Lax")
				.path("/")
				.maxAge(maxAge)
				.build();
	}

	// 동일한 이름/경로로 Max-Age=0 쿠키를 내려보내 브라우저가 기존 쿠키를 지우게 한다.
	@Override
	public ResponseCookie clear() {
		return ResponseCookie.from(COOKIE_NAME, "")
				.httpOnly(true)
				.secure(secure)
				.sameSite("Lax")
				.path("/")
				.maxAge(0)
				.build();
	}

	@Override
	public Duration getMaxAge() {
		return maxAge;
	}
}
