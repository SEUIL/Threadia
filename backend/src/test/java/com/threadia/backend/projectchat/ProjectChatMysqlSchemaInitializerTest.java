package com.threadia.backend.projectchat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProjectChatMysqlSchemaInitializerTest {

	@Test
	void rejectsDestructiveSchemaInitializationWithoutExplicitOptIn() {
		MockEnvironment environment = environmentWithoutOptIn();

		assertThatThrownBy(() -> ProjectChatMysqlSchemaInitializer.validateDestructiveSchemaExecution(
				environment, "jdbc:mysql://localhost:3306/threadia_test"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("THREADIA_DB_SCHEMA_VALIDATION=true");
	}

	@Test
	void rejectsNonLocalOrNonDisposableDatabaseUrls() {
		MockEnvironment environment = enabledEnvironment();

		assertThatThrownBy(() -> ProjectChatMysqlSchemaInitializer.validateDestructiveSchemaExecution(
				environment, "jdbc:mysql://db.example.com:3306/threadia_test"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("local disposable Threadia test database");
		assertThatThrownBy(() -> ProjectChatMysqlSchemaInitializer.validateDestructiveSchemaExecution(
				environment, "jdbc:mysql://localhost:3306/threadia"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("local disposable Threadia test database");
	}

	@Test
	void permitsExplicitlyOptedInLocalDisposableDatabaseUrl() {
		assertThatCode(() -> ProjectChatMysqlSchemaInitializer.validateDestructiveSchemaExecution(
				enabledEnvironment(), "jdbc:mysql://127.0.0.1:3306/threadia_ci_project_chat?serverTimezone=UTC"))
				.doesNotThrowAnyException();
	}

	private static MockEnvironment environmentWithoutOptIn() {
		return new MockEnvironment();
	}

	private static MockEnvironment enabledEnvironment() {
		return environmentWithoutOptIn().withProperty("THREADIA_DB_SCHEMA_VALIDATION", "true");
	}
}
