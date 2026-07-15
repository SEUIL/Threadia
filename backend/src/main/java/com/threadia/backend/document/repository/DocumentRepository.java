package com.threadia.backend.document.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threadia.backend.document.domain.Document;
import com.threadia.backend.document.domain.DocumentType;

import jakarta.persistence.LockModeType;

public interface DocumentRepository extends JpaRepository<Document, Long> {
	@Query("select document from Document document join fetch document.project project join fetch document.author where project.id = :projectId and (:type is null or document.type = :type) and (:includeTrashed = true or document.trashedAt is null) and (:q is null or lower(document.title) like concat('%', lower(:q), '%')) order by document.id asc")
	List<Document> findByProjectFilters(@Param("projectId") Long projectId, @Param("type") DocumentType type, @Param("q") String q, @Param("includeTrashed") boolean includeTrashed);
	@Query("select document from Document document join fetch document.project project join fetch project.company join fetch document.author where document.id = :documentId")
	Optional<Document> findByIdWithProject(@Param("documentId") Long documentId);
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select document from Document document join fetch document.project project join fetch project.company join fetch document.author where document.id = :documentId")
	Optional<Document> findByIdForUpdate(@Param("documentId") Long documentId);
	@Query("select document from Document document where document.project.id = :projectId and document.trashedAt is null order by document.id asc")
	List<Document> findActiveByProjectId(@Param("projectId") Long projectId);
}
