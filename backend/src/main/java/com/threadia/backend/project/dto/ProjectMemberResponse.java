package com.threadia.backend.project.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.project.domain.ProjectMember;

public record ProjectMemberResponse(
		Long id,
		Long projectId,
		String role,
		String status,
		String joinedAt,
		String createdAt,
		String updatedAt,
		UserResponse user) {

	public static ProjectMemberResponse from(ProjectMember projectMember) {
		return new ProjectMemberResponse(
				projectMember.getId(),
				projectMember.getProject().getId(),
				projectMember.getRole().name(),
				projectMember.getStatus().name(),
				formatUtc(projectMember.getJoinedAt()),
				formatUtc(projectMember.getCreatedAt()),
				formatUtc(projectMember.getUpdatedAt()),
				UserResponse.from(projectMember.getUser()));
	}

	private static String formatUtc(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
