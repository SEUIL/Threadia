package com.threadia.backend.thread.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.thread.domain.ThreadComment;

public interface ThreadCommentRepository extends JpaRepository<ThreadComment, Long> {
	@Query("select comment from ThreadComment comment join fetch comment.author where comment.thread.id = :threadId order by comment.id asc")
	List<ThreadComment> findByThreadIdOrderByIdAsc(@Param("threadId") Long threadId);
}
