package com.threadia.backend.project.dto;

import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;

import jakarta.validation.constraints.NotNull;

public record ProjectMemberUpdateRequest(
		@NotNull ProjectMemberRole role,
		@NotNull ProjectMemberStatus status) {
}
