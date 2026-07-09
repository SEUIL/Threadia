package com.threadia.backend.company.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.dto.CompanyCreateData;
import com.threadia.backend.company.dto.CompanyData;
import com.threadia.backend.company.dto.CompanyMemberSummaryResponse;
import com.threadia.backend.company.dto.CompanyResponse;
import com.threadia.backend.company.dto.CompanyStatsData;
import com.threadia.backend.company.exception.CompanyNotFoundException;
import com.threadia.backend.company.service.CompanyService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CompanyService companyService;

	@Test
	void createReturnsCreatedCompanyAndAdminMembership() throws Exception {
		CompanyResponse company = companyResponse("Threadia Inc.");
		CompanyMemberSummaryResponse member = new CompanyMemberSummaryResponse(100L, "COMPANY_ADMIN", "ACTIVE");
		when(companyService.create(org.mockito.ArgumentMatchers.eq("valid-token"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(new CompanyCreateData(company, member));

		mockMvc.perform(post("/api/v1/companies")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Threadia Inc.",
							  "representativeName": "Admin",
							  "description": "workspace",
							  "industryCategory": "IT",
							  "logoUrl": null
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.company.id").value(10))
				.andExpect(jsonPath("$.data.company.name").value("Threadia Inc."))
				.andExpect(jsonPath("$.data.companyMember.role").value("COMPANY_ADMIN"))
				.andExpect(jsonPath("$.data.companyMember.status").value("ACTIVE"))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void getReturnsForbiddenWhenRequesterIsNotActiveMember() throws Exception {
		when(companyService.get("member-token", 10L))
				.thenThrow(new ForbiddenException("회사 접근 권한이 없습니다."));

		mockMvc.perform(get("/api/v1/companies/10")
					.header(HttpHeaders.AUTHORIZATION, "Bearer member-token"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void getReturnsNotFoundWhenCompanyDoesNotExist() throws Exception {
		when(companyService.get("valid-token", 9999L))
				.thenThrow(new CompanyNotFoundException(9999L));

		mockMvc.perform(get("/api/v1/companies/9999")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void updateReturnsNotFoundWhenCompanyDoesNotExist() throws Exception {
		when(companyService.update(org.mockito.ArgumentMatchers.eq("admin-token"), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
				.thenThrow(new CompanyNotFoundException(10L));

		mockMvc.perform(patch("/api/v1/companies/10")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Threadia Corp.",
							  "representativeName": "Admin",
							  "description": "updated",
							  "industryCategory": "IT",
							  "logoUrl": "https://example.com/logo.png"
							}
							"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void statsReturnsCompanyCounts() throws Exception {
		when(companyService.stats("valid-token", 10L))
				.thenReturn(new CompanyStatsData(10L, 3L, 2L, 4L, Map.of("ACTIVE", 2L, "PLANNING", 2L)));

		mockMvc.perform(get("/api/v1/companies/10/stats")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.companyId").value(10))
				.andExpect(jsonPath("$.data.memberCount").value(3))
				.andExpect(jsonPath("$.data.departmentCount").value(2))
				.andExpect(jsonPath("$.data.projectCount").value(4))
				.andExpect(jsonPath("$.data.projectStatusCounts.ACTIVE").value(2));
	}

	@Test
	void statsReturnsForbiddenWhenRequesterIsMember() throws Exception {
		when(companyService.stats("member-token", 10L))
				.thenThrow(new ForbiddenException("회사 관리자 권한이 필요합니다."));

		mockMvc.perform(get("/api/v1/companies/10/stats")
					.header(HttpHeaders.AUTHORIZATION, "Bearer member-token"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void updateLogoReturnsUpdatedCompany() throws Exception {
		when(companyService.updateLogo(
						org.mockito.ArgumentMatchers.eq("admin-token"),
						org.mockito.ArgumentMatchers.eq(10L),
						org.mockito.ArgumentMatchers.any()))
				.thenReturn(new CompanyData(companyResponse("Threadia Inc.", "https://example.com/logo.png")));

		mockMvc.perform(post("/api/v1/companies/10/logo")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "logoUrl": "https://example.com/logo.png"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.company.id").value(10))
				.andExpect(jsonPath("$.data.company.logoUrl").value("https://example.com/logo.png"));
	}

	@Test
	void unknownUrlReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/companies/10/unknown")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createReturnsValidationErrorWhenNameIsBlank() throws Exception {
		mockMvc.perform(post("/api/v1/companies")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "",
							  "representativeName": "Admin"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	private static CompanyResponse companyResponse(String name) {
		return companyResponse(name, null);
	}

	private static CompanyResponse companyResponse(String name, String logoUrl) {
		return new CompanyResponse(
				10L,
				name,
				"Admin",
				"workspace",
				"IT",
				logoUrl,
				"ACTIVE",
				"2026-07-07T00:00:00Z",
				"2026-07-07T00:00:00Z");
	}
}
