package com.threadia.backend.projectchat.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.project.domain.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "messages")
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_messages_project"))
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_messages_author"))
	private User author;

	@Column(name = "thread_id")
	private Long threadId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	protected Message() {
	}

	public Message(Project project, User author, String content, LocalDateTime createdAt) {
		this.project = project;
		this.author = author;
		this.content = content;
		this.createdAt = createdAt;
	}

	public Long getId() { return id; }
	public Project getProject() { return project; }
	public User getAuthor() { return author; }
	public Long getThreadId() { return threadId; }
	public String getContent() { return content; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
