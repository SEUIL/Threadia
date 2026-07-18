package com.threadia.backend.thread.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.thread.domain.Thread;

public record ThreadResponse(Long id, Long projectId, String targetType, Long targetId, String topic, Long createdByUserId, String createdAt, String updatedAt) {
	public static ThreadResponse from(Thread thread) {
		return new ThreadResponse(thread.getId(), thread.getProject().getId(), thread.getTargetType().name(), thread.getTargetId(), thread.getTopic(), thread.getCreatedBy().getId(), utc(thread.getCreatedAt()), utc(thread.getUpdatedAt()));
	}
	private static String utc(LocalDateTime value) { return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME); }
}
