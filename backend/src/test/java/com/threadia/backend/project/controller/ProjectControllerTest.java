package com.threadia.backend.project.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.threadia.backend.project.dto.ProjectCreateData;
import com.threadia.backend.project.dto.ProjectData;
import com.threadia.backend.project.dto.ProjectMemberSummaryResponse;
import com.threadia.backend.project.dto.ProjectResponse;
import com.threadia.backend.project.dto.ProjectsData;
import com.threadia.backend.project.exception.ProjectConflictException;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.service.ProjectService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProjectService projectService;

	@Test
	void listReturnsProjects() throws Exception {
		when(projectService.list("valid-token", 10L))
				.thenReturn(new ProjectsData(List.of(projectResponse(20L, "Threadia MVP", "THD"))));

		mockMvc.perform(get("/api/v1/companies/10/projects")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.projects[0].id").value(20))
				.andExpect(jsonPath("$.data.projects[0].companyId").value(10))
				.andExpect(jsonPath("$.data.projects[0].name").value("Threadia MVP"))
				.andExpect(jsonPath("$.data.projects[0].projectKey").value("THD"))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void createReturnsCreatedProjectAndAdminMembership() throws Exception {
		ProjectMemberSummaryResponse member = new ProjectMemberSummaryResponse(200L, "PROJECT_ADMIN", "ACTIVE");
		when(projectService.create(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
				.thenReturn(new ProjectCreateData(projectResponse(20L, "Threadia MVP", "THD"), member));

		mockMvc.perform(post("/api/v1/companies/10/projects")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Threadia MVP",
							  "projectKey": "THD",
							  "description": "Threadia MVP 개발 프로젝트",
							  "status": "PLANNING",
							  "startDate": "2026-06-17",
							  "endDate": "2026-07-31"
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.project.id").value(20))
				.andExpect(jsonPath("$.data.project.projectKey").value("THD"))
				.andExpect(jsonPath("$.data.projectMember.role").value("PROJECT_ADMIN"))
				.andExpect(jsonPath("$.data.projectMember.status").value("ACTIVE"));
	}

	@Test
	void createReturnsForbiddenWhenRequesterIsNotCompanyAdmin() throws Exception {
		when(projectService.create(org.mockito.ArgumentMatchers.eq("member-token"), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
				.thenThrow(new ForbiddenException("회사 관리자 권한이 필요합니다."));

		mockMvc.perform(post("/api/v1/companies/10/projects")
					.header(HttpHeaders.AUTHORIZATION, "Bearer member-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Threadia MVP",
							  "projectKey": "THD"
							}
							"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void createReturnsConflictWhenProjectKeyAlreadyExists() throws Exception {
		when(projectService.create(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
				.thenThrow(ProjectConflictException.duplicateProjectKey());

		mockMvc.perform(post("/api/v1/companies/10/projects")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Threadia MVP",
							  "projectKey": "THD"
							}
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONFLICT"));
	}

	@Test
	void createReturnsValidationErrorWhenNameIsBlank() throws Exception {
		mockMvc.perform(post("/api/v1/companies/10/projects")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "",
							  "projectKey": "THD"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void createReturnsValidationErrorWhenStatusIsArchived() throws Exception {
		mockMvc.perform(post("/api/v1/companies/10/projects")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Threadia MVP",
							  "projectKey": "THD",
							  "status": "ARCHIVED"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void createReturnsValidationErrorWhenEndDateIsBeforeStartDate() throws Exception {
		mockMvc.perform(post("/api/v1/companies/10/projects")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Threadia MVP",
							  "projectKey": "THD",
							  "startDate": "2026-07-31",
							  "endDate": "2026-06-17"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void detailReturnsProject() throws Exception {
		when(projectService.detail("valid-token", 20L))
				.thenReturn(new ProjectData(projectResponse(20L, "Threadia MVP", "THD")));

		mockMvc.perform(get("/api/v1/projects/20")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.project.id").value(20))
				.andExpect(jsonPath("$.data.project.companyId").value(10))
				.andExpect(jsonPath("$.data.project.name").value("Threadia MVP"))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void detailReturnsNotFoundWhenProjectDoesNotExist() throws Exception {
		when(projectService.detail("valid-token", 404L)).thenThrow(new ProjectNotFoundException(404L));

		mockMvc.perform(get("/api/v1/projects/404")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void detailReturnsForbiddenWhenRequesterCannotViewProject() throws Exception {
		when(projectService.detail("outsider-token", 20L)).thenThrow(new ForbiddenException("프로젝트 접근 권한이 없습니다."));

		mockMvc.perform(get("/api/v1/projects/20")
					.header(HttpHeaders.AUTHORIZATION, "Bearer outsider-token"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void updateReturnsUpdatedProject() throws Exception {
		when(projectService.update(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.any()))
				.thenReturn(new ProjectData(projectResponse(20L, "Threadia MVP Sprint 1", "THD", "ACTIVE")));

		mockMvc.perform(patch("/api/v1/projects/20")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Threadia MVP Sprint 1",
							  "description": "Sprint 1 범위로 조정된 프로젝트",
							  "status": "ACTIVE",
							  "startDate": "2026-06-17",
							  "endDate": "2026-07-31"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.project.id").value(20))
				.andExpect(jsonPath("$.data.project.name").value("Threadia MVP Sprint 1"))
				.andExpect(jsonPath("$.data.project.status").value("ACTIVE"));
	}

	@Test
	void updateReturnsForbiddenWhenRequesterCannotManageProject() throws Exception {
		when(projectService.update(org.mockito.ArgumentMatchers.eq("member-token"), org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.any()))
				.thenThrow(new ForbiddenException("프로젝트 관리자 권한이 필요합니다."));

		mockMvc.perform(patch("/api/v1/projects/20")
					.header(HttpHeaders.AUTHORIZATION, "Bearer member-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Threadia MVP",
							  "status": "ACTIVE"
							}
							"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void updateReturnsValidationErrorWhenStatusIsArchived() throws Exception {
		mockMvc.perform(patch("/api/v1/projects/20")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Threadia MVP",
							  "status": "ARCHIVED"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void updateReturnsValidationErrorWhenEndDateIsBeforeStartDate() throws Exception {
		mockMvc.perform(patch("/api/v1/projects/20")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Threadia MVP",
							  "status": "ACTIVE",
							  "startDate": "2026-07-31",
							  "endDate": "2026-06-17"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void updateReturnsConflictWhenProjectAlreadyArchived() throws Exception {
		when(projectService.update(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.any()))
				.thenThrow(ProjectConflictException.archivedProjectCannotBeUpdated());

		mockMvc.perform(patch("/api/v1/projects/20")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Threadia MVP",
							  "status": "ACTIVE"
							}
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONFLICT"));
	}

	private static ProjectResponse projectResponse(Long id, String name, String projectKey) {
		return projectResponse(id, name, projectKey, "PLANNING");
	}

	private static ProjectResponse projectResponse(Long id, String name, String projectKey, String status) {
		return new ProjectResponse(
				id,
				10L,
				name,
				projectKey,
				"Threadia MVP 개발 프로젝트",
				status,
				"2026-06-17",
				"2026-07-31",
				"2026-07-07T00:00:00Z",
				"2026-07-07T00:00:00Z");
	}
}
