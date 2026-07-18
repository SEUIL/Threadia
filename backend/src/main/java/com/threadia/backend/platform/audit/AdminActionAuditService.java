package com.threadia.backend.platform.audit;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.supportcase.domain.SupportCase;

@Service
public class AdminActionAuditService {

	private final AdminActionAuditRepository auditRepository;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	@Autowired
	public AdminActionAuditService(AdminActionAuditRepository auditRepository, ObjectMapper objectMapper) {
		this(auditRepository, objectMapper, Clock.systemUTC());
	}

	AdminActionAuditService(AdminActionAuditRepository auditRepository, ObjectMapper objectMapper, Clock clock) {
		this.auditRepository = auditRepository;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void recordUserMutation(
			SupportCase supportCase,
			User actor,
			AdminAction action,
			AuditTargetType targetType,
			Long targetId,
			String reason,
			Map<String, Object> beforeState,
			Map<String, Object> afterState) {
		record(
				supportCase,
				actor,
				null,
				AuditAuthoritySource.USER,
				null,
				action,
				targetType,
				targetId,
				reason,
				beforeState,
				afterState);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void recordPlatformMutation(
			SupportCase supportCase,
			User actor,
			PlatformAdmin platformAdmin,
			AdminAction action,
			AuditTargetType targetType,
			Long targetId,
			String reason,
			Map<String, Object> beforeState,
			Map<String, Object> afterState) {
		record(
				supportCase,
				actor,
				platformAdmin,
				AuditAuthoritySource.PLATFORM_ADMIN,
				platformAdmin.getRole(),
				action,
				targetType,
				targetId,
				reason,
				beforeState,
				afterState);
	}

	private void record(
			SupportCase supportCase,
			User actor,
			PlatformAdmin platformAdmin,
			AuditAuthoritySource authoritySource,
			PlatformAdminRole authorityRole,
			AdminAction action,
			AuditTargetType targetType,
			Long targetId,
			String reason,
			Map<String, Object> beforeState,
			Map<String, Object> afterState) {
		auditRepository.save(new AdminActionAudit(
				supportCase,
				actor,
				platformAdmin,
				authoritySource,
				authorityRole,
				action,
				targetType,
				targetId,
				reason,
				serialize(beforeState),
				serialize(afterState),
				LocalDateTime.now(clock)));
	}

	private String serialize(Map<String, Object> state) {
		if (state == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(state);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("audit state를 직렬화할 수 없습니다.", exception);
		}
	}
}
