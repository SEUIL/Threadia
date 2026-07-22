package com.threadia.backend.issue.dto;

import java.time.LocalDate;

import com.threadia.backend.issue.domain.IssuePriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IssueFromMessageRequest(
		@NotNull Long messageId,
		@NotBlank @Size(max = 200) String title,
		String description,
		IssuePriority priority,
		Long assigneeUserId,
		LocalDate dueDate,
		Long sprintId) {
}
