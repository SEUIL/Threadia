package com.threadia.backend.thread.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.thread.domain.Thread;
import com.threadia.backend.thread.domain.ThreadTargetType;

public interface ThreadRepository extends JpaRepository<Thread, Long> {
	@Query("select thread from Thread thread join fetch thread.project project join fetch project.company join fetch thread.createdBy where thread.targetType = :targetType and thread.targetId = :targetId")
	Optional<Thread> findByTargetTypeAndTargetId(@Param("targetType") ThreadTargetType targetType, @Param("targetId") Long targetId);
	@Query("select thread from Thread thread join fetch thread.project project join fetch project.company join fetch thread.createdBy where thread.id = :threadId")
	Optional<Thread> findByIdWithProject(@Param("threadId") Long threadId);
	@Modifying
	@Query(value = "insert ignore into threads (project_id, target_type, target_id, topic, created_by_user_id, created_at, updated_at) values (:projectId, :targetType, :targetId, :topic, :createdByUserId, :createdAt, :updatedAt)", nativeQuery = true)
	int insertIgnore(@Param("projectId") Long projectId, @Param("targetType") String targetType, @Param("targetId") Long targetId, @Param("topic") String topic, @Param("createdByUserId") Long createdByUserId, @Param("createdAt") LocalDateTime createdAt, @Param("updatedAt") LocalDateTime updatedAt);
}
