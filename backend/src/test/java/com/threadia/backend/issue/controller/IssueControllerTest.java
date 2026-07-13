package com.threadia.backend.issue.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import com.threadia.backend.issue.dto.IssueData;
import com.threadia.backend.issue.dto.IssueResponse;
import com.threadia.backend.issue.dto.IssuesData;
import com.threadia.backend.issue.exception.IssueNotFoundException;
import com.threadia.backend.issue.service.IssueService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(IssueController.class)
class IssueControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IssueService issueService;

	@Test
	void listReturnsActiveIssues() throws Exception {
		when(issueService.list("valid-token", 20L)).thenReturn(new IssuesData(List.of(
				new IssueResponse(100L, "THD-1", "첫 이슈", "BACKLOG"),
				new IssueResponse(101L, "THD-2", "진행 이슈", "IN_PROGRESS"))));

		mockMvc.perform(get("/api/v1/projects/20/issues")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.issues[0].id").value(100))
				.andExpect(jsonPath("$.data.issues[0].issueKey").value("THD-1"))
				.andExpect(jsonPath("$.data.issues[0].title").value("첫 이슈"))
				.andExpect(jsonPath("$.data.issues[0].status").value("BACKLOG"))
				.andExpect(jsonPath("$.data.issues[1].status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void createReturnsCreatedIssue() throws Exception {
		when(issueService.create(eq("valid-token"), eq(20L), any()))
				.thenReturn(new IssueData(new IssueResponse(100L, "THD-1", "새 이슈", "BACKLOG")));

		mockMvc.perform(post("/api/v1/projects/20/issues")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "title": "새 이슈"
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.issue.id").value(100))
				.andExpect(jsonPath("$.data.issue.issueKey").value("THD-1"))
				.andExpect(jsonPath("$.data.issue.status").value("BACKLOG"));
	}

	@Test
	void createReturnsValidationErrorForBlankTitle() throws Exception {
		mockMvc.perform(post("/api/v1/projects/20/issues")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "title": " "
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void createWithCallerSuppliedDoneStillReturnsBacklog() throws Exception {
		when(issueService.create(eq("valid-token"), eq(20L), any()))
				.thenReturn(new IssueData(new IssueResponse(100L, "THD-1", "새 이슈", "BACKLOG")));

		mockMvc.perform(post("/api/v1/projects/20/issues")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "title": "새 이슈",
							  "status": "DONE"
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.issue.status").value("BACKLOG"));
		verify(issueService).create(eq("valid-token"), eq(20L), any());
	}

	@Test
	void updateStatusReturnsChangedIssue() throws Exception {
		when(issueService.updateStatus(eq("valid-token"), eq(100L), any()))
				.thenReturn(new IssueData(new IssueResponse(100L, "THD-1", "첫 이슈", "TODO")));

		mockMvc.perform(patch("/api/v1/issues/100/status")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "status": "TODO" }
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.issue.id").value(100))
				.andExpect(jsonPath("$.data.issue.status").value("TODO"));
	}

	@Test
	void updateStatusReturnsValidationErrorWhenStatusIsMissing() throws Exception {
		mockMvc.perform(patch("/api/v1/issues/100/status")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void updateStatusReturnsForbiddenForRegularMemberMovingToDone() throws Exception {
		when(issueService.updateStatus(eq("member-token"), eq(100L), any()))
				.thenThrow(new ForbiddenException("일반 프로젝트 멤버는 이슈를 DONE 상태로 변경할 수 없습니다."));

		mockMvc.perform(patch("/api/v1/issues/100/status")
					.header(HttpHeaders.AUTHORIZATION, "Bearer member-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "status": "DONE" }
							"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void updateStatusReturnsNotFoundForMissingIssue() throws Exception {
		when(issueService.updateStatus(eq("valid-token"), eq(404L), any()))
				.thenThrow(new IssueNotFoundException(404L));

		mockMvc.perform(patch("/api/v1/issues/404/status")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "status": "TODO" }
							"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}
}
