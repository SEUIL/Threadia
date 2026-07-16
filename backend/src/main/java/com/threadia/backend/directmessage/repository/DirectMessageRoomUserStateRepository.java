package com.threadia.backend.directmessage.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.directmessage.domain.DirectMessageRoomUserState;
import com.threadia.backend.directmessage.domain.DirectMessageRoomUserStateId;

import jakarta.persistence.LockModeType;

public interface DirectMessageRoomUserStateRepository extends JpaRepository<DirectMessageRoomUserState, DirectMessageRoomUserStateId> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select state from DirectMessageRoomUserState state left join fetch state.lastReadDirectMessage where state.room.id = :roomId and state.user.id = :userId")
	Optional<DirectMessageRoomUserState> findByRoomIdAndUserIdForUpdate(@Param("roomId") Long roomId, @Param("userId") Long userId);
	@Modifying
	@Query(value = "insert ignore into dm_room_user_states (room_id, user_id, updated_at) values (:roomId, :userId, :now)", nativeQuery = true)
	int insertIgnore(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("now") LocalDateTime now);
}
