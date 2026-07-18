package com.threadia.backend.platform.audit;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record AuditResponse(
		Long id,
		Long supportCaseId,
		Long actorUserId,
		Long actorPlatformAdminId,
		AuditAuthoritySource authoritySource,
		String authorityRole,
		AdminAction action,
		AuditTargetType targetType,
		Long targetId,
		String reason,
		Map<String, Object> beforeState,
		Map<String, Object> afterState,
		String createdAt) {

	static AuditResponse from(AdminActionAudit audit, Map<String, Object> beforeState, Map<String, Object> afterState) {
		return new AuditResponse(
				audit.getId(), audit.getSupportCase() == null ? null : audit.getSupportCase().getId(), audit.getActor().getId(),
				audit.getActorPlatformAdmin() == null ? null : audit.getActorPlatformAdmin().getId(), audit.getAuthoritySource(),
				audit.getAuthorityRole() == null ? null : audit.getAuthorityRole().name(), audit.getAction(), audit.getTargetType(),
				audit.getTargetId(), audit.getReason(), beforeState, afterState,
				audit.getCreatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
	}
}
