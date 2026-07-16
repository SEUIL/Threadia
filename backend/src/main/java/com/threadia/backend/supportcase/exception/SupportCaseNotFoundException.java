package com.threadia.backend.supportcase.exception;

public class SupportCaseNotFoundException extends RuntimeException {

	public SupportCaseNotFoundException(Long supportCaseId) {
		super("support case를 찾을 수 없습니다: " + supportCaseId);
	}
}
