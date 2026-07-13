package com.threadia.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
	private static final List<String> PROJECT_TABLES = List.of(
			"projects",
			"project_members"
	);
	private static final List<String> ISSUE_TABLES = List.of(
			"issues",
			"issue_status_history"
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

	@Test
	void appliesSchemaSqlAndCreatesProjectTables() {
		applySchemaSql();

		Set<String> existingTables = jdbcTemplate.queryForList("""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name IN ('projects', 'project_members')
				""", String.class)
				.stream()
				.collect(Collectors.toSet());

		assertThat(existingTables).containsExactlyInAnyOrderElementsOf(PROJECT_TABLES);
	}

	@Test
	void appliesSchemaSqlAndCreatesProjectReferenceConstraints() {
		applySchemaSql();

		assertRequiredColumn("projects", "company_id", "bigint");
		assertRequiredColumn("projects", "name", "varchar");
		assertRequiredColumn("projects", "project_key", "varchar");
		assertEnumColumn("projects", "status", "PLANNING", "PLANNING", "ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED");
		assertNullableColumn("projects", "start_date", "date");
		assertNullableColumn("projects", "end_date", "date");
		assertRequiredColumn("projects", "created_by_user_id", "bigint");
		assertNullableColumn("projects", "archived_at", "datetime");
		assertNullableColumn("projects", "archived_by_user_id", "bigint");
		assertNullableColumn("projects", "archive_reason", "text");
		assertRequiredColumn("projects", "created_at", "datetime");
		assertRequiredColumn("projects", "updated_at", "datetime");
		assertIndex("projects", "PRIMARY", "id");
		assertIndex("projects", "uq_projects_company_key", "company_id", "project_key");
		assertIndex("projects", "idx_projects_created_by", "created_by_user_id");
		assertIndex("projects", "idx_projects_archived_by", "archived_by_user_id");
		assertUniqueConstraint("projects", "uq_projects_company_key");
		assertForeignKey("projects", "fk_projects_company", "company_id", "companies", "id");
		assertForeignKey("projects", "fk_projects_created_by", "created_by_user_id", "users", "id");
		assertForeignKey("projects", "fk_projects_archived_by", "archived_by_user_id", "users", "id");
		assertCheckConstraint("chk_projects_date_range");
		assertCheckConstraint("chk_projects_archive_state");

		assertRequiredColumn("project_members", "project_id", "bigint");
		assertRequiredColumn("project_members", "user_id", "bigint");
		assertEnumColumn("project_members", "role", null, "PROJECT_ADMIN", "MEMBER");
		assertEnumColumn("project_members", "status", "ACTIVE", "ACTIVE", "REMOVED");
		assertNullableColumn("project_members", "joined_at", "datetime");
		assertRequiredColumn("project_members", "created_at", "datetime");
		assertRequiredColumn("project_members", "updated_at", "datetime");
		assertIndex("project_members", "PRIMARY", "id");
		assertIndex("project_members", "uq_pm_project_user", "project_id", "user_id");
		assertIndex("project_members", "idx_pm_user", "user_id");
		assertUniqueConstraint("project_members", "uq_pm_project_user");
		assertForeignKey("project_members", "fk_pm_project", "project_id", "projects", "id");
		assertForeignKey("project_members", "fk_pm_user", "user_id", "users", "id");
	}

	@Test
	void appliesSchemaSqlAndCreatesIssueTables() {
		applySchemaSql();

		Set<String> existingTables = jdbcTemplate.queryForList("""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name IN ('issues', 'issue_status_history')
				""", String.class)
				.stream()
				.collect(Collectors.toSet());

		assertThat(existingTables).containsExactlyInAnyOrderElementsOf(ISSUE_TABLES);
	}

	@Test
	void appliesSchemaSqlAndCreatesIssueReferenceConstraints() {
		applySchemaSql();

		assertRequiredColumn("issues", "project_id", "bigint");
		assertRequiredColumn("issues", "issue_key", "varchar");
		assertRequiredColumn("issues", "reporter_user_id", "bigint");
		assertRequiredColumn("issues", "title", "varchar");
		assertEnumColumn("issues", "status", "BACKLOG", "BACKLOG", "TODO", "IN_PROGRESS", "IN_REVIEW", "DONE");
		assertNullableColumn("issues", "trashed_at", "datetime");
		assertRequiredColumn("issues", "created_at", "datetime");
		assertRequiredColumn("issues", "updated_at", "datetime");
		assertIndex("issues", "uq_issues_project_key", "project_id", "issue_key");
		assertIndex("issues", "idx_issues_project_status", "project_id", "status");
		assertUniqueConstraint("issues", "uq_issues_project_key");
		assertForeignKey("issues", "fk_issues_project", "project_id", "projects", "id");
		assertForeignKey("issues", "fk_issues_reporter", "reporter_user_id", "users", "id");
		assertCheckConstraint("chk_issues_source");
		assertCheckConstraint("chk_issues_trash_pair");

		assertRequiredColumn("issue_status_history", "issue_id", "bigint");
		assertRequiredColumn("issue_status_history", "changed_by_user_id", "bigint");
		assertNullableEnumColumn("issue_status_history", "from_status", "BACKLOG", "TODO", "IN_PROGRESS", "IN_REVIEW", "DONE");
		assertEnumColumn("issue_status_history", "to_status", null, "BACKLOG", "TODO", "IN_PROGRESS", "IN_REVIEW", "DONE");
		assertRequiredColumn("issue_status_history", "changed_at", "datetime");
		assertIndex("issue_status_history", "idx_ish_issue_changed", "issue_id", "changed_at");
		assertIndex("issue_status_history", "idx_ish_changed_by", "changed_by_user_id");
		assertForeignKey("issue_status_history", "fk_ish_issue", "issue_id", "issues", "id");
		assertForeignKey("issue_status_history", "fk_ish_changed_by", "changed_by_user_id", "users", "id");
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

	private void assertRequiredColumn(String tableName, String columnName, String dataType) {
		assertColumn(tableName, columnName, dataType, "NO");
	}

	private void assertNullableColumn(String tableName, String columnName, String dataType) {
		assertColumn(tableName, columnName, dataType, "YES");
	}

	private void assertColumn(String tableName, String columnName, String dataType, String nullable) {
		Map<String, Object> column = columnMetadata(tableName, columnName);

		assertThat(column.get("DATA_TYPE")).isEqualTo(dataType);
		assertThat(column.get("IS_NULLABLE")).isEqualTo(nullable);
	}

	private void assertEnumColumn(String tableName, String columnName, String defaultValue, String... enumValues) {
		Map<String, Object> column = columnMetadata(tableName, columnName);

		assertThat(column.get("DATA_TYPE")).isEqualTo("enum");
		assertThat(column.get("IS_NULLABLE")).isEqualTo("NO");
		assertThat((String) column.get("COLUMN_TYPE"))
				.contains(enumValues);
		if (defaultValue != null) {
			assertThat(String.valueOf(column.get("COLUMN_DEFAULT")).replace("'", ""))
					.isEqualTo(defaultValue);
		}
	}

	private void assertNullableEnumColumn(String tableName, String columnName, String... enumValues) {
		Map<String, Object> column = columnMetadata(tableName, columnName);

		assertThat(column.get("DATA_TYPE")).isEqualTo("enum");
		assertThat(column.get("IS_NULLABLE")).isEqualTo("YES");
		assertThat((String) column.get("COLUMN_TYPE")).contains(enumValues);
	}

	private Map<String, Object> columnMetadata(String tableName, String columnName) {
		return jdbcTemplate.queryForMap("""
				SELECT data_type, is_nullable, column_default, column_type
				FROM information_schema.columns
				WHERE table_schema = DATABASE()
				  AND table_name = ?
				  AND column_name = ?
				""", tableName, columnName);
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
