package com.threadia.backend.directmessage.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.company.domain.Company;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

class DirectMessageEntityMappingTests {
	@Test void roomsUseNormalizedPairUniqueConstraintAndRequiredRelations() {
		assertThat(DirectMessageRoom.class.getAnnotation(Entity.class)).isNotNull();
		assertThat(DirectMessageRoom.class.getAnnotation(Table.class).name()).isEqualTo("direct_message_rooms");
		assertThat(DirectMessageRoom.class.getAnnotation(Table.class).uniqueConstraints()[0].name()).isEqualTo("uq_dmr_company_users");
		assertRelation(DirectMessageRoom.class, "company", Company.class, "company_id", "fk_dmr_company", false);
		assertRelation(DirectMessageRoom.class, "user1", User.class, "user1_id", "fk_dmr_user1", false);
		assertRelation(DirectMessageRoom.class, "user2", User.class, "user2_id", "fk_dmr_user2", false);
	}
	@Test void messagesAndReadStateMapExistingSchemaForeignKeys() {
		assertThat(DirectMessage.class.getAnnotation(Table.class).name()).isEqualTo("direct_messages");
		assertRelation(DirectMessage.class, "room", DirectMessageRoom.class, "room_id", "fk_dm_room", false);
		assertRelation(DirectMessage.class, "sender", User.class, "sender_user_id", "fk_dm_sender", false);
		assertThat(DirectMessageRoomUserState.class.getAnnotation(Table.class).name()).isEqualTo("dm_room_user_states");
		assertThat(DirectMessageRoomUserState.class.getAnnotation(IdClass.class).value()).isEqualTo(DirectMessageRoomUserStateId.class);
		assertRelation(DirectMessageRoomUserState.class, "room", DirectMessageRoom.class, "room_id", "fk_dmrus_room", false);
		assertRelation(DirectMessageRoomUserState.class, "user", User.class, "user_id", "fk_dmrus_user", false);
		assertRelation(DirectMessageRoomUserState.class, "lastReadDirectMessage", DirectMessage.class, "last_read_direct_message_id", "fk_dmrus_last_read", true);
	}
	@Test void readPointerOnlyAdvancesMonotonically() {
		User first = user(1L); User second = user(2L); Company company = new Company("company", null, null, null, null, "ACTIVE", first, LocalDateTime.now(), LocalDateTime.now()); setId(company, 10L);
		DirectMessageRoom room = new DirectMessageRoom(company, first, second, LocalDateTime.now()); setId(room, 30L);
		DirectMessage newer = new DirectMessage(room, second, "newer", LocalDateTime.now()); setId(newer, 50L); DirectMessage older = new DirectMessage(room, second, "older", LocalDateTime.now()); setId(older, 40L);
		DirectMessageRoomUserState state = new DirectMessageRoomUserState(room, first, LocalDateTime.now()); state.advanceTo(newer, LocalDateTime.now()); state.advanceTo(older, LocalDateTime.now());
		assertThat(state.getLastReadDirectMessage()).isSameAs(newer);
	}
	private static User user(Long id) { User user = new User("user" + id + "@example.com", "hash", "user", null, com.threadia.backend.auth.domain.UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now()); setId(user, id); return user; }
	private static void setId(Object target, Long id) { try { Field field = target.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(target, id); } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); } }
	private static void assertRelation(Class<?> type, String fieldName, Class<?> target, String column, String foreignKey, boolean nullable) {
		try {
			Field field = type.getDeclaredField(fieldName); ManyToOne relation = field.getAnnotation(ManyToOne.class); JoinColumn join = field.getAnnotation(JoinColumn.class);
			assertThat(field.getType()).isEqualTo(target); assertThat(relation.fetch()).isEqualTo(FetchType.LAZY); assertThat(relation.optional()).isEqualTo(nullable);
			assertThat(join.name()).isEqualTo(column); assertThat(join.nullable()).isEqualTo(nullable); assertThat(join.foreignKey().name()).isEqualTo(foreignKey);
		} catch (NoSuchFieldException exception) { throw new AssertionError(exception); }
	}
}
