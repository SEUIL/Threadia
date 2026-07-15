package com.threadia.backend.projectchat;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class ProjectChatMysqlSchemaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static final Path SCHEMA_PATH = Path.of("..", "db", "schema.sql").toAbsolutePath().normalize();

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		assertThat(SCHEMA_PATH).isRegularFile();

		Environment environment = applicationContext.getEnvironment();
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl(requiredProperty(environment, "spring.datasource.url"));
		dataSource.setUsername(requiredProperty(environment, "spring.datasource.username"));
		dataSource.setPassword(requiredProperty(environment, "spring.datasource.password"));

		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new FileSystemResource(SCHEMA_PATH));
		populator.execute(dataSource);
	}

	private static String requiredProperty(Environment environment, String propertyName) {
		String value = environment.getProperty(propertyName);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Missing required test property: " + propertyName);
		}
		return value;
	}
}
