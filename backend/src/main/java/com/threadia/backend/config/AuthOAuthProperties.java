package com.threadia.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthOAuthProperties {

	private final String successRedirectUrl;
	private final String failureRedirectUrl;

	public AuthOAuthProperties(
			@Value("${threadia.oauth.success-redirect-url:http://localhost:3000/hub}") String successRedirectUrl,
			@Value("${threadia.oauth.failure-redirect-url:http://localhost:3000/login?oauth=failed}") String failureRedirectUrl) {
		this.successRedirectUrl = successRedirectUrl;
		this.failureRedirectUrl = failureRedirectUrl;
	}

	public String getSuccessRedirectUrl() {
		return successRedirectUrl;
	}

	public String getFailureRedirectUrl() {
		return failureRedirectUrl;
	}
}
