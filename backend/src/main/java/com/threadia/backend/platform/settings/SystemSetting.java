package com.threadia.backend.platform.settings;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "system_settings")
public class SystemSetting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "setting_key", nullable = false, length = 100, unique = true)
	private SystemSettingKey key;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "value_json", nullable = false, columnDefinition = "JSON")
	private JsonNode value;

	@Column(name = "value_schema_version", nullable = false)
	private int valueSchemaVersion;

	@Version
	@Column(nullable = false)
	private Long version = 1L;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
	private LocalDateTime updatedAt;

	protected SystemSetting() {
	}

	public SystemSetting(SystemSettingKey key, JsonNode value, int valueSchemaVersion, LocalDateTime createdAt, LocalDateTime updatedAt) {
		this.key = key;
		this.value = value;
		this.valueSchemaVersion = valueSchemaVersion;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void replaceValue(JsonNode value, LocalDateTime updatedAt) {
		this.value = value;
		this.updatedAt = updatedAt;
	}

	public Long getId() { return id; }
	public SystemSettingKey getKey() { return key; }
	public JsonNode getValue() { return value; }
	public int getValueSchemaVersion() { return valueSchemaVersion; }
	public long getVersion() { return version; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}
