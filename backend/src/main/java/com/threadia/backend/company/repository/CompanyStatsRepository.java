package com.threadia.backend.company.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyStatsRepository {

	private final JdbcTemplate jdbcTemplate;

	public CompanyStatsRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public long countDepartments(Long companyId) {
		Long count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM departments WHERE company_id = ?",
				Long.class,
				companyId);
		return count == null ? 0L : count;
	}

	public long countProjects(Long companyId) {
		Long count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM projects WHERE company_id = ?",
				Long.class,
				companyId);
		return count == null ? 0L : count;
	}

	public Map<String, Long> countProjectsByStatus(Long companyId) {
		List<Map.Entry<String, Long>> rows = jdbcTemplate.query(
				"SELECT status, COUNT(*) AS count FROM projects WHERE company_id = ? GROUP BY status",
				(rs, rowNum) -> Map.entry(rs.getString("status"), rs.getLong("count")),
				companyId);
		Map<String, Long> counts = new LinkedHashMap<>();
		for (Map.Entry<String, Long> row : rows) {
			counts.put(row.getKey(), row.getValue());
		}
		return counts;
	}
}
