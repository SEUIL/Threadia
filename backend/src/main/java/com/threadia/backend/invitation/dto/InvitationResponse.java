package com.threadia.backend.invitation.dto;

public record InvitationResponse(
		Long id,
		String scopeType,
		Long companyId,
		String companyName,
		Long projectId,
		/**
		 * PROJECT scope 표시용 프로젝트 이름. COMPANY scope면 null이다.
		 * 초대 대상자는 아직 프로젝트 member가 아니라 프로젝트 상세를 조회할 수 없으므로
		 * (403), 수신 화면이 어느 프로젝트 초대인지 보여주려면 여기 담아 내려줘야 한다.
		 */
		String projectName,
		Long inviterUserId,
		String inviterNickname,
		Long inviteeUserId,
		String inviteeEmail,
		String inviteeNickname,
		String role,
		String status,
		String expiresAt) {
}
