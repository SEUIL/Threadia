package com.threadia.backend.directmessage.dto;

import java.time.LocalDateTime;

public record DirectMessageReadData(Long roomId, Long lastReadDirectMessageId, long unreadCount, LocalDateTime readAt) { }
