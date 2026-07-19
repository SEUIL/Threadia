package com.threadia.backend.platform.domain;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "platform_admins",
		uniqueConstraints = @UniqueConstraint(name = "uq_platform_admins_user", columnNames = "user_id"))
public class PlatformAdmin {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_platform_admins_user"))
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PlatformAdminRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PlatformAdminStatus status;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	@Column(name = "revoked_at", columnDefinition = "DATETIME")
	private LocalDateTime revokedAt;

	@Column(name = "reactivated_at", columnDefinition = "DATETIME")
	private LocalDateTime reactivatedAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime updatedAt;

	protected PlatformAdmin() {
	}

	public PlatformAdmin(User user, PlatformAdminRole role, LocalDateTime createdAt) {
		this.user = user;
		this.role = role;
		this.status = PlatformAdminStatus.ACTIVE;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public void revoke(LocalDateTime revokedAt) {
		this.status = PlatformAdminStatus.REVOKED;
		this.revokedAt = revokedAt;
		this.updatedAt = revokedAt;
	}

	public void reactivate(LocalDateTime reactivatedAt) {
		this.status = PlatformAdminStatus.ACTIVE;
		this.revokedAt = null;
		this.reactivatedAt = reactivatedAt;
		this.updatedAt = reactivatedAt;
	}

	public void changeRole(PlatformAdminRole role, LocalDateTime updatedAt) {
		this.role = role;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public PlatformAdminRole getRole() {
		return role;
	}

	public PlatformAdminStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getRevokedAt() {
		return revokedAt;
	}

	public LocalDateTime getReactivatedAt() {
		return reactivatedAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
