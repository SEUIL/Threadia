package com.threadia.backend.issue.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.project.domain.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

class IssueEntityMappingTests {

	@Test
	void issueMapsMinimumVerticalSliceToIssuesSchema() {
		assertTable(Issue.class, "issues");
		assertRequiredLazyRelation(Issue.class, "project", Project.class, "project_id", "fk_issues_project");
		assertRequiredLazyRelation(Issue.class, "reporter", User.class, "reporter_user_id", "fk_issues_reporter");
		assertRequiredColumn(Issue.class, "issueKey", "issue_key", 40);
		assertRequiredColumn(Issue.class, "title", "", 200);
		assertStringEnum(Issue.class, "status");
		assertRequiredColumn(Issue.class, "status", "", 20);
		assertDateTimeColumn(Issue.class, "trashedAt", "trashed_at", true);
		assertDateTimeColumn(Issue.class, "createdAt", "created_at", false);
		assertDateTimeColumn(Issue.class, "updatedAt", "updated_at", false);
		assertThat(Issue.class.getAnnotation(Table.class).uniqueConstraints())
				.anySatisfy(constraint -> {
					assertThat(constraint.name()).isEqualTo("uq_issues_project_key");
					assertThat(constraint.columnNames()).containsExactly("project_id", "issue_key");
				});
	}

	@Test
	void statusHistoryMapsNullablePriorStateAndRequiredActor() {
		assertTable(IssueStatusHistory.class, "issue_status_history");
		assertRequiredLazyRelation(IssueStatusHistory.class, "issue", Issue.class, "issue_id", "fk_ish_issue");
		assertRequiredLazyRelation(IssueStatusHistory.class, "changedBy", User.class, "changed_by_user_id", "fk_ish_changed_by");
		assertStringEnum(IssueStatusHistory.class, "fromStatus");
		assertThat(column(IssueStatusHistory.class, "fromStatus").nullable()).isTrue();
		assertStringEnum(IssueStatusHistory.class, "toStatus");
		assertThat(column(IssueStatusHistory.class, "toStatus").nullable()).isFalse();
		assertDateTimeColumn(IssueStatusHistory.class, "changedAt", "changed_at", false);
	}

	@Test
	void issueStatusesMatchFixedKanbanColumns() {
		assertThat(IssueStatus.values()).containsExactly(
				IssueStatus.BACKLOG,
				IssueStatus.TODO,
				IssueStatus.IN_PROGRESS,
				IssueStatus.IN_REVIEW,
				IssueStatus.DONE);
		assertThat(newInstance(Issue.class).getStatus()).isEqualTo(IssueStatus.BACKLOG);
	}

	private static void assertTable(Class<?> type, String name) {
		assertThat(type.getAnnotation(Entity.class)).isNotNull();
		assertThat(type.getAnnotation(Table.class).name()).isEqualTo(name);
	}

	private static void assertRequiredLazyRelation(
			Class<?> type,
			String fieldName,
			Class<?> targetType,
			String columnName,
			String foreignKeyName) {
		Field field = field(type, fieldName);
		ManyToOne relation = field.getAnnotation(ManyToOne.class);
		JoinColumn join = field.getAnnotation(JoinColumn.class);
		assertThat(field.getType()).isEqualTo(targetType);
		assertThat(relation.fetch()).isEqualTo(FetchType.LAZY);
		assertThat(relation.optional()).isFalse();
		assertThat(join.name()).isEqualTo(columnName);
		assertThat(join.nullable()).isFalse();
		assertThat(join.foreignKey().name()).isEqualTo(foreignKeyName);
	}

	private static void assertRequiredColumn(Class<?> type, String fieldName, String columnName, int length) {
		Column column = column(type, fieldName);
		assertThat(column.name()).isEqualTo(columnName);
		assertThat(column.nullable()).isFalse();
		assertThat(column.length()).isEqualTo(length);
	}

	private static void assertStringEnum(Class<?> type, String fieldName) {
		assertThat(field(type, fieldName).getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
	}

	private static void assertDateTimeColumn(Class<?> type, String fieldName, String columnName, boolean nullable) {
		Field field = field(type, fieldName);
		Column column = field.getAnnotation(Column.class);
		assertThat(field.getType()).isEqualTo(LocalDateTime.class);
		assertThat(column.name()).isEqualTo(columnName);
		assertThat(column.nullable()).isEqualTo(nullable);
		assertThat(column.columnDefinition()).isEqualTo("DATETIME");
	}

	private static Column column(Class<?> type, String fieldName) {
		return field(type, fieldName).getAnnotation(Column.class);
	}

	private static Field field(Class<?> type, String fieldName) {
		try {
			return type.getDeclaredField(fieldName);
		} catch (NoSuchFieldException exception) {
			throw new AssertionError("Missing field " + type.getSimpleName() + "." + fieldName, exception);
		}
	}

	private static <T> T newInstance(Class<T> type) {
		try {
			var constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError("Cannot instantiate " + type.getSimpleName(), exception);
		}
	}
}
