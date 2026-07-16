package com.threadia.backend.directmessage.exception;

public class DirectMessageRoomNotFoundException extends RuntimeException {
	public DirectMessageRoomNotFoundException(Long roomId) { super("DM 방을 찾을 수 없습니다: " + roomId); }
}
