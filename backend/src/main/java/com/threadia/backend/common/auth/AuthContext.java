package com.threadia.backend.common.auth;

import com.threadia.backend.config.jwt.AuthenticatedUser;

public record AuthContext(Long userId, String email) {

	public static AuthContext from(AuthenticatedUser authenticatedUser) {
		return new AuthContext(authenticatedUser.userId(), authenticatedUser.email());
	}
}
