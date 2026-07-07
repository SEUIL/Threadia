package com.threadia.backend.invitation.dto;

public record InvitationResponse(
		Long id,
		String scopeType,
		Long companyId,
		Long projectId,
		Long inviteeUserId,
		String role,
		String status,
		String expiresAt) {
}
