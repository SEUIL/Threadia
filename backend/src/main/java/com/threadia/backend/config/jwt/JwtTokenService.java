package com.threadia.backend.config.jwt;

// access token 발급/검증 계약. AuthService(발급), MeService(검증)에서 사용한다.
public interface JwtTokenService {

	// 로그인 성공 시 30분 만료 access token을 발급한다. subject=userId, claim=email.
	String issueAccessToken(Long userId, String email);

	// Authorization 헤더에서 "Bearer " prefix가 제거된 raw token을 검증하고 인증 사용자 정보를 반환한다.
	// ACTIVE 상태 검증은 하지 않는다 — service/controller 계층에서 UserRepository로 별도 확인한다.
	AuthenticatedUser parseAccessToken(String accessToken);

	// login 응답의 expiresIn을 실제 발급 만료초와 일치시키기 위해 노출한다(값 이중 관리 방지).
	long getAccessTokenExpirationSeconds();
}
