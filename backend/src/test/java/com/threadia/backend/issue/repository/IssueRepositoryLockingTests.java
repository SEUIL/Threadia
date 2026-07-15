package com.threadia.backend.issue.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

class IssueRepositoryLockingTests {

	@Test
	void sequenceReadUsesPessimisticCurrentRead() throws Exception {
		Method method = IssueRepository.class.getMethod("findByProjectIdForSequenceUpdate", Long.class);

		assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
		assertThat(method.getAnnotation(Query.class).value())
				.contains("where issue.project.id = :projectId")
				.doesNotContain("max(", "count(");
	}

	@Test
	void statusUpdateLocksOnlyIssueWithoutJoinFetchedProjectLock() throws Exception {
		Method method = IssueRepository.class.getMethod("findByIdForUpdate", Long.class);

		assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
		assertThat(method.getAnnotation(Query.class).value()).doesNotContain("join fetch");
	}
}
