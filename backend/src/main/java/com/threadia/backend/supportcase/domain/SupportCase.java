package com.threadia.backend.supportcase.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.supportcase.exception.SupportCaseConflictException;

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
@Table(name = "support_cases")
public class SupportCase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reporter_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_support_cases_reporter"))
	private User reporter;

	@Enumerated(EnumType.STRING)
	@Column(name = "case_type", nullable = false, length = 20)
	private SupportCaseType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SupportCaseStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", length = 20)
	private SupportCaseTargetType targetType;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "target_id", columnDefinition = "CHAR(36)")
	private UUID targetId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String body;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime updatedAt;

	protected SupportCase() {
	}

	public SupportCase(
			User reporter,
			SupportCaseType type,
			SupportCaseTargetType targetType,
			UUID targetId,
			String title,
			String body,
			LocalDateTime createdAt) {
		this.reporter = reporter;
		this.type = type;
		this.status = SupportCaseStatus.OPEN;
		this.targetType = targetType;
		this.targetId = targetId;
		this.title = title;
		this.body = body;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public void transitionTo(SupportCaseStatus nextStatus, LocalDateTime changedAt) {
		if (nextStatus == null || !status.canTransitionTo(nextStatus)) {
			throw new SupportCaseConflictException("허용되지 않은 support case 상태 전이입니다.");
		}
		status = nextStatus;
		updatedAt = changedAt;
	}

	public Long getId() {
		return id;
	}

	public User getReporter() {
		return reporter;
	}

	public SupportCaseType getType() {
		return type;
	}

	public SupportCaseStatus getStatus() {
		return status;
	}

	public SupportCaseTargetType getTargetType() {
		return targetType;
	}

	public UUID getTargetId() {
		return targetId;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
