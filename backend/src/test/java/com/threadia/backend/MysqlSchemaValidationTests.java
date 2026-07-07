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

	private static final List<String> SPRINT_1_5_TABLES = List.of(
			"users",
			"oauth_accounts",
			"password_reset_tokens",
			"refresh_tokens",
			"companies",
			"company_members",
			"invitations"
	);

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void appliesSchemaSqlAndCreatesSprint15AuthAndInvitationTables() {
		applySchemaSql();

		Set<String> existingTables = jdbcTemplate.queryForList("""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name IN (
				    'users',
				    'oauth_accounts',
				    'password_reset_tokens',
				    'refresh_tokens',
				    'companies',
				    'company_members',
				    'invitations'
				  )
				""", String.class)
				.stream()
				.collect(Collectors.toSet());

		assertThat(existingTables).containsExactlyInAnyOrderElementsOf(SPRINT_1_5_TABLES);
	}

	@Test
	void appliesSchemaSqlAndCreatesInvitationReferenceConstraints() {
		applySchemaSql();

		assertIndex("companies", "PRIMARY", "id");
		assertIndex("companies", "idx_companies_created_by", "created_by_user_id");
		assertForeignKey("companies", "fk_companies_created_by", "created_by_user_id", "users", "id");

		assertIndex("company_members", "PRIMARY", "id");
		assertIndex("company_members", "uq_cm_company_user", "company_id", "user_id");
		assertIndex("company_members", "idx_cm_user", "user_id");
		assertIndex("company_members", "idx_cm_department", "department_id");
		assertUniqueConstraint("company_members", "uq_cm_company_user");
		assertForeignKey("company_members", "fk_cm_company", "company_id", "companies", "id");
		assertForeignKey("company_members", "fk_cm_user", "user_id", "users", "id");
		assertForeignKey("company_members", "fk_cm_department", "department_id", "departments", "id");

		assertIndex("invitations", "PRIMARY", "id");
		assertIndex("invitations", "idx_inv_company", "company_id");
		assertIndex("invitations", "idx_inv_project", "project_id");
		assertIndex("invitations", "idx_inv_inviter", "inviter_user_id");
		assertIndex("invitations", "idx_inv_invitee", "invitee_user_id");
		assertForeignKey("invitations", "fk_inv_company", "company_id", "companies", "id");
		assertForeignKey("invitations", "fk_inv_project", "project_id", "projects", "id");
		assertForeignKey("invitations", "fk_inv_inviter", "inviter_user_id", "users", "id");
		assertForeignKey("invitations", "fk_inv_invitee", "invitee_user_id", "users", "id");
		assertCheckConstraint("chk_inv_scope");
		assertCheckConstraint("chk_inv_self");
		assertCheckConstraint("chk_inv_role");
		assertCheckConstraint("chk_inv_expiry");
	}

	private void applySchemaSql() {
		Path schemaPath = Path.of("..", "db", "schema.sql").toAbsolutePath().normalize();

		assertThat(schemaPath).isRegularFile();

		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new FileSystemResource(schemaPath));
		populator.execute(dataSource);
	}

	private void assertIndex(String tableName, String indexName, String... columnNames) {
		List<String> actualColumns = jdbcTemplate.queryForList("""
				SELECT column_name
				FROM information_schema.statistics
				WHERE table_schema = DATABASE()
				  AND table_name = ?
				  AND index_name = ?
				ORDER BY seq_in_index
				""", String.class, tableName, indexName);

		assertThat(actualColumns).containsExactly(columnNames);
	}

	private void assertUniqueConstraint(String tableName, String constraintName) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM information_schema.table_constraints
				WHERE constraint_schema = DATABASE()
				  AND table_name = ?
				  AND constraint_name = ?
				  AND constraint_type = 'UNIQUE'
				""", Integer.class, tableName, constraintName);

		assertThat(count).isEqualTo(1);
	}

	private void assertForeignKey(
			String tableName,
			String constraintName,
			String columnName,
			String referencedTableName,
			String referencedColumnName) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM information_schema.key_column_usage
				WHERE constraint_schema = DATABASE()
				  AND table_name = ?
				  AND constraint_name = ?
				  AND column_name = ?
				  AND referenced_table_name = ?
				  AND referenced_column_name = ?
				""", Integer.class, tableName, constraintName, columnName, referencedTableName, referencedColumnName);

		assertThat(count).isEqualTo(1);
	}

	private void assertCheckConstraint(String constraintName) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM information_schema.check_constraints
				WHERE constraint_schema = DATABASE()
				  AND constraint_name = ?
				""", Integer.class, constraintName);

		assertThat(count).isEqualTo(1);
	}
}
