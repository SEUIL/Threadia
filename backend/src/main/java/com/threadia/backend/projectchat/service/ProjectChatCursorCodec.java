package com.threadia.backend.projectchat.service;

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

import com.threadia.backend.projectchat.domain.Message;
import com.threadia.backend.projectchat.exception.ProjectChatValidationException;

@Component
public class ProjectChatCursorCodec {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final int MINIMUM_SECRET_BYTES = 32;
	private final SecretKeySpec key;

	public ProjectChatCursorCodec(@Value("${threadia.project-chat.cursor-hmac-secret:}") String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"threadia.project-chat.cursor-hmac-secret이 설정되지 않았습니다. THREADIA_PROJECT_CHAT_CURSOR_HMAC_SECRET 환경변수를 확인하세요.");
		}
		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < MINIMUM_SECRET_BYTES) {
			throw new IllegalStateException("threadia.project-chat.cursor-hmac-secret은 UTF-8 기준 32바이트 이상이어야 합니다.");
		}
		this.key = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
	}

	public String encode(Long projectId, Message message) {
		String payload = projectId + ":" + message.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli() + ":" + message.getId();
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
		return encodeBase64(payloadBytes) + "." + encodeBase64(sign(payloadBytes));
	}

	public Cursor decode(String cursor, Long projectId) {
		try {
			String[] values = cursor.split("\\.", -1);
			if (values.length != 2 || values[0].isEmpty() || values[1].isEmpty()) {
				throw ProjectChatValidationException.invalidCursor();
			}
			byte[] payload = Base64.getUrlDecoder().decode(values[0]);
			byte[] signature = Base64.getUrlDecoder().decode(values[1]);
			if (!MessageDigest.isEqual(sign(payload), signature)) {
				throw ProjectChatValidationException.invalidCursor();
			}
			String[] parts = new String(payload, StandardCharsets.UTF_8).split(":", -1);
			if (parts.length != 3 || !projectId.equals(Long.valueOf(parts[0]))) {
				throw ProjectChatValidationException.invalidCursor();
			}
			return new Cursor(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(parts[1])), ZoneOffset.UTC), Long.valueOf(parts[2]));
		} catch (IllegalArgumentException exception) {
			throw ProjectChatValidationException.invalidCursor();
		}
	}

	private byte[] sign(byte[] payload) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(key);
			return mac.doFinal(payload);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("프로젝트 채팅 cursor 서명기를 초기화할 수 없습니다.", exception);
		}
	}

	private static String encodeBase64(byte[] value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	public record Cursor(LocalDateTime createdAt, Long id) {
	}
}
