package com.threadia.backend.common.exception;

public class UserNotFoundException extends RuntimeException {

	private final String email;

	public UserNotFoundException(String email) {
		super("등록된 사용자를 찾을 수 없습니다.");
		this.email = email;
	}

	public String getEmail() {
		return email;
	}
}
