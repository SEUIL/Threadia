package com.threadia.backend.issue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.issue.dto.IssueCreateRequest;
import com.threadia.backend.issue.dto.IssueData;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.project.repository.ProjectRepository;

@SpringBootTest(properties = "threadia.jwt.secret=01234567890123456789012345678901")
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
// Requires the canonical schema on a real MySQL database; worker transactions commit and are explicitly cleaned up.
class IssueMysqlConcurrencyTests {

	private static final Duration LATCH_TIMEOUT = Duration.ofSeconds(10);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private IssueService issueService;

	@MockitoBean
	private AuthContextService authContextService;

	@MockitoBean
	private PlatformAdminBootstrap platformAdminBootstrap;

	private Long userId;
	private Long companyId;
	private Long projectId;
	private String email;
	private String projectKey;

	@BeforeEach
	void setUp() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		email = "issue-concurrency-" + suffix + "@example.com";
		projectKey = "IC" + suffix.toUpperCase();
		jdbcTemplate.update("""
				insert into users (email, password_hash, nickname, created_at, updated_at)
				values (?, 'hashed', 'Issue Admin', now(), now())
				""", email);
		userId = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);

		String companyName = "Issue Concurrency Co " + suffix;
		jdbcTemplate.update("""
				insert into companies (name, created_by_user_id, created_at, updated_at)
				values (?, ?, now(), now())
				""", companyName, userId);
		companyId = jdbcTemplate.queryForObject(
				"select id from companies where created_by_user_id = ? and name = ?",
				Long.class,
				userId,
				companyName);

		jdbcTemplate.update("""
				insert into company_members (company_id, user_id, role, created_at, updated_at)
				values (?, ?, 'COMPANY_ADMIN', now(), now())
				""", companyId, userId);

		jdbcTemplate.update("""
				insert into projects (company_id, name, project_key, created_by_user_id, created_at, updated_at)
				values (?, 'Issue Concurrency Project', ?, ?, now(), now())
				""", companyId, projectKey, userId);
		projectId = jdbcTemplate.queryForObject(
				"select id from projects where company_id = ? and project_key = ?",
				Long.class,
				companyId,
				projectKey);

		AuthContext authContext = new AuthContext(userId, email);
		when(authContextService.fromAccessToken(anyString())).thenReturn(authContext);
		when(authContextService.requireActiveUser(any())).thenAnswer(invocation -> userRepository.findById(userId).orElseThrow());
	}

	@AfterEach
	void cleanUp() {
		if (projectId != null) {
			jdbcTemplate.update("delete from activity_events where project_id = ?", projectId);
			jdbcTemplate.update("""
					delete history
					from issue_status_history history
					join issues issue on issue.id = history.issue_id
					where issue.project_id = ?
					""", projectId);
			jdbcTemplate.update("delete from issues where project_id = ?", projectId);
			jdbcTemplate.update("delete from project_members where project_id = ?", projectId);
			jdbcTemplate.update("delete from projects where id = ?", projectId);
		}
		if (companyId != null) {
			jdbcTemplate.update("delete from company_members where company_id = ?", companyId);
			jdbcTemplate.update("delete from companies where id = ?", companyId);
		}
		if (userId != null) {
			jdbcTemplate.update("delete from users where id = ?", userId);
		}
	}

	@Test
	void concurrentCreatesAfterAuthSnapshotProduceConsecutiveProjectIssueKeys() throws Exception {
		CountDownLatch blockerHasProjectLock = new CountDownLatch(1);
		CountDownLatch releaseBlocker = new CountDownLatch(1);
		CountDownLatch workersReady = new CountDownLatch(2);
		CountDownLatch startCreates = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(3);
		try {
			Future<?> blocker = executor.submit(() -> transactionTemplate().executeWithoutResult(status -> {
				projectRepository.findByIdForUpdate(projectId).orElseThrow();
				blockerHasProjectLock.countDown();
				await(releaseBlocker);
			}));
			await(blockerHasProjectLock);

			Future<IssueData> first = executor.submit(() -> createAfterAuthSnapshot("첫 동시 생성", workersReady, startCreates));
			Future<IssueData> second = executor.submit(() -> createAfterAuthSnapshot("두 번째 동시 생성", workersReady, startCreates));
			await(workersReady);
			startCreates.countDown();
			Thread.sleep(250);
			releaseBlocker.countDown();

			Set<String> returnedKeys = Set.of(
					first.get(LATCH_TIMEOUT.toSeconds(), TimeUnit.SECONDS).issue().issueKey(),
					second.get(LATCH_TIMEOUT.toSeconds(), TimeUnit.SECONDS).issue().issueKey());
			blocker.get(LATCH_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

			assertThat(returnedKeys).containsExactlyInAnyOrder(projectKey + "-1", projectKey + "-2");
			List<String> persistedKeys = jdbcTemplate.queryForList(
					"select issue_key from issues where project_id = ? order by issue_key",
					String.class,
					projectId);
			assertThat(persistedKeys).containsExactly(projectKey + "-1", projectKey + "-2");
			Integer historyCount = jdbcTemplate.queryForObject("""
					select count(*)
					from issue_status_history history
					join issues issue on issue.id = history.issue_id
					where issue.project_id = ?
					""", Integer.class, projectId);
			assertThat(historyCount).isEqualTo(2);
		} finally {
			releaseBlocker.countDown();
			startCreates.countDown();
			executor.shutdownNow();
			executor.awaitTermination(LATCH_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
		}
	}

	private IssueData createAfterAuthSnapshot(
			String title,
			CountDownLatch workersReady,
			CountDownLatch startCreates) {
		return transactionTemplate().execute(status -> {
			userRepository.findById(userId).orElseThrow();
			workersReady.countDown();
			await(startCreates);
			return issueService.create("concurrency-token", projectId, new IssueCreateRequest(title));
		});
	}

	private TransactionTemplate transactionTemplate() {
		return new TransactionTemplate(transactionManager);
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(LATCH_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
				throw new IllegalStateException("동시성 테스트 latch 대기 시간이 초과되었습니다.");
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("동시성 테스트 latch 대기가 중단되었습니다.", exception);
		}
	}
}
