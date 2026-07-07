package com.threadia.backend.invitation.exception;

public class InvitationNotFoundException extends RuntimeException {

	public InvitationNotFoundException(Long invitationId) {
		super("초대를 찾을 수 없습니다: " + invitationId);
	}
}
