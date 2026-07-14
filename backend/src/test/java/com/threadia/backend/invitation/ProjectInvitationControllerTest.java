package com.threadia.backend.invitation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.invitation.dto.InvitationResponse;
import com.threadia.backend.invitation.dto.ProjectInvitationData;
import com.threadia.backend.invitation.dto.ProjectInvitationRequest;
import com.threadia.backend.invitation.dto.ProjectInvitationsData;
import com.threadia.backend.invitation.exception.InvitationConflictException;
import com.threadia.backend.project.domain.ProjectMemberRole;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ProjectInvitationController.class)
class ProjectInvitationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProjectInvitationService projectInvitationService;

	@Test
	void createProjectInvitationReturnsCreatedInvitation() throws Exception {
		when(projectInvitationService.create("token", 10L, new ProjectInvitationRequest(2L, ProjectMemberRole.MEMBER)))
				.thenReturn(new ProjectInvitationData(invitation(100L, "PENDING")));

		mockMvc.perform(post("/api/v1/projects/10/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"inviteeUserId\":2,\"role\":\"MEMBER\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.invitation.scopeType").value("PROJECT"))
				.andExpect(jsonPath("$.data.invitation.projectId").value(10));
	}

	@Test
	void listProjectInvitationsReturnsProjectScopeOnly() throws Exception {
		when(projectInvitationService.list("token", 10L))
				.thenReturn(new ProjectInvitationsData(List.of(invitation(100L, "PENDING"))));

		mockMvc.perform(get("/api/v1/projects/10/invitations").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.invitations[0].scopeType").value("PROJECT"))
				.andExpect(jsonPath("$.data.invitations[0].projectId").value(10));
	}

	@Test
	void createReturnsForbiddenForNonProjectAdmin() throws Exception {
		when(projectInvitationService.create(any(), any(), any()))
				.thenThrow(new ForbiddenException("프로젝트 관리자 권한이 필요합니다."));

		mockMvc.perform(post("/api/v1/projects/10/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"inviteeUserId\":2,\"role\":\"MEMBER\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void createReturnsConflictForSelfInvitation() throws Exception {
		when(projectInvitationService.create(any(), any(), any()))
				.thenThrow(InvitationConflictException.selfInvitation());

		mockMvc.perform(post("/api/v1/projects/10/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"inviteeUserId\":1,\"role\":\"MEMBER\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("INVITATION_SELF_NOT_ALLOWED"));
	}

	@Test
	void createRejectsMissingAuthenticationAndInvalidRoleOrBody() throws Exception {
		mockMvc.perform(post("/api/v1/projects/10/invitations")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"inviteeUserId\":2,\"role\":\"MEMBER\"}"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/projects/10/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"inviteeUserId\":2,\"role\":\"COMPANY_ADMIN\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/v1/projects/10/invitations")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	private static InvitationResponse invitation(Long id, String status) {
		return new InvitationResponse(
				id, "PROJECT", 1L, "Threadia", 10L, 1L, "admin", 2L,
				"member@example.com", "member", "MEMBER", status, "2026-07-14T10:15:30Z");
	}
}
