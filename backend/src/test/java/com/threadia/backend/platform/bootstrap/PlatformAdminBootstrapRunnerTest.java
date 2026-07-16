package com.threadia.backend.platform.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.platform.domain.PlatformAdminBootstrapLock;
import com.threadia.backend.platform.repository.PlatformAdminBootstrapLockRepository;
import com.threadia.backend.platform.repository.PlatformAdminRepository;

@ExtendWith(MockitoExtension.class)
class PlatformAdminBootstrapRunnerTest {

	@Mock
	private PlatformAdminBootstrapService bootstrapService;

	@Mock
	private PlatformAdminBootstrapLockRepository bootstrapLockRepository;

	@Mock
	private PlatformAdminRepository platformAdminRepository;

	@Mock
	private UserRepository userRepository;

	@Test
	void delegatesConfiguredEmailToTransactionalBootstrapService() {
		PlatformAdminBootstrap runner = new PlatformAdminBootstrap(bootstrapService, "bootstrap@example.com");

		runner.run(null);

		verify(bootstrapService).bootstrap("bootstrap@example.com");
	}

	@Test
	void propagatesMissingBootstrapEmailFailureFromRealBootstrapService() {
		when(bootstrapLockRepository.findByIdForUpdate(PlatformAdminBootstrapLock.SINGLETON_ID))
				.thenReturn(Optional.of(new PlatformAdminBootstrapLock(PlatformAdminBootstrapLock.SINGLETON_ID)));
		when(platformAdminRepository.count()).thenReturn(0L);

		PlatformAdminBootstrap runner = new PlatformAdminBootstrap(realBootstrapService(), "   ");

		assertThatThrownBy(() -> runner.run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("THREADIA_PLATFORM_ADMIN_BOOTSTRAP_EMAIL");
		verifyNoInteractions(userRepository);
	}

	private PlatformAdminBootstrapService realBootstrapService() {
		return new PlatformAdminBootstrapService(
				bootstrapLockRepository,
				platformAdminRepository,
				userRepository);
	}
}
