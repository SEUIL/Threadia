package com.threadia.backend.supportcase.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.supportcase.dto.InternalNoteRequest;
import com.threadia.backend.supportcase.dto.InternalNoteResponse;
import com.threadia.backend.supportcase.dto.InternalNotesData;
import com.threadia.backend.supportcase.dto.PublicReplyRequest;
import com.threadia.backend.supportcase.dto.PublicReplyResponse;
import com.threadia.backend.supportcase.dto.SupportCaseDetailData;
import com.threadia.backend.supportcase.dto.SupportCaseStatusUpdateRequest;
import com.threadia.backend.supportcase.dto.SupportCasesData;
import com.threadia.backend.supportcase.service.PlatformSupportCaseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/platform/support-cases")
public class PlatformSupportCaseController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final PlatformSupportCaseService supportCaseService;

	public PlatformSupportCaseController(PlatformSupportCaseService supportCaseService) {
		this.supportCaseService = supportCaseService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<SupportCasesData>> listAll(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		return ResponseEntity.ok(ApiResponse.of(supportCaseService.listAll(extractAccessToken(authorizationHeader))));
	}

	@GetMapping("/{supportCaseId}")
	public ResponseEntity<ApiResponse<SupportCaseDetailData>> get(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long supportCaseId) {
		return ResponseEntity.ok(ApiResponse.of(supportCaseService.get(
				extractAccessToken(authorizationHeader), supportCaseId)));
	}

	@PostMapping("/{supportCaseId}/public-replies")
	public ResponseEntity<ApiResponse<PublicReplyResponse>> createPublicReply(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long supportCaseId,
			@Valid @RequestBody PublicReplyRequest request) {
		PublicReplyResponse data = supportCaseService.createPublicReply(
				extractAccessToken(authorizationHeader), supportCaseId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
	}

	@GetMapping("/{supportCaseId}/internal-notes")
	public ResponseEntity<ApiResponse<InternalNotesData>> listInternalNotes(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long supportCaseId) {
		return ResponseEntity.ok(ApiResponse.of(supportCaseService.listInternalNotes(
				extractAccessToken(authorizationHeader), supportCaseId)));
	}

	@PostMapping("/{supportCaseId}/internal-notes")
	public ResponseEntity<ApiResponse<InternalNoteResponse>> createInternalNote(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long supportCaseId,
			@Valid @RequestBody InternalNoteRequest request) {
		InternalNoteResponse data = supportCaseService.createInternalNote(
				extractAccessToken(authorizationHeader), supportCaseId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
	}

	@PatchMapping("/{supportCaseId}/status")
	public ResponseEntity<ApiResponse<SupportCaseDetailData>> updateStatus(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long supportCaseId,
			@Valid @RequestBody SupportCaseStatusUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.of(supportCaseService.updateStatus(
				extractAccessToken(authorizationHeader), supportCaseId, request)));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
