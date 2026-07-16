package com.threadia.backend.directmessage.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class DirectMessageMarkReadRequest {
	@NotNull
	@Positive
	private final Long lastReadDirectMessageId;

	@JsonCreator
	public DirectMessageMarkReadRequest(
			@JsonProperty("lastReadDirectMessageId") Long lastReadDirectMessageId,
			@JsonProperty("readAt") JsonNode readAt) {
		if (readAt != null) {
			throw new IllegalArgumentException("readAt은 서버가 UTC로 저장합니다.");
		}
		this.lastReadDirectMessageId = lastReadDirectMessageId;
	}

	public DirectMessageMarkReadRequest(Long lastReadDirectMessageId) {
		this(lastReadDirectMessageId, null);
	}

	public Long lastReadDirectMessageId() {
		return lastReadDirectMessageId;
	}
}
