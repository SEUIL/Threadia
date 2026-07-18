package com.threadia.backend.platform.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.domain.PlatformAdminStatus;

import jakarta.persistence.LockModeType;

public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, Long> {

	Optional<PlatformAdmin> findByUser_Id(Long userId);

	Optional<PlatformAdmin> findByUser_IdAndStatus(Long userId, PlatformAdminStatus status);

	List<PlatformAdmin> findAllByOrderByCreatedAtAscIdAsc();

	long countByStatusAndRole(PlatformAdminStatus status, PlatformAdminRole role);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select admin from PlatformAdmin admin where admin.id = :id")
	Optional<PlatformAdmin> findByIdForUpdate(@Param("id") Long id);
}
