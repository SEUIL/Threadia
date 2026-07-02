package com.threadia.backend.common.auth;

import com.threadia.backend.auth.domain.User;

// Sprint 1 minimal Auth context: id/email/nickname/avatarUrl 4개 필드만 노출한다.
// passwordHash, status, createdAt, updatedAt은 응답에 포함하지 않는다.
public record UserResponse(Long id, String email, String nickname, String avatarUrl) {

	// User Entity -> 응답 DTO 변환. signup/login/me 전부 이걸로 user 정보를 만든다.
	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getNickname(), user.getAvatarUrl());
	}
}
