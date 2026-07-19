package com.threadia.backend.directmessage.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.directmessage.domain.DirectMessageRoom;

public record DirectMessageRoomResponse(Long id, Long companyId, UserResponse otherUser, long unreadCount, String createdAt) {
	public static DirectMessageRoomResponse from(DirectMessageRoom room, Long currentUserId, long unreadCount) {
		return new DirectMessageRoomResponse(room.getId(), room.getCompany().getId(), UserResponse.from(room.otherUser(currentUserId)), unreadCount, formatUtc(room.getCreatedAt()));
	}
	private static String formatUtc(LocalDateTime value) { return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME); }
}
