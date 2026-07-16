package com.threadia.backend.projectchat.controller;

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

import com.threadia.backend.projectchat.dto.MarkReadData;
import com.threadia.backend.projectchat.dto.MessageData;
import com.threadia.backend.projectchat.dto.MessageResponse;
import com.threadia.backend.projectchat.dto.MessagesData;
import com.threadia.backend.projectchat.dto.UnreadCountData;
import com.threadia.backend.projectchat.exception.ProjectChatConflictException;
import com.threadia.backend.projectchat.service.ProjectChatService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ProjectChatController.class)
class ProjectChatControllerTest {
	@Autowired private MockMvc mockMvc;
	@MockitoBean private ProjectChatService projectChatService;

	@Test
	void listUsesMessagesAndNextCursorContract() throws Exception {
		when(projectChatService.list("token", 20L, null, null)).thenReturn(new MessagesData(
				List.of(new MessageResponse(7L, 20L, 1L, "내용", null, "2026-07-15T10:00:00Z")), "opaque-cursor"));
		mockMvc.perform(get("/api/v1/projects/20/messages").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.messages[0].id").value(7))
				.andExpect(jsonPath("$.data.nextCursor").value("opaque-cursor"));
	}

	@Test
	void createReturnsCreatedAndRejectsBlankContent() throws Exception {
		when(projectChatService.create(eq("token"), eq(20L), any())).thenReturn(new MessageData(
				new MessageResponse(7L, 20L, 1L, "내용", null, "2026-07-15T10:00:00Z")));
		mockMvc.perform(post("/api/v1/projects/20/messages").header(HttpHeaders.AUTHORIZATION, "Bearer token")
				.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"내용\",\"threadId\":null}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.data.message.id").value(7));
		mockMvc.perform(post("/api/v1/projects/20/messages").header(HttpHeaders.AUTHORIZATION, "Bearer token")
				.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\" \"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void threadIsClearlyDeferredAndReadEndpointsReturnCalculatedData() throws Exception {
		when(projectChatService.create(eq("token"), eq(20L), any())).thenThrow(ProjectChatConflictException.threadDeferred());
		mockMvc.perform(post("/api/v1/projects/20/messages").header(HttpHeaders.AUTHORIZATION, "Bearer token")
				.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"내용\",\"threadId\":3}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("THREAD_DEFERRED"));
		when(projectChatService.markRead(eq("token"), eq(20L), any())).thenReturn(new MarkReadData(20L, 7L, 2L));
		mockMvc.perform(post("/api/v1/projects/20/messages/mark-read").header(HttpHeaders.AUTHORIZATION, "Bearer token")
				.contentType(MediaType.APPLICATION_JSON).content("{\"lastReadMessageId\":7}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.unreadCount").value(2));
		when(projectChatService.unreadCount("token", 20L)).thenReturn(new UnreadCountData(20L, 2L));
		mockMvc.perform(get("/api/v1/projects/20/messages/unread-count").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.unreadCount").value(2));
	}
}
