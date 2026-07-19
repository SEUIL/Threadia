package com.threadia.backend.issue.dto;

import java.time.LocalDate;

import com.threadia.backend.issue.domain.IssuePriority;

public record IssueUpdateRequest(
		String title,
		String description,
		IssuePriority priority,
		Long assigneeUserId,
		LocalDate dueDate,
		Long sprintId) {
}
