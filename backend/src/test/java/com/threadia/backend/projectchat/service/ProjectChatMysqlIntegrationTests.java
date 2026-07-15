package com.threadia.backend.projectchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.projectchat.ProjectChatMysqlSchemaInitializer;
import com.threadia.backend.projectchat.dto.MarkReadRequest;
import com.threadia.backend.projectchat.dto.MessagesData;

@SpringBootTest(properties = {
		"threadia.jwt.secret=01234567890123456789012345678901",
		"threadia.project-chat.cursor-hmac-secret=test-only-project-chat-cursor-hmac-secret"})
@ContextConfiguration(initializers = ProjectChatMysqlSchemaInitializer.class)
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_SCHEMA_VALIDATION", matches = "true")
class ProjectChatMysqlIntegrationTests {
	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	@Autowired private JdbcTemplate jdbcTemplate;
	@Autowired private UserRepository userRepository;
	@Autowired private ProjectChatService projectChatService;
	@MockitoBean private AuthContextService authContextService;

	private Long readerId;
	private Long authorId;
	private Long companyId;
	private Long projectId;
	private String readerEmail;
	private Long firstMessageId;
	private Long secondMessageId;
	private Long thirdMessageId;
	private Long ownMessageId;

	@BeforeEach
	void setUp() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		readerEmail = "chat-reader-" + suffix + "@example.com";
		insertUser(readerEmail, "Chat Reader");
		readerId = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, readerEmail);
		insertUser("chat-author-" + suffix + "@example.com", "Chat Author");
		authorId = jdbcTemplate.queryForObject("select id from users where email like ?", Long.class, "chat-author-" + suffix + "%");
		jdbcTemplate.update("insert into companies (name, created_by_user_id, created_at, updated_at) values (?, ?, utc_timestamp(), utc_timestamp())", "Chat Co " + suffix, readerId);
		companyId = jdbcTemplate.queryForObject("select id from companies where created_by_user_id = ?", Long.class, readerId);
		jdbcTemplate.update("insert into company_members (company_id, user_id, role, created_at, updated_at) values (?, ?, 'COMPANY_ADMIN', utc_timestamp(), utc_timestamp())", companyId, readerId);
		jdbcTemplate.update("insert into projects (company_id, name, project_key, created_by_user_id, created_at, updated_at) values (?, ?, ?, ?, utc_timestamp(), utc_timestamp())", companyId, "Chat Project", "CH" + suffix.toUpperCase(), readerId);
		projectId = jdbcTemplate.queryForObject("select id from projects where company_id = ?", Long.class, companyId);
		firstMessageId = insertMessage(authorId, "first", "2026-07-15 10:00:00");
		secondMessageId = insertMessage(authorId, "second", "2026-07-15 10:00:00");
		thirdMessageId = insertMessage(authorId, "third", "2026-07-15 10:00:01");
		ownMessageId = insertMessage(readerId, "own", "2026-07-15 10:00:02");
		AuthContext context = new AuthContext(readerId, readerEmail);
		when(authContextService.fromAccessToken(anyString())).thenReturn(context);
		when(authContextService.requireActiveUser(any())).thenAnswer(invocation -> userRepository.findById(readerId).orElseThrow());
	}

	@AfterEach
	void cleanUp() {
		if (projectId != null) {
			jdbcTemplate.update("delete from project_chat_member_states where project_id = ?", projectId);
			jdbcTemplate.update("delete from messages where project_id = ?", projectId);
			jdbcTemplate.update("delete from project_members where project_id = ?", projectId);
			jdbcTemplate.update("delete from projects where id = ?", projectId);
		}
		if (companyId != null) {
			jdbcTemplate.update("delete from company_members where company_id = ?", companyId);
			jdbcTemplate.update("delete from companies where id = ?", companyId);
		}
		if (authorId != null) jdbcTemplate.update("delete from users where id = ?", authorId);
		if (readerId != null) jdbcTemplate.update("delete from users where id = ?", readerId);
	}

	@Test
	void mysqlKeysetAndUnreadQueriesUseCreatedAtThenIdAndExcludeOwnMessages() {
		MessagesData firstPage = projectChatService.list("token", projectId, null, 2);
		assertThat(firstPage.messages()).extracting("id").containsExactly(ownMessageId, thirdMessageId);
		MessagesData secondPage = projectChatService.list("token", projectId, firstPage.nextCursor(), 2);
		assertThat(secondPage.messages()).extracting("id").containsExactly(secondMessageId, firstMessageId);

		projectChatService.markRead("token", projectId, new MarkReadRequest(firstMessageId));
		assertThat(projectChatService.unreadCount("token", projectId).unreadCount()).isEqualTo(2L);
	}

	@Test
	void concurrentFirstMarkReadUpsertsOneStateAndKeepsLatestPointer() throws Exception {
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<?> old = executor.submit(() -> markReadAfterStart(firstMessageId, ready, start));
			Future<?> latest = executor.submit(() -> markReadAfterStart(thirdMessageId, ready, start));
			await(ready); start.countDown();
			old.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
			latest.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
			assertThat(jdbcTemplate.queryForObject("select count(*) from project_chat_member_states where project_id = ? and user_id = ?", Integer.class, projectId, readerId)).isEqualTo(1);
			assertThat(jdbcTemplate.queryForObject("select last_read_message_id from project_chat_member_states where project_id = ? and user_id = ?", Long.class, projectId, readerId)).isEqualTo(thirdMessageId);
		} finally {
			start.countDown(); executor.shutdownNow(); executor.awaitTermination(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
		}
	}

	private void markReadAfterStart(Long messageId, CountDownLatch ready, CountDownLatch start) { ready.countDown(); await(start); projectChatService.markRead("token", projectId, new MarkReadRequest(messageId)); }
	private Long insertMessage(Long author, String content, String createdAt) { jdbcTemplate.update("insert into messages (project_id, author_user_id, content, created_at) values (?, ?, ?, ?)", projectId, author, content, createdAt); return jdbcTemplate.queryForObject("select id from messages where project_id = ? and content = ?", Long.class, projectId, content); }
	private void insertUser(String email, String nickname) { jdbcTemplate.update("insert into users (email, password_hash, nickname, created_at, updated_at) values (?, 'hash', ?, utc_timestamp(), utc_timestamp())", email, nickname); }
	private static void await(CountDownLatch latch) { try { if (!latch.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) throw new IllegalStateException("동시성 테스트 latch 대기 시간이 초과되었습니다."); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException("동시성 테스트 latch 대기가 중단되었습니다.", exception); } }
}
