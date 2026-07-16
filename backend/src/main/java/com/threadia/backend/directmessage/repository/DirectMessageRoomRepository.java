package com.threadia.backend.directmessage.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.directmessage.domain.DirectMessageRoom;

import jakarta.persistence.LockModeType;

public interface DirectMessageRoomRepository extends JpaRepository<DirectMessageRoom, Long> {
	@Query("select room from DirectMessageRoom room join fetch room.user1 join fetch room.user2 where room.company.id = :companyId and (room.user1.id = :userId or room.user2.id = :userId) order by room.createdAt desc, room.id desc")
	List<DirectMessageRoom> findByCompanyIdAndParticipant(@Param("companyId") Long companyId, @Param("userId") Long userId);
	@Query("select room from DirectMessageRoom room join fetch room.company join fetch room.user1 join fetch room.user2 where room.company.id = :companyId and room.user1.id = :user1Id and room.user2.id = :user2Id")
	Optional<DirectMessageRoom> findByCompanyIdAndUserPair(@Param("companyId") Long companyId, @Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select room from DirectMessageRoom room join fetch room.company join fetch room.user1 join fetch room.user2 where room.company.id = :companyId and room.user1.id = :user1Id and room.user2.id = :user2Id")
	Optional<DirectMessageRoom> findByCompanyIdAndUserPairForUpdate(@Param("companyId") Long companyId, @Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
	@Query("select room from DirectMessageRoom room join fetch room.company join fetch room.user1 join fetch room.user2 where room.id = :roomId")
	Optional<DirectMessageRoom> findByIdWithParticipants(@Param("roomId") Long roomId);
	@Query("select room from DirectMessageRoom room join fetch room.company join fetch room.user1 join fetch room.user2 where room.user1.id = :userId or room.user2.id = :userId")
	List<DirectMessageRoom> findByParticipant(@Param("userId") Long userId);
	@Modifying
	@Query(value = "insert ignore into direct_message_rooms (company_id, user1_id, user2_id, created_at) values (:companyId, :user1Id, :user2Id, :createdAt)", nativeQuery = true)
	int insertIgnore(@Param("companyId") Long companyId, @Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id, @Param("createdAt") LocalDateTime createdAt);
}
