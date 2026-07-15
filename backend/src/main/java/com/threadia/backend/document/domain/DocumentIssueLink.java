package com.threadia.backend.document.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.issue.domain.Issue;
import com.threadia.backend.project.domain.Project;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "document_issue_links", uniqueConstraints = @UniqueConstraint(name = "uq_dil_document_issue", columnNames = {"document_id", "issue_id"}))
public class DocumentIssueLink {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dil_project")) private Project project;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dil_document")) private Document document;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "issue_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dil_issue")) private Issue issue;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dil_created_by")) private User createdBy;
	@jakarta.persistence.Column(name = "created_at", nullable = false, columnDefinition = "DATETIME") private LocalDateTime createdAt;
	protected DocumentIssueLink() { }
	public DocumentIssueLink(Project project, Document document, Issue issue, User createdBy, LocalDateTime createdAt) { this.project = project; this.document = document; this.issue = issue; this.createdBy = createdBy; this.createdAt = createdAt; }
	public Long getId() { return id; }
	public Project getProject() { return project; }
	public Document getDocument() { return document; }
	public Issue getIssue() { return issue; }
	public User getCreatedBy() { return createdBy; }
}
