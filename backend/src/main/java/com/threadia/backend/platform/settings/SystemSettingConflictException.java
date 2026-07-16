package com.threadia.backend.platform.settings;

public class SystemSettingConflictException extends RuntimeException {
	private final String code;

	public SystemSettingConflictException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
