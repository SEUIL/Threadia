package com.threadia.backend.directmessage.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.directmessage.domain.DirectMessage;

public record DirectMessageResponse(Long id, Long roomId, UserResponse sender, String content, String createdAt) {
	public static DirectMessageResponse from(DirectMessage message) {
		return new DirectMessageResponse(message.getId(), message.getRoom().getId(), UserResponse.from(message.getSender()), message.getContent(), formatUtc(message.getCreatedAt()));
	}
	private static String formatUtc(LocalDateTime value) { return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME); }
}
