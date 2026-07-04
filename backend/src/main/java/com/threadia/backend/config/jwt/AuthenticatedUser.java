package com.threadia.backend.config.jwt;

// JwtTokenService.parseAccessToken()의 반환값. JWT payload에서 뽑아낸 최소 정보만 담는다.
public record AuthenticatedUser(Long userId, String email) {
}
