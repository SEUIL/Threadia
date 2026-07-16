package com.threadia.backend.projectchat.exception;

public class ProjectChatConflictException extends RuntimeException {
	private ProjectChatConflictException(String message) { super(message); }
	public static ProjectChatConflictException threadDeferred() { return new ProjectChatConflictException("메시지 thread 연결은 아직 지원하지 않습니다."); }
}
