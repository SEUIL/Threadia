package com.threadia.backend.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class ProjectMemberEntityMappingTests {

	@Test
	void projectMemberMapsToProjectMembersSchema() {
		assertTable(ProjectMember.class, "project_members");
		assertNamedUniqueConstraint(ProjectMember.class, "uq_pm_project_user", "project_id", "user_id");
		assertRequiredLazyRelation(ProjectMember.class, "project", Project.class, "project_id", "fk_pm_project");
		assertRequiredLazyRelation(ProjectMember.class, "user", User.class, "user_id", "fk_pm_user");
	}

	@Test
	void projectMemberColumnsMapRoleStatusAndTimestamps() {
		assertStringEnum(ProjectMember.class, "role");
		assertStringEnum(ProjectMember.class, "status");
		assertRequiredColumn(ProjectMember.class, "role", "", 20);
		assertRequiredColumn(ProjectMember.class, "status", "", 20);
		assertDateTimeColumn(ProjectMember.class, "joinedAt", "joined_at");
		assertRequiredDateTimeColumn(ProjectMember.class, "createdAt", "created_at");
		assertRequiredDateTimeColumn(ProjectMember.class, "updatedAt", "updated_at");
	}

	@Test
	void projectMemberEnumsMatchSchemaValues() {
		assertThat(ProjectMemberRole.values()).containsExactly(ProjectMemberRole.PROJECT_ADMIN, ProjectMemberRole.MEMBER);
		assertThat(ProjectMemberStatus.values()).containsExactly(ProjectMemberStatus.ACTIVE, ProjectMemberStatus.REMOVED);
		assertThat(newInstance(ProjectMember.class).getStatus()).isEqualTo(ProjectMemberStatus.ACTIVE);
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

	private static void assertStringEnum(Class<?> entityType, String fieldName) {
		Enumerated enumerated = field(entityType, fieldName).getAnnotation(Enumerated.class);

		assertThat(enumerated).isNotNull();
		assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
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
