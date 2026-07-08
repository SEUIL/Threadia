package com.threadia.backend.company.exception;

public class DepartmentConflictException extends RuntimeException {

	public DepartmentConflictException(String message) {
		super(message);
	}

	public static DepartmentConflictException duplicateName() {
		return new DepartmentConflictException("이미 존재하는 부서명입니다.");
	}
}
