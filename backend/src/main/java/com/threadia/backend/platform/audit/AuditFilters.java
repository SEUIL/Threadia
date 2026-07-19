package com.threadia.backend.platform.audit;

import java.time.LocalDateTime;

public record AuditFilters(
		Long actorUserId,
		AdminAction action,
		AuditTargetType targetType,
		Long targetId,
		Long supportCaseId,
		LocalDateTime createdFrom,
		LocalDateTime createdTo) {

	String actorUserBinding() { return binding(actorUserId); }
	String actionBinding() { return action == null ? "-" : action.name(); }
	String targetTypeBinding() { return targetType == null ? "-" : targetType.name(); }
	String targetIdBinding() { return binding(targetId); }
	String supportCaseBinding() { return binding(supportCaseId); }
	String createdFromBinding() { return timeBinding(createdFrom); }
	String createdToBinding() { return timeBinding(createdTo); }

	private static String binding(Long value) { return value == null ? "-" : value.toString(); }
	private static String timeBinding(LocalDateTime value) {
		return value == null ? "-" : Long.toString(value.toInstant(java.time.ZoneOffset.UTC).toEpochMilli());
	}
}
