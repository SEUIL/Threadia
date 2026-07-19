package com.threadia.backend.thread.domain;

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

class ThreadEntityMappingTests {
	@Test
	void threadMapsToExistingRepresentativeThreadSchema() {
		assertThat(Thread.class.getAnnotation(Entity.class)).isNotNull();
		assertThat(Thread.class.getAnnotation(Table.class).name()).isEqualTo("threads");
		assertThat(Thread.class.getAnnotation(Table.class).uniqueConstraints()).anySatisfy(constraint -> {
			assertThat(constraint.name()).isEqualTo("uq_threads_target");
			assertThat(constraint.columnNames()).containsExactly("target_type", "target_id");
		});
		assertRelation(Thread.class, "project", Project.class, "project_id", "fk_threads_project");
		assertRelation(Thread.class, "createdBy", User.class, "created_by_user_id", "fk_threads_created_by");
		assertThat(field(Thread.class, "targetType").getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
		assertColumn(Thread.class, "targetType", "target_type", false, 20);
		assertColumn(Thread.class, "targetId", "target_id", false, 255);
		assertColumn(Thread.class, "topic", "", true, 200);
		assertDateTime(Thread.class, "createdAt", "created_at");
		assertDateTime(Thread.class, "updatedAt", "updated_at");
	}

	@Test
	void threadCommentMapsToAppendOnlyCommentSchema() {
		assertThat(ThreadComment.class.getAnnotation(Entity.class)).isNotNull();
		assertThat(ThreadComment.class.getAnnotation(Table.class).name()).isEqualTo("thread_comments");
		assertRelation(ThreadComment.class, "thread", Thread.class, "thread_id", "fk_tc_thread");
		assertRelation(ThreadComment.class, "author", User.class, "author_user_id", "fk_tc_author");
		Column content = field(ThreadComment.class, "content").getAnnotation(Column.class);
		assertThat(content.nullable()).isFalse();
		assertThat(content.columnDefinition()).isEqualTo("TEXT");
		assertDateTime(ThreadComment.class, "createdAt", "created_at");
		assertDateTime(ThreadComment.class, "updatedAt", "updated_at");
	}

	private static void assertRelation(Class<?> type, String name, Class<?> target, String columnName, String foreignKey) {
		Field field = field(type, name); ManyToOne relation = field.getAnnotation(ManyToOne.class); JoinColumn join = field.getAnnotation(JoinColumn.class);
		assertThat(field.getType()).isEqualTo(target); assertThat(relation.fetch()).isEqualTo(FetchType.LAZY); assertThat(relation.optional()).isFalse(); assertThat(join.name()).isEqualTo(columnName); assertThat(join.nullable()).isFalse(); assertThat(join.foreignKey().name()).isEqualTo(foreignKey);
	}
	private static void assertColumn(Class<?> type, String name, String columnName, boolean nullable, int length) { Column column = field(type, name).getAnnotation(Column.class); assertThat(column.name()).isEqualTo(columnName); assertThat(column.nullable()).isEqualTo(nullable); assertThat(column.length()).isEqualTo(length); }
	private static void assertDateTime(Class<?> type, String name, String columnName) { Column column = field(type, name).getAnnotation(Column.class); assertThat(field(type, name).getType()).isEqualTo(LocalDateTime.class); assertThat(column.name()).isEqualTo(columnName); assertThat(column.nullable()).isFalse(); assertThat(column.columnDefinition()).isEqualTo("DATETIME"); }
	private static Field field(Class<?> type, String name) { try { return type.getDeclaredField(name); } catch (NoSuchFieldException exception) { throw new AssertionError("Missing field " + type.getSimpleName() + "." + name, exception); } }
}
