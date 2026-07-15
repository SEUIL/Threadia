package com.threadia.backend.directmessage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

class DirectMessageRepositoryQueryTests {

	@Test
	void roomQueriesUseNormalizedPairAndParticipantScope() {
		assertThat(query(DirectMessageRoomRepository.class, "findByCompanyIdAndUserPair", Long.class, Long.class, Long.class))
				.contains("room.company.id = :companyId", "room.user1.id = :user1Id", "room.user2.id = :user2Id");
		assertThat(query(DirectMessageRoomRepository.class, "findByCompanyIdAndUserPairForUpdate", Long.class, Long.class, Long.class))
				.contains("room.company.id = :companyId", "room.user1.id = :user1Id", "room.user2.id = :user2Id");
		assertThat(query(DirectMessageRoomRepository.class, "findByCompanyIdAndParticipant", Long.class, Long.class))
				.contains("room.company.id = :companyId", "room.user1.id = :userId or room.user2.id = :userId");
		assertThat(query(DirectMessageRoomRepository.class, "findByParticipant", Long.class))
				.contains("room.user1.id = :userId or room.user2.id = :userId");
		assertThat(query(DirectMessageRoomRepository.class, "insertIgnore", Long.class, Long.class, Long.class, java.time.LocalDateTime.class))
				.contains("insert ignore into direct_message_rooms");
	}

	@Test
	void concurrentPairRereadUsesPessimisticWriteLock() throws NoSuchMethodException {
		Method lockMethod = DirectMessageRoomRepository.class.getMethod("findByCompanyIdAndUserPairForUpdate", Long.class, Long.class, Long.class);
		assertThat(lockMethod.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
	}

	@Test
	void unreadQueriesAreScopedPerUserAndExcludeOwnMessages() {
		assertThat(query(DirectMessageRepository.class, "countUnreadByRoomIdAndUserId", Long.class, Long.class))
				.contains("state.user.id = :userId", "message.sender.id <> :userId", "message.id > state.lastReadDirectMessage.id");
		assertThat(query(DirectMessageRepository.class, "countUnreadByUserId", Long.class))
				.contains("message.room.user1.id = :userId or message.room.user2.id = :userId", "message.sender.id <> :userId");
	}

	@Test
	void readStateQueryLocksOneUsersStateAndCreatesItIdempotently() throws NoSuchMethodException {
		Method lockMethod = DirectMessageRoomUserStateRepository.class.getMethod("findByRoomIdAndUserIdForUpdate", Long.class, Long.class);
		assertThat(lockMethod.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
		assertThat(query(DirectMessageRoomUserStateRepository.class, "insertIgnore", Long.class, Long.class, java.time.LocalDateTime.class))
				.contains("insert ignore into dm_room_user_states");
	}

	private static String query(Class<?> repositoryType, String methodName, Class<?>... parameterTypes) {
		try {
			return repositoryType.getMethod(methodName, parameterTypes).getAnnotation(Query.class).value();
		} catch (NoSuchMethodException exception) {
			throw new AssertionError(exception);
		}
	}
}
