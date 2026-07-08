package com.threadia.backend.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.company.domain.Company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class ProjectEntityMappingTests {

	@Test
	void projectMapsToProjectsSchema() {
		assertTable(Project.class, "projects");
		assertNamedUniqueConstraint(Project.class, "uq_projects_company_key", "company_id", "project_key");
		assertRequiredLazyRelation(Project.class, "company", Company.class, "company_id", "fk_projects_company");
		assertRequiredLazyRelation(Project.class, "createdBy", User.class, "created_by_user_id", "fk_projects_created_by");
		assertNullableLazyRelation(Project.class, "archivedBy", User.class, "archived_by_user_id", "fk_projects_archived_by");
	}

	@Test
	void projectColumnsMapStatusDatesAndArchiveFields() {
		assertRequiredColumn(Project.class, "name", "", 150);
		assertRequiredColumn(Project.class, "projectKey", "project_key", 20);
		assertThat(column(Project.class, "description").columnDefinition()).isEqualTo("TEXT");
		assertStringEnum(Project.class, "status");
		assertRequiredColumn(Project.class, "status", "", 20);
		assertDateColumn(Project.class, "startDate", "start_date");
		assertDateColumn(Project.class, "endDate", "end_date");
		assertDateTimeColumn(Project.class, "archivedAt", "archived_at");
		assertThat(column(Project.class, "archiveReason").name()).isEqualTo("archive_reason");
		assertThat(column(Project.class, "archiveReason").columnDefinition()).isEqualTo("TEXT");
		assertRequiredDateTimeColumn(Project.class, "createdAt", "created_at");
		assertRequiredDateTimeColumn(Project.class, "updatedAt", "updated_at");
	}

	@Test
	void projectEnumsMatchSchemaValues() {
		assertThat(ProjectStatus.values()).containsExactly(
				ProjectStatus.PLANNING,
				ProjectStatus.ACTIVE,
				ProjectStatus.PAUSED,
				ProjectStatus.COMPLETED,
				ProjectStatus.ARCHIVED);
		assertThat(newInstance(Project.class).getStatus()).isEqualTo(ProjectStatus.PLANNING);
	}

	private static void assertTable(Class<?> entityType, String tableName) {
		assertThat(entityType.getAnnotation(Entity.class)).isNotNull();
		assertThat(entityType.getAnnotation(Table.class).name()).isEqualTo(tableName);
	}

	private static void assertNamedUniqueConstraint(Class<?> entityType, String constraintName, String... columnNames) {
		UniqueConstraint[] uniqueConstraints = entityType.getAnnotation(Table.class).uniqueConstraints();

		assertThat(uniqueConstraints)
				.anySatisfy(constraint -> {
					assertThat(constraint.name()).isEqualTo(constraintName);
					assertThat(constraint.columnNames()).containsExactly(columnNames);
				});
	}

	private static void assertRequiredLazyRelation(
			Class<?> entityType,
			String fieldName,
			Class<?> targetType,
			String columnName,
			String foreignKeyName) {
		Field relation = field(entityType, fieldName);
		ManyToOne manyToOne = relation.getAnnotation(ManyToOne.class);
		JoinColumn joinColumn = relation.getAnnotation(JoinColumn.class);

		assertThat(relation.getType()).isEqualTo(targetType);
		assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
		assertThat(manyToOne.optional()).isFalse();
		assertThat(joinColumn.name()).isEqualTo(columnName);
		assertThat(joinColumn.nullable()).isFalse();
		assertThat(joinColumn.foreignKey().name()).isEqualTo(foreignKeyName);
	}

	private static void assertNullableLazyRelation(
			Class<?> entityType,
			String fieldName,
			Class<?> targetType,
			String columnName,
			String foreignKeyName) {
		Field relation = field(entityType, fieldName);
		ManyToOne manyToOne = relation.getAnnotation(ManyToOne.class);
		JoinColumn joinColumn = relation.getAnnotation(JoinColumn.class);

		assertThat(relation.getType()).isEqualTo(targetType);
		assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
		assertThat(manyToOne.optional()).isTrue();
		assertThat(joinColumn.name()).isEqualTo(columnName);
		assertThat(joinColumn.nullable()).isTrue();
		assertThat(joinColumn.foreignKey().name()).isEqualTo(foreignKeyName);
	}

	private static void assertStringEnum(Class<?> entityType, String fieldName) {
		Enumerated enumerated = field(entityType, fieldName).getAnnotation(Enumerated.class);

		assertThat(enumerated).isNotNull();
		assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
	}

	private static void assertDateColumn(Class<?> entityType, String fieldName, String columnName) {
		Field date = field(entityType, fieldName);
		Column column = date.getAnnotation(Column.class);

		assertThat(date.getType()).isEqualTo(LocalDate.class);
		assertThat(column.name()).isEqualTo(columnName);
		assertThat(column.columnDefinition()).isEqualTo("DATE");
	}

	private static void assertDateTimeColumn(Class<?> entityType, String fieldName, String columnName) {
		Field dateTime = field(entityType, fieldName);
		Column column = dateTime.getAnnotation(Column.class);

		assertThat(dateTime.getType()).isEqualTo(LocalDateTime.class);
		assertThat(column.name()).isEqualTo(columnName);
		assertThat(column.columnDefinition()).isEqualTo("DATETIME");
	}

	private static void assertRequiredDateTimeColumn(Class<?> entityType, String fieldName, String columnName) {
		assertDateTimeColumn(entityType, fieldName, columnName);
		assertThat(column(entityType, fieldName).nullable()).isFalse();
	}

	private static void assertRequiredColumn(
			Class<?> entityType,
			String fieldName,
			String columnName,
			int length) {
		Column column = column(entityType, fieldName);

		assertThat(column.name()).isEqualTo(columnName);
		assertThat(column.nullable()).isFalse();
		assertThat(column.length()).isEqualTo(length);
	}

	private static Column column(Class<?> entityType, String fieldName) {
		return field(entityType, fieldName).getAnnotation(Column.class);
	}

	private static Field field(Class<?> entityType, String fieldName) {
		try {
			return entityType.getDeclaredField(fieldName);
		} catch (NoSuchFieldException exception) {
			throw new AssertionError("Missing field " + entityType.getSimpleName() + "." + fieldName, exception);
		}
	}

	private static <T> T newInstance(Class<T> entityType) {
		try {
			var constructor = entityType.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError("Cannot instantiate " + entityType.getSimpleName(), exception);
		}
	}
}
