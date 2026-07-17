package com.threadia.backend.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.project.domain.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

class NotificationEntityMappingTests {

	@Test
	void eventMapsExistingNotificationEventColumnsAndCatalogs() throws Exception {
		assertThat(NotificationEvent.class.getAnnotation(Entity.class)).isNotNull();
		assertThat(NotificationEvent.class.getAnnotation(Table.class).name()).isEqualTo("notification_events");
		assertJoin(NotificationEvent.class, "company", Company.class, "company_id", "fk_ne_company");
		assertJoin(NotificationEvent.class, "project", Project.class, "project_id", "fk_ne_project");
		assertJoin(NotificationEvent.class, "actor", User.class, "actor_user_id", "fk_ne_actor");
		assertColumn(NotificationEvent.class, "activityEventId", "activity_event_id", true);
		assertColumn(NotificationEvent.class, "targetType", "target_type", false);
		assertColumn(NotificationEvent.class, "targetId", "target_id", false);
		assertColumn(NotificationEvent.class, "metadata", "metadata_json", true);
		assertThat(NotificationType.values()).containsExactly(
				NotificationType.ISSUE_ASSIGNED,
				NotificationType.ISSUE_STATUS_CHANGED,
				NotificationType.THREAD_COMMENTED,
				NotificationType.MENTIONED,
				NotificationType.DOCUMENT_UPDATED,
				NotificationType.DOCUMENT_LINKED_TO_ISSUE,
				NotificationType.SPRINT_STARTED,
				NotificationType.SPRINT_ENDED,
				NotificationType.PROJECT_MESSAGE,
				NotificationType.DIRECT_MESSAGE,
				NotificationType.MEMBER_ADDED,
				NotificationType.ROLE_CHANGED);
		assertThat(NotificationTargetType.values()).containsExactly(
				NotificationTargetType.ISSUE,
				NotificationTargetType.DOCUMENT,
				NotificationTargetType.MESSAGE,
				NotificationTargetType.THREAD,
				NotificationTargetType.DIRECT_MESSAGE_ROOM,
				NotificationTargetType.PROJECT,
				NotificationTargetType.SPRINT);
	}

	@Test
	void recipientMapsOwnershipAndPreservesFirstReadTime() throws Exception {
		assertThat(NotificationRecipient.class.getAnnotation(Table.class).name()).isEqualTo("notification_recipients");
		assertJoin(NotificationRecipient.class, "notificationEvent", NotificationEvent.class,
				"notification_event_id", "fk_nr_event");
		assertJoin(NotificationRecipient.class, "recipient", User.class, "recipient_user_id", "fk_nr_recipient");
		assertColumn(NotificationRecipient.class, "readAt", "read_at", true);
		assertColumn(NotificationRecipient.class, "createdAt", "created_at", false);

		LocalDateTime createdAt = LocalDateTime.parse("2026-07-17T10:00:00");
		LocalDateTime firstReadAt = LocalDateTime.parse("2026-07-17T10:01:00");
		NotificationRecipient recipient = new NotificationRecipient(null, null, null, createdAt);
		recipient.markRead(firstReadAt);
		recipient.markRead(firstReadAt.plusMinutes(1));

		assertThat(recipient.getReadAt()).isEqualTo(firstReadAt);
	}

	private static void assertJoin(
			Class<?> type,
			String fieldName,
			Class<?> target,
			String columnName,
			String foreignKeyName) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		assertThat(field.getType()).isEqualTo(target);
		assertThat(joinColumn.name()).isEqualTo(columnName);
		assertThat(joinColumn.foreignKey().name()).isEqualTo(foreignKeyName);
	}

	private static void assertColumn(
			Class<?> type,
			String fieldName,
			String columnName,
			boolean nullable) throws Exception {
		Column column = type.getDeclaredField(fieldName).getAnnotation(Column.class);
		assertThat(column.name()).isEqualTo(columnName);
		assertThat(column.nullable()).isEqualTo(nullable);
	}
}
