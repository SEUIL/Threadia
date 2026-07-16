package com.threadia.backend.projectchat.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.project.domain.Project;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_chat_member_states")
public class ProjectChatMemberState {

	@EmbeddedId
	private ProjectChatMemberStateId id;

	@MapsId("projectId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pcms_project"))
	private Project project;

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pcms_user"))
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "last_read_message_id", foreignKey = @ForeignKey(name = "fk_pcms_last_read"))
	private Message lastReadMessage;

	@Column(name = "last_read_at", columnDefinition = "DATETIME")
	private LocalDateTime lastReadAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime updatedAt;

	protected ProjectChatMemberState() {
	}

	public ProjectChatMemberState(Project project, User user, Message lastReadMessage, LocalDateTime readAt) {
		this.id = new ProjectChatMemberStateId(project.getId(), user.getId());
		this.project = project;
		this.user = user;
		this.lastReadMessage = lastReadMessage;
		this.lastReadAt = readAt;
		this.updatedAt = readAt;
	}

	public void moveReadPointerTo(Message message, LocalDateTime readAt) {
		this.lastReadMessage = message;
		this.lastReadAt = readAt;
		this.updatedAt = readAt;
	}

	public ProjectChatMemberStateId getId() { return id; }
	public Project getProject() { return project; }
	public User getUser() { return user; }
	public Message getLastReadMessage() { return lastReadMessage; }
	public LocalDateTime getLastReadAt() { return lastReadAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}
