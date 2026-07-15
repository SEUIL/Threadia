package com.threadia.backend.directmessage.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public record DirectMessageReadData(Long roomId, Long lastReadDirectMessageId, long unreadCount, String readAt) {
	public static DirectMessageReadData of(Long roomId, Long lastReadDirectMessageId, long unreadCount, LocalDateTime readAt) {
		return new DirectMessageReadData(roomId, lastReadDirectMessageId, unreadCount, formatUtc(readAt));
	}
	private static String formatUtc(LocalDateTime value) { return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME); }
}
