package com.threadia.backend.directmessage.domain;

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
@Table(name = "direct_messages")
public class DirectMessage {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dm_room"))
	private DirectMessageRoom room;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sender_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dm_sender"))
	private User sender;
	@Column(nullable = false, columnDefinition = "TEXT") private String content;
	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME") private LocalDateTime createdAt;
	protected DirectMessage() { }
	public DirectMessage(DirectMessageRoom room, User sender, String content, LocalDateTime createdAt) {
		this.room = room; this.sender = sender; this.content = content; this.createdAt = createdAt;
	}
	public Long getId() { return id; }
	public DirectMessageRoom getRoom() { return room; }
	public User getSender() { return sender; }
	public String getContent() { return content; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
