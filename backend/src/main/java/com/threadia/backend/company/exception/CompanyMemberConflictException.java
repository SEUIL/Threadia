package com.threadia.backend.company.exception;

public class CompanyMemberConflictException extends RuntimeException {

	public CompanyMemberConflictException(String message) {
		super(message);
	}

	public static CompanyMemberConflictException lastActiveCompanyAdmin() {
		return new CompanyMemberConflictException("마지막 활성 회사 관리자는 변경할 수 없습니다.");
	}

	public static CompanyMemberConflictException leftMemberReactivation() {
		return new CompanyMemberConflictException("탈퇴한 회사 구성원은 직접 재활성화할 수 없습니다.");
	}

	public static CompanyMemberConflictException removalRequiresDelete() {
		return new CompanyMemberConflictException("회사 구성원 제거는 삭제 API를 사용해야 합니다.");
	}

	public static CompanyMemberConflictException alreadyLeft() {
		return new CompanyMemberConflictException("이미 탈퇴한 회사 구성원입니다.");
	}

	public static CompanyMemberConflictException selfRemoval() {
		return new CompanyMemberConflictException("자기 자신은 회사에서 제거할 수 없습니다.");
	}
}
