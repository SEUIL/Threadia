package com.threadia.backend.company.dto;

import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;

import jakarta.validation.constraints.NotNull;

public record CompanyMemberUpdateRequest(
		@NotNull CompanyMemberRole role,
		@NotNull CompanyMemberStatus status,
		Long departmentId) {
}
