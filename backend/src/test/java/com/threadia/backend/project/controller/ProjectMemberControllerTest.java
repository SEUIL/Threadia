package com.threadia.backend.project.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.dto.ProjectMemberData;
import com.threadia.backend.project.dto.ProjectMemberRemoveData;
import com.threadia.backend.project.dto.ProjectMemberResponse;
import com.threadia.backend.project.dto.ProjectMembersData;
import com.threadia.backend.project.exception.ProjectMemberConflictException;
import com.threadia.backend.project.exception.ProjectMemberNotFoundException;
import com.threadia.backend.project.service.ProjectMemberService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ProjectMemberController.class)
class ProjectMemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProjectMemberService projectMemberService;

	@Test
	void listReturnsProjectMembersWithNestedUserInfo() throws Exception {
		when(projectMemberService.list("valid-token", 20L, null, null, null))
				.thenReturn(new ProjectMembersData(List.of(projectMemberResponse(200L, "PROJECT_ADMIN", "ACTIVE"))));

		mockMvc.perform(get("/api/v1/projects/20/project-members")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.projectMembers[0].id").value(200))
				.andExpect(jsonPath("$.data.projectMembers[0].projectId").value(20))
				.andExpect(jsonPath("$.data.projectMembers[0].role").value("PROJECT_ADMIN"))
				.andExpect(jsonPath("$.data.projectMembers[0].status").value("ACTIVE"))
				.andExpect(jsonPath("$.data.projectMembers[0].user.email").value("user200@example.com"))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void listPassesQueryParamsToService() throws Exception {
		when(projectMemberService.list("valid-token", 20L, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE, " Admin "))
				.thenReturn(new ProjectMembersData(List.of()));

		mockMvc.perform(get("/api/v1/projects/20/project-members")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.param("role", "PROJECT_ADMIN")
					.param("status", "ACTIVE")
					.param("q", " Admin "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.projectMembers").isArray());

		verify(projectMemberService).list("valid-token", 20L, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE, " Admin ");
	}

	@Test
	void updateReturnsUpdatedProjectMember() throws Exception {
		when(projectMemberService.update(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(200L), org.mockito.ArgumentMatchers.any()))
				.thenReturn(new ProjectMemberData(projectMemberResponse(200L, "MEMBER", "REMOVED")));

		mockMvc.perform(patch("/api/v1/project-members/200")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content(updateBody("MEMBER", "REMOVED")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.projectMember.id").value(200))
				.andExpect(jsonPath("$.data.projectMember.projectId").value(20))
				.andExpect(jsonPath("$.data.projectMember.role").value("MEMBER"))
				.andExpect(jsonPath("$.data.projectMember.status").value("REMOVED"));
	}

	@Test
	void updateReturnsValidationErrorWhenRoleOrStatusIsMissing() throws Exception {
		mockMvc.perform(patch("/api/v1/project-members/200")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void removeReturnsRemovedTrue() throws Exception {
		when(projectMemberService.remove("admin-token", 200L))
				.thenReturn(new ProjectMemberRemoveData(true));

		mockMvc.perform(delete("/api/v1/project-members/200")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.removed").value(true));
	}

	@Test
	void listReturnsForbiddenWhenRequesterCannotViewProjectMembers() throws Exception {
		when(projectMemberService.list("outsider-token", 20L, null, null, null))
				.thenThrow(new ForbiddenException("프로젝트 접근 권한이 없습니다."));

		mockMvc.perform(get("/api/v1/projects/20/project-members")
					.header(HttpHeaders.AUTHORIZATION, "Bearer outsider-token"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void updateReturnsNotFoundWhenProjectMemberDoesNotExist() throws Exception {
		when(projectMemberService.update(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(999L), org.mockito.ArgumentMatchers.any()))
				.thenThrow(new ProjectMemberNotFoundException(999L));

		mockMvc.perform(patch("/api/v1/project-members/999")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content(updateBody("MEMBER", "ACTIVE")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void removeReturnsConflictWhenRemovingLastActiveProjectAdmin() throws Exception {
		when(projectMemberService.remove("admin-token", 200L))
				.thenThrow(ProjectMemberConflictException.lastActiveProjectAdmin());

		mockMvc.perform(delete("/api/v1/project-members/200")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONFLICT"));
	}

	private static ProjectMemberResponse projectMemberResponse(Long id, String role, String status) {
		return new ProjectMemberResponse(
				id,
				20L,
				role,
				status,
				"2026-07-07T00:00:00Z",
				"2026-07-07T00:00:00Z",
				"2026-07-07T00:00:00Z",
				new UserResponse(id, "user" + id + "@example.com", "user" + id, null));
	}

	private static String updateBody(String role, String status) {
		return """
				{
				  "role": "%s",
				  "status": "%s"
				}
				""".formatted(role, status);
	}
}
