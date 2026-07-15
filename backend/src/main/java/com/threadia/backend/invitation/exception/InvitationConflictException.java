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

	public static InvitationConflictException alreadyProjectMember() {
		return new InvitationConflictException("ALREADY_PROJECT_MEMBER", "이미 프로젝트 구성원입니다.");
	}

	public static InvitationConflictException targetUserNotActiveCompanyMember() {
		return new InvitationConflictException("TARGET_NOT_ACTIVE_COMPANY_MEMBER", "대상 사용자가 활성 회사 구성원이 아닙니다.");
	}

	public static InvitationConflictException selfInvitation() {
		return new InvitationConflictException("INVITATION_SELF_NOT_ALLOWED", "자기 자신에게 초대할 수 없습니다.");
	}

	public static InvitationConflictException alreadyPending() {
		return new InvitationConflictException("INVITATION_ALREADY_PENDING", "이미 대기 중인 초대가 있습니다.");
	}

	public static InvitationConflictException suspendedCompanyMember() {
		return new InvitationConflictException("COMPANY_MEMBER_SUSPENDED", "정지된 회사 구성원은 초대 수락으로 복구할 수 없습니다.");
	}

	public static InvitationConflictException expired() {
		return new InvitationConflictException("INVITATION_EXPIRED", "초대가 만료되었습니다.");
	}

	public static InvitationConflictException canceled() {
		return new InvitationConflictException("INVITATION_CANCELED", "취소된 초대입니다.");
	}

	public static InvitationConflictException invalidStatus() {
		return new InvitationConflictException("INVITATION_INVALID_STATUS", "처리할 수 없는 초대 상태입니다.");
	}

	public String getCode() {
		return code;
	}
}
