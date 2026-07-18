package com.threadia.backend.supportcase.dto;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.supportcase.domain.SupportCaseInternalNote;

public record InternalNoteResponse(
		Long id,
		Long authorUserId,
		PlatformAdminRole authorRole,
		String body,
		String createdAt) {

	public static InternalNoteResponse from(SupportCaseInternalNote note) {
		return new InternalNoteResponse(
				note.getId(),
				note.getAuthor().getUser().getId(),
				note.getAuthor().getRole(),
				note.getBody(),
				note.getCreatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
	}
}
