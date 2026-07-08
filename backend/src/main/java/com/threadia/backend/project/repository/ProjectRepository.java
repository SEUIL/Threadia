package com.threadia.backend.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.project.domain.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	Optional<Project> findByCompany_IdAndProjectKey(Long companyId, String projectKey);

	boolean existsByCompany_IdAndProjectKey(Long companyId, String projectKey);
}
