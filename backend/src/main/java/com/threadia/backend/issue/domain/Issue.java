package com.threadia.backend.issue.domain;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "issues",
		uniqueConstraints = @UniqueConstraint(name = "uq_issues_project_key", columnNames = {"project_id", "issue_key"}))
public class Issue {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_issues_project"))
	private Project project;

	@Column(name = "issue_key", nullable = false, length = 40)
	private String issueKey;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reporter_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_issues_reporter"))
	private User reporter;

	@Column(nullable = false, length = 200)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private IssueStatus status = IssueStatus.BACKLOG;

	@Column(name = "trashed_at", columnDefinition = "DATETIME")
	private LocalDateTime trashedAt;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime updatedAt;

	protected Issue() {
	}

	public Issue(
			Project project,
			String issueKey,
			User reporter,
			String title,
			IssueStatus status,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this.project = project;
		this.issueKey = issueKey;
		this.reporter = reporter;
		this.title = title;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public Project getProject() {
		return project;
	}

	public String getIssueKey() {
		return issueKey;
	}

	public User getReporter() {
		return reporter;
	}

	public String getTitle() {
		return title;
	}

	public IssueStatus getStatus() {
		return status;
	}

	public void setStatus(IssueStatus status) {
		this.status = status;
	}

	public LocalDateTime getTrashedAt() {
		return trashedAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
