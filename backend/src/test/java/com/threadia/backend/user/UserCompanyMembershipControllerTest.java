package com.threadia.backend.user;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UserCompanyMembershipController.class)
class UserCompanyMembershipControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserCompanyMembershipService userCompanyMembershipService;

	@Test
	void 유효한_토큰이면_ACTIVE_회사_소속_목록을_반환한다() throws Exception {
		when(userCompanyMembershipService.list("valid-token"))
				.thenReturn(new UserCompanyMembershipsData(List.of(
						new UserCompanyMembershipResponse(101L, 10L, "Threadia", "COMPANY_ADMIN", "ACTIVE"))));

		mockMvc.perform(get("/api/v1/users/me/company-memberships")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.companyMemberships[0].companyMemberId").value(101))
				.andExpect(jsonPath("$.data.companyMemberships[0].companyId").value(10))
				.andExpect(jsonPath("$.data.companyMemberships[0].companyName").value("Threadia"))
				.andExpect(jsonPath("$.data.companyMemberships[0].role").value("COMPANY_ADMIN"))
				.andExpect(jsonPath("$.data.companyMemberships[0].status").value("ACTIVE"))
				.andExpect(jsonPath("$.data.companyMemberships[0].id").doesNotExist())
				.andExpect(jsonPath("$.data.companyMemberships[0].user").doesNotExist())
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void 소속_회사가_없으면_빈_배열을_반환한다() throws Exception {
		when(userCompanyMembershipService.list("valid-token"))
				.thenReturn(new UserCompanyMembershipsData(List.of()));

		mockMvc.perform(get("/api/v1/users/me/company-memberships")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.companyMemberships").isArray())
				.andExpect(jsonPath("$.data.companyMemberships").isEmpty());
	}

	@Test
	void Authorization_헤더가_없으면_401_UNAUTHORIZED_envelope으로_응답한다() throws Exception {
		mockMvc.perform(get("/api/v1/users/me/company-memberships"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void Bearer_형식이_아니면_401_UNAUTHORIZED_envelope으로_응답한다() throws Exception {
		mockMvc.perform(get("/api/v1/users/me/company-memberships")
					.header(HttpHeaders.AUTHORIZATION, "Basic abcdef"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void 만료되거나_유효하지_않은_토큰이면_서비스_예외를_401로_전파한다() throws Exception {
		when(userCompanyMembershipService.list("expired-token"))
				.thenThrow(new JwtAuthenticationException("access token이 만료되었습니다."));

		mockMvc.perform(get("/api/v1/users/me/company-memberships")
					.header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void 토큰은_유효하지만_사용자가_ACTIVE가_아니면_서비스_예외를_401로_전파한다() throws Exception {
		when(userCompanyMembershipService.list("suspended-token"))
				.thenThrow(new AuthenticationFailedException("인증에 실패했습니다."));

		mockMvc.perform(get("/api/v1/users/me/company-memberships")
					.header(HttpHeaders.AUTHORIZATION, "Bearer suspended-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}
}
