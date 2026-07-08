package com.threadia.backend.company.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.threadia.backend.company.dto.DepartmentData;
import com.threadia.backend.company.dto.DepartmentDeleteData;
import com.threadia.backend.company.dto.DepartmentResponse;
import com.threadia.backend.company.dto.DepartmentsData;
import com.threadia.backend.company.exception.DepartmentConflictException;
import com.threadia.backend.company.exception.DepartmentNotFoundException;
import com.threadia.backend.company.service.DepartmentService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DepartmentService departmentService;

	@Test
	void listReturnsDepartments() throws Exception {
		when(departmentService.list("admin-token", 10L))
				.thenReturn(new DepartmentsData(List.of(departmentResponse(1L, "디자인팀"))));

		mockMvc.perform(get("/api/v1/companies/10/departments")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.departments[0].id").value(1))
				.andExpect(jsonPath("$.data.departments[0].name").value("디자인팀"))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void createReturnsCreatedDepartment() throws Exception {
		when(departmentService.create(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
				.thenReturn(new DepartmentData(departmentResponse(1L, "디자인팀")));

		mockMvc.perform(post("/api/v1/companies/10/departments")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "name": "디자인팀" }
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.department.id").value(1))
				.andExpect(jsonPath("$.data.department.name").value("디자인팀"));
	}

	@Test
	void updateReturnsConflictWhenNameAlreadyExists() throws Exception {
		when(departmentService.update(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
				.thenThrow(DepartmentConflictException.duplicateName());

		mockMvc.perform(patch("/api/v1/departments/1")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "name": "개발팀" }
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONFLICT"));
	}

	@Test
	void deleteReturnsAffectedMemberCount() throws Exception {
		when(departmentService.delete("admin-token", 1L))
				.thenReturn(new DepartmentDeleteData(true, 3L));

		mockMvc.perform(delete("/api/v1/departments/1")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deleted").value(true))
				.andExpect(jsonPath("$.data.affectedMemberCount").value(3));
	}

	@Test
	void listReturnsForbiddenWhenRequesterIsNotCompanyAdmin() throws Exception {
		when(departmentService.list("member-token", 10L))
				.thenThrow(new ForbiddenException("회사 관리자 권한이 필요합니다."));

		mockMvc.perform(get("/api/v1/companies/10/departments")
					.header(HttpHeaders.AUTHORIZATION, "Bearer member-token"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void deleteReturnsNotFoundWhenDepartmentDoesNotExist() throws Exception {
		when(departmentService.delete("admin-token", 99L))
				.thenThrow(new DepartmentNotFoundException(99L));

		mockMvc.perform(delete("/api/v1/departments/99")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void createReturnsValidationErrorWhenNameIsBlank() throws Exception {
		mockMvc.perform(post("/api/v1/companies/10/departments")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "name": "" }
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	private static DepartmentResponse departmentResponse(Long id, String name) {
		return new DepartmentResponse(
				id,
				10L,
				name,
				"2026-07-07T00:00:00Z",
				"2026-07-07T00:00:00Z");
	}
}
