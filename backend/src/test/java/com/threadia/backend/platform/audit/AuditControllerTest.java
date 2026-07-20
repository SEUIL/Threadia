package com.threadia.backend.platform.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuditController.class)
class AuditControllerTest {
	@Autowired private MockMvc mockMvc;
	@MockitoBean private AuditReadService auditReadService;

	@Test
	void returnsAuditPageForPlatformAdminRequest() throws Exception {
		when(auditReadService.list(eq("token"), any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(new AuditPageData(List.of(new AuditResponse(1L, null, 2L, null, AuditAuthoritySource.USER, null,
						AdminAction.SUPPORT_CASE_CREATED, AuditTargetType.SUPPORT_CASE, 3L, "reason", null,
						Map.of("status", "OPEN"), "2026-07-18T00:00:00Z")), null, false, 20));

		mockMvc.perform(get("/api/v1/platform/audits").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.audits[0].afterState.status").value("OPEN"));
	}

	@Test
	void invalidCursorUsesValidationError() throws Exception {
		when(auditReadService.list(eq("token"), any(), any(), any(), any(), any(), any(), any(), eq("bad"), any()))
				.thenThrow(new AuditValidationException("audit cursor가 올바르지 않습니다."));

		mockMvc.perform(get("/api/v1/platform/audits?cursor=bad").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}
}
