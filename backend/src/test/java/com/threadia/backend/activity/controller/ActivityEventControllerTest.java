package com.threadia.backend.activity.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.activity.dto.ActivityEventMetadataResponse;
import com.threadia.backend.activity.dto.ActivityEventPageData;
import com.threadia.backend.activity.dto.ActivityEventResponse;
import com.threadia.backend.activity.service.ActivityEventService;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.common.exception.ForbiddenException;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ActivityEventController.class)
class ActivityEventControllerTest {

	@Autowired private MockMvc mockMvc;
	@MockitoBean private ActivityEventService activityEvents;

	@Test
	void listReturnsOffsetPageEnvelope() throws Exception {
		ActivityEventResponse event = new ActivityEventResponse(10L, "ISSUE_CREATED", "ISSUE", 100L, 1L,
				new ActivityEventMetadataResponse("THD-1", "이슈", 2L, null, "BACKLOG"), LocalDateTime.of(2026, 7, 19, 0, 0));
		when(activityEvents.list("token", 20L, 0, 20)).thenReturn(new ActivityEventPageData(List.of(event), 0, 20, 1));

		mockMvc.perform(get("/api/v1/projects/20/activity-events").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.events[0].eventType").value("ISSUE_CREATED"))
				.andExpect(jsonPath("$.data.events[0].metadata.issueKey").value("THD-1"))
				.andExpect(jsonPath("$.data.events[0].metadata.reporterUserId").doesNotExist())
				.andExpect(jsonPath("$.data.page").value(0)).andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.total").value(1));
	}

	@Test
	void listRejectsInvalidPagination() throws Exception {
		mockMvc.perform(get("/api/v1/projects/20/activity-events?page=-1&size=101").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void listReturnsUnauthorizedForMissingBearerToken() throws Exception {
		mockMvc.perform(get("/api/v1/projects/20/activity-events"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void listReturnsForbiddenForUnauthorizedProject() throws Exception {
		when(activityEvents.list(eq("token"), eq(20L), anyInt(), anyInt())).thenThrow(new ForbiddenException("프로젝트 접근 권한이 없습니다."));
		mockMvc.perform(get("/api/v1/projects/20/activity-events").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}
}
