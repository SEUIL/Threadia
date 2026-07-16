package com.threadia.backend.supportcase.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.supportcase.domain.SupportCaseInternalNote;

public interface SupportCaseInternalNoteRepository extends JpaRepository<SupportCaseInternalNote, Long> {

	List<SupportCaseInternalNote> findAllBySupportCase_IdOrderByCreatedAtAscIdAsc(Long supportCaseId);
}
