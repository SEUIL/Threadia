package com.threadia.backend.attachment.dto;

import java.time.LocalDateTime;

import com.threadia.backend.attachment.domain.Attachment;
import com.threadia.backend.attachment.domain.AttachmentStatus;
import com.threadia.backend.attachment.domain.AttachmentTargetType;

public record AttachmentResponse(Long id, AttachmentTargetType targetType, Long targetId, String filename, String contentType,
		long contentLength, AttachmentStatus status, LocalDateTime completedAt, LocalDateTime createdAt) {
	public static AttachmentResponse from(Attachment attachment) {
		return new AttachmentResponse(attachment.getId(), attachment.getTargetType(), attachment.getTargetId(), attachment.getOriginalFilename(),
				attachment.getContentType(), attachment.getContentLength(), attachment.getStatus(), attachment.getCompletedAt(), attachment.getCreatedAt());
	}
}
