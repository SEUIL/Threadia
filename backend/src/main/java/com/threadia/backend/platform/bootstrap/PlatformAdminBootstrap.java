package com.threadia.backend.platform.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PlatformAdminBootstrap implements ApplicationRunner {

	private final PlatformAdminBootstrapService bootstrapService;
	private final String bootstrapEmail;

	public PlatformAdminBootstrap(
			PlatformAdminBootstrapService bootstrapService,
			@Value("${threadia.platform-admin.bootstrap-email:}") String bootstrapEmail) {
		this.bootstrapService = bootstrapService;
		this.bootstrapEmail = bootstrapEmail;
	}

	@Override
	public void run(ApplicationArguments args) {
		bootstrapService.bootstrap(bootstrapEmail);
	}
}
