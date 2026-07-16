package com.threadia.backend.projectchat.exception;

public class ProjectChatValidationException extends RuntimeException {
	private ProjectChatValidationException(String message) { super(message); }
	public static ProjectChatValidationException invalidCursor() { return new ProjectChatValidationException("메시지 목록 cursor가 올바르지 않습니다."); }
	public static ProjectChatValidationException invalidLimit() { return new ProjectChatValidationException("메시지 목록 limit은 1 이상 100 이하여야 합니다."); }
}
