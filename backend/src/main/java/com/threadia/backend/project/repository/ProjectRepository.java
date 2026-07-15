package com.threadia.backend.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.project.domain.Project;

import jakarta.persistence.LockModeType;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	Optional<Project> findByCompany_IdAndProjectKey(Long companyId, String projectKey);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select project
			from Project project
			where project.id = :projectId
			""")
	Optional<Project> findByIdForUpdate(@Param("projectId") Long projectId);

	List<Project> findByCompany_IdOrderByIdAsc(Long companyId);

	@Query("""
			select project
			from Project project
			join ProjectMember projectMember on projectMember.project = project
			where project.company.id = :companyId
			  and projectMember.user.id = :userId
			  and projectMember.status = com.threadia.backend.project.domain.ProjectMemberStatus.ACTIVE
			order by project.id asc
			""")
	List<Project> findActiveMemberProjectsByCompanyIdAndUserIdOrderByIdAsc(
			@Param("companyId") Long companyId,
			@Param("userId") Long userId);

	boolean existsByCompany_IdAndProjectKey(Long companyId, String projectKey);
}
