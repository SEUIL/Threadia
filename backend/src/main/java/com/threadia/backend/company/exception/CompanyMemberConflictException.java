package com.threadia.backend.company.exception;

public class CompanyMemberConflictException extends RuntimeException {

	public CompanyMemberConflictException(String message) {
		super(message);
	}

	public static CompanyMemberConflictException lastActiveCompanyAdmin() {
		return new CompanyMemberConflictException("마지막 활성 회사 관리자는 변경할 수 없습니다.");
	}
}
