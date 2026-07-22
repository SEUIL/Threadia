package com.threadia.backend.hubpresence.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.hubpresence.dto.HubPresenceOnlineData;
import com.threadia.backend.hubpresence.service.HubPresenceService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(HubPresenceController.class)
class HubPresenceControllerTest {

	@Autowired private MockMvc mockMvc;
	@MockitoBean private HubPresenceService hubPresence;

	@Test
	void heartbeatReturnsOnlineUserIds() throws Exception {
		when(hubPresence.heartbeat("token", 1L)).thenReturn(new HubPresenceOnlineData(List.of(1L, 2L)));

		mockMvc.perform(post("/api/v1/companies/1/hub-presence/heartbeat").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.onlineUserIds[0]").value(1))
				.andExpect(jsonPath("$.data.onlineUserIds[1]").value(2));
	}

	@Test
	void heartbeatReturnsUnauthorizedForMissingBearerToken() throws Exception {
		mockMvc.perform(post("/api/v1/companies/1/hub-presence/heartbeat"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void heartbeatReturnsForbiddenForNonMember() throws Exception {
		when(hubPresence.heartbeat("token", 1L)).thenThrow(new ForbiddenException("회사 접근 권한이 없습니다."));

		mockMvc.perform(post("/api/v1/companies/1/hub-presence/heartbeat").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}
}
