package com.threadia.backend.projectchat.repository;

import java.util.Optional;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.projectchat.domain.ProjectChatMemberState;
import com.threadia.backend.projectchat.domain.ProjectChatMemberStateId;

import jakarta.persistence.LockModeType;

public interface ProjectChatMemberStateRepository extends JpaRepository<ProjectChatMemberState, ProjectChatMemberStateId> {

	@Modifying
	@Query(value = """
			insert into project_chat_member_states (project_id, user_id, updated_at)
			values (:projectId, :userId, :updatedAt)
			on duplicate key update project_id = values(project_id)
			""", nativeQuery = true)
	int insertIfAbsent(
			@Param("projectId") Long projectId,
			@Param("userId") Long userId,
			@Param("updatedAt") LocalDateTime updatedAt);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select state from ProjectChatMemberState state
			left join fetch state.lastReadMessage
			where state.id = :id
			""")
	Optional<ProjectChatMemberState> findByIdForUpdate(@Param("id") ProjectChatMemberStateId id);
}
