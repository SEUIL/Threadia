package com.threadia.backend.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.auth.dto.PasswordResetData;
import com.threadia.backend.auth.service.AuthService;
import com.threadia.backend.auth.service.PasswordResetService;
import com.threadia.backend.common.exception.AuthenticationFailedException;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
class PasswordResetControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private PasswordResetService passwordResetService;

	@Test
	void password_reset_request_성공시_200과_success_data를_반환한다() throws Exception {
		when(passwordResetService.request(any())).thenReturn(PasswordResetData.ofSuccess());

		mockMvc.perform(post("/api/v1/auth/password-reset/request")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"USER@Example.com"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.success").value(true))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void password_reset_request_email이_비면_400_VALIDATION_ERROR_envelope으로_응답한다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/password-reset/request")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":""}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void password_reset_confirm_성공시_200과_success_data를_반환한다() throws Exception {
		when(passwordResetService.confirm(any())).thenReturn(PasswordResetData.ofSuccess());

		mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"token":"reset-token","newPassword":"newpass1234"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.success").value(true))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void password_reset_confirm_token이_유효하지_않으면_401_UNAUTHORIZED_envelope으로_응답한다() throws Exception {
		when(passwordResetService.confirm(any()))
				.thenThrow(new AuthenticationFailedException("password reset token is invalid."));

		mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"token":"bad-token","newPassword":"newpass1234"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void password_reset_confirm_password_규칙을_어기면_400_VALIDATION_ERROR_envelope으로_응답한다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"token":"reset-token","newPassword":"short"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.details.newPassword").exists());
	}
}
