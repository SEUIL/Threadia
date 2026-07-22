package com.threadia.backend.document.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.document.domain.DocumentIssueLink;

public interface DocumentIssueLinkRepository extends JpaRepository<DocumentIssueLink, Long> {
	boolean existsByDocument_IdAndIssue_Id(Long documentId, Long issueId);
	@Query("select link from DocumentIssueLink link join fetch link.project project join fetch link.document document join fetch document.project join fetch document.author join fetch link.issue issue join fetch link.createdBy where project.id = :projectId order by link.id asc")
	List<DocumentIssueLink> findByProjectIdWithEndpoints(@Param("projectId") Long projectId);
	@Query("select link from DocumentIssueLink link join fetch link.project join fetch link.document document join fetch document.project join fetch document.author join fetch link.issue issue join fetch link.createdBy where issue.id = :issueId and document.trashedAt is null and issue.trashedAt is null order by link.id asc")
	List<DocumentIssueLink> findVisibleByIssueId(@Param("issueId") Long issueId);
	@Query("select link from DocumentIssueLink link join fetch link.project join fetch link.document document join fetch document.project join fetch document.author join fetch link.issue issue join fetch link.createdBy where document.id = :documentId and document.trashedAt is null and issue.trashedAt is null order by link.id asc")
	List<DocumentIssueLink> findVisibleByDocumentId(@Param("documentId") Long documentId);
	@Query("""
			select distinct recipient
			from DocumentIssueLink link, User recipient, ProjectMember projectMember, CompanyMember companyMember
			where link.document.id = :documentId
			  and link.document.trashedAt is null
			  and link.issue.trashedAt is null
			  and (recipient = link.issue.reporter or recipient = link.issue.assignee)
			  and recipient.status = com.threadia.backend.auth.domain.UserStatus.ACTIVE
			  and recipient.id <> :excludedUserId
			  and projectMember.project.id = link.project.id
			  and projectMember.user.id = recipient.id
			  and projectMember.status = com.threadia.backend.project.domain.ProjectMemberStatus.ACTIVE
			  and companyMember.company.id = link.project.company.id
			  and companyMember.user.id = recipient.id
			  and companyMember.status = com.threadia.backend.company.domain.CompanyMemberStatus.ACTIVE
			order by recipient.id asc
			""")
	List<User> findActiveNotificationRecipientsByDocumentIdExcludingUserId(
			@Param("documentId") Long documentId,
			@Param("excludedUserId") Long excludedUserId);
	@Query("select link from DocumentIssueLink link join fetch link.project project join fetch project.company join fetch link.createdBy where link.id = :linkId")
	Optional<DocumentIssueLink> findByIdWithProject(@Param("linkId") Long linkId);
}
