package com.threadia.backend.projectchat.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.projectchat.domain.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

	Optional<Message> findByIdAndProject_Id(Long id, Long projectId);

	@Query("""
			select message from Message message
			join fetch message.author
			where message.project.id = :projectId
			  and (:cursorCreatedAt is null or message.createdAt < :cursorCreatedAt
			       or (message.createdAt = :cursorCreatedAt and message.id < :cursorId))
			order by message.createdAt desc, message.id desc
			""")
	List<Message> findPageByProjectId(
			@Param("projectId") Long projectId,
			@Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
			@Param("cursorId") Long cursorId,
			Pageable pageable);

	@Query("""
			select count(message) from Message message
			where message.project.id = :projectId and message.author.id <> :userId
			""")
	long countUnreadWithoutPointer(@Param("projectId") Long projectId, @Param("userId") Long userId);

	@Query("""
			select count(message) from Message message
			where message.project.id = :projectId and message.author.id <> :userId
			  and (message.createdAt > :readCreatedAt
			       or (message.createdAt = :readCreatedAt and message.id > :readMessageId))
			""")
	long countUnreadAfterPointer(
			@Param("projectId") Long projectId,
			@Param("userId") Long userId,
			@Param("readCreatedAt") LocalDateTime readCreatedAt,
			@Param("readMessageId") Long readMessageId);
}
