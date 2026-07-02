package com.threadia.backend.auth;

public record LogoutData(boolean success) {

	public static LogoutData ofSuccess() {
		return new LogoutData(true);
	}
}
