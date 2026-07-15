package com.threadia.backend.invitation.dto;

import com.threadia.backend.company.domain.CompanyMemberRole;

import jakarta.validation.constraints.NotNull;

public record CompanyInvitationRequest(
		@NotNull Long inviteeUserId,
		@NotNull CompanyMemberRole role) {
}
