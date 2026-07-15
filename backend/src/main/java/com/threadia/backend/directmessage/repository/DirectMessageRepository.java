package com.threadia.backend.directmessage.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.directmessage.domain.DirectMessage;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
	@Query("select message from DirectMessage message join fetch message.sender where message.room.id = :roomId order by message.createdAt asc, message.id asc")
	List<DirectMessage> findByRoomIdOrderByCreatedAtAscIdAsc(@Param("roomId") Long roomId);
	@Query("select count(message) from DirectMessage message left join DirectMessageRoomUserState state on state.room.id = message.room.id and state.user.id = :userId where message.room.id = :roomId and message.sender.id <> :userId and (state.lastReadDirectMessage is null or message.id > state.lastReadDirectMessage.id)")
	long countUnreadByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
	@Query("select count(message) from DirectMessage message left join DirectMessageRoomUserState state on state.room.id = message.room.id and state.user.id = :userId where (message.room.user1.id = :userId or message.room.user2.id = :userId) and message.sender.id <> :userId and (state.lastReadDirectMessage is null or message.id > state.lastReadDirectMessage.id)")
	long countUnreadByUserId(@Param("userId") Long userId);
}
