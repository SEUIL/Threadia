package com.threadia.backend.directmessage.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.directmessage.dto.DirectMessageCandidateResponse;
import com.threadia.backend.directmessage.dto.DirectMessageCandidatesData;
import com.threadia.backend.directmessage.dto.DirectMessageData;
import com.threadia.backend.directmessage.dto.DirectMessageReadData;
import com.threadia.backend.directmessage.dto.DirectMessageResponse;
import com.threadia.backend.directmessage.dto.DirectMessageRoomCreationResult;
import com.threadia.backend.directmessage.dto.DirectMessageRoomData;
import com.threadia.backend.directmessage.dto.DirectMessageRoomResponse;
import com.threadia.backend.directmessage.dto.DirectMessageRoomsData;
import com.threadia.backend.directmessage.dto.DirectMessageUnreadCountData;
import com.threadia.backend.directmessage.dto.DirectMessagesData;
import com.threadia.backend.directmessage.dto.DirectMessageSendRequest;
import com.threadia.backend.directmessage.exception.DirectMessageRoomNotFoundException;
import com.threadia.backend.directmessage.service.DirectMessageService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DirectMessageController.class)
class DirectMessageControllerTest {
	@Autowired MockMvc mockMvc;
	@Autowired ObjectMapper objectMapper;
	@MockitoBean DirectMessageService directMessageService;
	@Test void exposesAllSixRestEndpointsWithExpectedEnvelopes() throws Exception {
		DirectMessageRoomResponse room = new DirectMessageRoomResponse(30L, 10L, new UserResponse(2L, "other@example.com", "other", null), 1, "2026-07-15T00:00:00Z");
		when(directMessageService.listRooms("token", 10L)).thenReturn(new DirectMessageRoomsData(List.of(room)));
		when(directMessageService.getOrCreateRoom(eq("token"), eq(10L), any())).thenReturn(new DirectMessageRoomCreationResult(new DirectMessageRoomData(room), true));
		when(directMessageService.listMessages("token", 30L)).thenReturn(new DirectMessagesData(List.of(new DirectMessageResponse(40L, 30L, new UserResponse(2L, "other@example.com", "other", null), "hello", "2026-07-15T00:00:00Z"))));
		when(directMessageService.send(eq("token"), eq(30L), any())).thenReturn(new DirectMessageData(new DirectMessageResponse(41L, 30L, new UserResponse(1L, "me@example.com", "me", null), "reply", "2026-07-15T00:00:01Z")));
		when(directMessageService.markRead(eq("token"), eq(30L), any())).thenReturn(new DirectMessageReadData(30L, 40L, 0, "2026-07-15T00:00:02Z"));
		when(directMessageService.unreadCount("token")).thenReturn(new DirectMessageUnreadCountData(1));
		mockMvc.perform(get("/api/v1/companies/10/direct-messages/rooms").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.rooms[0].otherUser.id").value(2))
				.andExpect(jsonPath("$.data.rooms[0].createdAt").value("2026-07-15T00:00:00Z"))
				.andExpect(jsonPath("$.meta.requestId").isString())
				.andExpect(jsonPath("$.error").doesNotExist());
		mockMvc.perform(post("/api/v1/companies/10/direct-messages/rooms").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"otherUserId\":2}")).andExpect(status().isCreated()).andExpect(jsonPath("$.data.room.id").value(30)).andExpect(jsonPath("$.data.room.createdAt").value("2026-07-15T00:00:00Z"));
		mockMvc.perform(get("/api/v1/direct-messages/rooms/30/messages").header(HttpHeaders.AUTHORIZATION, "Bearer token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.messages[0].content").value("hello")).andExpect(jsonPath("$.data.messages[0].createdAt").value("2026-07-15T00:00:00Z"));
		mockMvc.perform(post("/api/v1/direct-messages/rooms/30/messages").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"reply\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.data.message.id").value(41)).andExpect(jsonPath("$.data.message.createdAt").value("2026-07-15T00:00:01Z"));
		mockMvc.perform(post("/api/v1/direct-messages/rooms/30/mark-read").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"lastReadDirectMessageId\":40}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.lastReadDirectMessageId").value(40)).andExpect(jsonPath("$.data.readAt").value("2026-07-15T00:00:02Z"));
		mockMvc.perform(get("/api/v1/direct-messages/unread-count").header(HttpHeaders.AUTHORIZATION, "Bearer token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.unreadCount").value(1));
	}
	@Test void validatesRequestBodiesAndMapsParticipantRoomToNotFound() throws Exception {
		mockMvc.perform(post("/api/v1/direct-messages/rooms/30/messages").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
		mockMvc.perform(post("/api/v1/direct-messages/rooms/30/mark-read").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"lastReadDirectMessageId\":40,\"readAt\":\"client-time\"}")).andExpect(status().isBadRequest());
		when(directMessageService.listMessages("token", 404L)).thenThrow(new DirectMessageRoomNotFoundException(404L));
		mockMvc.perform(get("/api/v1/direct-messages/rooms/404/messages").header(HttpHeaders.AUTHORIZATION, "Bearer token")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}
	@Test void trimsContentBeforeValidatingTheMaximumLength() throws Exception {
		String normalized = "x".repeat(10000); String padded = " " + normalized + " ";
		when(directMessageService.send(eq("token"), eq(30L), any())).thenReturn(new DirectMessageData(new DirectMessageResponse(41L, 30L, new UserResponse(1L, "me@example.com", "me", null), normalized, "2026-07-15T00:00:01Z")));
		mockMvc.perform(post("/api/v1/direct-messages/rooms/30/messages").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"" + padded + "\"}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.data.message.content").value(normalized));
		ArgumentCaptor<DirectMessageSendRequest> requestCaptor = ArgumentCaptor.forClass(DirectMessageSendRequest.class);
		verify(directMessageService).send(eq("token"), eq(30L), requestCaptor.capture());
		assertThat(requestCaptor.getValue().content()).isEqualTo(normalized);
		mockMvc.perform(post("/api/v1/direct-messages/rooms/30/messages").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"   \"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}
	@Test void listsCandidatesForMemberAndCompanyAdminWithMinimalFields() throws Exception {
		DirectMessageCandidatesData candidates = new DirectMessageCandidatesData(List.of(
				new DirectMessageCandidateResponse(2L, "alpha", "https://cdn.example/avatar.png")));
		when(directMessageService.listCandidates("member-token", 10L)).thenReturn(candidates);
		when(directMessageService.listCandidates("admin-token", 10L)).thenReturn(candidates);

		for (String token : List.of("member-token", "admin-token")) {
			String body = mockMvc.perform(get("/api/v1/companies/10/direct-messages/candidates")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.candidates[0].id").value(2))
					.andExpect(jsonPath("$.data.candidates[0].nickname").value("alpha"))
					.andExpect(jsonPath("$.data.candidates[0].avatarUrl").value("https://cdn.example/avatar.png"))
					.andExpect(jsonPath("$.meta.requestId").isString())
					.andReturn().getResponse().getContentAsString();
			JsonNode item = objectMapper.readTree(body).path("data").path("candidates").get(0);
			Set<String> fields = new java.util.HashSet<>();
			Iterator<String> fieldNames = item.fieldNames();
			fieldNames.forEachRemaining(fields::add);
			assertThat(fields).containsExactlyInAnyOrder("id", "nickname", "avatarUrl");
		}
	}
	@Test void returnsEmptyCandidateList() throws Exception {
		when(directMessageService.listCandidates("token", 10L)).thenReturn(new DirectMessageCandidatesData(List.of()));
		mockMvc.perform(get("/api/v1/companies/10/direct-messages/candidates").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.candidates").isEmpty());
	}
	@Test void rejectsMissingMalformedAndSuspendedCandidateAuthorization() throws Exception {
		mockMvc.perform(get("/api/v1/companies/10/direct-messages/candidates"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
		mockMvc.perform(get("/api/v1/companies/10/direct-messages/candidates").header(HttpHeaders.AUTHORIZATION, "Basic token"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
		when(directMessageService.listCandidates("suspended-token", 10L)).thenThrow(new JwtAuthenticationException("활성 사용자만 접근할 수 있습니다."));
		mockMvc.perform(get("/api/v1/companies/10/direct-messages/candidates").header(HttpHeaders.AUTHORIZATION, "Bearer suspended-token"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}
	@Test void mapsCandidateCompanyAndMembershipFailuresToForbidden() throws Exception {
		when(directMessageService.listCandidates("forbidden-token", 10L)).thenThrow(new ForbiddenException("회사 접근 권한이 없습니다."));
		mockMvc.perform(get("/api/v1/companies/10/direct-messages/candidates").header(HttpHeaders.AUTHORIZATION, "Bearer forbidden-token"))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}
}
