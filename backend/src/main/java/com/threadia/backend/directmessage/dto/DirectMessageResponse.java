package com.threadia.backend.directmessage.dto;

import java.time.LocalDateTime;

import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.directmessage.domain.DirectMessage;

public record DirectMessageResponse(Long id, Long roomId, UserResponse sender, String content, LocalDateTime createdAt) {
	public static DirectMessageResponse from(DirectMessage message) {
		return new DirectMessageResponse(message.getId(), message.getRoom().getId(), UserResponse.from(message.getSender()), message.getContent(), message.getCreatedAt());
	}
}
