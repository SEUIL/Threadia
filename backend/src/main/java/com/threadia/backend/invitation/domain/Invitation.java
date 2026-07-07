package com.threadia.backend.invitation.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.company.domain.Company;

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
@Table(name = "invitations")
public class Invitation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inv_company"))
	private Company company;

	@Column(name = "project_id")
	private Long projectId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "inviter_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inv_inviter"))
	private User inviter;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "invitee_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inv_invitee"))
	private User invitee;

	@Enumerated(EnumType.STRING)
	@Column(name = "scope_type", nullable = false, length = 20)
	private InvitationScopeType scopeType;

	@Column(nullable = false, length = 50)
	private String role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InvitationStatus status = InvitationStatus.PENDING;

	@Column(name = "expires_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime expiresAt;

	@Column(name = "accepted_at", columnDefinition = "DATETIME")
	private LocalDateTime acceptedAt;

	@Column(name = "declined_at", columnDefinition = "DATETIME")
	private LocalDateTime declinedAt;

	@Column(name = "canceled_at", columnDefinition = "DATETIME")
	private LocalDateTime canceledAt;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime updatedAt;

	protected Invitation() {
	}

	public Invitation(
			Company company,
			Long projectId,
			User inviter,
			User invitee,
			InvitationScopeType scopeType,
			String role,
			InvitationStatus status,
			LocalDateTime expiresAt,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this.company = company;
		this.projectId = projectId;
		this.inviter = inviter;
		this.invitee = invitee;
		this.scopeType = scopeType;
		this.role = role;
		this.status = status;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public User getInviter() {
		return inviter;
	}

	public void setInviter(User inviter) {
		this.inviter = inviter;
	}

	public User getInvitee() {
		return invitee;
	}

	public void setInvitee(User invitee) {
		this.invitee = invitee;
	}

	public InvitationScopeType getScopeType() {
		return scopeType;
	}

	public void setScopeType(InvitationScopeType scopeType) {
		this.scopeType = scopeType;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public InvitationStatus getStatus() {
		return status;
	}

	public void setStatus(InvitationStatus status) {
		this.status = status;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public LocalDateTime getAcceptedAt() {
		return acceptedAt;
	}

	public void setAcceptedAt(LocalDateTime acceptedAt) {
		this.acceptedAt = acceptedAt;
	}

	public LocalDateTime getDeclinedAt() {
		return declinedAt;
	}

	public void setDeclinedAt(LocalDateTime declinedAt) {
		this.declinedAt = declinedAt;
	}

	public LocalDateTime getCanceledAt() {
		return canceledAt;
	}

	public void setCanceledAt(LocalDateTime canceledAt) {
		this.canceledAt = canceledAt;
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
}
