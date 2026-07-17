package com.threadia.backend.search.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectSearchRepository {

	private final JdbcTemplate jdbcTemplate;

	public ProjectSearchRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<SearchRow> search(Long projectId, String pattern, SearchCursor cursor, int limit) {
		StringBuilder sql = new StringBuilder("""
				select source_type, source_id, title, content, sort_at
				from (
				    select 'ISSUE' as source_type, i.id as source_id, i.title as title, i.description as content, i.updated_at as sort_at
				    from issues i
				    where i.project_id = ?
				      and i.trashed_at is null
				      and (i.issue_key like ? escape '\\\\' or i.title like ? escape '\\\\' or i.description like ? escape '\\\\')
				    union all
				    select 'DOCUMENT' as source_type, d.id as source_id, d.title as title, d.content as content, d.updated_at as sort_at
				    from documents d
				    where d.project_id = ?
				      and d.trashed_at is null
				      and (d.title like ? escape '\\\\' or d.content like ? escape '\\\\')
				    union all
				    select 'THREAD' as source_type, t.id as source_id, t.topic as title, null as content, t.updated_at as sort_at
				    from threads t
				    where t.project_id = ?
				      and (
				          t.topic like ? escape '\\\\'
				          or exists (
				              select 1
				              from messages reply
				              where reply.project_id = ?
				                and reply.thread_id = t.id
				                and reply.content like ? escape '\\\\'
				          )
				      )
				    union all
				    select 'PROJECT_MESSAGE' as source_type, m.id as source_id, m.content as title, m.content as content, m.created_at as sort_at
				    from messages m
				    where m.project_id = ?
				      and m.thread_id is null
				      and m.content like ? escape '\\\\'
				) search_results
				""");
		List<Object> parameters = new ArrayList<>(List.of(projectId, pattern, pattern, pattern, projectId, pattern, pattern, projectId, pattern, projectId, pattern, projectId, pattern));
		if (cursor != null) {
			sql.append("""
					where sort_at < ?
					   or (sort_at = ? and source_type > ?)
					   or (sort_at = ? and source_type = ? and source_id < ?)
					""");
			parameters.add(Timestamp.valueOf(cursor.sortAt()));
			parameters.add(Timestamp.valueOf(cursor.sortAt()));
			parameters.add(cursor.sourceType());
			parameters.add(Timestamp.valueOf(cursor.sortAt()));
			parameters.add(cursor.sourceType());
			parameters.add(cursor.sourceId());
		}
		sql.append("order by sort_at desc, source_type asc, source_id desc limit ?");
		parameters.add(limit);
		return jdbcTemplate.query(
				sql.toString(),
				(rs, rowNum) -> new SearchRow(
						rs.getLong("source_id"),
						rs.getString("source_type"),
						rs.getString("title"),
						rs.getString("content"),
						rs.getTimestamp("sort_at").toLocalDateTime()),
				parameters.toArray());
	}

	public record SearchCursor(LocalDateTime sortAt, String sourceType, Long sourceId) {
	}

	public record SearchRow(Long sourceId, String sourceType, String title, String content, LocalDateTime sortAt) {
	}
}
