package com.threadia.backend.attachment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.threadia.backend.attachment.domain.Attachment;
import com.threadia.backend.attachment.domain.AttachmentStatus;
import com.threadia.backend.attachment.domain.AttachmentTargetType;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
	List<Attachment> findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtAsc(AttachmentTargetType targetType, Long targetId, AttachmentStatus status);
	List<Attachment> findByStatusAndDeletedAtIsNotNull(AttachmentStatus status);
	List<Attachment> findByStatusAndCreatedAtBefore(AttachmentStatus status, LocalDateTime createdAt);
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@org.springframework.data.jpa.repository.Query("select attachment from Attachment attachment where attachment.id = :id")
	Optional<Attachment> findByIdForUpdate(@Param("id") Long id);
}
