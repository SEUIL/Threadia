package com.threadia.backend.supportcase.domain;

import java.time.LocalDateTime;

import com.threadia.backend.platform.domain.PlatformAdmin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "support_case_public_replies")
public class SupportCasePublicReply {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "support_case_id", nullable = false, foreignKey = @ForeignKey(name = "fk_scpr_case"))
	private SupportCase supportCase;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "author_platform_admin_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_scpr_author"))
	private PlatformAdmin author;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String body;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	protected SupportCasePublicReply() {
	}

	public SupportCasePublicReply(
			SupportCase supportCase,
			PlatformAdmin author,
			String body,
			LocalDateTime createdAt) {
		this.supportCase = supportCase;
		this.author = author;
		this.body = body;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public SupportCase getSupportCase() {
		return supportCase;
	}

	public PlatformAdmin getAuthor() {
		return author;
	}

	public String getBody() {
		return body;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
