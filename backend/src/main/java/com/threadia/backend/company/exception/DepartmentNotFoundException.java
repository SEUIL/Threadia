package com.threadia.backend.company.exception;

public class DepartmentNotFoundException extends RuntimeException {

	public DepartmentNotFoundException(Long departmentId) {
		super("부서를 찾을 수 없습니다. departmentId=" + departmentId);
	}
}
