package com.threadia.backend.directmessage.service;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.directmessage.dto.DirectMessageRoomCreateRequest;
import com.threadia.backend.directmessage.dto.DirectMessageRoomCreationResult;

@SpringBootTest(properties = "threadia.jwt.secret=01234567890123456789012345678901")
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
class DirectMessageMysqlConcurrencyTests {
	private static final Duration TIMEOUT = Duration.ofSeconds(10);
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired UserRepository userRepository;
	@Autowired DirectMessageService directMessageService;
	@MockitoBean AuthContextService authContextService;
	private Long firstUserId; private Long secondUserId; private Long companyId;
	@BeforeEach void setUp() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		jdbcTemplate.update("insert into users (email, password_hash, nickname, created_at, updated_at) values (?, 'hash', 'first', now(), now())", "dm-first-" + suffix + "@example.com");
		jdbcTemplate.update("insert into users (email, password_hash, nickname, created_at, updated_at) values (?, 'hash', 'second', now(), now())", "dm-second-" + suffix + "@example.com");
		firstUserId = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, "dm-first-" + suffix + "@example.com"); secondUserId = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, "dm-second-" + suffix + "@example.com");
		jdbcTemplate.update("insert into companies (name, created_by_user_id, created_at, updated_at) values (?, ?, now(), now())", "DM Concurrency " + suffix, firstUserId);
		companyId = jdbcTemplate.queryForObject("select id from companies where name = ?", Long.class, "DM Concurrency " + suffix);
		jdbcTemplate.update("insert into company_members (company_id, user_id, role, created_at, updated_at) values (?, ?, 'COMPANY_ADMIN', now(), now())", companyId, firstUserId);
		jdbcTemplate.update("insert into company_members (company_id, user_id, role, created_at, updated_at) values (?, ?, 'MEMBER', now(), now())", companyId, secondUserId);
		AuthContext auth = new AuthContext(firstUserId, "first@example.com"); when(authContextService.fromAccessToken(anyString())).thenReturn(auth); when(authContextService.requireActiveUser(any())).thenAnswer(invocation -> userRepository.findById(firstUserId).orElseThrow());
	}
	@AfterEach void cleanUp() {
		if (companyId != null) { jdbcTemplate.update("delete from dm_room_user_states where room_id in (select id from direct_message_rooms where company_id = ?)", companyId); jdbcTemplate.update("delete from direct_messages where room_id in (select id from direct_message_rooms where company_id = ?)", companyId); jdbcTemplate.update("delete from direct_message_rooms where company_id = ?", companyId); jdbcTemplate.update("delete from company_members where company_id = ?", companyId); jdbcTemplate.update("delete from companies where id = ?", companyId); }
		if (firstUserId != null) jdbcTemplate.update("delete from users where id = ?", firstUserId); if (secondUserId != null) jdbcTemplate.update("delete from users where id = ?", secondUserId);
	}
	@Test void concurrentGetOrCreateReturnsOneNormalizedRoom() throws Exception {
		CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1); ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<DirectMessageRoomCreationResult> first = executor.submit(() -> createAfterBarrier(ready, start)); Future<DirectMessageRoomCreationResult> second = executor.submit(() -> createAfterBarrier(ready, start));
			assertThat(ready.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isTrue(); start.countDown();
			Long firstRoomId = first.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS).data().room().id(); Long secondRoomId = second.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS).data().room().id();
			assertThat(firstRoomId).isEqualTo(secondRoomId); assertThat(jdbcTemplate.queryForObject("select count(*) from direct_message_rooms where company_id = ? and user1_id = ? and user2_id = ?", Integer.class, companyId, Math.min(firstUserId, secondUserId), Math.max(firstUserId, secondUserId))).isEqualTo(1);
		} finally { start.countDown(); executor.shutdownNow(); executor.awaitTermination(TIMEOUT.toSeconds(), TimeUnit.SECONDS); }
	}
	@Test void oppositeDirectionCallerReusesTheSameNormalizedRoom() {
		AuthContext reverseAuth = new AuthContext(secondUserId, "second@example.com");
		when(authContextService.fromAccessToken("reverse-token")).thenReturn(reverseAuth);
		when(authContextService.requireActiveUser(reverseAuth)).thenAnswer(invocation -> userRepository.findById(secondUserId).orElseThrow());
		DirectMessageRoomCreationResult result = directMessageService.getOrCreateRoom("reverse-token", companyId, new DirectMessageRoomCreateRequest(firstUserId));
		assertThat(result.data().room().companyId()).isEqualTo(companyId);
		assertThat(jdbcTemplate.queryForObject("select count(*) from direct_message_rooms where company_id = ? and user1_id = ? and user2_id = ?", Integer.class, companyId, Math.min(firstUserId, secondUserId), Math.max(firstUserId, secondUserId))).isEqualTo(1);
	}
	private DirectMessageRoomCreationResult createAfterBarrier(CountDownLatch ready, CountDownLatch start) {
		ready.countDown(); try { if (!start.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) throw new IllegalStateException("동시 생성 시작 대기 시간이 초과되었습니다."); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException("동시 생성 대기가 중단되었습니다.", exception); }
		return directMessageService.getOrCreateRoom("token", companyId, new DirectMessageRoomCreateRequest(secondUserId));
	}
}
