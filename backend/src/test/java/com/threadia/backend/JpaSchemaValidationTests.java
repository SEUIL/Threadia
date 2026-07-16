package com.threadia.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
class JpaSchemaValidationTests {

	@MockitoBean
	private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void validatesJpaMappingsAgainstMysqlSchema() {
	}
}
