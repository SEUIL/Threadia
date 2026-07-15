package com.threadia.backend.sprint.dto;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
public record SprintIssueAssignmentRequest(@NotNull @NotEmpty List<@NotNull Long> issueIds) {}
