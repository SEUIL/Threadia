package com.threadia.backend.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.auth.dto.LoginData;
import com.threadia.backend.auth.service.AuthService;
import com.threadia.backend.auth.service.LoginResult;
import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.common.exception.EmailAlreadyExistsException;

// signup/login이 API 명세서 13.2의 status code, 응답 envelope, Set-Cookie header와 실제로 일치하는지 HTTP 계층에서 검증한다.
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void signup_성공시_201과_user_data를_반환한다() throws Exception {
		UserResponse user = new UserResponse(1L, "user@example.com", "nick", null);
		when(authService.signup(any())).thenReturn(new com.threadia.backend.auth.dto.SignupData(user));

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@example.com","password":"password1234","nickname":"nick"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.user.id").value(1))
				.andExpect(jsonPath("$.data.user.email").value("user@example.com"))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void signup_필수값이_비면_400_VALIDATION_ERROR_envelope으로_응답한다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"","password":"","nickname":""}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void signup_avatarUrl이_500자를_넘으면_400_VALIDATION_ERROR_envelope으로_응답한다() throws Exception {
		String tooLongAvatarUrl = "https://example.com/" + "a".repeat(500);

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@example.com","password":"password1234","nickname":"nick","avatarUrl":"%s"}
								""".formatted(tooLongAvatarUrl)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.details.avatarUrl").exists());
	}

	@Test
	void signup_password가_72자를_넘으면_400_VALIDATION_ERROR_envelope으로_응답한다() throws Exception {
		// 영문+숫자 조합을 유지하면서 73자(BCrypt 72byte 상한 초과)를 만든다.
		String tooLongPassword = "a1".repeat(37); // 74자

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@example.com","password":"%s","nickname":"nick"}
								""".formatted(tooLongPassword)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.details.password").exists());
	}

	@Test
	void signup_중복_이메일이면_409_CONFLICT_envelope으로_응답한다() throws Exception {
		when(authService.signup(any())).thenThrow(new EmailAlreadyExistsException("dup@example.com"));

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"dup@example.com","password":"password1234","nickname":"nick"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONFLICT"));
	}

	@Test
	void signup_동시가입_중복키_위반이면_409_CONFLICT_envelope으로_응답한다() throws Exception {
		// existsByEmail 통과 후 save에서 uq_users_email 위반 -> MySQL 1062 -> DataIntegrityViolationException.
		DataIntegrityViolationException duplicate = new DataIntegrityViolationException(
				"duplicate", new SQLException("Duplicate entry", "23000", 1062));
		when(authService.signup(any())).thenThrow(duplicate);

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"race@example.com","password":"password1234","nickname":"nick"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONFLICT"));
	}

	@Test
	void signup_중복키가_아닌_무결성_위반이면_500_INTERNAL_ERROR_envelope으로_응답한다() throws Exception {
		// NOT NULL(1048) 같은 위반은 클라이언트가 고칠 값 문제가 아니므로 409로 위장하지 않고 500으로 흘려보낸다.
		DataIntegrityViolationException notNull = new DataIntegrityViolationException(
				"not null", new SQLException("Column cannot be null", "23000", 1048));
		when(authService.signup(any())).thenThrow(notNull);

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"bug@example.com","password":"password1234","nickname":"nick"}
								"""))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
	}

	@Test
	void login_성공시_200과_accessToken_body_그리고_Set_Cookie로_refreshToken을_내려준다() throws Exception {
		UserResponse user = new UserResponse(1L, "user@example.com", "nick", null);
		LoginData data = LoginData.of("jwt-token", 1800L, user);
		ResponseCookie cookie = ResponseCookie.from("refreshToken", "raw-refresh-token")
				.httpOnly(true)
				.sameSite("Lax")
				.path("/")
				.build();
		when(authService.login(any())).thenReturn(new LoginResult(data, cookie));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@example.com","password":"password1234"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.expiresIn").value(1800))
				.andExpect(jsonPath("$.data.companies").isArray())
				.andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=raw-refresh-token")))
				.andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
				.andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Lax")));
	}

	@Test
	void login_인증_실패시_401_UNAUTHORIZED_envelope으로_응답하고_body에_refreshToken을_노출하지_않는다() throws Exception {
		when(authService.login(any()))
				.thenThrow(new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다."));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@example.com","password":"wrong-password"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
				.andExpect(header().doesNotExist("Set-Cookie"));
	}
}
