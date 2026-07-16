package com.threadia.backend.supportcase.dto;

import jakarta.validation.constraints.NotBlank;

public record InternalNoteRequest(
		@NotBlank String body,
		@NotBlank String reason) {
}
