package com.threadia.backend.platform.audit;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionAuditRepository extends JpaRepository<AdminActionAudit, Long> {

	List<AdminActionAudit> findAllBySupportCase_IdOrderByCreatedAtAscIdAsc(Long supportCaseId);
}
