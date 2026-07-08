package com.threadia.backend.project.domain;

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
		name = "project_members",
		uniqueConstraints = @UniqueConstraint(name = "uq_pm_project_user", columnNames = {"project_id", "user_id"}))
public class ProjectMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pm_project"))
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pm_user"))
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectMemberRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectMemberStatus status = ProjectMemberStatus.ACTIVE;

	@Column(name = "joined_at", columnDefinition = "DATETIME")
	private LocalDateTime joinedAt;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime updatedAt;

	protected ProjectMember() {
	}

	public ProjectMember(
			Project project,
			User user,
			ProjectMemberRole role,
			ProjectMemberStatus status,
			LocalDateTime joinedAt,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this.project = project;
		this.user = user;
		this.role = role;
		this.status = status;
		this.joinedAt = joinedAt;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public ProjectMemberRole getRole() {
		return role;
	}

	public void setRole(ProjectMemberRole role) {
		this.role = role;
	}

	public ProjectMemberStatus getStatus() {
		return status;
	}

	public void setStatus(ProjectMemberStatus status) {
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
}
