package com.threadia.backend.auth.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.threadia.backend.auth.service.GoogleOAuthLoginService;
import com.threadia.backend.auth.service.OAuthLoginResult;
import com.threadia.backend.config.AuthOAuthProperties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

	private final GoogleOAuthLoginService googleOAuthLoginService;
	private final AuthOAuthProperties properties;

	public OAuth2LoginSuccessHandler(GoogleOAuthLoginService googleOAuthLoginService, AuthOAuthProperties properties) {
		this.googleOAuthLoginService = googleOAuthLoginService;
		this.properties = properties;
	}

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		try {
			OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
			OAuthLoginResult result = googleOAuthLoginService.login(oauth2User);
			response.addHeader(HttpHeaders.SET_COOKIE, result.refreshCookie().toString());
			response.sendRedirect(properties.getSuccessRedirectUrl());
		} catch (RuntimeException e) {
			log.warn("Google OAuth callback failed");
			response.sendRedirect(properties.getFailureRedirectUrl());
		}
	}
}
