package com.threadia.backend.config.cookie;

import java.time.Duration;

import org.springframework.http.ResponseCookie;

// refresh token을 담는 HttpOnly 쿠키 생성/제거 계약. login(create), logout(clear)에서 사용한다.
public interface RefreshTokenCookieProvider {

	// HttpOnly, SameSite=Lax, Path=/, Max-Age=1209600(14일)
	ResponseCookie create(String rawRefreshToken);

	// Max-Age=0 (로그아웃 시 쿠키 제거용)
	ResponseCookie clear();

	// AuthService가 RefreshToken entity의 expiresAt을 쿠키 Max-Age와 동일한 값으로 계산하기 위해 노출한다(값 이중 관리 방지).
	Duration getMaxAge();
}
