package com.threadia.backend.projectchat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

class ProjectChatMemberStateRepositoryLockingTests {

	@Test
	void atomicallyInsertsMissingStateBeforePessimisticRowLock() throws Exception {
		Method insertIfAbsent = ProjectChatMemberStateRepository.class.getMethod("insertIfAbsent", Long.class, Long.class, java.time.LocalDateTime.class);
		Query insert = insertIfAbsent.getAnnotation(Query.class);
		assertThat(insertIfAbsent.getAnnotation(Modifying.class)).isNotNull();
		assertThat(insert.nativeQuery()).isTrue();
		assertThat(insert.value().toLowerCase()).contains("insert into project_chat_member_states", "on duplicate key update");

		Method findByIdForUpdate = ProjectChatMemberStateRepository.class.getMethod("findByIdForUpdate", com.threadia.backend.projectchat.domain.ProjectChatMemberStateId.class);
		assertThat(findByIdForUpdate.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
		assertThat(findByIdForUpdate.getAnnotation(Query.class).value()).contains("left join fetch state.lastReadMessage");
	}
}
