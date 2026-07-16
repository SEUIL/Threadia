package com.threadia.backend.supportcase.dto;

import jakarta.validation.constraints.NotBlank;

public record PublicReplyRequest(
		@NotBlank String body,
		@NotBlank String reason) {
}
