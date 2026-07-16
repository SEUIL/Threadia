package com.threadia.backend.platform.settings;

import java.time.ZoneOffset;

import com.fasterxml.jackson.databind.JsonNode;

public record SystemSettingResponse(
		Long id,
		SystemSettingKey key,
		JsonNode value,
		int valueSchemaVersion,
		long version,
		String createdAt,
		String updatedAt) {
	public static SystemSettingResponse from(SystemSetting setting) {
		return new SystemSettingResponse(
				setting.getId(),
				setting.getKey(),
				setting.getValue(),
				setting.getValueSchemaVersion(),
				setting.getVersion(),
				setting.getCreatedAt().toInstant(ZoneOffset.UTC).toString(),
				setting.getUpdatedAt().toInstant(ZoneOffset.UTC).toString());
	}
}
