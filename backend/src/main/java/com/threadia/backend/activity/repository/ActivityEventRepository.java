package com.threadia.backend.activity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.activity.domain.ActivityEvent;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

	@Query("select event from ActivityEvent event where event.project.id = :projectId order by event.createdAt desc, event.id desc")
	Page<ActivityEvent> findPageByProjectId(@Param("projectId") Long projectId, Pageable pageable);
}
