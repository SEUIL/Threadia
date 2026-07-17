package com.threadia.backend.notification.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.notification.domain.NotificationEvent;
import com.threadia.backend.notification.domain.NotificationRecipient;

public record NotificationResponse(
		Long notificationRecipientId,
		Long notificationEventId,
		String type,
		String title,
		String body,
		String readAt,
		NotificationTargetResponse target,
		String createdAt) {

	public static NotificationResponse from(NotificationRecipient recipient) {
		NotificationEvent event = recipient.getNotificationEvent();
		return new NotificationResponse(
				recipient.getId(),
				event.getId(),
				event.getType().name(),
				event.getTitle(),
				event.getBody(),
				formatUtc(recipient.getReadAt()),
				new NotificationTargetResponse(event.getTargetType().name(), event.getTargetId()),
				formatUtc(recipient.getCreatedAt()));
	}

	public static String formatUtc(LocalDateTime value) {
		return value == null ? null : value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
