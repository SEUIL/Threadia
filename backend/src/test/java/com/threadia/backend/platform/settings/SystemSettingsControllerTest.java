package com.threadia.backend.platform.settings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadia.backend.common.exception.ForbiddenException;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(SystemSettingsController.class)
class SystemSettingsControllerTest {
	@Autowired private MockMvc mockMvc;
	@MockitoBean private SystemSettingsService settingsService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void platformAdminReadsCatalog() throws Exception {
		when(settingsService.list("platform-token")).thenReturn(new SystemSettingsData(List.of(
				new SystemSettingResponse(1L, SystemSettingKey.SUPPORT_CENTER_STATUS, objectMapper.readTree("{\"status\":\"OPEN\",\"message\":\"정상\"}"), 1, 1L, "2026-07-16T00:00:00Z", "2026-07-16T00:00:00Z"))));

		mockMvc.perform(get("/api/v1/platform/system-settings").header(HttpHeaders.AUTHORIZATION, "Bearer platform-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.settings[0].key").value("SUPPORT_CENTER_STATUS"));
	}

	@Test
	void platformAdminReadsOneCatalogValue() throws Exception {
		when(settingsService.get("platform-token", SystemSettingKey.SUPPORT_CENTER_STATUS))
				.thenReturn(new SystemSettingResponse(1L, SystemSettingKey.SUPPORT_CENTER_STATUS, objectMapper.readTree("{\"status\":\"OPEN\",\"message\":\"정상\"}"), 1, 1L, "2026-07-16T00:00:00Z", "2026-07-16T00:00:00Z"));

		mockMvc.perform(get("/api/v1/platform/system-settings/SUPPORT_CENTER_STATUS").header(HttpHeaders.AUTHORIZATION, "Bearer platform-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.key").value("SUPPORT_CENTER_STATUS"));
	}

	@Test
	void nonPlatformAdminCannotReadOneCatalogValue() throws Exception {
		when(settingsService.get("regular-token", SystemSettingKey.SUPPORT_CENTER_STATUS))
				.thenThrow(new ForbiddenException("플랫폼 관리자 권한이 필요합니다."));

		mockMvc.perform(get("/api/v1/platform/system-settings/SUPPORT_CENTER_STATUS").header(HttpHeaders.AUTHORIZATION, "Bearer regular-token"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void platformAdminWriteIsForbidden() throws Exception {
		when(settingsService.replace(eq("platform-token"), eq(SystemSettingKey.SUPPORT_CENTER_STATUS), any()))
				.thenThrow(new ForbiddenException("SUPER_ADMIN 권한이 필요합니다."));

		mockMvc.perform(put("/api/v1/platform/system-settings/SUPPORT_CENTER_STATUS")
					.header(HttpHeaders.AUTHORIZATION, "Bearer platform-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"value\":{\"status\":\"OPEN\",\"message\":\"정상\"},\"expectedVersion\":1,\"reason\":\"운영 상태 유지\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void staleVersionAndNoChangeUseDistinctConflictCodes() throws Exception {
		when(settingsService.replace(eq("super-token"), eq(SystemSettingKey.SUPPORT_CENTER_STATUS), any()))
				.thenThrow(new SystemSettingConflictException("SYSTEM_SETTING_VERSION_CONFLICT", "stale"));
		mockMvc.perform(put("/api/v1/platform/system-settings/SUPPORT_CENTER_STATUS")
					.header(HttpHeaders.AUTHORIZATION, "Bearer super-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"value\":{\"status\":\"OPEN\",\"message\":\"정상\"},\"expectedVersion\":1,\"reason\":\"운영 상태 유지\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("SYSTEM_SETTING_VERSION_CONFLICT"));
	}

	@Test
	void invalidRequestIsValidationError() throws Exception {
		mockMvc.perform(put("/api/v1/platform/system-settings/SUPPORT_CENTER_STATUS")
					.header(HttpHeaders.AUTHORIZATION, "Bearer super-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"value\":{},\"expectedVersion\":0,\"reason\":\"x\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void acceptsRawReasonOver500WhenTrimmedReasonIsExactly500() throws Exception {
		String trimmedReason = "r".repeat(500);
		when(settingsService.replace(eq("super-token"), eq(SystemSettingKey.SUPPORT_CENTER_STATUS), argThat(request -> request.reason().trim().equals(trimmedReason))))
				.thenReturn(new SystemSettingResponse(1L, SystemSettingKey.SUPPORT_CENTER_STATUS, objectMapper.readTree("{\"status\":\"OPEN\",\"message\":\"정상\"}"), 1, 2L, "2026-07-16T00:00:00Z", "2026-07-16T00:00:00Z"));

		mockMvc.perform(put("/api/v1/platform/system-settings/SUPPORT_CENTER_STATUS")
					.header(HttpHeaders.AUTHORIZATION, "Bearer super-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"value\":{\"status\":\"OPEN\",\"message\":\"정상\"},\"expectedVersion\":1,\"reason\":\" " + trimmedReason + " \"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.version").value(2));
	}
}
