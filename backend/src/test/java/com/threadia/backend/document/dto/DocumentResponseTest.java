package com.threadia.backend.document.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.document.domain.Document;
import com.threadia.backend.document.domain.DocumentType;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectStatus;

class DocumentResponseTest {
	@Test
	void formatsAllTimestampsAsLiteralUtcZ() {
		LocalDateTime timestamp = LocalDateTime.parse("2026-07-15T12:34:56");
		User user = new User("user@threadia.com", "hash", "사용자", null, UserStatus.ACTIVE, timestamp, timestamp);
		id(user, 1L);
		Company company = new Company("Threadia", "대표", null, null, null, com.threadia.backend.company.domain.CompanyStatus.ACTIVE, user, timestamp, timestamp);
		id(company, 10L);
		Project project = new Project(company, "프로젝트", "THD", null, ProjectStatus.ACTIVE, null, null, user, null, null, null, timestamp, timestamp);
		id(project, 20L);
		Document document = new Document(project, user, "문서", DocumentType.DOC, "내용", timestamp);
		id(document, 30L);
		document.trash(user, timestamp);
		assertThat(DocumentResponse.from(document)).extracting(DocumentResponse::trashedAt, DocumentResponse::createdAt, DocumentResponse::updatedAt).containsOnly("2026-07-15T12:34:56Z");
	}
	private static void id(Object value, Long id) { try { Field field = value.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(value, id); } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); } }
}
