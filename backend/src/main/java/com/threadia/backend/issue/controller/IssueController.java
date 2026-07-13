package com.threadia.backend.issue.controller;

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
import com.threadia.backend.issue.dto.IssueCreateRequest;
import com.threadia.backend.issue.dto.IssueData;
import com.threadia.backend.issue.dto.IssueStatusUpdateRequest;
import com.threadia.backend.issue.dto.IssuesData;
import com.threadia.backend.issue.service.IssueService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class IssueController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final IssueService issueService;

	public IssueController(IssueService issueService) {
		this.issueService = issueService;
	}

	@GetMapping("/projects/{projectId}/issues")
	public ResponseEntity<ApiResponse<IssuesData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectId) {
		IssuesData data = issueService.list(extractAccessToken(authorizationHeader), projectId);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	@PostMapping("/projects/{projectId}/issues")
	public ResponseEntity<ApiResponse<IssueData>> create(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectId,
			@Valid @RequestBody IssueCreateRequest request) {
		IssueData data = issueService.create(extractAccessToken(authorizationHeader), projectId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
	}

	@PatchMapping("/issues/{issueId}/status")
	public ResponseEntity<ApiResponse<IssueData>> updateStatus(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long issueId,
			@Valid @RequestBody IssueStatusUpdateRequest request) {
		IssueData data = issueService.updateStatus(extractAccessToken(authorizationHeader), issueId, request);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
