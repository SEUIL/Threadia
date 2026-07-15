package com.threadia.backend.directmessage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DirectMessageSendRequest(@NotBlank @Size(max = 10000) String content) {
	public DirectMessageSendRequest {
		content = content == null ? null : content.trim();
	}
}
