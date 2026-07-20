package com.threadia.backend.company.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "companies")
public class Company {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(name = "representative_name", length = 100)
	private String representativeName;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "industry_category", length = 100)
	private String industryCategory;

	@Column(name = "logo_url", length = 500)
	private String logoUrl;

	@Column(nullable = false, length = 50)
	@Convert(converter = CompanyStatusConverter.class)
	private CompanyStatus status = CompanyStatus.ACTIVE;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_companies_created_by"))
	private User createdBy;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime updatedAt;

	protected Company() {
	}

	public Company(
			String name,
			String representativeName,
			String description,
			String industryCategory,
			String logoUrl,
			CompanyStatus status,
			User createdBy,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this.name = name;
		this.representativeName = representativeName;
		this.description = description;
		this.industryCategory = industryCategory;
		this.logoUrl = logoUrl;
		this.status = status;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public Company(
			String name,
			String representativeName,
			String description,
			String industryCategory,
			String logoUrl,
			String status,
			User createdBy,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this(
				name,
				representativeName,
				description,
				industryCategory,
				logoUrl,
				CompanyStatus.valueOf(status),
				createdBy,
				createdAt,
				updatedAt);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRepresentativeName() {
		return representativeName;
	}

	public void setRepresentativeName(String representativeName) {
		this.representativeName = representativeName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getIndustryCategory() {
		return industryCategory;
	}

	public void setIndustryCategory(String industryCategory) {
		this.industryCategory = industryCategory;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	public CompanyStatus getStatus() {
		return status;
	}

	public void setStatus(CompanyStatus status) {
		this.status = status;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
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
