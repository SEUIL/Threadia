package com.threadia.backend.issue.exception;

public class IssueConflictException extends RuntimeException {

	public IssueConflictException(String message) { super(message); }

	public static IssueConflictException trashed() { return new IssueConflictException("휴지통 이슈는 변경할 수 없습니다."); }
}
