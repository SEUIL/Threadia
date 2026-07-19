package com.threadia.backend.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.notification.domain.NotificationRecipient;
import com.threadia.backend.notification.domain.NotificationType;

import jakarta.persistence.LockModeType;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

	@Query("""
			select recipient from NotificationRecipient recipient
			join fetch recipient.notificationEvent event
			where recipient.recipient.id = :userId
			  and (:companyId is null or event.company.id = :companyId)
			  and (:projectId is null or event.project.id = :projectId)
			  and (:type is null or event.type = :type)
			  and (:unreadOnly = false or recipient.readAt is null)
			  and (:cursorCreatedAt is null or recipient.createdAt < :cursorCreatedAt
			       or (recipient.createdAt = :cursorCreatedAt and recipient.id < :cursorId))
			order by recipient.createdAt desc, recipient.id desc
			""")
	List<NotificationRecipient> findPage(
			@Param("userId") Long userId,
			@Param("companyId") Long companyId,
			@Param("projectId") Long projectId,
			@Param("type") NotificationType type,
			@Param("unreadOnly") boolean unreadOnly,
			@Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
			@Param("cursorId") Long cursorId,
			Pageable pageable);

	@Query("""
			select count(recipient) from NotificationRecipient recipient
			where recipient.recipient.id = :userId
			  and recipient.readAt is null
			  and (:companyId is null or recipient.notificationEvent.company.id = :companyId)
			  and (:projectId is null or recipient.notificationEvent.project.id = :projectId)
			  and (:type is null or recipient.notificationEvent.type = :type)
			""")
	long countUnread(
			@Param("userId") Long userId,
			@Param("companyId") Long companyId,
			@Param("projectId") Long projectId,
			@Param("type") NotificationType type);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select recipient from NotificationRecipient recipient
			where recipient.id = :notificationRecipientId
			  and recipient.recipient.id = :userId
			""")
	Optional<NotificationRecipient> findOwnedByIdForUpdate(
			@Param("notificationRecipientId") Long notificationRecipientId,
			@Param("userId") Long userId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			update notification_recipients recipient
			join notification_events event on event.id = recipient.notification_event_id
			set recipient.read_at = :readAt
			where recipient.recipient_user_id = :userId
			  and recipient.read_at is null
			  and (:companyId is null or event.company_id = :companyId)
			  and (:projectId is null or event.project_id = :projectId)
			  and (:type is null or event.type = :type)
			""", nativeQuery = true)
	int markAllRead(
			@Param("userId") Long userId,
			@Param("companyId") Long companyId,
			@Param("projectId") Long projectId,
			@Param("type") String type,
			@Param("readAt") LocalDateTime readAt);
}
