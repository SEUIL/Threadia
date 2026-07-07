package com.threadia.backend.invitation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.invitation.domain.Invitation;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
}
