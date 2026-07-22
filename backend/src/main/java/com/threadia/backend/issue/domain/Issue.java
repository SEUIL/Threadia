package com.threadia.backend.issue.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.projectchat.domain.Message;
import com.threadia.backend.sprint.domain.Sprint;

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_user_id", foreignKey = @ForeignKey(name = "fk_issues_assignee"))
	private User assignee;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sprint_id", foreignKey = @ForeignKey(name = "fk_issues_sprint"))
	private Sprint sprint;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 20)
	private IssueSourceType sourceType = IssueSourceType.MANUAL;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_message_id", foreignKey = @ForeignKey(name = "fk_issues_source_message"))
	private Message sourceMessage;

	@Column(nullable = false, length = 200)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private IssueStatus status = IssueStatus.BACKLOG;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private IssuePriority priority = IssuePriority.MEDIUM;

	@Column(name = "due_date", columnDefinition = "DATE")
	private LocalDate dueDate;

	@Column(name = "trashed_at", columnDefinition = "DATETIME")
	private LocalDateTime trashedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trashed_by_user_id", foreignKey = @ForeignKey(name = "fk_issues_trashed_by"))
	private User trashedBy;

	@Column(name = "restored_at", columnDefinition = "DATETIME")
	private LocalDateTime restoredAt;

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

	public void setDescription(String description) { this.description = description; }
	public void setPriority(IssuePriority priority) { this.priority = priority; }
	public void setAssignee(User assignee) { this.assignee = assignee; }
	public void setSprint(Sprint sprint) { this.sprint = sprint; }
	public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
	public void setTitle(String title) { this.title = title; }
	public void setSourceMessage(Message sourceMessage) { this.sourceType = IssueSourceType.MESSAGE; this.sourceMessage = sourceMessage; }
	public void trash(User actor, LocalDateTime now) { this.trashedAt = now; this.trashedBy = actor; this.updatedAt = now; }
	public void restore(LocalDateTime now) { this.trashedAt = null; this.trashedBy = null; this.restoredAt = now; this.updatedAt = now; }

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

	public String getDescription() { return description; }
	public IssueSourceType getSourceType() { return sourceType; }
	public Message getSourceMessage() { return sourceMessage; }
	public IssuePriority getPriority() { return priority; }
	public User getAssignee() { return assignee; }
	public Sprint getSprint() { return sprint; }
	public LocalDate getDueDate() { return dueDate; }
	public User getTrashedBy() { return trashedBy; }
	public LocalDateTime getRestoredAt() { return restoredAt; }

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
