package com.threadia.backend.platform.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.platform.domain.PlatformAdminBootstrapLock;

import jakarta.persistence.LockModeType;

public interface PlatformAdminBootstrapLockRepository
		extends JpaRepository<PlatformAdminBootstrapLock, Integer> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select bootstrapLock from PlatformAdminBootstrapLock bootstrapLock where bootstrapLock.id = :id")
	Optional<PlatformAdminBootstrapLock> findByIdForUpdate(@Param("id") Integer id);
}
