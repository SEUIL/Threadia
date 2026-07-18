package com.threadia.backend.platform.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.platform.domain.PlatformAdmin;

public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, Long> {

	Optional<PlatformAdmin> findByUser_Id(Long userId);
}
