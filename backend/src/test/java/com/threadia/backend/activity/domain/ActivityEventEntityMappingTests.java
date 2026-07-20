package com.threadia.backend.activity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.project.domain.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

class ActivityEventEntityMappingTests {

	@Test
	void mapsExistingActivityEventsSchemaWithoutSchemaChanges() throws Exception {
		assertThat(ActivityEvent.class.getAnnotation(Entity.class)).isNotNull();
		assertThat(ActivityEvent.class.getAnnotation(Table.class).name()).isEqualTo("activity_events");
		assertRelation("company", Company.class, "company_id", "fk_ae_company", false);
		assertRelation("project", Project.class, "project_id", "fk_ae_project", true);
		assertRelation("actor", User.class, "actor_user_id", "fk_ae_actor", true);
		assertThat(column("eventType").name()).isEqualTo("event_type");
		assertThat(column("targetType").name()).isEqualTo("target_type");
		assertThat(column("targetId").name()).isEqualTo("target_id");
		assertThat(column("metadata").name()).isEqualTo("metadata_json");
		assertThat(column("createdAt").name()).isEqualTo("created_at");
	}

	private static void assertRelation(String name, Class<?> target, String columnName, String foreignKey, boolean nullable) throws Exception {
		Field field = ActivityEvent.class.getDeclaredField(name);
		ManyToOne relation = field.getAnnotation(ManyToOne.class);
		JoinColumn column = field.getAnnotation(JoinColumn.class);
		assertThat(field.getType()).isEqualTo(target);
		assertThat(relation.fetch()).isEqualTo(FetchType.LAZY);
		assertThat(column.name()).isEqualTo(columnName);
		assertThat(column.foreignKey().name()).isEqualTo(foreignKey);
		assertThat(column.nullable()).isEqualTo(nullable);
	}

	private static Column column(String name) throws Exception { return ActivityEvent.class.getDeclaredField(name).getAnnotation(Column.class); }
}
