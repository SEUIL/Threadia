package com.threadia.backend.sprint.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.threadia.backend.sprint.dto.*;
import com.threadia.backend.sprint.service.SprintService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(SprintController.class)
class SprintControllerTest {
	@Autowired private MockMvc mockMvc;
	@MockitoBean private SprintService service;
	private static SprintData sprint(String status) { return new SprintData(new SprintResponse(30L,20L,"Sprint 1",null,status,LocalDate.parse("2026-07-01"),LocalDate.parse("2026-07-14"),null,null,null)); }

	@Test void createReturnsPlannedSprint() throws Exception {
		when(service.create(eq("token"),eq(20L),any())).thenReturn(sprint("PLANNED"));
		mockMvc.perform(post("/api/v1/projects/20/sprints").header(HttpHeaders.AUTHORIZATION,"Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Sprint 1\"}"))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.data.sprint.status").value("PLANNED"));
	}
	@Test void startAndCompleteExposeLifecycleEndpoints() throws Exception {
		when(service.start("token",30L)).thenReturn(sprint("ACTIVE")); when(service.complete(eq("token"),eq(30L),any())).thenReturn(sprint("COMPLETED"));
		mockMvc.perform(post("/api/v1/sprints/30/start").header(HttpHeaders.AUTHORIZATION,"Bearer token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.sprint.status").value("ACTIVE"));
		mockMvc.perform(post("/api/v1/sprints/30/complete").header(HttpHeaders.AUTHORIZATION,"Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"summary\":\"done\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.sprint.status").value("COMPLETED"));
	}
	@Test void assignmentRequiresIssueIds() throws Exception {
		mockMvc.perform(post("/api/v1/sprints/30/issues").header(HttpHeaders.AUTHORIZATION,"Bearer token").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}
	@Test void sprintTimestampsSerializeWithUtcZSuffix() throws Exception {
		SprintData response = new SprintData(new SprintResponse(30L,20L,"Sprint 1",null,"COMPLETED",LocalDate.parse("2026-07-01"),LocalDate.parse("2026-07-14"),LocalDateTime.of(2026, 7, 1, 9, 0),LocalDateTime.of(2026, 7, 14, 18, 0),"done"));
		when(service.detail("token",30L)).thenReturn(response);
		mockMvc.perform(get("/api/v1/sprints/30").header(HttpHeaders.AUTHORIZATION,"Bearer token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sprint.startedAt").value("2026-07-01T09:00:00Z"))
				.andExpect(jsonPath("$.data.sprint.completedAt").value("2026-07-14T18:00:00Z"));
	}
}
