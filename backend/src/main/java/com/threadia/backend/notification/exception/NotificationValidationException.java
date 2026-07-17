package com.threadia.backend.notification.exception;

public class NotificationValidationException extends RuntimeException {

	private NotificationValidationException(String message) {
		super(message);
	}

	public static NotificationValidationException invalidType() {
		return new NotificationValidationException("알림 type이 올바르지 않습니다.");
	}

	public static NotificationValidationException invalidSize() {
		return new NotificationValidationException("알림 목록 size는 1 이상 100 이하여야 합니다.");
	}

	public static NotificationValidationException invalidCursor() {
		return new NotificationValidationException("알림 목록 cursor가 올바르지 않습니다.");
	}

	public static NotificationValidationException inconsistentCompanyAndProject() {
		return new NotificationValidationException("companyId와 projectId의 회사 범위가 일치하지 않습니다.");
	}
}
