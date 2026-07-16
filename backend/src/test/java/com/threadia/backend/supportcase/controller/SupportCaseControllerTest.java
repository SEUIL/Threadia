package com.threadia.backend.supportcase.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.supportcase.domain.SupportCaseStatus;
import com.threadia.backend.supportcase.domain.SupportCaseType;
import com.threadia.backend.supportcase.dto.PublicReplyResponse;
import com.threadia.backend.supportcase.dto.SupportCaseDetailData;
import com.threadia.backend.supportcase.dto.SupportCaseResponse;
import com.threadia.backend.supportcase.exception.SupportCaseValidationException;
import com.threadia.backend.supportcase.service.SupportCaseService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(SupportCaseController.class)
class SupportCaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SupportCaseService supportCaseService;

	@Test
	void regularDetailReturnsOnlyCaseAndPublicReplies() throws Exception {
		when(supportCaseService.getOwn("user-token", 10L)).thenReturn(new SupportCaseDetailData(
				supportCaseResponse(),
				List.of(new PublicReplyResponse(
						20L,
						2L,
						PlatformAdminRole.PLATFORM_ADMIN,
						"Public response",
						"2026-07-15T04:00:00Z"))));

		mockMvc.perform(get("/api/v1/support-cases/10")
					.header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.supportCase.id").value(10))
				.andExpect(jsonPath("$.data.publicReplies[0].body").value("Public response"))
				.andExpect(jsonPath("$.data.internalNotes").doesNotExist());
	}

	@Test
	void createRejectsReasonShorterThanThreeCharacters() throws Exception {
		when(supportCaseService.create(
				eq("user-token"),
				argThat(request -> request != null && "x".equals(request.reason()))))
				.thenThrow(new SupportCaseValidationException(
						"audit reason은 공백 제외 3자 이상 500자 이하여야 합니다."));

		mockMvc.perform(post("/api/v1/support-cases")
					.header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "INQUIRY",
							  "title": "Help",
							  "body": "Need help",
							  "reason": "x"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void createAllowsRawReasonLongerThan500WhenTrimmedReasonIsExactly500() throws Exception {
		String reason = "  " + "r".repeat(500) + "  ";
		when(supportCaseService.create(eq("user-token"), any())).thenReturn(new SupportCaseDetailData(
				supportCaseResponse(),
				List.of()));

		mockMvc.perform(post("/api/v1/support-cases")
					.header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "INQUIRY",
							  "title": "Help",
							  "body": "Need help",
							  "reason": "%s"
							}
							""".formatted(reason)))
				.andExpect(status().isCreated());
		verify(supportCaseService).create(
				eq("user-token"),
				argThat(request -> request.reason().equals(reason)));
	}

	@Test
	void listRequiresBearerHeader() throws Exception {
		mockMvc.perform(get("/api/v1/support-cases"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	private static SupportCaseResponse supportCaseResponse() {
		return new SupportCaseResponse(
				10L,
				1L,
				SupportCaseType.INQUIRY,
				SupportCaseStatus.OPEN,
				null,
				null,
				"Help",
				"Need help",
				"2026-07-15T03:00:00Z",
				"2026-07-15T03:00:00Z");
	}
}
