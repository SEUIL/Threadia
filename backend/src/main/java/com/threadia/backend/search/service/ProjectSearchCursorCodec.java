package com.threadia.backend.search.service;

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

import com.threadia.backend.project.exception.ProjectValidationException;
import com.threadia.backend.search.repository.ProjectSearchRepository.SearchCursor;
import com.threadia.backend.search.repository.ProjectSearchRepository.SearchRow;

@Component
public class ProjectSearchCursorCodec {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final int MINIMUM_SECRET_BYTES = 32;
	private static final String DOMAIN = "SEARCH";
	private static final String VERSION = "1";

	private final SecretKeySpec key;

	public ProjectSearchCursorCodec(@Value("${threadia.project-chat.cursor-hmac-secret:}") String secret) {
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

	public String encode(Long userId, Long projectId, String normalizedQuery, SearchRow row) {
		String payload = String.join(":",
				DOMAIN,
				VERSION,
				userId.toString(),
				projectId.toString(),
				encodeBase64(normalizedQuery.getBytes(StandardCharsets.UTF_8)),
				Long.toString(row.sortAt().toInstant(ZoneOffset.UTC).toEpochMilli()),
				row.sourceType(),
				row.sourceId().toString());
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
		return encodeBase64(payloadBytes) + "." + encodeBase64(sign(payloadBytes));
	}

	public SearchCursor decode(String cursor, Long userId, Long projectId, String normalizedQuery) {
		try {
			String[] values = cursor.split("\\.", -1);
			if (values.length != 2 || values[0].isEmpty() || values[1].isEmpty()) {
				throw invalidCursor();
			}
			byte[] payload = Base64.getUrlDecoder().decode(values[0]);
			byte[] signature = Base64.getUrlDecoder().decode(values[1]);
			if (!MessageDigest.isEqual(sign(payload), signature)) {
				throw invalidCursor();
			}
			String[] parts = new String(payload, StandardCharsets.UTF_8).split(":", -1);
			if (parts.length != 8
					|| !DOMAIN.equals(parts[0])
					|| !VERSION.equals(parts[1])
					|| !userId.equals(Long.valueOf(parts[2]))
					|| !projectId.equals(Long.valueOf(parts[3]))
					|| !encodeBase64(normalizedQuery.getBytes(StandardCharsets.UTF_8)).equals(parts[4])
					|| !isSourceType(parts[6])) {
				throw invalidCursor();
			}
			long sourceId = Long.parseLong(parts[7]);
			if (sourceId < 1) {
				throw invalidCursor();
			}
			return new SearchCursor(
					LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(parts[5])), ZoneOffset.UTC),
					parts[6],
					sourceId);
		} catch (IllegalArgumentException exception) {
			throw invalidCursor();
		}
	}

	private byte[] sign(byte[] payload) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(key);
			return mac.doFinal(payload);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("검색 cursor 서명기를 초기화할 수 없습니다.", exception);
		}
	}

	private static boolean isSourceType(String value) {
		return value.equals("DOCUMENT") || value.equals("ISSUE") || value.equals("PROJECT_MESSAGE") || value.equals("THREAD");
	}

	private static String encodeBase64(byte[] value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	private static ProjectValidationException invalidCursor() {
		return new ProjectValidationException("cursor 형식이 올바르지 않습니다.");
	}
}
