package com.threadia.backend.notification.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.threadia.backend.notification.domain.NotificationRecipient;
import com.threadia.backend.notification.exception.NotificationValidationException;

@Component
public class NotificationCursorCodec {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final int MINIMUM_SECRET_BYTES = 32;
	private static final String VERSION = "1";
	private final SecretKeySpec key;

	public NotificationCursorCodec(@Value("${threadia.notification.cursor-hmac-secret:}") String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"threadia.notification.cursor-hmac-secret이 설정되지 않았습니다. THREADIA_NOTIFICATION_CURSOR_HMAC_SECRET 환경변수를 확인하세요.");
		}
		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < MINIMUM_SECRET_BYTES) {
			throw new IllegalStateException("threadia.notification.cursor-hmac-secret은 UTF-8 기준 32바이트 이상이어야 합니다.");
		}
		this.key = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
	}

	public String encode(Long userId, NotificationFilters filters, NotificationRecipient recipient) {
		String payload = String.join(":",
				VERSION,
				userId.toString(),
				filters.companyBinding(),
				filters.projectBinding(),
				filters.typeBinding(),
				filters.unreadBinding(),
				Long.toString(recipient.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()),
				recipient.getId().toString());
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
		return encodeBase64(payloadBytes) + "." + encodeBase64(sign(payloadBytes));
	}

	public Cursor decode(String cursor, Long userId, NotificationFilters filters) {
		try {
			String[] values = cursor.split("\\.", -1);
			if (values.length != 2 || values[0].isEmpty() || values[1].isEmpty()) {
				throw NotificationValidationException.invalidCursor();
			}
			byte[] payload = Base64.getUrlDecoder().decode(values[0]);
			byte[] signature = Base64.getUrlDecoder().decode(values[1]);
			if (!MessageDigest.isEqual(sign(payload), signature)) {
				throw NotificationValidationException.invalidCursor();
			}
			String[] parts = new String(payload, StandardCharsets.UTF_8).split(":", -1);
			if (parts.length != 8
					|| !VERSION.equals(parts[0])
					|| !userId.equals(Long.valueOf(parts[1]))
					|| !filters.companyBinding().equals(parts[2])
					|| !filters.projectBinding().equals(parts[3])
					|| !filters.typeBinding().equals(parts[4])
					|| !filters.unreadBinding().equals(parts[5])) {
				throw NotificationValidationException.invalidCursor();
			}
			return new Cursor(
					LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(parts[6])), ZoneOffset.UTC),
					Long.valueOf(parts[7]));
		} catch (IllegalArgumentException exception) {
			throw NotificationValidationException.invalidCursor();
		}
	}

	private byte[] sign(byte[] payload) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(key);
			return mac.doFinal(payload);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("알림 cursor 서명기를 초기화할 수 없습니다.", exception);
		}
	}

	private static String encodeBase64(byte[] value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	public record Cursor(LocalDateTime createdAt, Long id) {
	}
}
