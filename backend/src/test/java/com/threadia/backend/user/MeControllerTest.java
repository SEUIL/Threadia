package com.threadia.backend.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

// GET /auth/me가 API 명세서 13.2의 minimal Auth context(id/email/nickname/avatarUrl)와 token 없음/만료 401 계약을 지키는지 HTTP 계층에서 검증한다.
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(MeController.class)
class MeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MeService meService;

	@Test
	void 유효한_토큰이면_200과_4개_필드만_담은_user_data를_반환한다() throws Exception {
		UserResponse user = new UserResponse(1L, "user@example.com", "nick", null);
		when(meService.getMe("valid-token")).thenReturn(new MeData(user));

		mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.id").value(1))
				.andExpect(jsonPath("$.data.user.email").value("user@example.com"))
				.andExpect(jsonPath("$.data.user.nickname").value("nick"))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void Authorization_헤더가_없으면_401_UNAUTHORIZED_envelope으로_응답한다() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void Bearer_형식이_아니면_401_UNAUTHORIZED_envelope으로_응답한다() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Basic abcdef"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void 만료된_토큰이면_401_UNAUTHORIZED_envelope으로_응답한다() throws Exception {
		when(meService.getMe("expired-token"))
				.thenThrow(new JwtAuthenticationException("access token이 만료되었습니다."));

		mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void 토큰은_유효하지만_사용자가_ACTIVE가_아니면_401_UNAUTHORIZED_envelope으로_응답한다() throws Exception {
		when(meService.getMe(any())).thenThrow(new AuthenticationFailedException("인증에 실패했습니다."));

		mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer suspended-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}
}
