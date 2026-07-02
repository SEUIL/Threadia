package com.threadia.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
class MysqlConnectionTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void connectsToMysql84AndRunsSimpleQuery() {
		Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
		String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);

		assertThat(result).isEqualTo(1);
		assertThat(version).startsWith("8.4.");
	}
}
