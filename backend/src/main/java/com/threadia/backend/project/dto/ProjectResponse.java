package com.threadia.backend.project.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.project.domain.Project;

public record ProjectResponse(
		Long id,
		Long companyId,
		String name,
		String projectKey,
		String description,
		String status,
		String startDate,
		String endDate,
		String createdAt,
		String updatedAt) {

	public static ProjectResponse from(Project project) {
		return new ProjectResponse(
				project.getId(),
				project.getCompany().getId(),
				project.getName(),
				project.getProjectKey(),
				project.getDescription(),
				project.getStatus().name(),
				formatDate(project.getStartDate()),
				formatDate(project.getEndDate()),
				formatUtc(project.getCreatedAt()),
				formatUtc(project.getUpdatedAt()));
	}

	private static String formatDate(LocalDate value) {
		return value == null ? null : value.toString();
	}

	private static String formatUtc(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
