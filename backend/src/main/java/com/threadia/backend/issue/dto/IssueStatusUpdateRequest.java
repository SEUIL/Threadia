package com.threadia.backend.issue.dto;

import com.threadia.backend.issue.domain.IssueStatus;

import jakarta.validation.constraints.NotNull;

public record IssueStatusUpdateRequest(@NotNull IssueStatus status) {
}
