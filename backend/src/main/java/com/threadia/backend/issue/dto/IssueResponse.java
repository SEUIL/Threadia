package com.threadia.backend.issue.dto;

import java.time.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.threadia.backend.issue.domain.Issue;

public record IssueResponse(Long id, String issueKey, String title, String status, String description, String priority, Long assigneeUserId, Long sprintId, LocalDate dueDate, String sourceType, @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC") LocalDateTime trashedAt, @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC") LocalDateTime restoredAt) {
	public IssueResponse(Long id, String issueKey, String title, String status) { this(id, issueKey, title, status, null, "MEDIUM", null, null, null, "MANUAL", null, null); }

	public static IssueResponse from(Issue issue) {
		return new IssueResponse(
				issue.getId(),
				issue.getIssueKey(),
				issue.getTitle(),
			issue.getStatus().name(), issue.getDescription(), issue.getPriority().name(),
			issue.getAssignee() == null ? null : issue.getAssignee().getId(),
			issue.getSprint() == null ? null : issue.getSprint().getId(), issue.getDueDate(), "MANUAL", issue.getTrashedAt(), issue.getRestoredAt());
	}
}
