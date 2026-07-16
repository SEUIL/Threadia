package com.threadia.backend.auth.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.threadia.backend.config.AuthOAuthProperties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

	private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

	private final AuthOAuthProperties properties;

	public OAuth2LoginFailureHandler(AuthOAuthProperties properties) {
		this.properties = properties;
	}

	@Override
	public void onAuthenticationFailure(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		String logMessage = "Google OAuth authentication failed: " + exception.getClass().getSimpleName();
		if (exception instanceof OAuth2AuthenticationException oauth2AuthenticationException
				&& oauth2AuthenticationException.getError() != null) {
			logMessage += " " + oauth2AuthenticationException.getError().getErrorCode();
		}
		log.warn(logMessage);
		response.sendRedirect(properties.getFailureRedirectUrl());
	}
}
