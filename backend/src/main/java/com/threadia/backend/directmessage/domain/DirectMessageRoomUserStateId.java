package com.threadia.backend.directmessage.domain;

import java.io.Serializable;
import java.util.Objects;

public class DirectMessageRoomUserStateId implements Serializable {
	private Long room;
	private Long user;
	public DirectMessageRoomUserStateId() { }
	public DirectMessageRoomUserStateId(Long room, Long user) { this.room = room; this.user = user; }
	@Override public boolean equals(Object other) {
		return other instanceof DirectMessageRoomUserStateId that && Objects.equals(room, that.room) && Objects.equals(user, that.user);
	}
	@Override public int hashCode() { return Objects.hash(room, user); }
}
