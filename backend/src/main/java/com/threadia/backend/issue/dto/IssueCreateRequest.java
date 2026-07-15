package com.threadia.backend.issue.dto;

import java.time.LocalDate;
import com.threadia.backend.issue.domain.IssuePriority;
import jakarta.validation.constraints.*;

public record IssueCreateRequest(@NotBlank @Size(max = 200) String title, String description, IssuePriority priority, Long assigneeUserId, LocalDate dueDate, Long sprintId) {
	public IssueCreateRequest(String title) { this(title, null, null, null, null, null); }
}
