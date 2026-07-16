package com.threadia.backend.projectchat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.project.domain.Project;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

class ProjectChatEntityMappingTests {

	@Test
	void messageMapsImmutableProjectMessageColumns() throws Exception {
		assertThat(Message.class.getAnnotation(Entity.class)).isNotNull();
		assertThat(Message.class.getAnnotation(Table.class).name()).isEqualTo("messages");
		assertJoin(Message.class, "project", Project.class, "project_id", "fk_messages_project");
		assertJoin(Message.class, "author", User.class, "author_user_id", "fk_messages_author");
		assertThat(Message.class.getDeclaredField("threadId").getAnnotation(jakarta.persistence.Column.class).name()).isEqualTo("thread_id");
		assertThat(Message.class.getDeclaredField("content").getAnnotation(jakarta.persistence.Column.class).nullable()).isFalse();
	}

	@Test
	void stateUsesProjectAndUserCompositeKeyWithMapsId() throws Exception {
		assertThat(ProjectChatMemberState.class.getAnnotation(Table.class).name()).isEqualTo("project_chat_member_states");
		assertThat(ProjectChatMemberState.class.getDeclaredField("id").getAnnotation(EmbeddedId.class)).isNotNull();
		assertMapsId(ProjectChatMemberState.class, "project", "projectId", "project_id", "fk_pcms_project");
		assertMapsId(ProjectChatMemberState.class, "user", "userId", "user_id", "fk_pcms_user");
		assertThat(ProjectChatMemberState.class.getDeclaredField("lastReadMessage").getAnnotation(JoinColumn.class).foreignKey().name())
				.isEqualTo("fk_pcms_last_read");
		ProjectChatMemberStateId id = new ProjectChatMemberStateId(20L, 1L);
		assertThat(id).isEqualTo(new ProjectChatMemberStateId(20L, 1L));
	}

	private static void assertJoin(Class<?> type, String fieldName, Class<?> target, String column, String foreignKey) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		assertThat(field.getType()).isEqualTo(target);
		assertThat(joinColumn.name()).isEqualTo(column);
		assertThat(joinColumn.foreignKey().name()).isEqualTo(foreignKey);
	}

	private static void assertMapsId(Class<?> type, String fieldName, String value, String column, String foreignKey) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		assertThat(field.getAnnotation(MapsId.class).value()).isEqualTo(value);
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		assertThat(joinColumn.name()).isEqualTo(column);
		assertThat(joinColumn.foreignKey().name()).isEqualTo(foreignKey);
	}
}
