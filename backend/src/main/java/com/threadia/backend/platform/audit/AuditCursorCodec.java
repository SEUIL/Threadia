package com.threadia.backend.platform.audit;

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

@Component
public class AuditCursorCodec {

	private static final String DOMAIN = "AUDIT";
	private static final String VERSION = "1";
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final int MINIMUM_SECRET_BYTES = 32;
	private final SecretKeySpec key;

	public AuditCursorCodec(@Value("${threadia.audit.cursor-hmac-secret:}") String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("threadia.audit.cursor-hmac-secret이 설정되지 않았습니다.");
		}
		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < MINIMUM_SECRET_BYTES) {
			throw new IllegalStateException("threadia.audit.cursor-hmac-secret은 UTF-8 기준 32바이트 이상이어야 합니다.");
		}
		this.key = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
	}

	public String encode(Long requesterUserId, AuditFilters filters, AdminActionAudit audit) {
		String payload = String.join(":", DOMAIN, VERSION, requesterUserId.toString(), filters.actorUserBinding(),
				filters.actionBinding(), filters.targetTypeBinding(), filters.targetIdBinding(), filters.supportCaseBinding(),
				filters.createdFromBinding(), filters.createdToBinding(),
				Long.toString(audit.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()), audit.getId().toString());
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
		return base64(payloadBytes) + "." + base64(sign(payloadBytes));
	}

	public Cursor decode(String cursor, Long requesterUserId, AuditFilters filters) {
		try {
			String[] values = cursor.split("\\.", -1);
			if (values.length != 2 || values[0].isEmpty() || values[1].isEmpty()) {
				throw invalidCursor();
			}
			byte[] payload = Base64.getUrlDecoder().decode(values[0]);
			if (!MessageDigest.isEqual(sign(payload), Base64.getUrlDecoder().decode(values[1]))) {
				throw invalidCursor();
			}
			String[] parts = new String(payload, StandardCharsets.UTF_8).split(":", -1);
			if (parts.length != 12 || !DOMAIN.equals(parts[0]) || !VERSION.equals(parts[1])
					|| !requesterUserId.equals(Long.valueOf(parts[2]))
					|| !filters.actorUserBinding().equals(parts[3]) || !filters.actionBinding().equals(parts[4])
					|| !filters.targetTypeBinding().equals(parts[5]) || !filters.targetIdBinding().equals(parts[6])
					|| !filters.supportCaseBinding().equals(parts[7]) || !filters.createdFromBinding().equals(parts[8])
					|| !filters.createdToBinding().equals(parts[9])) {
				throw invalidCursor();
			}
			long id = Long.parseLong(parts[11]);
			if (id < 1) {
				throw invalidCursor();
			}
			return new Cursor(LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(parts[10])), ZoneOffset.UTC), id);
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
			throw new IllegalStateException("audit cursor 서명기를 초기화할 수 없습니다.", exception);
		}
	}

	private static String base64(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
	private static AuditValidationException invalidCursor() { return new AuditValidationException("audit cursor가 올바르지 않습니다."); }

	public record Cursor(LocalDateTime createdAt, Long id) {
	}
}
