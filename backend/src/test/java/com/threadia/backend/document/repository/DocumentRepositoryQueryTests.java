package com.threadia.backend.document.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class DocumentRepositoryQueryTests {
	@Test
	void detailAndLinkReadQueriesFetchRequiredRelations() throws Exception {
		assertThat(query(DocumentRepository.class, "findByProjectFilters")).contains("join fetch document.project", "join fetch document.author");
		assertThat(query(DocumentRepository.class, "findByIdWithProject")).contains("join fetch document.project", "join fetch document.author");
		assertThat(query(DocumentIssueLinkRepository.class, "findByProjectIdWithEndpoints")).contains("join fetch link.project", "join fetch link.document", "join fetch document.project", "join fetch document.author", "join fetch link.issue", "join fetch link.createdBy");
		assertThat(query(DocumentIssueLinkRepository.class, "findVisibleByIssueId")).contains("join fetch document.project", "join fetch document.author", "document.trashedAt is null", "issue.trashedAt is null");
		assertThat(query(DocumentIssueLinkRepository.class, "findVisibleByDocumentId")).contains("join fetch document.project", "join fetch document.author", "document.trashedAt is null", "issue.trashedAt is null");
	}
	private static String query(Class<?> type, String methodName) throws Exception { for (Method method : type.getMethods()) { if (method.getName().equals(methodName)) return method.getAnnotation(Query.class).value(); } throw new AssertionError("query not found"); }
}
