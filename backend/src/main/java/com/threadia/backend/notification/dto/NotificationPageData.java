package com.threadia.backend.notification.dto;

import java.util.List;

public record NotificationPageData(
		List<NotificationResponse> notifications,
		String nextCursor,
		boolean hasNext,
		int size) {
}
