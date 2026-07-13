package com.threadia.backend.issue.exception;

public class IssueNotFoundException extends RuntimeException {

	public IssueNotFoundException(Long issueId) {
		super("이슈를 찾을 수 없습니다. id=" + issueId);
	}
}
