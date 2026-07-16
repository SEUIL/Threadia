package com.threadia.backend.thread.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;

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
@Table(name = "thread_comments")
public class ThreadComment {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "thread_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tc_thread")) private Thread thread;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tc_author")) private User author;
	@Column(nullable = false, columnDefinition = "TEXT") private String content;
	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME") private LocalDateTime createdAt;
	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME") private LocalDateTime updatedAt;

	protected ThreadComment() { }
	public ThreadComment(Thread thread, User author, String content, LocalDateTime createdAt) {
		this.thread = thread; this.author = author; this.content = content; this.createdAt = createdAt; this.updatedAt = createdAt;
	}
	public Long getId() { return id; }
	public Thread getThread() { return thread; }
	public User getAuthor() { return author; }
	public String getContent() { return content; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
