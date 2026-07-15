package com.threadia.backend.invitation.dto;

import com.threadia.backend.project.domain.ProjectMemberRole;

import jakarta.validation.constraints.NotNull;

public record ProjectInvitationRequest(
		@NotNull Long inviteeUserId,
		@NotNull ProjectMemberRole role) {
}
