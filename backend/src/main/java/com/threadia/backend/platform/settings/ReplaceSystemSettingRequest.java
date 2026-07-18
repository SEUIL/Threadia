package com.threadia.backend.platform.settings;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReplaceSystemSettingRequest(
		@NotNull JsonNode value,
		@NotNull @Positive Long expectedVersion,
		@NotBlank String reason) {
}
