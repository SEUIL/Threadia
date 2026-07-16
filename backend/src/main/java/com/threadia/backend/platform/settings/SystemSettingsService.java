package com.threadia.backend.platform.settings;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.platform.audit.AdminAction;
import com.threadia.backend.platform.audit.AdminActionAuditService;
import com.threadia.backend.platform.audit.AuditReason;
import com.threadia.backend.platform.audit.AuditTargetType;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;

@Service
public class SystemSettingsService {

	private final PlatformAdminAccessService platformAdminAccessService;
	private final SystemSettingRepository settingRepository;
	private final SystemSettingCatalogValidator catalogValidator;
	private final AdminActionAuditService auditService;
	private final Clock clock;

	@Autowired
	public SystemSettingsService(
			PlatformAdminAccessService platformAdminAccessService,
			SystemSettingRepository settingRepository,
			SystemSettingCatalogValidator catalogValidator,
			AdminActionAuditService auditService) {
		this(platformAdminAccessService, settingRepository, catalogValidator, auditService, Clock.systemUTC());
	}

	SystemSettingsService(
			PlatformAdminAccessService platformAdminAccessService,
			SystemSettingRepository settingRepository,
			SystemSettingCatalogValidator catalogValidator,
			AdminActionAuditService auditService,
			Clock clock) {
		this.platformAdminAccessService = platformAdminAccessService;
		this.settingRepository = settingRepository;
		this.catalogValidator = catalogValidator;
		this.auditService = auditService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public SystemSettingsData list(String accessToken) {
		platformAdminAccessService.requireAdmin(accessToken);
		List<SystemSettingResponse> settings = settingRepository.findAll(Sort.by("key"))
				.stream()
				.map(SystemSettingResponse::from)
				.toList();
		return new SystemSettingsData(settings);
	}

	@Transactional(readOnly = true)
	public SystemSettingResponse get(String accessToken, SystemSettingKey key) {
		platformAdminAccessService.requireAdmin(accessToken);
		SystemSetting setting = settingRepository.findByKey(key)
				.orElseThrow(() -> new SystemSettingNotFoundException(key));
		return SystemSettingResponse.from(setting);
	}

	@Transactional
	public SystemSettingResponse replace(String accessToken, SystemSettingKey key, ReplaceSystemSettingRequest request) {
		PlatformAdminAuthority authority = requireSuperAdmin(accessToken);
		String reason = AuditReason.requireValid(request.reason());
		if (request.expectedVersion() == null || request.expectedVersion() <= 0) {
			throw new SystemSettingValidationException("expectedVersion은 양수여야 합니다.");
		}
		catalogValidator.validate(key, request.value());
		SystemSetting setting = settingRepository.findByKeyForUpdate(key)
				.orElseThrow(() -> new SystemSettingNotFoundException(key));
		if (setting.getVersion() != request.expectedVersion()) {
			throw new SystemSettingConflictException("SYSTEM_SETTING_VERSION_CONFLICT", "시스템 설정 버전이 최신 상태가 아닙니다.");
		}
		if (setting.getValue().equals(request.value())) {
			throw new SystemSettingConflictException("SYSTEM_SETTING_NO_CHANGE", "시스템 설정 값이 기존 값과 같습니다.");
		}
		long beforeVersion = setting.getVersion();
		JsonNode beforeValue = setting.getValue().deepCopy();
		setting.replaceValue(request.value().deepCopy(), LocalDateTime.now(clock));
		settingRepository.flush();
		auditService.recordPlatformMutation(
				null,
				authority.actor(),
				authority.platformAdmin(),
				AdminAction.SYSTEM_SETTING_REPLACED,
				AuditTargetType.SYSTEM_SETTING,
				setting.getId(),
				reason,
				auditSnapshot(setting.getKey(), setting.getValueSchemaVersion(), beforeVersion, beforeValue),
				auditSnapshot(setting.getKey(), setting.getValueSchemaVersion(), setting.getVersion(), setting.getValue()));
		return SystemSettingResponse.from(setting);
	}

	private PlatformAdminAuthority requireSuperAdmin(String accessToken) {
		PlatformAdminAuthority authority = platformAdminAccessService.requireAdmin(accessToken);
		if (authority.platformAdmin().getRole() != PlatformAdminRole.SUPER_ADMIN) {
			throw new ForbiddenException("SUPER_ADMIN 권한이 필요합니다.");
		}
		return authority;
	}

	private static Map<String, Object> auditSnapshot(SystemSettingKey key, int valueSchemaVersion, long version, JsonNode value) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("settingKey", key.name());
		snapshot.put("valueSchemaVersion", valueSchemaVersion);
		snapshot.put("version", version);
		snapshot.put("value", redactAuditValue(key, value));
		return snapshot;
	}

	private static JsonNode redactAuditValue(SystemSettingKey key, JsonNode value) {
		JsonNode redacted = value.deepCopy();
		if (!(redacted instanceof ObjectNode objectValue)) {
			return redacted;
		}
		switch (key) {
			case REPLY_TEMPLATES -> objectValue.path("templates").forEach(template -> {
				if (template instanceof ObjectNode templateObject && !templateObject.path("body").isNull()) {
					templateObject.put("body", "[REDACTED]");
				}
			});
			case ANNOUNCEMENT_BANNER, SUPPORT_CENTER_STATUS -> {
				if (!objectValue.path("message").isNull()) {
					objectValue.put("message", "[REDACTED]");
				}
			}
			default -> {
			}
		}
		return redacted;
	}
}
