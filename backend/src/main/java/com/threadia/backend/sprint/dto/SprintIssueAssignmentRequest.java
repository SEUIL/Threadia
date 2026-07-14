package com.threadia.backend.sprint.dto;
import java.util.List;
import jakarta.validation.constraints.NotNull;
public record SprintIssueAssignmentRequest(@NotNull List<Long> issueIds) {}
