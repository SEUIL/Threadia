package com.threadia.backend.activity.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.threadia.backend.activity.domain.ActivityEvent;

public record ActivityEventResponse(
		Long id,
		String eventType,
		String targetType,
		Long targetId,
		Long actorUserId,
		ActivityEventMetadataResponse metadata,
		LocalDateTime createdAt) {

	public static ActivityEventResponse from(ActivityEvent event) {
		JsonNode metadata = event.getMetadata();
		return new ActivityEventResponse(
				event.getId(),
				event.getEventType().name(),
				event.getTargetType().name(),
				event.getTargetId(),
				event.getActor() == null ? null : event.getActor().getId(),
				new ActivityEventMetadataResponse(
						text(metadata, "issueKey"),
						text(metadata, "title"),
						longValue(metadata, "assigneeUserId"),
						text(metadata, "fromStatus"),
						text(metadata, "toStatus")),
				event.getCreatedAt());
	}

	private static String text(JsonNode metadata, String field) {
		JsonNode value = metadata == null ? null : metadata.get(field);
		return value != null && value.isTextual() ? value.textValue() : null;
	}

	private static Long longValue(JsonNode metadata, String field) {
		JsonNode value = metadata == null ? null : metadata.get(field);
		return value != null && value.canConvertToLong() ? value.longValue() : null;
	}
}
