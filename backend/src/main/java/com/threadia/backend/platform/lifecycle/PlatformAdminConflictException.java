package com.threadia.backend.platform.lifecycle;

public class PlatformAdminConflictException extends RuntimeException {
	private final String code;

	public PlatformAdminConflictException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
