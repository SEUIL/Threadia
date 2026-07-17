package com.threadia.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.threadia.backend.notification.domain.NotificationRecipient;
import com.threadia.backend.notification.domain.NotificationType;
import com.threadia.backend.notification.exception.NotificationValidationException;

class NotificationCursorCodecTest {

	private static final String SECRET = "test-only-notification-cursor-hmac-secret";

	@Test
	void signsUserFilterAndDescendingKeysetPayload() {
		NotificationCursorCodec codec = new NotificationCursorCodec(SECRET);
		NotificationFilters filters = new NotificationFilters(10L, 20L, NotificationType.MENTIONED, true);
		NotificationRecipient recipient = recipient(80L, "2026-07-17T10:00:00");

		String cursor = codec.encode(1L, filters, recipient);
		NotificationCursorCodec.Cursor decoded = codec.decode(cursor, 1L, filters);

		assertThat(cursor).contains(".");
		assertThat(decoded.createdAt()).isEqualTo(recipient.getCreatedAt());
		assertThat(decoded.id()).isEqualTo(80L);
	}

	@Test
	void rejectsTamperingAndReuseByAnotherUserOrNormalizedFilter() {
		NotificationCursorCodec codec = new NotificationCursorCodec(SECRET);
		NotificationFilters filters = new NotificationFilters(10L, 20L, NotificationType.MENTIONED, true);
		String cursor = codec.encode(1L, filters, recipient(80L, "2026-07-17T10:00:00"));
		String tampered = cursor.substring(0, cursor.length() - 1) + (cursor.endsWith("A") ? "B" : "A");

		assertThatThrownBy(() -> codec.decode(tampered, 1L, filters))
				.isInstanceOf(NotificationValidationException.class);
		assertThatThrownBy(() -> codec.decode(cursor, 2L, filters))
				.isInstanceOf(NotificationValidationException.class);
		assertThatThrownBy(() -> codec.decode(cursor, 1L,
				new NotificationFilters(10L, 20L, NotificationType.MENTIONED, false)))
				.isInstanceOf(NotificationValidationException.class);
		assertThatThrownBy(() -> codec.decode(cursor, 1L,
				new NotificationFilters(10L, 21L, NotificationType.MENTIONED, true)))
				.isInstanceOf(NotificationValidationException.class);
	}

	@Test
	void rejectsMissingOrShortSecret() {
		assertThatThrownBy(() -> new NotificationCursorCodec(" ")).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new NotificationCursorCodec("a".repeat(31))).isInstanceOf(IllegalStateException.class);
		assertThat(new NotificationCursorCodec("a".repeat(32))).isNotNull();
	}

	private static NotificationRecipient recipient(Long id, String createdAt) {
		NotificationRecipient recipient = new NotificationRecipient(null, null, null, LocalDateTime.parse(createdAt));
		setId(recipient, id);
		return recipient;
	}

	private static void setId(Object entity, Long id) {
		try {
			Field field = entity.getClass().getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
