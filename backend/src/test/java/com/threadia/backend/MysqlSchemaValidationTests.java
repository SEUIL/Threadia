package com.threadia.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_SCHEMA_VALIDATION", matches = "true")
class MysqlSchemaValidationTests {

	private static final List<String> AUTH_TABLES = List.of(
			"users",
			"oauth_accounts",
			"password_reset_tokens",
			"refresh_tokens"
	);

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void appliesSchemaSqlAndCreatesSprint1AuthTables() {
		Path schemaPath = Path.of("..", "db", "schema.sql").toAbsolutePath().normalize();

		assertThat(schemaPath).isRegularFile();

		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new FileSystemResource(schemaPath));
		populator.execute(dataSource);

		Set<String> existingTables = jdbcTemplate.queryForList("""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name IN ('users', 'oauth_accounts', 'password_reset_tokens', 'refresh_tokens')
				""", String.class)
				.stream()
				.collect(Collectors.toSet());

		assertThat(existingTables).containsExactlyInAnyOrderElementsOf(AUTH_TABLES);
	}
}
