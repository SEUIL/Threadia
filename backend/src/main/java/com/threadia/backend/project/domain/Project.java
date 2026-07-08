package com.threadia.backend.project.domain;

import java.time.LocalDate;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "projects",
		uniqueConstraints = @UniqueConstraint(name = "uq_projects_company_key", columnNames = {"company_id", "project_key"}))
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_projects_company"))
	private Company company;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(name = "project_key", nullable = false, length = 20)
	private String projectKey;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectStatus status = ProjectStatus.PLANNING;

	@Column(name = "start_date", columnDefinition = "DATE")
	private LocalDate startDate;

	@Column(name = "end_date", columnDefinition = "DATE")
	private LocalDate endDate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_projects_created_by"))
	private User createdBy;

	@Column(name = "archived_at", columnDefinition = "DATETIME")
	private LocalDateTime archivedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "archived_by_user_id", foreignKey = @ForeignKey(name = "fk_projects_archived_by"))
	private User archivedBy;

	@Column(name = "archive_reason", columnDefinition = "TEXT")
	private String archiveReason;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime updatedAt;

	protected Project() {
	}

	public Project(
			Company company,
			String name,
			String projectKey,
			String description,
			ProjectStatus status,
			LocalDate startDate,
			LocalDate endDate,
			User createdBy,
			LocalDateTime archivedAt,
			User archivedBy,
			String archiveReason,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this.company = company;
		this.name = name;
		this.projectKey = projectKey;
		this.description = description;
		this.status = status;
		this.startDate = startDate;
		this.endDate = endDate;
		this.createdBy = createdBy;
		this.archivedAt = archivedAt;
		this.archivedBy = archivedBy;
		this.archiveReason = archiveReason;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ProjectStatus getStatus() {
		return status;
	}

	public void setStatus(ProjectStatus status) {
		this.status = status;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public LocalDateTime getArchivedAt() {
		return archivedAt;
	}

	public void setArchivedAt(LocalDateTime archivedAt) {
		this.archivedAt = archivedAt;
	}

	public User getArchivedBy() {
		return archivedBy;
	}

	public void setArchivedBy(User archivedBy) {
		this.archivedBy = archivedBy;
	}

	public String getArchiveReason() {
		return archiveReason;
	}

	public void setArchiveReason(String archiveReason) {
		this.archiveReason = archiveReason;
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
