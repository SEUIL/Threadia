package com.threadia.backend.notification.domain;

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
@Table(name = "notification_events")
public class NotificationEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "activity_event_id")
	private Long activityEventId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id", foreignKey = @ForeignKey(name = "fk_ne_company"))
	private Company company;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", foreignKey = @ForeignKey(name = "fk_ne_project"))
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_user_id", foreignKey = @ForeignKey(name = "fk_ne_actor"))
	private User actor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private NotificationType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 50)
	private NotificationTargetType targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String body;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata_json", columnDefinition = "JSON")
	private JsonNode metadata;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	protected NotificationEvent() {
	}

	public NotificationEvent(
			Long activityEventId,
			Company company,
			Project project,
			User actor,
			NotificationType type,
			NotificationTargetType targetType,
			Long targetId,
			String title,
			String body,
			JsonNode metadata,
			LocalDateTime createdAt) {
		this.activityEventId = activityEventId;
		this.company = company;
		this.project = project;
		this.actor = actor;
		this.type = type;
		this.targetType = targetType;
		this.targetId = targetId;
		this.title = title;
		this.body = body;
		this.metadata = metadata;
		this.createdAt = createdAt;
	}

	public Long getId() { return id; }
	public Long getActivityEventId() { return activityEventId; }
	public Company getCompany() { return company; }
	public Project getProject() { return project; }
	public User getActor() { return actor; }
	public NotificationType getType() { return type; }
	public NotificationTargetType getTargetType() { return targetType; }
	public Long getTargetId() { return targetId; }
	public String getTitle() { return title; }
	public String getBody() { return body; }
	public JsonNode getMetadata() { return metadata; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
