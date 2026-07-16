package com.threadia.backend.supportcase.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.supportcase.domain.SupportCasePublicReply;

public interface SupportCasePublicReplyRepository extends JpaRepository<SupportCasePublicReply, Long> {

	List<SupportCasePublicReply> findAllBySupportCase_IdOrderByCreatedAtAscIdAsc(Long supportCaseId);
}
