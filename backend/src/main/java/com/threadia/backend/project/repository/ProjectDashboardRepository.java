package com.threadia.backend.project.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.threadia.backend.project.dto.ProjectDashboardData;

@Repository
public class ProjectDashboardRepository {

	private static final int RECENT_ITEM_LIMIT = 10;

	private final JdbcTemplate jdbcTemplate;

	public ProjectDashboardRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public ProjectDashboardData load(Long projectId) {
		ProjectDashboardData.Progress progress = progress(projectId);
		List<ProjectDashboardData.RecentIssue> recentIssues = recentIssues(projectId);
		List<ProjectDashboardData.RecentDocument> recentDocuments = recentDocuments(projectId);
		return new ProjectDashboardData(progress, recentIssues, recentDocuments, activeSprint(projectId));
	}

	private ProjectDashboardData.Progress progress(Long projectId) {
		return jdbcTemplate.queryForObject(
				"select count(*) as total_issue_count, coalesce(sum(status = 'DONE'), 0) as done_issue_count "
						+ "from issues where project_id = ? and trashed_at is null",
				(rs, rowNum) -> {
					long totalIssueCount = rs.getLong("total_issue_count");
					long doneIssueCount = rs.getLong("done_issue_count");
					int percentage = totalIssueCount == 0 ? 0 : (int) (doneIssueCount * 100 / totalIssueCount);
					return new ProjectDashboardData.Progress(totalIssueCount, doneIssueCount, percentage);
				},
				projectId);
	}

	private List<ProjectDashboardData.RecentIssue> recentIssues(Long projectId) {
		return jdbcTemplate.query(
				"select id, issue_key, title, status, updated_at from issues "
						+ "where project_id = ? and trashed_at is null order by updated_at desc, id desc limit ?",
				(rs, rowNum) -> new ProjectDashboardData.RecentIssue(
						rs.getLong("id"),
						rs.getString("issue_key"),
						rs.getString("title"),
						rs.getString("status"),
						localDateTime(rs, "updated_at")),
				projectId,
				RECENT_ITEM_LIMIT);
	}

	private List<ProjectDashboardData.RecentDocument> recentDocuments(Long projectId) {
		return jdbcTemplate.query(
				"select id, title, document_type, updated_at from documents "
						+ "where project_id = ? and trashed_at is null order by updated_at desc, id desc limit ?",
				(rs, rowNum) -> new ProjectDashboardData.RecentDocument(
						rs.getLong("id"),
						rs.getString("title"),
						rs.getString("document_type"),
						localDateTime(rs, "updated_at")),
				projectId,
				RECENT_ITEM_LIMIT);
	}

	private ProjectDashboardData.SprintSummary activeSprint(Long projectId) {
		List<ProjectDashboardData.SprintSummary> activeSprints = jdbcTemplate.query(
				"select s.id, s.name, s.goal, s.start_date, s.end_date, count(i.id) as issue_count, "
						+ "coalesce(sum(i.status = 'DONE'), 0) as done_issue_count "
						+ "from sprints s left join issues i on i.sprint_id = s.id and i.trashed_at is null "
						+ "where s.project_id = ? and s.status = 'ACTIVE' "
						+ "group by s.id, s.name, s.goal, s.start_date, s.end_date order by s.id desc limit 1",
				this::mapSprintSummary,
				projectId);
		return activeSprints.isEmpty() ? null : activeSprints.get(0);
	}

	private ProjectDashboardData.SprintSummary mapSprintSummary(ResultSet rs, int rowNum) throws SQLException {
		return new ProjectDashboardData.SprintSummary(
				rs.getLong("id"),
				rs.getString("name"),
				rs.getString("goal"),
				localDate(rs, "start_date"),
				localDate(rs, "end_date"),
				rs.getLong("issue_count"),
				rs.getLong("done_issue_count"));
	}

	private static LocalDateTime localDateTime(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getTimestamp(columnLabel).toLocalDateTime();
	}

	private static LocalDate localDate(ResultSet rs, String columnLabel) throws SQLException {
		java.sql.Date value = rs.getDate(columnLabel);
		return value == null ? null : value.toLocalDate();
	}
}
