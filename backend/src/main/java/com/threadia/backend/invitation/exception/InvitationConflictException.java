package com.threadia.backend.invitation.exception;

public class InvitationConflictException extends RuntimeException {

	private final String code;

	private InvitationConflictException(String code, String message) {
		super(message);
		this.code = code;
	}

	public static InvitationConflictException alreadyCompanyMember() {
		return new InvitationConflictException("ALREADY_COMPANY_MEMBER", "이미 회사 구성원입니다.");
	}

	public static InvitationConflictException alreadyPending() {
		return new InvitationConflictException("INVITATION_ALREADY_PENDING", "이미 대기 중인 초대가 있습니다.");
	}

	public String getCode() {
		return code;
	}
}
