package com.threadia.backend.directmessage.dto;

import com.threadia.backend.auth.domain.User;

public record DirectMessageCandidateResponse(Long id, String nickname, String avatarUrl) {
	public static DirectMessageCandidateResponse from(User user) {
		return new DirectMessageCandidateResponse(user.getId(), user.getNickname(), user.getAvatarUrl());
	}
}
