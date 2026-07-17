package com.threadia.backend.notification.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "notification_recipients",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_nr_event_recipient",
				columnNames = {"notification_event_id", "recipient_user_id"}),
		indexes = @Index(
				name = "idx_nr_recipient_read_created",
				columnList = "recipient_user_id, read_at, created_at, id"))
public class NotificationRecipient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "notification_event_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_nr_event"))
	private NotificationEvent notificationEvent;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "recipient_user_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_nr_recipient"))
	private User recipient;

	@Column(name = "read_at", columnDefinition = "DATETIME")
	private LocalDateTime readAt;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	protected NotificationRecipient() {
	}

	public NotificationRecipient(
			NotificationEvent notificationEvent,
			User recipient,
			LocalDateTime readAt,
			LocalDateTime createdAt) {
		this.notificationEvent = notificationEvent;
		this.recipient = recipient;
		this.readAt = readAt;
		this.createdAt = createdAt;
	}

	public void markRead(LocalDateTime readAt) {
		if (this.readAt == null) {
			this.readAt = readAt;
		}
	}

	public Long getId() { return id; }
	public NotificationEvent getNotificationEvent() { return notificationEvent; }
	public User getRecipient() { return recipient; }
	public LocalDateTime getReadAt() { return readAt; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
