package com.threadia.backend.company.exception;

public class CompanyNotFoundException extends RuntimeException {

	public CompanyNotFoundException(Long companyId) {
		super("회사를 찾을 수 없습니다. companyId=" + companyId);
	}
}
