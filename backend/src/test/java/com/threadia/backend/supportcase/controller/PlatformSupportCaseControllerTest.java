package com.threadia.backend.supportcase.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.supportcase.domain.SupportCaseStatus;
import com.threadia.backend.supportcase.dto.InternalNoteResponse;
import com.threadia.backend.supportcase.dto.InternalNotesData;
import com.threadia.backend.supportcase.dto.PublicReplyResponse;
import com.threadia.backend.supportcase.exception.SupportCaseConflictException;
import com.threadia.backend.supportcase.service.PlatformSupportCaseService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(PlatformSupportCaseController.class)
class PlatformSupportCaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PlatformSupportCaseService supportCaseService;

	@Test
	void regularUserCannotListPlatformSupportCases() throws Exception {
		when(supportCaseService.listAll("regular-token"))
				.thenThrow(new ForbiddenException("플랫폼 관리자 권한이 필요합니다."));

		mockMvc.perform(get("/api/v1/platform/support-cases")
					.header(HttpHeaders.AUTHORIZATION, "Bearer regular-token"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void platformAdminCreatesPublicReply() throws Exception {
		when(supportCaseService.createPublicReply(eq("admin-token"), eq(10L), any()))
				.thenReturn(new PublicReplyResponse(
						20L,
						2L,
						PlatformAdminRole.SUPER_ADMIN,
						"Public response",
						"2026-07-15T04:00:00Z"));

		mockMvc.perform(post("/api/v1/platform/support-cases/10/public-replies")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "body": "Public response",
							  "reason": "Respond to reporter"
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.id").value(20))
				.andExpect(jsonPath("$.data.body").value("Public response"));
	}

	@Test
	void internalNoteRejectsMissingIndependentReason() throws Exception {
		mockMvc.perform(post("/api/v1/platform/support-cases/10/internal-notes")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "body": "Private note"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void platformAdminReadsInternalNotesFromDedicatedEndpoint() throws Exception {
		when(supportCaseService.listInternalNotes("admin-token", 10L))
				.thenReturn(new InternalNotesData(List.of(new InternalNoteResponse(
						30L,
						2L,
						PlatformAdminRole.PLATFORM_ADMIN,
						"Private note",
						"2026-07-15T05:00:00Z"))));

		mockMvc.perform(get("/api/v1/platform/support-cases/10/internal-notes")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.internalNotes[0].body").value("Private note"));
	}

	@Test
	void invalidStatusTransitionReturnsConflict() throws Exception {
		when(supportCaseService.updateStatus(eq("admin-token"), eq(10L), any()))
				.thenThrow(new SupportCaseConflictException("허용되지 않은 support case 상태 전이입니다."));

		mockMvc.perform(patch("/api/v1/platform/support-cases/10/status")
					.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "status": "RESOLVED",
							  "reason": "Resolve immediately"
							}
							"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONFLICT"));
	}
}
