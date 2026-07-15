package com.threadia.backend.project.exception;

public class ProjectMemberConflictException extends RuntimeException {

	public ProjectMemberConflictException(String message) {
		super(message);
	}

	public static ProjectMemberConflictException lastActiveProjectAdmin() {
		return new ProjectMemberConflictException("마지막 활성 프로젝트 관리자는 변경할 수 없습니다.");
	}

	public static ProjectMemberConflictException targetUserNotActiveCompanyMember() {
		return new ProjectMemberConflictException("대상 사용자가 활성 회사 구성원이 아닙니다.");
	}
}
