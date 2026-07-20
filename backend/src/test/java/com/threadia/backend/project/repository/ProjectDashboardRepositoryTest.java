package com.threadia.backend.project.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.threadia.backend.project.dto.ProjectDashboardData;

@ExtendWith(MockitoExtension.class)
class ProjectDashboardRepositoryTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Test
	void loadUsesProjectScopedQueriesWithNonTrashedRowsStableOrderingAndBoundLimits() {
		when(jdbcTemplate.queryForObject(
				anyString(),
				ArgumentMatchers.<RowMapper<ProjectDashboardData.Progress>>any(),
				any(Object[].class)))
				.thenReturn(new ProjectDashboardData.Progress(0, 0, 0));
		when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Object>>any(), any(Object[].class)))
				.thenReturn(List.of());

		ProjectDashboardData data = new ProjectDashboardRepository(jdbcTemplate).load(20L);

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object[]> parameters = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).queryForObject(sql.capture(), ArgumentMatchers.<RowMapper<ProjectDashboardData.Progress>>any(), parameters.capture());
		assertThat(sql.getValue())
				.contains("project_id = ?", "trashed_at is null", "status = 'DONE'")
				.doesNotContain("for update");
		assertThat(parameters.getValue()).containsExactly(20L);
		assertThat(data.progress()).isEqualTo(new ProjectDashboardData.Progress(0, 0, 0));

		ArgumentCaptor<String> recentSql = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object[]> recentParameters = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate, org.mockito.Mockito.times(3)).query(recentSql.capture(), ArgumentMatchers.<RowMapper<Object>>any(), recentParameters.capture());
		assertThat(recentSql.getAllValues())
				.allSatisfy(value -> assertThat(value).contains("project_id = ?"))
				.anySatisfy(value -> assertThat(value).contains("from issues", "trashed_at is null", "order by updated_at desc, id desc", "limit ?"))
				.anySatisfy(value -> assertThat(value).contains("from documents", "trashed_at is null", "order by updated_at desc, id desc", "limit ?"))
				.anySatisfy(value -> assertThat(value).contains("from sprints s", "s.status = 'ACTIVE'", "i.trashed_at is null", "order by s.id desc limit 1"));
		assertThat(recentParameters.getAllValues())
				.anySatisfy(value -> assertThat(value).containsExactly(20L, 10))
				.anySatisfy(value -> assertThat(value).containsExactly(20L));
		assertThat(data.recentIssues()).isEmpty();
		assertThat(data.recentDocuments()).isEmpty();
		assertThat(data.sprintSummary()).isNull();
	}
}
