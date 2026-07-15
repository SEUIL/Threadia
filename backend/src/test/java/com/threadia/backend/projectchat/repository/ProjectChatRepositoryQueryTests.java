package com.threadia.backend.projectchat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.data.domain.PageRequest;

import com.threadia.backend.projectchat.ProjectChatMysqlSchemaInitializer;

@SpringBootTest
@ContextConfiguration(initializers = ProjectChatMysqlSchemaInitializer.class)
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_SCHEMA_VALIDATION", matches = "true")
class ProjectChatRepositoryQueryTests {
	@Autowired private MessageRepository messageRepository;

	@Test
	void compilesAndExecutesKeysetAndUnreadJpaQueriesAgainstMysql() {
		assertThat(messageRepository.findPageByProjectId(-1L, LocalDateTime.now(), Long.MAX_VALUE, PageRequest.of(0, 1))).isEmpty();
		assertThat(messageRepository.countUnreadWithoutPointer(-1L, -1L)).isZero();
		assertThat(messageRepository.countUnreadAfterPointer(-1L, -1L, LocalDateTime.now(), 0L)).isZero();
	}
}
