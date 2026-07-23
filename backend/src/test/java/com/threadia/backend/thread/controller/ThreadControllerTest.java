package com.threadia.backend.thread.controller;

import static org.mockito.ArgumentMatchers.any;
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

import com.threadia.backend.thread.dto.ThreadAuthorResponse;
import com.threadia.backend.thread.dto.ThreadCommentData;
import com.threadia.backend.thread.dto.ThreadCommentResponse;
import com.threadia.backend.thread.dto.ThreadCommentsData;
import com.threadia.backend.thread.dto.ThreadCreationResult;
import com.threadia.backend.thread.dto.ThreadData;
import com.threadia.backend.thread.dto.ThreadResponse;
import com.threadia.backend.thread.service.ThreadService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ThreadController.class)
class ThreadControllerTest {
	@Autowired private MockMvc mockMvc;
	@MockitoBean private ThreadService threadService;

	@Test
	void getOrCreateReturnsCreatedForNewRepresentativeThread() throws Exception {
		when(threadService.getOrCreate(eq("token"), eq(20L), any())).thenReturn(new ThreadCreationResult(data(), true));
		mockMvc.perform(post("/api/v1/projects/20/threads").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"targetType\":\"ISSUE\",\"targetId\":100,\"topic\":\" 논의 \"}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.data.thread.id").value(10)).andExpect(jsonPath("$.data.thread.createdByUserId").value(1)).andExpect(jsonPath("$.data.thread.targetType").value("ISSUE")).andExpect(jsonPath("$.data.thread.createdAt").value("2026-07-16T10:00:00Z")).andExpect(jsonPath("$.data.thread.author.id").value(1)).andExpect(jsonPath("$.data.thread.author.nickname").value("작성자")).andExpect(jsonPath("$.data.thread.author.avatarUrl").value("https://cdn.threadia.test/avatar.png")).andExpect(jsonPath("$.data.thread.author.email").doesNotExist());
	}

	@Test
	void getOrCreateReturnsOkForExistingRepresentativeThread() throws Exception {
		when(threadService.getOrCreate(eq("token"), eq(20L), any())).thenReturn(new ThreadCreationResult(data(), false));
		mockMvc.perform(post("/api/v1/projects/20/threads").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"targetType\":\"DOCUMENT\",\"targetId\":100}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.thread.id").value(10)).andExpect(jsonPath("$.data.thread.author.id").value(1)).andExpect(jsonPath("$.data.thread.author.nickname").value("작성자")).andExpect(jsonPath("$.data.thread.author.avatarUrl").value("https://cdn.threadia.test/avatar.png")).andExpect(jsonPath("$.data.thread.author.email").doesNotExist());
	}

	@Test
	void lookupRequiresBothTargetParameters() throws Exception {
		mockMvc.perform(get("/api/v1/projects/20/threads?targetType=ISSUE").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void listsAndCreatesAppendOnlyComments() throws Exception {
		when(threadService.listComments("token", 10L)).thenReturn(new ThreadCommentsData(List.of(new ThreadCommentResponse(1L, 10L, 1L, "첫 댓글", "2026-07-16T10:01:00Z", author(1L)))));
		when(threadService.createComment(eq("token"), eq(10L), any())).thenReturn(new ThreadCommentData(new ThreadCommentResponse(2L, 10L, 2L, "둘째 댓글", "2026-07-16T10:02:00Z", author(2L))));
		mockMvc.perform(get("/api/v1/threads/10/comments").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.comments[0].content").value("첫 댓글")).andExpect(jsonPath("$.data.comments[0].author.id").value(1)).andExpect(jsonPath("$.data.comments[0].author.nickname").value("작성자"))
					.andExpect(jsonPath("$.data.comments[0].author.avatarUrl").value("https://cdn.threadia.test/avatar.png")).andExpect(jsonPath("$.data.comments[0].author.email").doesNotExist());
		mockMvc.perform(post("/api/v1/threads/10/comments").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"둘째 댓글\"}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.data.comment.authorUserId").value(2)).andExpect(jsonPath("$.data.comment.author.id").value(2)).andExpect(jsonPath("$.data.comment.author.nickname").value("작성자")).andExpect(jsonPath("$.data.comment.author.avatarUrl").value("https://cdn.threadia.test/avatar.png")).andExpect(jsonPath("$.data.comment.author.email").doesNotExist());
		verify(threadService).createComment(eq("token"), eq(10L), any());
	}

	private static ThreadData data() { return new ThreadData(new ThreadResponse(10L, 20L, "ISSUE", 100L, "논의", 1L, "2026-07-16T10:00:00Z", "2026-07-16T10:00:00Z", author(1L))); }

	private static ThreadAuthorResponse author(long id) { return new ThreadAuthorResponse(id, "작성자", "https://cdn.threadia.test/avatar.png"); }
}
