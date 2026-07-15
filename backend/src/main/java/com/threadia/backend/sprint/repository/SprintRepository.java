package com.threadia.backend.sprint.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.threadia.backend.sprint.domain.*;
import jakarta.persistence.LockModeType;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
	List<Sprint> findByProject_IdOrderByIdAsc(Long projectId);
	@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from Sprint s where s.id = :id") Optional<Sprint> findByIdForUpdate(@Param("id") Long id);
	boolean existsByProject_IdAndStatus(Long projectId, SprintStatus status);
}
