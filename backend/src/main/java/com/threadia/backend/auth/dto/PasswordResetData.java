package com.threadia.backend.auth.dto;

public record PasswordResetData(boolean success) {

	public static PasswordResetData ofSuccess() {
		return new PasswordResetData(true);
	}
}
