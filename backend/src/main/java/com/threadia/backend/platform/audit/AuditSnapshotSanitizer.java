package com.threadia.backend.platform.audit;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class AuditSnapshotSanitizer {

	public Map<String, Object> sanitize(AdminAction action, AuditTargetType targetType, Map<String, Object> state) {
		if (state == null) {
			return null;
		}
		return switch (action) {
			case SUPPORT_CASE_CREATED -> copyAllowed(targetType, AuditTargetType.SUPPORT_CASE, state,
					"id", "reporterUserId", "type", "status", "targetType", "targetId", "createdAt", "updatedAt");
			case PUBLIC_REPLY_CREATED -> copyAllowed(targetType, AuditTargetType.PUBLIC_REPLY, state,
					"id", "supportCaseId", "authorPlatformAdminId", "createdAt");
			case INTERNAL_NOTE_CREATED -> copyAllowed(targetType, AuditTargetType.INTERNAL_NOTE, state,
					"id", "supportCaseId", "authorPlatformAdminId", "createdAt");
			case STATUS_CHANGED -> copyAllowed(targetType, AuditTargetType.SUPPORT_CASE, state, "status", "updatedAt");
			case COMPANY_STATUS_CHANGED -> copyAllowed(targetType, AuditTargetType.COMPANY, state, "status");
			case SYSTEM_SETTING_REPLACED -> copyAllowed(targetType, AuditTargetType.SYSTEM_SETTING, state,
					"settingKey", "valueSchemaVersion", "version");
			case PLATFORM_ADMIN_GRANTED, PLATFORM_ADMIN_REVOKED, PLATFORM_ADMIN_REACTIVATED, PLATFORM_ADMIN_ROLE_CHANGED ->
					copyAllowed(targetType, AuditTargetType.PLATFORM_ADMIN, state,
							"id", "userId", "role", "status", "createdAt", "revokedAt", "reactivatedAt", "updatedAt");
		};
	}

	private Map<String, Object> copyAllowed(
			AuditTargetType actualTargetType,
			AuditTargetType expectedTargetType,
			Map<String, Object> state,
			String... allowedFields) {
		if (actualTargetType != expectedTargetType) {
			throw new IllegalArgumentException("audit action과 target type 조합이 올바르지 않습니다.");
		}
		Map<String, Object> sanitized = new LinkedHashMap<>();
		for (String field : allowedFields) {
			if (state.containsKey(field)) {
				sanitized.put(field, state.get(field));
			}
		}
		return sanitized;
	}
}
