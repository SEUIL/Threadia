package com.threadia.backend.supportcase.dto;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.supportcase.domain.SupportCasePublicReply;

public record PublicReplyResponse(
		Long id,
		Long authorUserId,
		PlatformAdminRole authorRole,
		String body,
		String createdAt) {

	public static PublicReplyResponse from(SupportCasePublicReply reply) {
		return new PublicReplyResponse(
				reply.getId(),
				reply.getAuthor().getUser().getId(),
				reply.getAuthor().getRole(),
				reply.getBody(),
				reply.getCreatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
	}
}
