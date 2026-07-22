package com.threadia.backend.hubpresence.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.hubpresence.dto.HubPresenceOnlineData;
import com.threadia.backend.hubpresence.service.HubPresenceService;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/hub-presence")
public class HubPresenceController {

	private static final String BEARER_PREFIX = "Bearer ";
	private final HubPresenceService hubPresence;

	public HubPresenceController(HubPresenceService hubPresence) {
		this.hubPresence = hubPresence;
	}

	// heartbeat 자체가 "지금 이 순간 온라인 목록"을 바로 돌려준다 — 보내고 나서 또
	// 목록을 조회하는 별도 GET을 만들 필요가 없다(폴링 주기마다 왕복 한 번으로 충분).
	@PostMapping("/heartbeat")
	public ResponseEntity<ApiResponse<HubPresenceOnlineData>> heartbeat(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId) {
		return ResponseEntity.ok(ApiResponse.of(hubPresence.heartbeat(extractAccessToken(authorizationHeader), companyId)));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
