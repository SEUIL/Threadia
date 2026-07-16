package com.threadia.backend.projectchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.projectchat.domain.Message;
import com.threadia.backend.projectchat.exception.ProjectChatValidationException;

class ProjectChatCursorCodecTest {

	@Test
	void signsAndVerifiesProjectScopedKeysetPayload() {
		ProjectChatCursorCodec codec = new ProjectChatCursorCodec("test-only-project-chat-cursor-hmac-secret");
		Message message = message(9L, LocalDateTime.parse("2026-07-15T09:00:02"));
		String cursor = codec.encode(20L, message);
		assertThat(cursor).contains(".");
		ProjectChatCursorCodec.Cursor decoded = codec.decode(cursor, 20L);
		assertThat(decoded.id()).isEqualTo(9L);
		assertThat(decoded.createdAt()).isEqualTo(message.getCreatedAt());
	}

	@Test
	void rejectsMalformedTamperedAndOtherProjectCursors() {
		ProjectChatCursorCodec codec = new ProjectChatCursorCodec("test-only-project-chat-cursor-hmac-secret");
		String cursor = codec.encode(20L, message(9L, LocalDateTime.parse("2026-07-15T09:00:02")));
		assertThatThrownBy(() -> codec.decode("not-a-cursor", 20L)).isInstanceOf(ProjectChatValidationException.class);
		String tampered = cursor.substring(0, cursor.length() - 1) + (cursor.endsWith("A") ? "B" : "A");
		assertThatThrownBy(() -> codec.decode(tampered, 20L)).isInstanceOf(ProjectChatValidationException.class);
		assertThatThrownBy(() -> codec.decode(cursor, 21L)).isInstanceOf(ProjectChatValidationException.class);
	}

	@Test
	void rejectsMissingProductionSecret() {
		assertThatThrownBy(() -> new ProjectChatCursorCodec(" ")).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void rejectsSecretShorterThan32Utf8BytesAndAccepts32Bytes() {
		assertThatThrownBy(() -> new ProjectChatCursorCodec("a".repeat(31))).isInstanceOf(IllegalStateException.class);
		assertThat(new ProjectChatCursorCodec("a".repeat(32))).isNotNull();
	}

	private static Message message(Long id, LocalDateTime createdAt) {
		User user = new User("user@example.com", "hash", "user", null, UserStatus.ACTIVE, createdAt, createdAt); setId(user, 1L);
		Company company = new Company("Company", "name", null, null, null, CompanyStatus.ACTIVE, user, createdAt, createdAt); setId(company, 10L);
		Project project = new Project(company, "Project", "PRJ", null, ProjectStatus.ACTIVE, null, null, user, null, null, null, createdAt, createdAt); setId(project, 20L);
		Message message = new Message(project, user, "content", createdAt); setId(message, id); return message;
	}

	private static void setId(Object entity, Long id) { try { Field field = entity.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(entity, id); } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); } }
}
