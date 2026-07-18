package com.threadia.backend.thread.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.projectchat.ProjectChatMysqlSchemaInitializer;
import com.threadia.backend.thread.domain.ThreadTargetType;
import com.threadia.backend.thread.dto.ThreadCreationResult;
import com.threadia.backend.thread.dto.ThreadGetOrCreateRequest;

@SpringBootTest(properties = "threadia.jwt.secret=01234567890123456789012345678901")
@ContextConfiguration(initializers = ProjectChatMysqlSchemaInitializer.class)
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_SCHEMA_VALIDATION", matches = "true")
class ThreadMysqlConcurrencyTests {
	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	@Autowired private JdbcTemplate jdbcTemplate;
	@Autowired private UserRepository userRepository;
	@Autowired private ThreadService threadService;
	@MockitoBean private AuthContextService authContextService;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;

	private Long userId;
	private Long companyId;
	private Long projectId;
	private Long issueId;
	private String email;

	@BeforeEach
	void setUp() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		email = "thread-concurrency-" + suffix + "@example.com";
		jdbcTemplate.update("insert into users (email, password_hash, nickname, created_at, updated_at) values (?, 'hash', 'Thread Admin', utc_timestamp(), utc_timestamp())", email);
		userId = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
		jdbcTemplate.update("insert into companies (name, created_by_user_id, created_at, updated_at) values (?, ?, utc_timestamp(), utc_timestamp())", "Thread Concurrency " + suffix, userId);
		companyId = jdbcTemplate.queryForObject("select id from companies where created_by_user_id = ?", Long.class, userId);
		jdbcTemplate.update("insert into company_members (company_id, user_id, role, created_at, updated_at) values (?, ?, 'COMPANY_ADMIN', utc_timestamp(), utc_timestamp())", companyId, userId);
		jdbcTemplate.update("insert into projects (company_id, name, project_key, created_by_user_id, created_at, updated_at) values (?, ?, ?, ?, utc_timestamp(), utc_timestamp())", companyId, "Thread Project", "TH" + suffix.toUpperCase(), userId);
		projectId = jdbcTemplate.queryForObject("select id from projects where company_id = ?", Long.class, companyId);
		jdbcTemplate.update("insert into issues (project_id, issue_key, reporter_user_id, title, created_at, updated_at) values (?, ?, ?, 'Thread target issue', utc_timestamp(), utc_timestamp())", projectId, "TH" + suffix.toUpperCase() + "-1", userId);
		issueId = jdbcTemplate.queryForObject("select id from issues where project_id = ?", Long.class, projectId);
		AuthContext context = new AuthContext(userId, email);
		when(authContextService.fromAccessToken(anyString())).thenReturn(context);
		when(authContextService.requireActiveUser(any())).thenAnswer(invocation -> userRepository.findById(userId).orElseThrow());
	}

	@AfterEach
	void cleanUp() {
		if (projectId != null) {
			jdbcTemplate.update("delete from thread_comments where thread_id in (select id from threads where project_id = ?)", projectId);
			jdbcTemplate.update("delete from threads where project_id = ?", projectId);
			jdbcTemplate.update("delete from issue_status_history where issue_id in (select id from issues where project_id = ?)", projectId);
			jdbcTemplate.update("delete from issues where project_id = ?", projectId);
			jdbcTemplate.update("delete from project_members where project_id = ?", projectId);
			jdbcTemplate.update("delete from projects where id = ?", projectId);
		}
		if (companyId != null) {
			jdbcTemplate.update("delete from company_members where company_id = ?", companyId);
			jdbcTemplate.update("delete from companies where id = ?", companyId);
		}
		if (userId != null) jdbcTemplate.update("delete from users where id = ?", userId);
	}

	@Test
	void concurrentGetOrCreateReturnsOneRepresentativeThreadAndPersistsOneRow() throws Exception {
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<ThreadCreationResult> first = executor.submit(() -> getOrCreateAfterBarrier(ready, start));
			Future<ThreadCreationResult> second = executor.submit(() -> getOrCreateAfterBarrier(ready, start));
			await(ready);
			start.countDown();
			Long firstThreadId = first.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS).data().thread().id();
			Long secondThreadId = second.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS).data().thread().id();
			assertThat(firstThreadId).isEqualTo(secondThreadId);
			assertThat(jdbcTemplate.queryForObject("select count(*) from threads where target_type = 'ISSUE' and target_id = ?", Integer.class, issueId)).isEqualTo(1);
			assertThat(jdbcTemplate.queryForObject("select id from threads where target_type = 'ISSUE' and target_id = ?", Long.class, issueId)).isEqualTo(firstThreadId);
		} finally {
			start.countDown();
			executor.shutdownNow();
			executor.awaitTermination(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
		}
	}

	private ThreadCreationResult getOrCreateAfterBarrier(CountDownLatch ready, CountDownLatch start) {
		ready.countDown();
		await(start);
		return threadService.getOrCreate("thread-concurrency-token", projectId, new ThreadGetOrCreateRequest(ThreadTargetType.ISSUE, issueId, "동시 논의"));
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) throw new IllegalStateException("동시성 테스트 latch 대기 시간이 초과되었습니다.");
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("동시성 테스트 latch 대기가 중단되었습니다.", exception);
		}
	}
}
