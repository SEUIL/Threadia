package com.threadia.backend.search.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.search.dto.SearchData;
import com.threadia.backend.search.dto.SearchResult;
import com.threadia.backend.search.service.ProjectSearchService;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ProjectSearchController.class)
class ProjectSearchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProjectSearchService projectSearchService;

	@Test
	void searchReturnsUnifiedResultsEnvelope() throws Exception {
		when(projectSearchService.search("token", 20L, "release", null, null)).thenReturn(new SearchData(
				List.of(new SearchResult(100L, "ISSUE", "릴리스 이슈", "릴리스 설명", LocalDateTime.parse("2026-07-17T00:00:00"))),
				"next-cursor"));

		mockMvc.perform(get("/api/v1/search")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.param("projectId", "20")
					.param("q", "release"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.results[0].type").value("ISSUE"))
				.andExpect(jsonPath("$.data.results[0].id").value(100))
				.andExpect(jsonPath("$.data.results[0].sortAt").value("2026-07-17T00:00:00Z"))
				.andExpect(jsonPath("$.data.nextCursor").value("next-cursor"))
				.andExpect(jsonPath("$.meta.requestId").exists());

		verify(projectSearchService).search("token", 20L, "release", null, null);
	}

	@Test
	void searchRejectsBlankQueryWithValidationError() throws Exception {
		mockMvc.perform(get("/api/v1/search")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.param("projectId", "20")
					.param("q", "   "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void searchRejectsQueryLongerThanOneHundredTrimmedCharacters() throws Exception {
		mockMvc.perform(get("/api/v1/search")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.param("projectId", "20")
					.param("q", " " + "a".repeat(101) + " "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void searchRequiresProjectId() throws Exception {
		mockMvc.perform(get("/api/v1/search")
					.header(HttpHeaders.AUTHORIZATION, "Bearer token")
					.param("q", "release"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}
}
