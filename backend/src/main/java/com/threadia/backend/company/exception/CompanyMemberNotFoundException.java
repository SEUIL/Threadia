package com.threadia.backend.company.exception;

public class CompanyMemberNotFoundException extends RuntimeException {

	public CompanyMemberNotFoundException(Long companyMemberId) {
		super("회사 구성원을 찾을 수 없습니다. companyMemberId=" + companyMemberId);
	}
}
