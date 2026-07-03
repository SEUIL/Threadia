package com.threadia.backend.auth.domain;

import java.time.LocalDateTime;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "refresh_tokens",
		uniqueConstraints = @UniqueConstraint(name = "uq_rt_token_hash", columnNames = "token_hash"))
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rt_user"))
	private User user;

	@Column(name = "token_hash", nullable = false, length = 255)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime expiresAt;

	@Column(name = "rotated_at", columnDefinition = "DATETIME")
	private LocalDateTime rotatedAt;

	@Column(name = "revoked_at", columnDefinition = "DATETIME")
	private LocalDateTime revokedAt;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime updatedAt;

	protected RefreshToken() {
	}

	public RefreshToken(
			User user,
			String tokenHash,
			LocalDateTime expiresAt,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public LocalDateTime getRotatedAt() {
		return rotatedAt;
	}

	public void setRotatedAt(LocalDateTime rotatedAt) {
		this.rotatedAt = rotatedAt;
	}

	public LocalDateTime getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(LocalDateTime revokedAt) {
		this.revokedAt = revokedAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public void revoke(LocalDateTime revokedAt) {
		this.revokedAt = revokedAt;
		this.updatedAt = revokedAt;
	}

	public void rotate(LocalDateTime rotatedAt) {
		this.rotatedAt = rotatedAt;
		revoke(rotatedAt);
	}
}
