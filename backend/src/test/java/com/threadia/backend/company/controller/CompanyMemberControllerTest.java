package com.threadia.backend.company.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.dto.CompanyMemberData;
import com.threadia.backend.company.dto.CompanyMemberResponse;
import com.threadia.backend.company.dto.CompanyMembersData;
import com.threadia.backend.company.exception.CompanyMemberConflictException;
import com.threadia.backend.company.exception.CompanyMemberNotFoundException;
import com.threadia.backend.company.exception.DepartmentNotFoundException;
import com.threadia.backend.company.service.CompanyMemberService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CompanyMemberController.class)
class CompanyMemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CompanyMemberService companyMemberService;

	@Test
	void listReturnsCompanyMembersWithNestedUserInfo() throws Exception {
		when(companyMemberService.list("admin-token", 10L, null, null, null))
				.thenReturn(new CompanyMembersData(List.of(companyMemberResponse(100L, 1L, "COMPANY_ADMIN", "ACTIVE"))));

		mockMvc.perform(get("/api/v1/companies/10/company-members")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.companyMembers[0].id").value(100))
				.andExpect(jsonPath("$.data.companyMembers[0].companyId").value(10))
				.andExpect(jsonPath("$.data.companyMembers[0].departmentId").value(1))
				.andExpect(jsonPath("$.data.companyMembers[0].role").value("COMPANY_ADMIN"))
				.andExpect(jsonPath("$.data.companyMembers[0].status").value("ACTIVE"))
				.andExpect(jsonPath("$.data.companyMembers[0].user.email").value("user100@example.com"))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void listPassesQueryParamsToService() throws Exception {
		when(companyMemberService.list("admin-token", 10L, CompanyMemberStatus.ACTIVE, 5L, " Admin "))
				.thenReturn(new CompanyMembersData(List.of()));

		mockMvc.perform(get("/api/v1/companies/10/company-members")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.param("status", "ACTIVE")
					.param("departmentId", "5")
					.param("q", " Admin "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.companyMembers").isArray());

		verify(companyMemberService).list("admin-token", 10L, CompanyMemberStatus.ACTIVE, 5L, " Admin ");
	}

	@Test
	void updateReturnsUpdatedCompanyMember() throws Exception {
		when(companyMemberService.update(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.any()))
				.thenReturn(new CompanyMemberData(companyMemberResponse(100L, null, "MEMBER", "SUSPENDED")));

		mockMvc.perform(patch("/api/v1/companies/10/company-members/100")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "role": "MEMBER",
							  "status": "SUSPENDED",
							  "departmentId": null
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.companyMember.id").value(100))
				.andExpect(jsonPath("$.data.companyMember.departmentId").doesNotExist())
				.andExpect(jsonPath("$.data.companyMember.role").value("MEMBER"))
				.andExpect(jsonPath("$.data.companyMember.status").value("SUSPENDED"));
	}

	@Test
	void updateReturnsValidationErrorWhenRoleOrStatusIsMissing() throws Exception {
		mockMvc.perform(patch("/api/v1/companies/10/company-members/100")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "departmentId": 1 }
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void listReturnsForbiddenWhenRequesterIsNotCompanyAdmin() throws Exception {
		when(companyMemberService.list("member-token", 10L, null, null, null))
				.thenThrow(new ForbiddenException("회사 관리자 권한이 필요합니다."));

		mockMvc.perform(get("/api/v1/companies/10/company-members")
					.header(HttpHeaders.AUTHORIZATION, "Bearer member-token"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void updateReturnsNotFoundWhenCompanyMemberDoesNotExistInCompanyScope() throws Exception {
		when(companyMemberService.update(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(999L), org.mockito.ArgumentMatchers.any()))
				.thenThrow(new CompanyMemberNotFoundException(999L));

		mockMvc.perform(patch("/api/v1/companies/10/company-members/999")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content(updateBody("MEMBER", "ACTIVE", 1L)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void updateReturnsNotFoundWhenDepartmentDoesNotBelongToCompany() throws Exception {
		when(companyMemberService.update(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.any()))
				.thenThrow(new DepartmentNotFoundException(99L));

		mockMvc.perform(patch("/api/v1/companies/10/company-members/100")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content(updateBody("MEMBER", "ACTIVE", 99L)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void updateReturnsConflictWhenChangingLastActiveCompanyAdmin() throws Exception {
		when(companyMemberService.update(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.any()))
				.thenThrow(CompanyMemberConflictException.lastActiveCompanyAdmin());

		mockMvc.perform(patch("/api/v1/companies/10/company-members/100")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content(updateBody("MEMBER", "ACTIVE", 1L)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONFLICT"));
	}

	private static CompanyMemberResponse companyMemberResponse(Long id, Long departmentId, String role, String status) {
		return new CompanyMemberResponse(
				id,
				10L,
				departmentId,
				role,
				status,
				"2026-07-07T00:00:00Z",
				"2026-07-07T00:00:00Z",
				"2026-07-07T00:00:00Z",
				new UserResponse(id, "user" + id + "@example.com", "user" + id, null));
	}

	private static String updateBody(String role, String status, Long departmentId) {
		return """
				{
				  "role": "%s",
				  "status": "%s",
				  "departmentId": %s
				}
				""".formatted(role, status, departmentId);
	}
}
