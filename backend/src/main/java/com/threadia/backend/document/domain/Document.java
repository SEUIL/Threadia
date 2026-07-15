package com.threadia.backend.document.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.project.domain.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents")
public class Document {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_documents_project")) private Project project;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_documents_author")) private User author;
	@Column(nullable = false, length = 200) private String title;
	@Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false, length = 20) private DocumentType type;
	@Column(nullable = false, columnDefinition = "LONGTEXT") private String content;
	@Column(name = "trashed_at", columnDefinition = "DATETIME") private LocalDateTime trashedAt;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "trashed_by_user_id", foreignKey = @ForeignKey(name = "fk_documents_trashed_by")) private User trashedBy;
	@Column(name = "restored_at", columnDefinition = "DATETIME") private LocalDateTime restoredAt;
	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME") private LocalDateTime createdAt;
	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME") private LocalDateTime updatedAt;

	protected Document() { }
	public Document(Project project, User author, String title, DocumentType type, String content, LocalDateTime createdAt) {
		this.project = project; this.author = author; this.title = title; this.type = type; this.content = content; this.createdAt = createdAt; this.updatedAt = createdAt;
	}
	public Long getId() { return id; }
	public Project getProject() { return project; }
	public User getAuthor() { return author; }
	public String getTitle() { return title; }
	public DocumentType getType() { return type; }
	public String getContent() { return content; }
	public LocalDateTime getTrashedAt() { return trashedAt; }
	public LocalDateTime getRestoredAt() { return restoredAt; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void update(String title, DocumentType type, String content, LocalDateTime updatedAt) { this.title = title; this.type = type; this.content = content; this.updatedAt = updatedAt; }
	public void trash(User actor, LocalDateTime trashedAt) { if (this.trashedAt == null) { this.trashedAt = trashedAt; this.trashedBy = actor; this.updatedAt = trashedAt; } }
	public void restore(LocalDateTime restoredAt) { if (this.trashedAt != null) { this.trashedAt = null; this.trashedBy = null; this.restoredAt = restoredAt; this.updatedAt = restoredAt; } }
}
