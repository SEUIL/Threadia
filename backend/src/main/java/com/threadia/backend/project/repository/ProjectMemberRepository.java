package com.threadia.backend.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.dto.ProjectMemberLockPreview;

import jakarta.persistence.LockModeType;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);

	@Query("""
			select new com.threadia.backend.project.dto.ProjectMemberLockPreview(
			    projectMember.project.id,
			    projectMember.project.company.id,
			    projectMember.user.id
			)
			from ProjectMember projectMember
			where projectMember.id = :projectMemberId
			""")
	Optional<ProjectMemberLockPreview> findLockPreviewById(@Param("projectMemberId") Long projectMemberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select projectMember
			from ProjectMember projectMember
			where projectMember.project.id = :projectId
			  and projectMember.user.id = :userId
			""")
	Optional<ProjectMember> findByProject_IdAndUser_IdForUpdate(
			@Param("projectId") Long projectId,
			@Param("userId") Long userId);

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
			where projectMember.project.id = :projectId
			  and (
			      projectMember.id = :projectMemberId
			      or (
			          projectMember.role = :adminRole
			          and projectMember.status = :activeStatus
			      )
			  )
			order by projectMember.id asc
			""")
	List<ProjectMember> findTargetAndActiveProjectAdminsByProjectIdForUpdate(
			@Param("projectId") Long projectId,
			@Param("projectMemberId") Long projectMemberId,
			@Param("adminRole") ProjectMemberRole adminRole,
			@Param("activeStatus") ProjectMemberStatus activeStatus);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select projectMember
			from ProjectMember projectMember
			join projectMember.project project
			where project.company.id = :companyId
			  and projectMember.status = :activeStatus
			  and (
			      projectMember.user.id = :userId
			      or (
			          projectMember.role = :adminRole
			          and project.id in (
			              select targetProjectMember.project.id
			              from ProjectMember targetProjectMember
			              where targetProjectMember.project.company.id = :companyId
			                and targetProjectMember.user.id = :userId
			                and targetProjectMember.status = :activeStatus
			          )
			      )
			  )
			order by project.id asc, projectMember.id asc
			""")
	List<ProjectMember> findActiveTargetMembershipsAndActiveProjectAdminsInTargetProjectsForUpdate(
			@Param("companyId") Long companyId,
			@Param("userId") Long userId,
			@Param("adminRole") ProjectMemberRole adminRole,
			@Param("activeStatus") ProjectMemberStatus activeStatus);

	boolean existsByProject_IdAndUser_IdAndStatus(Long projectId, Long userId, ProjectMemberStatus status);

	long countByProject_IdAndStatus(Long projectId, ProjectMemberStatus status);

	long countByProject_IdAndRoleAndStatus(Long projectId, ProjectMemberRole role, ProjectMemberStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update ProjectMember projectMember
			set projectMember.status = :removedStatus,
			    projectMember.updatedAt = :updatedAt
			where projectMember.user.id = :userId
			  and projectMember.status = :activeStatus
			  and projectMember.project.company.id = :companyId
			""")
	int removeActiveMembershipsByCompanyIdAndUserId(
			@Param("companyId") Long companyId,
			@Param("userId") Long userId,
			@Param("activeStatus") ProjectMemberStatus activeStatus,
			@Param("removedStatus") ProjectMemberStatus removedStatus,
			@Param("updatedAt") java.time.LocalDateTime updatedAt);
}
