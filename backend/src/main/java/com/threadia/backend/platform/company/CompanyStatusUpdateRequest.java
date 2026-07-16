package com.threadia.backend.platform.company;

import com.threadia.backend.company.domain.CompanyStatus;

import jakarta.validation.constraints.NotNull;

public record CompanyStatusUpdateRequest(
		@NotNull CompanyStatus status,
		String reason) {
}
