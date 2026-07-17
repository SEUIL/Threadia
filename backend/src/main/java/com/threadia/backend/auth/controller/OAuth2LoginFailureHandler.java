package com.threadia.backend.auth.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.threadia.backend.config.AuthOAuthProperties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

	private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);
	private static final int MAX_CAUSE_DEPTH = 10;
	private static final String OTHER_PROVIDER_ERROR = "other_provider_error";
	private static final Set<String> ALLOWED_PROVIDER_ERROR_CODES = Set.of(
			"invalid_grant",
			"invalid_client",
			"unauthorized_client",
			"invalid_request",
			"unsupported_grant_type",
			"temporarily_unavailable",
			"server_error");

	private final AuthOAuthProperties properties;

	public OAuth2LoginFailureHandler(AuthOAuthProperties properties) {
		this.properties = properties;
	}

	@Override
	public void onAuthenticationFailure(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		String wrapperClass = exception.getClass().getSimpleName();
		String wrapperErrorCode = resolveWrapperErrorCode(exception);
		String providerErrorCategory = resolveProviderErrorCategory(exception);
		if (wrapperErrorCode == null) {
			log.warn("Google OAuth authentication failed: wrapperClass={}, providerErrorCategory={}",
					wrapperClass, providerErrorCategory);
		} else {
			log.warn("Google OAuth authentication failed: wrapperClass={}, wrapperErrorCode={}, providerErrorCategory={}",
					wrapperClass, wrapperErrorCode, providerErrorCategory);
		}
		response.sendRedirect(properties.getFailureRedirectUrl());
	}

	static String resolveProviderErrorCategory(Throwable exception) {
		OAuth2AuthorizationException authorizationException = findOAuth2AuthorizationException(exception);
		if (authorizationException == null) {
			return OTHER_PROVIDER_ERROR;
		}

		OAuth2Error error = authorizationException.getError();
		String errorCode = error.getErrorCode();
		return ALLOWED_PROVIDER_ERROR_CODES.contains(errorCode) ? errorCode : OTHER_PROVIDER_ERROR;
	}

	private static String resolveWrapperErrorCode(AuthenticationException exception) {
		if (exception instanceof OAuth2AuthenticationException oauth2AuthenticationException
				&& oauth2AuthenticationException.getError() != null) {
			return oauth2AuthenticationException.getError().getErrorCode();
		}
		return null;
	}

	private static OAuth2AuthorizationException findOAuth2AuthorizationException(Throwable exception) {
		Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		Throwable current = exception;
		for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH && seen.add(current); depth++) {
			if (current instanceof OAuth2AuthorizationException oauth2AuthorizationException) {
				return oauth2AuthorizationException;
			}
			current = current.getCause();
		}
		return null;
	}
}
