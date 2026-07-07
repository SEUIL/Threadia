package com.threadia.backend.invitation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.invitation.domain.Invitation;
import com.threadia.backend.invitation.domain.InvitationScopeType;
import com.threadia.backend.invitation.domain.InvitationStatus;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

	boolean existsByCompany_IdAndInvitee_IdAndScopeTypeAndStatus(
			Long companyId,
			Long inviteeUserId,
			InvitationScopeType scopeType,
			InvitationStatus status);

	List<Invitation> findByCompany_IdAndScopeTypeOrderByCreatedAtDesc(Long companyId, InvitationScopeType scopeType);
}
