package com.threadia.backend.platform.settings;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
	Optional<SystemSetting> findByKey(SystemSettingKey key);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
	@Query("select setting from SystemSetting setting where setting.key = :key")
	Optional<SystemSetting> findByKeyForUpdate(@Param("key") SystemSettingKey key);
}
