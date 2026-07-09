package com.threadia.backend.project.exception;

public class ProjectMemberConflictException extends RuntimeException {

	public ProjectMemberConflictException(String message) {
		super(message);
	}

	public static ProjectMemberConflictException lastActiveProjectAdmin() {
		return new ProjectMemberConflictException("마지막 활성 프로젝트 관리자는 변경할 수 없습니다.");
	}
}
