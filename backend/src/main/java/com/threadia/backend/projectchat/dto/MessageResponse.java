package com.threadia.backend.projectchat.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.projectchat.domain.Message;

public record MessageResponse(Long id, Long projectId, Long authorUserId, String content, Long threadId, String createdAt) {
	public static MessageResponse from(Message message) {
		return new MessageResponse(message.getId(), message.getProject().getId(), message.getAuthor().getId(),
				message.getContent(), message.getThreadId(), formatUtc(message.getCreatedAt()));
	}

	private static String formatUtc(LocalDateTime value) {
		return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
