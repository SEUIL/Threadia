package com.threadia.backend.platform.audit;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;

@Service
public class AuditReadService {

	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 100;
	private static final Logger log = LoggerFactory.getLogger(AuditReadService.class);
	private final PlatformAdminAccessService accessService;
	private final AdminActionAuditRepository auditRepository;
	private final AuditCursorCodec cursorCodec;
	private final AuditSnapshotSanitizer snapshotSanitizer;
	private final ObjectMapper objectMapper;

	public AuditReadService(
			PlatformAdminAccessService accessService,
			AdminActionAuditRepository auditRepository,
			AuditCursorCodec cursorCodec,
			AuditSnapshotSanitizer snapshotSanitizer,
			ObjectMapper objectMapper) {
		this.accessService = accessService;
		this.auditRepository = auditRepository;
		this.cursorCodec = cursorCodec;
		this.snapshotSanitizer = snapshotSanitizer;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public AuditPageData list(
			String accessToken,
			Long actorUserId,
			String action,
			String targetType,
			Long targetId,
			Long supportCaseId,
			String createdFrom,
			String createdTo,
			String cursor,
			Integer requestedSize) {
		PlatformAdminAuthority requester = accessService.requireAdmin(accessToken);
		AuditFilters filters = filters(actorUserId, action, targetType, targetId, supportCaseId, createdFrom, createdTo);
		int size = pageSize(requestedSize);
		AuditCursorCodec.Cursor decoded = cursor == null || cursor.isBlank() ? null : cursorCodec.decode(cursor, requester.actor().getId(), filters);
		List<AdminActionAudit> rows = auditRepository.findAuditPage(filters.actorUserId(), filters.action(), filters.targetType(),
				filters.targetId(), filters.supportCaseId(), filters.createdFrom(), filters.createdTo(),
				decoded == null ? null : decoded.createdAt(), decoded == null ? null : decoded.id(), PageRequest.of(0, size + 1));
		boolean hasNext = rows.size() > size;
		List<AdminActionAudit> page = hasNext ? rows.subList(0, size) : rows;
		String nextCursor = hasNext ? cursorCodec.encode(requester.actor().getId(), filters, page.get(page.size() - 1)) : null;
		return new AuditPageData(page.stream().map(this::response).toList(), nextCursor, hasNext, size);
	}

	private AuditResponse response(AdminActionAudit audit) {
		return AuditResponse.from(audit, snapshot(audit, audit.getBeforeState()), snapshot(audit, audit.getAfterState()));
	}

	private Map<String, Object> snapshot(AdminActionAudit audit, String persistedState) {
		if (persistedState == null) {
			return null;
		}
		try {
			JsonNode node = objectMapper.readTree(persistedState);
			if (node == null || !node.isObject()) {
				return invalidSnapshot(audit.getId());
			}
			Map<String, Object> parsed = objectMapper.convertValue(node, new TypeReference<LinkedHashMap<String, Object>>() { });
			Map<String, Object> sanitized = snapshotSanitizer.sanitize(audit.getAction(), audit.getTargetType(), parsed);
			Map<String, Object> scalar = new LinkedHashMap<>();
			sanitized.forEach((key, value) -> {
				if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
					scalar.put(key, value);
				}
			});
			return scalar;
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			return invalidSnapshot(audit.getId());
		}
	}

	private Map<String, Object> invalidSnapshot(Long auditId) {
		log.warn("audit snapshot parse failed: auditId={}", auditId);
		return Map.of();
	}

	private static AuditFilters filters(Long actorUserId, String action, String targetType, Long targetId, Long supportCaseId, String createdFrom, String createdTo) {
		positiveOrNull(actorUserId, "actorUserId");
		positiveOrNull(targetId, "targetId");
		positiveOrNull(supportCaseId, "supportCaseId");
		AuditTargetType parsedTargetType = parseEnum(targetType, AuditTargetType.class, "targetType");
		if (targetId != null && parsedTargetType == null) {
			throw new AuditValidationException("targetId에는 targetType이 필요합니다.");
		}
		LocalDateTime from = parseUtc(createdFrom, "createdFrom");
		LocalDateTime to = parseUtc(createdTo, "createdTo");
		if (from != null && to != null && from.isAfter(to)) {
			throw new AuditValidationException("createdFrom은 createdTo보다 늦을 수 없습니다.");
		}
		return new AuditFilters(actorUserId, parseEnum(action, AdminAction.class, "action"), parsedTargetType, targetId, supportCaseId, from, to);
	}

	private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, String parameter) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new AuditValidationException(parameter + " 값이 올바르지 않습니다.");
		}
	}

	private static LocalDateTime parseUtc(String value, String parameter) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			OffsetDateTime parsed = OffsetDateTime.parse(value);
			if (!ZoneOffset.UTC.equals(parsed.getOffset())) {
				throw new AuditValidationException(parameter + "은 UTC ISO-8601 형식이어야 합니다.");
			}
			return parsed.toLocalDateTime();
		} catch (java.time.format.DateTimeParseException exception) {
			throw new AuditValidationException(parameter + "은 UTC ISO-8601 형식이어야 합니다.");
		}
	}

	private static void positiveOrNull(Long value, String parameter) {
		if (value != null && value < 1) {
			throw new AuditValidationException(parameter + "은 양수여야 합니다.");
		}
	}

	private static int pageSize(Integer requestedSize) {
		int size = requestedSize == null ? DEFAULT_SIZE : requestedSize;
		if (size < 1 || size > MAX_SIZE) {
			throw new AuditValidationException("size는 1 이상 100 이하여야 합니다.");
		}
		return size;
	}
}
