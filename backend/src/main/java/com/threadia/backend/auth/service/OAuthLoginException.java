package com.threadia.backend.auth.service;

import com.threadia.backend.common.exception.AuthenticationFailedException;

public class OAuthLoginException extends AuthenticationFailedException {

	public OAuthLoginException(String message) {
		super(message);
	}
}
