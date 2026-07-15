package com.threadia.backend.company.domain;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "company_members",
		uniqueConstraints = @UniqueConstraint(name = "uq_cm_company_user", columnNames = {"company_id", "user_id"}))
public class CompanyMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cm_company"))
	private Company company;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cm_user"))
	private User user;

	@Column(name = "department_id")
	private Long departmentId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CompanyMemberRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CompanyMemberStatus status = CompanyMemberStatus.ACTIVE;

	@Column(name = "joined_at", columnDefinition = "DATETIME")
	private LocalDateTime joinedAt;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime updatedAt;

	protected CompanyMember() {
	}

	public CompanyMember(
			Company company,
			User user,
			Long departmentId,
			CompanyMemberRole role,
			CompanyMemberStatus status,
			LocalDateTime joinedAt,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this.company = company;
		this.user = user;
		this.departmentId = departmentId;
		this.role = role;
		this.status = status;
		this.joinedAt = joinedAt;
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

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Long getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(Long departmentId) {
		this.departmentId = departmentId;
	}

	public CompanyMemberRole getRole() {
		return role;
	}

	public void setRole(CompanyMemberRole role) {
		this.role = role;
	}

	public CompanyMemberStatus getStatus() {
		return status;
	}

	public void setStatus(CompanyMemberStatus status) {
		this.status = status;
	}

	public LocalDateTime getJoinedAt() {
		return joinedAt;
	}

	public void setJoinedAt(LocalDateTime joinedAt) {
		this.joinedAt = joinedAt;
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

	public void activate(CompanyMemberRole role, LocalDateTime now) {
		this.role = role;
		this.status = CompanyMemberStatus.ACTIVE;
		this.joinedAt = now;
		this.updatedAt = now;
	}
}
