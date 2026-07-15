package com.threadia.backend.document.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.issue.domain.Issue;
import com.threadia.backend.project.domain.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

class DocumentEntityMappingTests {
	@Test
	void documentMapsExistingDocumentsSchema() {
		assertThat(Document.class.getAnnotation(Entity.class)).isNotNull();
		assertThat(Document.class.getAnnotation(Table.class).name()).isEqualTo("documents");
		assertRelation("project", Project.class, "project_id", "fk_documents_project");
		assertRelation("author", User.class, "author_user_id", "fk_documents_author");
		assertThat(column("title").length()).isEqualTo(200);
		assertThat(field("type").getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
		assertThat(column("content").columnDefinition()).isEqualTo("LONGTEXT");
		assertThat(column("trashedAt").name()).isEqualTo("trashed_at");
		assertThat(column("restoredAt").name()).isEqualTo("restored_at");
	}
	@Test
	void documentIssueLinkMapsExistingForeignKeysAndUniquePair() {
		assertThat(DocumentIssueLink.class.getAnnotation(Table.class).name()).isEqualTo("document_issue_links");
		assertRelation(DocumentIssueLink.class, "document", Document.class, "document_id", "fk_dil_document");
		assertRelation(DocumentIssueLink.class, "issue", Issue.class, "issue_id", "fk_dil_issue");
		assertThat(DocumentIssueLink.class.getAnnotation(Table.class).uniqueConstraints()).anySatisfy(unique -> assertThat(unique.columnNames()).containsExactly("document_id", "issue_id"));
	}
	private static void assertRelation(String name, Class<?> type, String column, String foreignKey) { assertRelation(Document.class, name, type, column, foreignKey); }
	private static void assertRelation(Class<?> owner, String name, Class<?> type, String column, String foreignKey) { Field field = field(owner, name); assertThat(field.getType()).isEqualTo(type); assertThat(field.getAnnotation(ManyToOne.class).optional()).isFalse(); assertThat(field.getAnnotation(JoinColumn.class).name()).isEqualTo(column); assertThat(field.getAnnotation(JoinColumn.class).foreignKey().name()).isEqualTo(foreignKey); }
	private static Column column(String name) { return field(name).getAnnotation(Column.class); }
	private static Field field(String name) { return field(Document.class, name); }
	private static Field field(Class<?> type, String name) { try { return type.getDeclaredField(name); } catch (NoSuchFieldException exception) { throw new AssertionError(exception); } }
}
