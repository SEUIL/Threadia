package com.threadia.backend.search.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.projectchat.ProjectChatMysqlSchemaInitializer;
import com.threadia.backend.search.repository.ProjectSearchRepository.SearchCursor;
import com.threadia.backend.search.repository.ProjectSearchRepository.SearchRow;

@SpringBootTest(properties = {
		"threadia.jwt.secret=01234567890123456789012345678901",
		"threadia.project-chat.cursor-hmac-secret=test-only-project-chat-cursor-hmac-secret"})
@ContextConfiguration(initializers = ProjectChatMysqlSchemaInitializer.class)
@Transactional
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_SCHEMA_VALIDATION", matches = "true")
class ProjectSearchRepositoryMysqlIntegrationTests {

	private static final String LITERAL = "edge%_\\marker";
	private static final LocalDateTime SAME_TIMESTAMP = LocalDateTime.parse("2026-07-17T10:00:00");

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private ProjectSearchRepository projectSearchRepository;
	@MockitoBean
	private PlatformAdminBootstrap platformAdminBootstrap;

	private Long projectId;
	private Long documentId;
	private Long issueId;
	private Long projectMessageId;
	private Long threadId;

	@BeforeEach
	void setUp() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		String email = "search-" + suffix + "@example.com";
		jdbcTemplate.update("insert into users (email, password_hash, nickname, created_at, updated_at) values (?, 'hash', ?, ?, ?)", email, "Search User", SAME_TIMESTAMP, SAME_TIMESTAMP);
		Long userId = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
		jdbcTemplate.update("insert into companies (name, created_by_user_id, created_at, updated_at) values (?, ?, ?, ?)", "Search Co " + suffix, userId, SAME_TIMESTAMP, SAME_TIMESTAMP);
		Long companyId = jdbcTemplate.queryForObject("select id from companies where created_by_user_id = ?", Long.class, userId);
		jdbcTemplate.update("insert into projects (company_id, name, project_key, created_by_user_id, created_at, updated_at) values (?, ?, ?, ?, ?, ?)", companyId, "Search Project", "SR" + suffix.substring(0, 8).toUpperCase(), userId, SAME_TIMESTAMP, SAME_TIMESTAMP);
		projectId = jdbcTemplate.queryForObject("select id from projects where company_id = ?", Long.class, companyId);

		issueId = insertIssue(userId, "SRCH-1", "Issue " + LITERAL, null, null);
		documentId = insertDocument(userId, "Document " + suffix, "Document " + LITERAL, null, null);
		insertIssue(userId, "SRCH-2", "Trashed issue " + LITERAL, SAME_TIMESTAMP, userId);
		insertDocument(userId, "Trashed document " + suffix, "Trashed document " + LITERAL, SAME_TIMESTAMP, userId);

		jdbcTemplate.update("insert into threads (project_id, target_type, target_id, topic, created_by_user_id, created_at, updated_at) values (?, 'ISSUE', ?, ?, ?, ?, ?)", projectId, issueId, "Unmatched topic", userId, SAME_TIMESTAMP, SAME_TIMESTAMP);
		threadId = jdbcTemplate.queryForObject("select id from threads where project_id = ? and target_id = ?", Long.class, projectId, issueId);
		insertThreadComment(userId, threadId, "Reply one " + LITERAL);
		insertThreadComment(userId, threadId, "Reply two " + LITERAL);
		projectMessageId = insertMessage(userId, null, "Top-level " + LITERAL);
	}

	@Test
	void executesFourTypeLiteralSearchWithoutTrashedRowsOrDuplicateThreadRepliesAndUsesStableKeysetOrder() {
		List<SearchRow> results = projectSearchRepository.search(projectId, literalLikePattern(LITERAL), null, 20);

		assertThat(results).extracting(SearchRow::sourceType).containsExactly("DOCUMENT", "ISSUE", "PROJECT_MESSAGE", "THREAD");
		assertThat(results).extracting(SearchRow::sourceId).containsExactly(documentId, issueId, projectMessageId, threadId);
		assertThat(results).extracting(SearchRow::sortAt).containsOnly(SAME_TIMESTAMP);
		assertThat(results).filteredOn(row -> row.sourceType().equals("THREAD")).hasSize(1);

		SearchRow second = results.get(1);
		List<SearchRow> nextPage = projectSearchRepository.search(projectId, literalLikePattern(LITERAL), new SearchCursor(second.sortAt(), second.sourceType(), second.sourceId()), 20);
		assertThat(nextPage).extracting(SearchRow::sourceType).containsExactly("PROJECT_MESSAGE", "THREAD");
	}

	private Long insertIssue(Long userId, String issueKey, String title, LocalDateTime trashedAt, Long trashedByUserId) {
		jdbcTemplate.update("insert into issues (project_id, issue_key, reporter_user_id, title, trashed_at, trashed_by_user_id, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)", projectId, issueKey, userId, title, trashedAt, trashedByUserId, SAME_TIMESTAMP, SAME_TIMESTAMP);
		return jdbcTemplate.queryForObject("select id from issues where project_id = ? and issue_key = ?", Long.class, projectId, issueKey);
	}

	private Long insertDocument(Long userId, String title, String content, LocalDateTime trashedAt, Long trashedByUserId) {
		jdbcTemplate.update("insert into documents (project_id, author_user_id, title, content, trashed_at, trashed_by_user_id, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)", projectId, userId, title, content, trashedAt, trashedByUserId, SAME_TIMESTAMP, SAME_TIMESTAMP);
		return jdbcTemplate.queryForObject("select id from documents where project_id = ? and title = ?", Long.class, projectId, title);
	}

	private Long insertMessage(Long userId, Long threadId, String content) {
		jdbcTemplate.update("insert into messages (project_id, author_user_id, thread_id, content, created_at) values (?, ?, ?, ?, ?)", projectId, userId, threadId, content, SAME_TIMESTAMP);
		return jdbcTemplate.queryForObject("select id from messages where project_id = ? and content = ?", Long.class, projectId, content);
	}

	private Long insertThreadComment(Long userId, Long threadId, String content) {
		jdbcTemplate.update("insert into thread_comments (thread_id, author_user_id, content, created_at, updated_at) values (?, ?, ?, ?, ?)", threadId, userId, content, SAME_TIMESTAMP, SAME_TIMESTAMP);
		return jdbcTemplate.queryForObject("select id from thread_comments where thread_id = ? and content = ?", Long.class, threadId, content);
	}

	private static String literalLikePattern(String value) {
		return "%" + value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
	}
}
