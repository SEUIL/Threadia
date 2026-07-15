package com.threadia.backend.directmessage.domain;

import java.time.LocalDateTime;

import com.threadia.backend.auth.domain.User;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "dm_room_user_states")
@IdClass(DirectMessageRoomUserStateId.class)
public class DirectMessageRoomUserState {
	@Id @ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dmrus_room"))
	private DirectMessageRoom room;
	@Id @ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dmrus_user"))
	private User user;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "last_read_direct_message_id", foreignKey = @ForeignKey(name = "fk_dmrus_last_read"))
	private DirectMessage lastReadDirectMessage;
	private LocalDateTime lastReadAt;
	private LocalDateTime updatedAt;
	protected DirectMessageRoomUserState() { }
	public DirectMessageRoomUserState(DirectMessageRoom room, User user, LocalDateTime now) { this.room = room; this.user = user; this.updatedAt = now; }
	public DirectMessage getLastReadDirectMessage() { return lastReadDirectMessage; }
	public LocalDateTime getLastReadAt() { return lastReadAt; }
	public void advanceTo(DirectMessage directMessage, LocalDateTime now) {
		if (lastReadDirectMessage == null || directMessage.getId() > lastReadDirectMessage.getId()) { lastReadDirectMessage = directMessage; lastReadAt = now; updatedAt = now; }
	}
}
