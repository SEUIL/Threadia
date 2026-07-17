package com.threadia.backend.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.threadia.backend.search.repository.ProjectSearchRepository.SearchCursor;

@ExtendWith(MockitoExtension.class)
class ProjectSearchRepositoryTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Test
	void searchScopesEverySourceToTheProjectExcludesTrashedRowsAndMatchesIssueKeys() {
		when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<ProjectSearchRepository.SearchRow>>any(), any(Object[].class))).thenReturn(List.of());

		new ProjectSearchRepository(jdbcTemplate).search(20L, "%release%", null, 21);

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object[]> parameters = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(sql.capture(), ArgumentMatchers.<RowMapper<ProjectSearchRepository.SearchRow>>any(), parameters.capture());
		assertThat(sql.getValue())
				.contains("i.project_id = ?", "d.project_id = ?", "t.project_id = ?", "reply.project_id = ?", "m.project_id = ?")
				.contains("i.trashed_at is null", "d.trashed_at is null")
				.contains("i.issue_key like ? escape '\\\\'")
				.contains("select 'PROJECT_MESSAGE' as source_type")
				.contains("or exists (", "reply.thread_id = t.id", "m.thread_id is null")
				.contains("escape '\\\\'")
				.contains("order by sort_at desc, source_type asc, source_id desc");
		assertThat(parameters.getValue()).containsExactly(20L, "%release%", "%release%", "%release%", 20L, "%release%", "%release%", 20L, "%release%", 20L, "%release%", 20L, "%release%", 21);
	}

	@Test
	void searchUsesCursorKeysetPredicateWithStableTieBreakers() {
		when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<ProjectSearchRepository.SearchRow>>any(), any(Object[].class))).thenReturn(List.of());
		SearchCursor cursor = new SearchCursor(LocalDateTime.parse("2026-07-17T04:00:00"), "ISSUE", 100L);

		new ProjectSearchRepository(jdbcTemplate).search(20L, "%release%", cursor, 21);

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).query(sql.capture(), ArgumentMatchers.<RowMapper<ProjectSearchRepository.SearchRow>>any(), any(Object[].class));
		assertThat(sql.getValue())
				.contains("where sort_at < ?")
				.contains("sort_at = ? and source_type > ?")
				.contains("sort_at = ? and source_type = ? and source_id < ?");
	}
}
