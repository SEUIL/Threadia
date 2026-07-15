package com.threadia.backend.directmessage.dto;

import java.time.LocalDateTime;

import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.directmessage.domain.DirectMessageRoom;

public record DirectMessageRoomResponse(Long id, Long companyId, UserResponse otherUser, long unreadCount, LocalDateTime createdAt) {
	public static DirectMessageRoomResponse from(DirectMessageRoom room, Long currentUserId, long unreadCount) {
		return new DirectMessageRoomResponse(room.getId(), room.getCompany().getId(), UserResponse.from(room.otherUser(currentUserId)), unreadCount, room.getCreatedAt());
	}
}
