package com.threadia.backend.activity.dto;

public record ActivityEventMetadataResponse(
		String issueKey,
		String title,
		Long assigneeUserId,
		String fromStatus,
		String toStatus) {
}
