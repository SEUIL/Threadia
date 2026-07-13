package com.threadia.backend.issue.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.issue.domain.Issue;

import jakarta.persistence.LockModeType;

public interface IssueRepository extends JpaRepository<Issue, Long> {

	@Query("""
			select issue
			from Issue issue
			where issue.project.id = :projectId
			  and issue.trashedAt is null
			order by case issue.status
				when com.threadia.backend.issue.domain.IssueStatus.BACKLOG then 0
				when com.threadia.backend.issue.domain.IssueStatus.TODO then 1
				when com.threadia.backend.issue.domain.IssueStatus.IN_PROGRESS then 2
				when com.threadia.backend.issue.domain.IssueStatus.IN_REVIEW then 3
				when com.threadia.backend.issue.domain.IssueStatus.DONE then 4
				end,
				issue.id asc
			""")
	List<Issue> findActiveByProjectIdInDisplayOrder(@Param("projectId") Long projectId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select issue
			from Issue issue
			where issue.project.id = :projectId
			""")
	List<Issue> findByProjectIdForSequenceUpdate(@Param("projectId") Long projectId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select issue
			from Issue issue
			where issue.id = :issueId
			""")
	Optional<Issue> findByIdForUpdate(@Param("issueId") Long issueId);
}
