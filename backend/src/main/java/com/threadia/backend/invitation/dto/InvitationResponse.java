package com.threadia.backend.invitation.dto;

public record InvitationResponse(
		Long id,
		String scopeType,
		Long companyId,
		String companyName,
		Long projectId,
		Long inviterUserId,
		String inviterNickname,
		Long inviteeUserId,
		String inviteeEmail,
		String inviteeNickname,
		String role,
		String status,
		String expiresAt) {
}
