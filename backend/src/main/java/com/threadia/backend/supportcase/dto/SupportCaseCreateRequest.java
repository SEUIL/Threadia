package com.threadia.backend.supportcase.dto;

import java.util.UUID;

import com.threadia.backend.supportcase.domain.SupportCaseTargetType;
import com.threadia.backend.supportcase.domain.SupportCaseType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupportCaseCreateRequest(
		@NotNull SupportCaseType type,
		@NotBlank @Size(max = 200) String title,
		@NotBlank String body,
		SupportCaseTargetType targetType,
		UUID targetId,
		@NotBlank String reason) {
}
