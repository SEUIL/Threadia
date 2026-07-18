package com.threadia.backend.supportcase.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.supportcase.domain.SupportCase;

import jakarta.persistence.LockModeType;

public interface SupportCaseRepository extends JpaRepository<SupportCase, Long> {

	List<SupportCase> findAllByReporter_IdOrderByCreatedAtDescIdDesc(Long reporterUserId);

	Optional<SupportCase> findByIdAndReporter_Id(Long supportCaseId, Long reporterUserId);

	List<SupportCase> findAllByOrderByCreatedAtDescIdDesc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select supportCase from SupportCase supportCase where supportCase.id = :supportCaseId")
	Optional<SupportCase> findByIdForUpdate(@Param("supportCaseId") Long supportCaseId);
}
