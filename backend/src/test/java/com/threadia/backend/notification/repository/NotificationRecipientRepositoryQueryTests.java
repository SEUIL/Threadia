package com.threadia.backend.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.threadia.backend.notification.domain.NotificationType;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.projectchat.ProjectChatMysqlSchemaInitializer;

@SpringBootTest
@ContextConfiguration(initializers = ProjectChatMysqlSchemaInitializer.class)
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_SCHEMA_VALIDATION", matches = "true")
class NotificationRecipientRepositoryQueryTests {

	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;
	@Autowired private NotificationRecipientRepository repository;

	@Test
	void compilesAndExecutesInboxCountOwnershipAndBulkReadQueriesAgainstMysql() {
		LocalDateTime cursorCreatedAt = LocalDateTime.parse("2026-07-17T10:00:00");
		assertThat(repository.findPage(
				-1L,
				null,
				null,
				NotificationType.MENTIONED,
				true,
				cursorCreatedAt,
				Long.MAX_VALUE,
				PageRequest.of(0, 1))).isEmpty();
		assertThat(repository.countUnread(-1L, null, null, null)).isZero();
		assertThat(repository.findOwnedByIdForUpdate(-1L, -1L)).isEmpty();
		assertThat(repository.markAllRead(-1L, null, null, null, cursorCreatedAt)).isZero();
	}
}
