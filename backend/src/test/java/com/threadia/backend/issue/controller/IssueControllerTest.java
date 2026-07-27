package com.threadia.backend.issue.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
import com.threadia.backend.issue.dto.IssueUpdateRequest;
import com.threadia.backend.issue.dto.IssuesData;
import com.threadia.backend.issue.exception.IssueNotFoundException;
import com.threadia.backend.issue.service.IssueService;
import com.threadia.backend.project.exception.ProjectValidationException;

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

	@Test
	void updateParsesIsoDueDateWithConfiguredObjectMapper() throws Exception {
		when(issueService.update(eq("valid-token"), eq(100L), any(), any()))
				.thenReturn(new IssueData(new IssueResponse(100L, "THD-1", "첫 이슈", "TODO")));

		mockMvc.perform(patch("/api/v1/issues/100")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"dueDate\":\"2026-07-20\"}"))
				.andExpect(status().isOk());

		org.mockito.ArgumentCaptor<IssueUpdateRequest> requestCaptor = org.mockito.ArgumentCaptor.forClass(IssueUpdateRequest.class);
		verify(issueService).update(eq("valid-token"), eq(100L), requestCaptor.capture(), any());
		org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().dueDate()).isEqualTo(LocalDate.parse("2026-07-20"));
	}

	@Test
	void updatePassesExplicitNullSprintIdAndItsFieldPresence() throws Exception {
		when(issueService.update(eq("valid-token"), eq(100L), any(), any()))
				.thenReturn(new IssueData(new IssueResponse(100L, "THD-1", "첫 이슈", "TODO")));

		mockMvc.perform(patch("/api/v1/issues/100")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"sprintId\":null}"))
				.andExpect(status().isOk());

		org.mockito.ArgumentCaptor<IssueUpdateRequest> requestCaptor = org.mockito.ArgumentCaptor.forClass(IssueUpdateRequest.class);
		org.mockito.ArgumentCaptor<java.util.Set<String>> fieldsCaptor = org.mockito.ArgumentCaptor.captor();
		verify(issueService).update(eq("valid-token"), eq(100L), requestCaptor.capture(), fieldsCaptor.capture());
		org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().sprintId()).isNull();
		org.assertj.core.api.Assertions.assertThat(fieldsCaptor.getValue()).containsExactly("sprintId");
	}

	@Test
	void updateRejectsNonNullSprintIdWithValidationError() throws Exception {
		when(issueService.update(eq("valid-token"), eq(100L), any(), any()))
				.thenThrow(new ProjectValidationException("sprintId는 null만 지정할 수 있습니다."));

		mockMvc.perform(patch("/api/v1/issues/100")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"sprintId\":30}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void updateReturnsValidationErrorForMalformedField() throws Exception {
		mockMvc.perform(patch("/api/v1/issues/100")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"dueDate\":\"not-a-date\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		verify(issueService, never()).update(any(), any(), any(), any());
	}

	@Test
	void updateReturnsValidationErrorForUnknownPriority() throws Exception {
		mockMvc.perform(patch("/api/v1/issues/100")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"priority\":\"UNKNOWN\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		verify(issueService, never()).update(any(), any(), any(), any());
	}

	@Test
	void updateReturnsValidationErrorForImmutableField() throws Exception {
		mockMvc.perform(patch("/api/v1/issues/100")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"issueKey\":\"THD-99\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		verify(issueService, never()).update(any(), any(), any(), any());
	}

	@Test
	void issueTimestampSerializesWithUtcZSuffix() throws Exception {
		IssueResponse issue = new IssueResponse(100L, "THD-1", "첫 이슈", "BACKLOG", null, "MEDIUM", null, null, null, "MANUAL", LocalDateTime.of(2026, 7, 14, 1, 2, 3), null);
		when(issueService.detail("valid-token", 100L)).thenReturn(new IssueData(issue));

		mockMvc.perform(get("/api/v1/issues/100").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.issue.trashedAt").value("2026-07-14T01:02:03Z"));
	}

	@Test
	void listPassesFullContractFilters() throws Exception {
		when(issueService.list("valid-token", 20L, com.threadia.backend.issue.domain.IssueStatus.TODO, 30L, 2L, false, true))
				.thenReturn(new IssuesData(List.of()));

		mockMvc.perform(get("/api/v1/projects/20/issues?status=TODO&sprintId=30&assigneeId=2&includeTrashed=true")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk());

		verify(issueService).list("valid-token", 20L, com.threadia.backend.issue.domain.IssueStatus.TODO, 30L, 2L, false, true);
	}

	@Test
	void listPassesUnassignedOnlyFilter() throws Exception {
		when(issueService.list("valid-token", 20L, null, null, null, true, false)).thenReturn(new IssuesData(List.of()));

		mockMvc.perform(get("/api/v1/projects/20/issues?unassignedOnly=true")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk());

		verify(issueService).list("valid-token", 20L, null, null, null, true, false);
	}

	@Test
	void listReturnsValidationErrorWhenUnassignedOnlyAndSprintIdAreCombined() throws Exception {
		when(issueService.list("valid-token", 20L, null, 30L, null, true, false))
				.thenThrow(new ProjectValidationException("unassignedOnly와 sprintId는 함께 사용할 수 없습니다."));

		mockMvc.perform(get("/api/v1/projects/20/issues?unassignedOnly=true&sprintId=30")
					.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		verify(issueService).list("valid-token", 20L, null, 30L, null, true, false);
	}

	@Test
	void trashAndRestoreReturnIssueData() throws Exception {
		IssueData data = new IssueData(new IssueResponse(100L, "THD-1", "첫 이슈", "BACKLOG"));
		when(issueService.trash("valid-token", 100L)).thenReturn(data);
		when(issueService.restore("valid-token", 100L)).thenReturn(data);

		mockMvc.perform(post("/api/v1/issues/100/trash").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.issue.id").value(100));
		mockMvc.perform(post("/api/v1/issues/100/restore").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.issue.id").value(100));
	}
}
