package com.threadia.backend.project.exception;

public class ProjectConflictException extends RuntimeException {

	public ProjectConflictException(String message) {
		super(message);
	}

	public static ProjectConflictException duplicateProjectKey() {
		return new ProjectConflictException("이미 사용 중인 프로젝트 키입니다.");
	}

	public static ProjectConflictException alreadyArchived() {
		return new ProjectConflictException("이미 archive된 프로젝트입니다.");
	}

	public static ProjectConflictException archivedProjectCannotBeUpdated() {
		return new ProjectConflictException("archive된 프로젝트는 수정할 수 없습니다.");
	}
}
