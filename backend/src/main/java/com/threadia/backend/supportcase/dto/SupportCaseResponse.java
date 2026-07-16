package com.threadia.backend.supportcase.dto;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.threadia.backend.supportcase.domain.SupportCase;
import com.threadia.backend.supportcase.domain.SupportCaseStatus;
import com.threadia.backend.supportcase.domain.SupportCaseTargetType;
import com.threadia.backend.supportcase.domain.SupportCaseType;

public record SupportCaseResponse(
		Long id,
		Long reporterUserId,
		SupportCaseType type,
		SupportCaseStatus status,
		SupportCaseTargetType targetType,
		UUID targetId,
		String title,
		String body,
		String createdAt,
		String updatedAt) {

	public static SupportCaseResponse from(SupportCase supportCase) {
		return new SupportCaseResponse(
				supportCase.getId(),
				supportCase.getReporter().getId(),
				supportCase.getType(),
				supportCase.getStatus(),
				supportCase.getTargetType(),
				supportCase.getTargetId(),
				supportCase.getTitle(),
				supportCase.getBody(),
				formatUtc(supportCase.getCreatedAt()),
				formatUtc(supportCase.getUpdatedAt()));
	}

	private static String formatUtc(java.time.LocalDateTime value) {
		return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
