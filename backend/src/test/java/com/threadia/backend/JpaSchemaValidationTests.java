package com.threadia.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
class JpaSchemaValidationTests {

	@Test
	void validatesJpaMappingsAgainstMysqlSchema() {
	}
}
