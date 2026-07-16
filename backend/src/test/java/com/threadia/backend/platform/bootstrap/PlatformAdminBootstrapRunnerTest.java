package com.threadia.backend.platform.bootstrap;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlatformAdminBootstrapRunnerTest {

	@Mock
	private PlatformAdminBootstrapService bootstrapService;

	@Test
	void delegatesConfiguredEmailToTransactionalBootstrapService() {
		PlatformAdminBootstrap runner = new PlatformAdminBootstrap(bootstrapService, "bootstrap@example.com");

		runner.run(null);

		verify(bootstrapService).bootstrap("bootstrap@example.com");
	}
}
