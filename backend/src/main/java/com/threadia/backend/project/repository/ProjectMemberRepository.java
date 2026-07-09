package com.threadia.backend.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;

import jakarta.persistence.LockModeType;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);

	@Query("""
			select projectMember
			from ProjectMember projectMember
			join fetch projectMember.project project
			join fetch projectMember.user user
			where project.id = :projectId
			  and (:role is null or projectMember.role = :role)
			  and (:status is null or projectMember.status = :status)
			  and (:q is null
			       or lower(user.email) like concat(concat('%', :q), '%')
			       or lower(user.nickname) like concat(concat('%', :q), '%'))
			order by projectMember.id asc
			""")
	List<ProjectMember> findByProjectIdWithFiltersOrderByIdAsc(
			@Param("projectId") Long projectId,
			@Param("role") ProjectMemberRole role,
			@Param("status") ProjectMemberStatus status,
			@Param("q") String q);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select projectMember
			from ProjectMember projectMember
			join fetch projectMember.project project
			join fetch project.company
			join fetch projectMember.user
			where projectMember.id = :projectMemberId
			""")
	Optional<ProjectMember> findByIdForUpdate(@Param("projectMemberId") Long projectMemberId);

	boolean existsByProject_IdAndUser_IdAndStatus(Long projectId, Long userId, ProjectMemberStatus status);

	long countByProject_IdAndStatus(Long projectId, ProjectMemberStatus status);

	long countByProject_IdAndRoleAndStatus(Long projectId, ProjectMemberRole role, ProjectMemberStatus status);
}
