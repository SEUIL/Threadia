package com.threadia.backend.activity.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.project.domain.Project;

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
@Table(name = "activity_events")
public class ActivityEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ae_company"))
	private Company company;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", foreignKey = @ForeignKey(name = "fk_ae_project"))
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_user_id", foreignKey = @ForeignKey(name = "fk_ae_actor"))
	private User actor;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 50)
	private ActivityEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 50)
	private ActivityTargetType targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata_json", columnDefinition = "JSON")
	private JsonNode metadata;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	protected ActivityEvent() {
	}

	public ActivityEvent(Company company, Project project, User actor, ActivityEventType eventType,
			ActivityTargetType targetType, Long targetId, JsonNode metadata, LocalDateTime createdAt) {
		this.company = company;
		this.project = project;
		this.actor = actor;
		this.eventType = eventType;
		this.targetType = targetType;
		this.targetId = targetId;
		this.metadata = metadata;
		this.createdAt = createdAt;
	}

	public Long getId() { return id; }
	public Company getCompany() { return company; }
	public Project getProject() { return project; }
	public User getActor() { return actor; }
	public ActivityEventType getEventType() { return eventType; }
	public ActivityTargetType getTargetType() { return targetType; }
	public Long getTargetId() { return targetId; }
	public JsonNode getMetadata() { return metadata; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
