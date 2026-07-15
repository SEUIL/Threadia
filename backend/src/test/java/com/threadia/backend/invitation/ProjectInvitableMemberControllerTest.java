package com.threadia.backend.invitation;

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
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.common.exception.UserNotFoundException;
import com.threadia.backend.invitation.dto.ProjectInvitableMemberData;
import com.threadia.backend.invitation.exception.InvitationConflictException;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ProjectInvitableMemberController.class)
class ProjectInvitableMemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProjectInvitationService projectInvitationService;

	@Test
	void findInvitableMemberReturnsMinimalUserResponse() throws Exception {
		UserResponse user = new UserResponse(2L, "member@example.com", "member", null);
		when(projectInvitationService.findInvitableMember("token", 10L, "  MEMBER@example.com  "))
				.thenReturn(new ProjectInvitableMemberData(user));

		mockMvc.perform(get("/api/v1/projects/10/invitable-members")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.param("email", "  MEMBER@example.com  "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.id").value(2))
				.andExpect(jsonPath("$.data.user.email").value("member@example.com"))
				.andExpect(jsonPath("$.data.user.nickname").value("member"))
				.andExpect(jsonPath("$.data.user.avatarUrl").isEmpty())
				.andExpect(jsonPath("$.data.user.status").doesNotExist())
				.andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.data.user.createdAt").doesNotExist())
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void findInvitableMemberReturnsBadRequestWhenEmailIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/projects/10/invitable-members")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void findInvitableMemberReturnsUnauthorizedWhenAuthorizationIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/projects/10/invitable-members")
					.param("email", "member@example.com"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void findInvitableMemberReturnsForbiddenForNonProjectManager() throws Exception {
		when(projectInvitationService.findInvitableMember("token", 10L, "member@example.com"))
				.thenThrow(new ForbiddenException("프로젝트 관리자 권한이 필요합니다."));

		mockMvc.perform(get("/api/v1/projects/10/invitable-members")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.param("email", "member@example.com"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void findInvitableMemberReturnsNotFoundForMissingUser() throws Exception {
		when(projectInvitationService.findInvitableMember("token", 10L, "ghost@example.com"))
				.thenThrow(new UserNotFoundException("ghost@example.com"));

		mockMvc.perform(get("/api/v1/projects/10/invitable-members")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.param("email", "ghost@example.com"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void findInvitableMemberReturnsCanonicalConflictForIneligibleUser() throws Exception {
		when(projectInvitationService.findInvitableMember("token", 10L, "member@example.com"))
				.thenThrow(InvitationConflictException.alreadyPending());

		mockMvc.perform(get("/api/v1/projects/10/invitable-members")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.param("email", "member@example.com"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("INVITATION_ALREADY_PENDING"));
	}
}
