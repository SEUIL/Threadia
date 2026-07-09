package com.threadia.backend.project.exception;

public class ProjectMemberNotFoundException extends RuntimeException {

	public ProjectMemberNotFoundException(Long projectMemberId) {
		super("프로젝트 멤버를 찾을 수 없습니다. id=" + projectMemberId);
	}
}
