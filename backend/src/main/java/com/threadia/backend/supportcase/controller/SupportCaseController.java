package com.threadia.backend.supportcase.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.supportcase.dto.SupportCaseCreateRequest;
import com.threadia.backend.supportcase.dto.SupportCaseDetailData;
import com.threadia.backend.supportcase.dto.SupportCasesData;
import com.threadia.backend.supportcase.service.SupportCaseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/support-cases")
public class SupportCaseController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final SupportCaseService supportCaseService;

	public SupportCaseController(SupportCaseService supportCaseService) {
		this.supportCaseService = supportCaseService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<SupportCaseDetailData>> create(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@Valid @RequestBody SupportCaseCreateRequest request) {
		SupportCaseDetailData data = supportCaseService.create(extractAccessToken(authorizationHeader), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<SupportCasesData>> listOwn(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		return ResponseEntity.ok(ApiResponse.of(supportCaseService.listOwn(extractAccessToken(authorizationHeader))));
	}

	@GetMapping("/{supportCaseId}")
	public ResponseEntity<ApiResponse<SupportCaseDetailData>> getOwn(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long supportCaseId) {
		return ResponseEntity.ok(ApiResponse.of(
				supportCaseService.getOwn(extractAccessToken(authorizationHeader), supportCaseId)));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
