package com.threadia.backend.user;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.common.exception.UserNotFoundException;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UserLookupController.class)
class UserLookupControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserLookupService userLookupService;

	@Test
	void lookupReturnsRegisteredUserOnly() throws Exception {
		UserResponse user = new UserResponse(2L, "member@example.com", "member", null);
		when(userLookupService.lookupByEmail("  MEMBER@example.com  ")).thenReturn(new UserLookupData(user));

		mockMvc.perform(get("/api/v1/users/lookup").param("email", "  MEMBER@example.com  "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.id").value(2))
				.andExpect(jsonPath("$.data.user.email").value("member@example.com"))
				.andExpect(jsonPath("$.data.user.nickname").value("member"))
				.andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void lookupReturnsNotFoundWhenUserDoesNotExist() throws Exception {
		when(userLookupService.lookupByEmail("ghost@example.com"))
				.thenThrow(new UserNotFoundException("ghost@example.com"));

		mockMvc.perform(get("/api/v1/users/lookup").param("email", "ghost@example.com"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void lookupReturnsBadRequestWhenEmailParameterIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/users/lookup"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}
}
