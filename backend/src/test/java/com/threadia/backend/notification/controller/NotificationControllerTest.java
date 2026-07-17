package com.threadia.backend.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.threadia.backend.notification.dto.NotificationPageData;
import com.threadia.backend.notification.dto.NotificationReadAllData;
import com.threadia.backend.notification.dto.NotificationReadData;
import com.threadia.backend.notification.dto.NotificationResponse;
import com.threadia.backend.notification.dto.NotificationTargetResponse;
import com.threadia.backend.notification.dto.NotificationUnreadCountData;
import com.threadia.backend.notification.exception.NotificationRecipientNotFoundException;
import com.threadia.backend.notification.exception.NotificationValidationException;
import com.threadia.backend.notification.service.NotificationService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

	@Autowired private MockMvc mockMvc;
	@MockitoBean private NotificationService notificationService;

	@Test
	void listUsesDedicatedTopLevelPageEnvelopeAndForwardsFilters() throws Exception {
		NotificationResponse notification = new NotificationResponse(
				8000L,
				8100L,
				"MENTIONED",
				"새 멘션이 있습니다.",
				"본문",
				null,
				new NotificationTargetResponse("THREAD", 6000L),
				"2026-07-17T10:00:00Z");
		when(notificationService.list("token", 10L, 20L, "MENTIONED", true, null, 20))
				.thenReturn(new NotificationPageData(List.of(notification), "opaque-cursor", true, 20));

		mockMvc.perform(get("/api/v1/notifications")
				.header(HttpHeaders.AUTHORIZATION, "Bearer token")
				.param("companyId", "10")
				.param("projectId", "20")
				.param("type", "MENTIONED")
				.param("unreadOnly", "true")
				.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].notificationRecipientId").value(8000))
				.andExpect(jsonPath("$.data[0].target.type").value("THREAD"))
				.andExpect(jsonPath("$.page.cursor").value("opaque-cursor"))
				.andExpect(jsonPath("$.page.hasNext").value(true))
				.andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$.meta.requestId").isNotEmpty());
	}

	@Test
	void unreadReadAndReadAllUseExistingSuccessEnvelope() throws Exception {
		when(notificationService.unreadCount("token", null, null, null))
				.thenReturn(new NotificationUnreadCountData(3L));
		mockMvc.perform(get("/api/v1/notifications/unread-count")
				.header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.unreadCount").value(3));

		when(notificationService.markRead("token", 8000L))
				.thenReturn(new NotificationReadData(8000L, "2026-07-17T10:05:00Z"));
		mockMvc.perform(post("/api/v1/notifications/8000/read")
				.header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.notificationRecipientId").value(8000))
				.andExpect(jsonPath("$.data.readAt").value("2026-07-17T10:05:00Z"));

		when(notificationService.markAllRead(eq("token"), any())).thenReturn(new NotificationReadAllData(5));
		mockMvc.perform(post("/api/v1/notifications/read-all")
				.header(HttpHeaders.AUTHORIZATION, "Bearer token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"type\":\"MENTIONED\",\"companyId\":10,\"projectId\":20}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.updatedCount").value(5));
	}

	@Test
	void mapsNotificationValidationAndNotFoundErrors() throws Exception {
		when(notificationService.list("token", null, null, "UNKNOWN", null, null, null))
				.thenThrow(NotificationValidationException.invalidType());
		mockMvc.perform(get("/api/v1/notifications")
				.header(HttpHeaders.AUTHORIZATION, "Bearer token")
				.param("type", "UNKNOWN"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		when(notificationService.markRead("token", 9999L))
				.thenThrow(new NotificationRecipientNotFoundException(9999L));
		mockMvc.perform(post("/api/v1/notifications/9999/read")
				.header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void rejectsMissingBearerHeaderBeforeCallingService() throws Exception {
		mockMvc.perform(get("/api/v1/notifications"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}
}
