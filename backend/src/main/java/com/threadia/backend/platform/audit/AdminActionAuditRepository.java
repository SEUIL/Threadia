package com.threadia.backend.platform.audit;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdminActionAuditRepository extends JpaRepository<AdminActionAudit, Long> {

	List<AdminActionAudit> findAllBySupportCase_IdOrderByCreatedAtAscIdAsc(Long supportCaseId);

	@Query("""
			select audit from AdminActionAudit audit
			where (:actorUserId is null or audit.actor.id = :actorUserId)
			  and (:action is null or audit.action = :action)
			  and (:targetType is null or audit.targetType = :targetType)
			  and (:targetId is null or audit.targetId = :targetId)
			  and (:supportCaseId is null or audit.supportCase.id = :supportCaseId)
			  and (:createdFrom is null or audit.createdAt >= :createdFrom)
			  and (:createdTo is null or audit.createdAt < :createdTo)
			  and (:cursorCreatedAt is null or audit.createdAt < :cursorCreatedAt
			       or (audit.createdAt = :cursorCreatedAt and audit.id < :cursorId))
			order by audit.createdAt desc, audit.id desc
			""")
	List<AdminActionAudit> findAuditPage(
			@Param("actorUserId") Long actorUserId,
			@Param("action") AdminAction action,
			@Param("targetType") AuditTargetType targetType,
			@Param("targetId") Long targetId,
			@Param("supportCaseId") Long supportCaseId,
			@Param("createdFrom") LocalDateTime createdFrom,
			@Param("createdTo") LocalDateTime createdTo,
			@Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
			@Param("cursorId") Long cursorId,
			Pageable pageable);
}
