package com.threadia.backend.thread.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.thread.domain.ThreadComment;

public record ThreadCommentResponse(Long id, Long threadId, Long authorUserId, String content, String createdAt) {
	public static ThreadCommentResponse from(ThreadComment comment) {
		return new ThreadCommentResponse(comment.getId(), comment.getThread().getId(), comment.getAuthor().getId(), comment.getContent(), comment.getCreatedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
	}
}
