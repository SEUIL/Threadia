package com.threadia.backend.projectchat.exception;

public class MessageNotFoundException extends RuntimeException {
	public MessageNotFoundException(Long messageId) {
		super("메시지를 찾을 수 없습니다.");
	}
}
