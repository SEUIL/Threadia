package com.threadia.backend.auth.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

public final class LoggingAuthorizationCodeTokenResponseClient
		implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

	private static final Logger log = LoggerFactory.getLogger(LoggingAuthorizationCodeTokenResponseClient.class);
	private static final String OTHER_PROVIDER_ERROR = "other_provider_error";
	private static final Set<String> ALLOWED_PROVIDER_ERROR_CODES = Set.of(
			"invalid_grant",
			"invalid_client",
			"unauthorized_client",
			"invalid_request",
			"unsupported_grant_type",
			"temporarily_unavailable",
			"server_error");

	private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate;

	public LoggingAuthorizationCodeTokenResponseClient(
			OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate) {
		this.delegate = delegate;
	}

	@Override
	public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {
		try {
			return delegate.getTokenResponse(authorizationCodeGrantRequest);
		} catch (OAuth2AuthorizationException exception) {
			log.warn("OAuth2 token exchange failed: providerErrorCategory={}, causeSimpleClassNames={}",
					resolveProviderErrorCategory(exception), resolveCauseSimpleClassNames(exception));
			throw exception;
		}
	}

	static String resolveProviderErrorCategory(OAuth2AuthorizationException exception) {
		String errorCode = exception.getError().getErrorCode();
		return ALLOWED_PROVIDER_ERROR_CODES.contains(errorCode) ? errorCode : OTHER_PROVIDER_ERROR;
	}

	static List<String> resolveCauseSimpleClassNames(Throwable throwable) {
		List<String> causeSimpleClassNames = new ArrayList<>(5);
		Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		Throwable current = throwable.getCause();

		while (current != null && causeSimpleClassNames.size() < 5 && visited.add(current)) {
			causeSimpleClassNames.add(current.getClass().getSimpleName());
			current = current.getCause();
		}

		return List.copyOf(causeSimpleClassNames);
	}
}
