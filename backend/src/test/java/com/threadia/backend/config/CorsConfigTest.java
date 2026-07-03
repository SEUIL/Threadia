package com.threadia.backend.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.auth.controller.AuthController;
import com.threadia.backend.auth.dto.SignupData;
import com.threadia.backend.auth.service.AuthService;
import com.threadia.backend.common.auth.UserResponse;

/**
 * CorsConfig가 실제로 Spring MVC CORS 처리 경로에 반영되는지 검증한다.
 *
 * <p>{@code @WebMvcTest}는 {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurer}
 * 구현체를 슬라이스 컨텍스트에 자동 포함시키므로, CorsConfig를 mock하지 않고 실제 Bean 그대로 로드해서
 * 진짜 CORS 인터셉터 로직을 태운다. mock으로는 "설정값이 존재한다"만 확인되고 "그 설정이 실제로
 * 요청을 통과/차단시키는지"는 검증되지 않기 때문이다.
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
@TestPropertySource(properties = "threadia.cors.allowed-origin=http://localhost:3000")
class CorsConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void 허용된_origin의_preflight_요청은_CORS_헤더와_함께_통과한다() throws Exception {
		mockMvc.perform(options("/api/v1/auth/login")
						.header(HttpHeaders.ORIGIN, "http://localhost:3000")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	void 허용되지_않은_origin의_preflight_요청은_403으로_거부된다() throws Exception {
		mockMvc.perform(options("/api/v1/auth/login")
						.header(HttpHeaders.ORIGIN, "http://evil.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isForbidden());
	}

	@Test
	void 허용된_origin의_실제_요청_응답에도_CORS_허용_헤더가_붙는다() throws Exception {
		UserResponse user = new UserResponse(1L, "user@example.com", "nick", null);
		when(authService.signup(any())).thenReturn(new SignupData(user));

		mockMvc.perform(post("/api/v1/auth/signup")
						.header(HttpHeaders.ORIGIN, "http://localhost:3000")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@example.com","password":"password1234","nickname":"nick"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	void 허용되지_않은_origin의_실제_요청에는_CORS_허용_헤더가_없다() throws Exception {
		UserResponse user = new UserResponse(1L, "user@example.com", "nick", null);
		when(authService.signup(any())).thenReturn(new SignupData(user));

		mockMvc.perform(post("/api/v1/auth/signup")
						.header(HttpHeaders.ORIGIN, "http://evil.example.com")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@example.com","password":"password1234","nickname":"nick"}
								"""))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}
}
