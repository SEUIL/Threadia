package com.threadia.backend.platform.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminBootstrapLock;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.repository.PlatformAdminBootstrapLockRepository;
import com.threadia.backend.platform.repository.PlatformAdminRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:platform-bootstrap-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
class PlatformAdminBootstrapConcurrencyIntegrationTest {

	@Autowired
	private PlatformAdminBootstrapService bootstrapService;

	@Autowired
	private PlatformAdminBootstrapLockRepository bootstrapLockRepository;

	@Autowired
	private PlatformAdminRepository platformAdminRepository;

	@Autowired
	private UserRepository userRepository;

	@MockitoBean
	private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void concurrentDifferentEmailsCreateExactlyOneSuperAdminAndOtherAttemptBecomesNoOp() throws Exception {
		bootstrapLockRepository.saveAndFlush(new PlatformAdminBootstrapLock(PlatformAdminBootstrapLock.SINGLETON_ID));
		LocalDateTime now = LocalDateTime.of(2026, 7, 15, 9, 0);
		User firstUser = userRepository.save(new User(
				"first-bootstrap@example.com",
				"hashed",
				"first",
				null,
				UserStatus.ACTIVE,
				now,
				now));
		User secondUser = userRepository.save(new User(
				"second-bootstrap@example.com",
				"hashed",
				"second",
				null,
				UserStatus.ACTIVE,
				now,
				now));
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<?> first = executor.submit(() -> bootstrapAfterSignal(
					"first-bootstrap@example.com", ready, start));
			Future<?> second = executor.submit(() -> bootstrapAfterSignal(
					"second-bootstrap@example.com", ready, start));
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			first.get(10, TimeUnit.SECONDS);
			second.get(10, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}

		List<PlatformAdmin> admins = platformAdminRepository.findAll();
		assertThat(admins).hasSize(1);
		assertThat(admins.get(0).getRole()).isEqualTo(PlatformAdminRole.SUPER_ADMIN);
		assertThat(List.of(
				platformAdminRepository.findByUser_Id(firstUser.getId()).isPresent(),
				platformAdminRepository.findByUser_Id(secondUser.getId()).isPresent()))
				.containsExactlyInAnyOrder(true, false);
	}

	private void bootstrapAfterSignal(
			String email,
			CountDownLatch ready,
			CountDownLatch start) {
		ready.countDown();
		try {
			if (!start.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("bootstrap start signal timeout");
			}
			bootstrapService.bootstrap(email);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("bootstrap concurrency test interrupted", exception);
		}
	}
}
