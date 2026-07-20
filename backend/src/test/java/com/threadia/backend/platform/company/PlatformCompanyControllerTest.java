package com.threadia.backend.platform.company;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.threadia.backend.company.dto.CompanyResponse;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(PlatformCompanyController.class)
class PlatformCompanyControllerTest {
	@Autowired private MockMvc mockMvc;
	@MockitoBean private PlatformCompanyService companyService;

	@Test
	void returnsCompanyPageInApiResponseEnvelope() throws Exception {
		CompanyResponse company = new CompanyResponse(10L, "Threadia", "대표", null, "IT", null, "ACTIVE", "2026-07-20T00:00:00Z", "2026-07-20T00:00:00Z");
		when(companyService.list(eq("token"), eq("ACTIVE"), eq("Threadia"), eq("cursor"), eq(10)))
				.thenReturn(new PlatformCompanyPageData(List.of(company), "next-cursor", true, 10));

		mockMvc.perform(get("/api/v1/platform/companies?status=ACTIVE&q=Threadia&cursor=cursor&size=10")
				.header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.companies[0].id").value(10))
				.andExpect(jsonPath("$.data.nextCursor").value("next-cursor"))
				.andExpect(jsonPath("$.data.hasNext").value(true))
				.andExpect(jsonPath("$.data.size").value(10));
	}

	@Test
	void validationErrorUsesExistingErrorEnvelope() throws Exception {
		when(companyService.list(eq("token"), any(), any(), eq("bad"), any()))
				.thenThrow(new PlatformCompanyValidationException("company cursor가 올바르지 않습니다."));

		mockMvc.perform(get("/api/v1/platform/companies?cursor=bad").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void missingBearerHeaderIsRejectedBeforeService() throws Exception {
		mockMvc.perform(get("/api/v1/platform/companies"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(companyService);
	}
}
