package com.threadia.backend.thread.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "threads", uniqueConstraints = @UniqueConstraint(name = "uq_threads_target", columnNames = {"target_type", "target_id"}))
public class Thread {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_threads_project")) private Project project;
	@Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 20) private ThreadTargetType targetType;
	@Column(name = "target_id", nullable = false) private Long targetId;
	@Column(length = 200) private String topic;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_threads_created_by")) private User createdBy;
	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME") private LocalDateTime createdAt;
	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME") private LocalDateTime updatedAt;

	protected Thread() { }
	public Thread(Project project, ThreadTargetType targetType, Long targetId, String topic, User createdBy, LocalDateTime createdAt) {
		this.project = project; this.targetType = targetType; this.targetId = targetId; this.topic = topic; this.createdBy = createdBy; this.createdAt = createdAt; this.updatedAt = createdAt;
	}
	public Long getId() { return id; }
	public Project getProject() { return project; }
	public ThreadTargetType getTargetType() { return targetType; }
	public Long getTargetId() { return targetId; }
	public String getTopic() { return topic; }
	public User getCreatedBy() { return createdBy; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void updateTopic(String topic, LocalDateTime updatedAt) { this.topic = topic; this.updatedAt = updatedAt; }
}
