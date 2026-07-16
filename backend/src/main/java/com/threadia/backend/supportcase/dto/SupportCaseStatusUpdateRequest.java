package com.threadia.backend.supportcase.dto;

import com.threadia.backend.supportcase.domain.SupportCaseStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupportCaseStatusUpdateRequest(
		@NotNull SupportCaseStatus status,
		@NotBlank String reason) {
}
