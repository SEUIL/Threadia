package com.threadia.backend.document.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.Query;

import com.threadia.backend.document.domain.Document;
import com.threadia.backend.document.domain.DocumentIssueLink;
import com.threadia.backend.document.domain.DocumentType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
class DocumentRepositoryJpaQueryCompilationTests {
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void hibernateCompilesDocumentRepositoryFetchQueriesInSpringContext() throws Exception {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			assertThat(entityManager.createQuery(query(DocumentRepository.class, "findByProjectFilters"), Document.class)
					.setParameter("projectId", 20L)
					.setParameter("type", DocumentType.DOC)
					.setParameter("q", "api")
					.setParameter("includeTrashed", false))
					.isNotNull();
			assertThat(entityManager.createQuery(query(DocumentRepository.class, "findByIdWithProject"), Document.class)
					.setParameter("documentId", 1L))
					.isNotNull();
			assertThat(entityManager.createQuery(query(DocumentRepository.class, "findActiveByProjectId"), Document.class)
					.setParameter("projectId", 20L))
					.isNotNull();
		} finally {
			entityManager.close();
		}
	}

	@Test
	void hibernateCompilesLinkFetchAndTrashedVisibilityQueriesInSpringContext() throws Exception {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			assertThat(entityManager.createQuery(query(DocumentIssueLinkRepository.class, "findByProjectIdWithEndpoints"), DocumentIssueLink.class)
					.setParameter("projectId", 20L))
					.isNotNull();
			assertThat(entityManager.createQuery(query(DocumentIssueLinkRepository.class, "findVisibleByIssueId"), DocumentIssueLink.class)
					.setParameter("issueId", 2L))
					.isNotNull();
			assertThat(entityManager.createQuery(query(DocumentIssueLinkRepository.class, "findVisibleByDocumentId"), DocumentIssueLink.class)
					.setParameter("documentId", 1L))
					.isNotNull();
			assertThat(entityManager.createQuery(query(DocumentIssueLinkRepository.class, "findByIdWithProject"), DocumentIssueLink.class)
					.setParameter("linkId", 10L))
					.isNotNull();
		} finally {
			entityManager.close();
		}
	}

	private static String query(Class<?> repositoryType, String methodName) throws Exception {
		for (Method method : repositoryType.getMethods()) {
			if (method.getName().equals(methodName)) {
				return method.getAnnotation(Query.class).value();
			}
		}
		throw new AssertionError("query not found: " + methodName);
	}
}
