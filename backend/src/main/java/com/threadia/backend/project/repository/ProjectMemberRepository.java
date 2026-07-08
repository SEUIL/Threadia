package com.threadia.backend.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberStatus;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);

	boolean existsByProject_IdAndUser_IdAndStatus(Long projectId, Long userId, ProjectMemberStatus status);

	long countByProject_IdAndStatus(Long projectId, ProjectMemberStatus status);
}
