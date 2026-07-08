package com.threadia.backend.project.exception;

public class ProjectConflictException extends RuntimeException {

	public ProjectConflictException(String message) {
		super(message);
	}

	public static ProjectConflictException duplicateProjectKey() {
		return new ProjectConflictException("이미 사용 중인 프로젝트 키입니다.");
	}
}
