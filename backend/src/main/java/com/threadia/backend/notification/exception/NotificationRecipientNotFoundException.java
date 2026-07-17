package com.threadia.backend.notification.exception;

public class NotificationRecipientNotFoundException extends RuntimeException {

	public NotificationRecipientNotFoundException(Long notificationRecipientId) {
		super("알림을 찾을 수 없습니다. notificationRecipientId=" + notificationRecipientId);
	}
}
