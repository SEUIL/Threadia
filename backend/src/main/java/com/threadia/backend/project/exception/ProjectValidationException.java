package com.threadia.backend.project.exception;

public class ProjectValidationException extends RuntimeException {

	public ProjectValidationException(String message) {
		super(message);
	}

	public static ProjectValidationException archivedStatusNotAllowed() {
		return new ProjectValidationException("프로젝트 생성 시 ARCHIVED 상태는 사용할 수 없습니다.");
	}

	public static ProjectValidationException invalidDateRange() {
		return new ProjectValidationException("프로젝트 종료일은 시작일보다 빠를 수 없습니다.");
	}
}
