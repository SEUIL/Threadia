package com.threadia.backend.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.threadia.backend.auth.dto.LoginData;
import com.threadia.backend.auth.service.GoogleOAuthLoginService;
import com.threadia.backend.auth.service.OAuthLoginResult;
import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.config.AuthOAuthProperties;

class OAuth2LoginSuccessHandlerTest {

	@Test
	void 성공하면_refresh_cookie를_내리고_frontend_success_url로_redirect한다() throws Exception {
		GoogleOAuthLoginService service = org.mockito.Mockito.mock(GoogleOAuthLoginService.class);
		ResponseCookie cookie = ResponseCookie.from("refreshToken", "raw-token")
				.httpOnly(true)
				.sameSite("Lax")
				.path("/")
				.build();
		LoginData data = LoginData.of("access-token", 1800L, new UserResponse(1L, "user@example.com", "nick", null));
		when(service.login(any())).thenReturn(new OAuthLoginResult(data, cookie));
		OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(
				service, new AuthOAuthProperties("http://localhost:3000/hub", "http://localhost:3000/login?oauth=failed"));

		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, new TestingAuthenticationToken(googleUser(), null));

		assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/hub");
		assertThat(response.getHeader("Set-Cookie")).contains("refreshToken=raw-token", "HttpOnly", "SameSite=Lax");
		verify(service).login(any());
	}

	@Test
	void service가_실패하면_failure_url로_redirect한다() throws Exception {
		GoogleOAuthLoginService service = org.mockito.Mockito.mock(GoogleOAuthLoginService.class);
		when(service.login(any())).thenThrow(new IllegalStateException("failed"));
		OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(
				service, new AuthOAuthProperties("http://localhost:3000/hub", "http://localhost:3000/login?oauth=failed"));

		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, new TestingAuthenticationToken(googleUser(), null));

		assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/login?oauth=failed");
		assertThat(response.getHeader("Set-Cookie")).isNull();
	}

	private static OAuth2User googleUser() {
		return new DefaultOAuth2User(java.util.List.of(), Map.of("sub", "google-sub"), "sub");
	}

	private static org.assertj.core.api.AbstractStringAssert<?> assertThat(String actual) {
		return org.assertj.core.api.Assertions.assertThat(actual);
	}
}
