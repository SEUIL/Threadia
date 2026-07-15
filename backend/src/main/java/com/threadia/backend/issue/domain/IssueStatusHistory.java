package com.threadia.backend.issue.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;

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
@Table(name = "issue_status_history")
public class IssueStatusHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "issue_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ish_issue"))
	private Issue issue;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "changed_by_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ish_changed_by"))
	private User changedBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 20)
	private IssueStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 20)
	private IssueStatus toStatus;

	@Column(name = "changed_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime changedAt;

	protected IssueStatusHistory() {
	}

	public IssueStatusHistory(
			Issue issue,
			User changedBy,
			IssueStatus fromStatus,
			IssueStatus toStatus,
			LocalDateTime changedAt) {
		this.issue = issue;
		this.changedBy = changedBy;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.changedAt = changedAt;
	}

	public Long getId() {
		return id;
	}

	public Issue getIssue() {
		return issue;
	}

	public User getChangedBy() {
		return changedBy;
	}

	public IssueStatus getFromStatus() {
		return fromStatus;
	}

	public IssueStatus getToStatus() {
		return toStatus;
	}

	public LocalDateTime getChangedAt() {
		return changedAt;
	}
}
