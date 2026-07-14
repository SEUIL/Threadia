package com.threadia.backend.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UserProfileController.class)
class UserProfileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MeService meService;

	@Test
	void nickname만_수정하면_200과_기존_UserResponse_shape을_반환한다() throws Exception {
		when(meService.updateProfile(any(), any()))
				.thenReturn(new MeData(new UserResponse(1L, "user@example.com", "new-nick", "https://example.com/a.png")));

		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"new-nick\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.id").value(1))
				.andExpect(jsonPath("$.data.user.email").value("user@example.com"))
				.andExpect(jsonPath("$.data.user.nickname").value("new-nick"))
				.andExpect(jsonPath("$.data.user.avatarUrl").value("https://example.com/a.png"))
				.andExpect(jsonPath("$.meta.requestId").exists());

		verify(meService).updateProfile(any(), any(ProfileUpdateRequest.class));
	}

	@Test
	void avatarUrl만_수정할_수_있다() throws Exception {
		when(meService.updateProfile(any(), any()))
				.thenReturn(new MeData(new UserResponse(1L, "user@example.com", "nick", "https://example.com/new.png")));

		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"avatarUrl\":\"https://example.com/new.png\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.avatarUrl").value("https://example.com/new.png"));
	}

	@Test
	void nickname과_avatarUrl을_함께_수정할_수_있다() throws Exception {
		when(meService.updateProfile(any(), any()))
				.thenReturn(new MeData(new UserResponse(1L, "user@example.com", "new-nick", "https://example.com/new.png")));

		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"new-nick\",\"avatarUrl\":\"https://example.com/new.png\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.nickname").value("new-nick"))
				.andExpect(jsonPath("$.data.user.avatarUrl").value("https://example.com/new.png"));
	}

	@Test
	void avatarUrl에_null을_명시하면_삭제_요청으로_허용한다() throws Exception {
		when(meService.updateProfile(any(), any()))
				.thenReturn(new MeData(new UserResponse(1L, "user@example.com", "nick", null)));

		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"avatarUrl\":null}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.avatarUrl").doesNotExist());
	}

	@Test
	void 빈_객체와_유효하지_않은_nickname은_400_VALIDATION_ERROR를_반환한다() throws Exception {
		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":null}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"a\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"%s\"}".formatted("a".repeat(101))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"avatarUrl\":\"%s\"}".formatted("a".repeat(501))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"valid-name\",\"email\":\"changed@example.com\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void nickname가_공백만_포함하면_400_VALIDATION_ERROR를_반환한다() throws Exception {
		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void Authorization_누락_형식오류_유효하지않은토큰_비활성사용자는_401을_반환한다() throws Exception {
		mockMvc.perform(patch("/api/v1/users/me/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"nick\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Basic invalid")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"nick\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

		when(meService.updateProfile(eq("invalid-token"), any()))
				.thenThrow(new JwtAuthenticationException("유효하지 않은 access token입니다."));
		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"nick\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

		when(meService.updateProfile(eq("suspended-token"), any()))
				.thenThrow(new AuthenticationFailedException("인증에 실패했습니다."));
		mockMvc.perform(patch("/api/v1/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer suspended-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"nick\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}
}
