package com.threadia.backend.projectchat;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class ProjectChatMysqlSchemaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static final Path SCHEMA_PATH = Path.of("..", "db", "schema.sql").toAbsolutePath().normalize();
	private static final String SCHEMA_VALIDATION_PROPERTY = "THREADIA_DB_SCHEMA_VALIDATION";
	private static final String DISPOSABLE_DATABASE_PATTERN = "threadia_(?:test|ci)(?:_[a-z0-9]+)*";

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		Environment environment = applicationContext.getEnvironment();
		String databaseUrl = requiredProperty(environment, "spring.datasource.url");
		validateDestructiveSchemaExecution(environment, databaseUrl);
		assertThat(SCHEMA_PATH).isRegularFile();

		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl(databaseUrl);
		dataSource.setUsername(requiredProperty(environment, "spring.datasource.username"));
		dataSource.setPassword(requiredProperty(environment, "spring.datasource.password"));

		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new FileSystemResource(SCHEMA_PATH));
		populator.execute(dataSource);
	}

	static void validateDestructiveSchemaExecution(Environment environment, String databaseUrl) {
		if (!"true".equals(environment.getProperty(SCHEMA_VALIDATION_PROPERTY))) {
			throw new IllegalStateException("Destructive schema initialization requires " + SCHEMA_VALIDATION_PROPERTY + "=true");
		}
		if (!databaseUrl.startsWith("jdbc:mysql://")) {
			throw new IllegalStateException("Destructive schema initialization requires a local disposable Threadia test database");
		}

		URI databaseUri;
		try {
			databaseUri = URI.create(databaseUrl.substring("jdbc:".length()));
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("Destructive schema initialization requires a valid MySQL JDBC URL", exception);
		}

		if (!isLocalHost(databaseUri.getHost()) || !isDisposableDatabase(databaseUri.getPath())) {
			throw new IllegalStateException("Destructive schema initialization requires a local disposable Threadia test database");
		}
	}

	private static boolean isLocalHost(String host) {
		return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "[::1]".equals(host) || "::1".equals(host);
	}

	private static boolean isDisposableDatabase(String path) {
		return path != null && path.matches("/" + DISPOSABLE_DATABASE_PATTERN);
	}

	private static String requiredProperty(Environment environment, String propertyName) {
		String value = environment.getProperty(propertyName);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Missing required test property: " + propertyName);
		}
		return value;
	}
}
