package com.threadia.backend.invitation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.invitation.domain.Invitation;
import com.threadia.backend.invitation.domain.InvitationScopeType;
import com.threadia.backend.invitation.domain.InvitationStatus;

import jakarta.persistence.LockModeType;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

	List<Invitation> findAllByCompany_IdAndInvitee_IdAndScopeTypeAndStatus(
			Long companyId,
			Long inviteeUserId,
			InvitationScopeType scopeType,
			InvitationStatus status);

	List<Invitation> findByCompany_IdAndScopeTypeOrderByCreatedAtDesc(Long companyId, InvitationScopeType scopeType);

	List<Invitation> findByInvitee_IdAndScopeTypeOrderByCreatedAtDesc(Long inviteeUserId, InvitationScopeType scopeType);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select invitation from Invitation invitation where invitation.id = :invitationId")
	Optional<Invitation> findByIdForUpdate(@Param("invitationId") Long invitationId);
}
