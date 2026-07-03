package com.threadia.backend.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import com.threadia.backend.config.AuthOAuthProperties;

class OAuth2LoginFailureHandlerTest {

	@Test
	void google_인증_실패시_failure_url로_redirect한다() throws Exception {
		OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler(
				new AuthOAuthProperties("http://localhost:3000/hub", "http://localhost:3000/login?oauth=failed"));
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.onAuthenticationFailure(
				new MockHttpServletRequest(), response, new BadCredentialsException("bad credentials"));

		assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/login?oauth=failed");
	}
}
