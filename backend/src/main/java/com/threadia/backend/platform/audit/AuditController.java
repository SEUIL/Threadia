package com.threadia.backend.platform.audit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

@RestController
@RequestMapping("/api/v1/platform/audits")
public class AuditController {

	private final AuditReadService auditReadService;

	public AuditController(AuditReadService auditReadService) {
		this.auditReadService = auditReadService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<AuditPageData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam(required = false) Long actorUserId,
			@RequestParam(required = false) String action,
			@RequestParam(required = false) String targetType,
			@RequestParam(required = false) Long targetId,
			@RequestParam(required = false) Long supportCaseId,
			@RequestParam(required = false) String createdFrom,
			@RequestParam(required = false) String createdTo,
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer size) {
		return ResponseEntity.ok(ApiResponse.of(auditReadService.list(accessToken(authorizationHeader), actorUserId, action,
				targetType, targetId, supportCaseId, createdFrom, createdTo, cursor, size)));
	}

	private static String accessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring("Bearer ".length());
	}
}
