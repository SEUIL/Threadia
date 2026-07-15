package com.threadia.backend.document.dto;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import com.threadia.backend.document.domain.Document;
public record DocumentResponse(Long id, Long projectId, Long authorUserId, String title, String type, String content, String trashedAt, String restoredAt, String createdAt, String updatedAt) { public static DocumentResponse from(Document d) { return new DocumentResponse(d.getId(), d.getProject().getId(), d.getAuthor().getId(), d.getTitle(), d.getType().name(), d.getContent(), formatUtc(d.getTrashedAt()), formatUtc(d.getRestoredAt()), formatUtc(d.getCreatedAt()), formatUtc(d.getUpdatedAt())); } private static String formatUtc(LocalDateTime value) { return value == null ? null : value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME); } }
