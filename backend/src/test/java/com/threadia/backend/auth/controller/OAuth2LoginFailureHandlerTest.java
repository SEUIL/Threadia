package com.threadia.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.threadia.backend.config.AuthOAuthProperties;

class OAuth2LoginFailureHandlerTest {

	@Test
	void google_인증_실패시_failure_url로_redirect한다() throws Exception {
		OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler(
				new AuthOAuthProperties("http://localhost:3000/hub", "http://localhost:3000/login?oauth=failed"));
		Logger logger = (Logger) LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		MockHttpServletResponse response = new MockHttpServletResponse();

		try {
			handler.onAuthenticationFailure(
					new MockHttpServletRequest(),
					response,
					new BadCredentialsException("oauth code=secret-token state=secret-state email=user@example.com"));
		} finally {
			logger.detachAppender(appender);
		}

		assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/login?oauth=failed");
		assertThat(appender.list).hasSize(1);
		ILoggingEvent event = appender.list.get(0);
		assertThat(event.getLevel()).isEqualTo(Level.WARN);
		assertThat(event.getFormattedMessage())
				.isEqualTo("Google OAuth authentication failed: BadCredentialsException");
		assertThat(event.getThrowableProxy()).isNull();
		assertThat(event.getFormattedMessage())
				.doesNotContain("oauth code=secret-token", "state=secret-state", "email=user@example.com");
	}

	@Test
	void oauth2_authentication_exception이면_error_code만_안전하게_기록한다() throws Exception {
		OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler(
				new AuthOAuthProperties("http://localhost:3000/hub", "http://localhost:3000/login?oauth=failed"));
		Logger logger = (Logger) LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		MockHttpServletResponse response = new MockHttpServletResponse();

		try {
			handler.onAuthenticationFailure(
					new MockHttpServletRequest(),
					response,
					new OAuth2AuthenticationException(new OAuth2Error("invalid_token_response")));
		} finally {
			logger.detachAppender(appender);
		}

		assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/login?oauth=failed");
		assertThat(appender.list).hasSize(1);
		ILoggingEvent event = appender.list.get(0);
		assertThat(event.getLevel()).isEqualTo(Level.WARN);
		assertThat(event.getFormattedMessage())
				.isEqualTo("Google OAuth authentication failed: OAuth2AuthenticationException invalid_token_response");
		assertThat(event.getThrowableProxy()).isNull();
		assertThat(event.getFormattedMessage())
				.doesNotContain("description", "uri", "state", "client", "email");
	}
}
