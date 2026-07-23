package com.threadia.backend.thread.dto;

import com.threadia.backend.auth.domain.User;

public record ThreadAuthorResponse(Long id, String nickname, String avatarUrl) {
	public static ThreadAuthorResponse from(User user) {
		if (user == null) return null;
		return new ThreadAuthorResponse(user.getId(), user.getNickname(), user.getAvatarUrl());
	}
}
