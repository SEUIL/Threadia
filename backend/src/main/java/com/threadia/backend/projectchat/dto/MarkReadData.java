package com.threadia.backend.projectchat.dto;

public record MarkReadData(Long projectId, Long lastReadMessageId, long unreadCount) {
}
