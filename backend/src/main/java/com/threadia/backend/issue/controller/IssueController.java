package com.threadia.backend.issue.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.issue.dto.IssueCreateRequest;
import com.threadia.backend.issue.dto.IssueData;
import com.threadia.backend.issue.dto.IssueStatusUpdateRequest;
import com.threadia.backend.issue.dto.IssueUpdateRequest;
import com.threadia.backend.issue.dto.IssuesData;
import com.threadia.backend.issue.domain.IssueStatus;
import com.threadia.backend.issue.service.IssueService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class IssueController {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final Set<String> UPDATE_FIELDS = Set.of("title", "description", "priority", "assigneeUserId", "dueDate", "sprintId");

	private final IssueService issueService;
	private final ObjectMapper objectMapper;

	public IssueController(IssueService issueService, ObjectMapper objectMapper) {
		this.issueService = issueService;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/projects/{projectId}/issues")
	public ResponseEntity<ApiResponse<IssuesData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectId,
			@RequestParam(required = false) IssueStatus status,
			@RequestParam(required = false) Long sprintId,
			@RequestParam(required = false) Long assigneeId,
			@RequestParam(defaultValue = "false") boolean unassignedOnly,
			@RequestParam(defaultValue = "false") boolean includeTrashed) {
		IssuesData data = status == null && sprintId == null && assigneeId == null && !unassignedOnly && !includeTrashed
				? issueService.list(extractAccessToken(authorizationHeader), projectId)
				: issueService.list(extractAccessToken(authorizationHeader), projectId, status, sprintId, assigneeId, unassignedOnly, includeTrashed);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	@GetMapping("/issues/{issueId}")
	public ResponseEntity<ApiResponse<IssueData>> detail(@RequestHeader(value = "Authorization", required = false) String authorizationHeader, @PathVariable Long issueId) {
		return ResponseEntity.ok(ApiResponse.of(issueService.detail(extractAccessToken(authorizationHeader), issueId)));
	}

	@PatchMapping("/issues/{issueId}")
	public ResponseEntity<ApiResponse<IssueData>> update(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long issueId,
			@RequestBody JsonNode request) {
		if (request == null || !request.isObject()) {
			throw new HttpMessageNotReadableException("이슈 수정 요청은 JSON 객체여야 합니다.");
		}
		java.util.Set<String> fields = new java.util.HashSet<>();
		request.fieldNames().forEachRemaining(fields::add);
		if (!UPDATE_FIELDS.containsAll(fields)) {
			throw new HttpMessageNotReadableException("이슈 수정 요청에 허용되지 않은 필드가 있습니다.");
		}
		IssueUpdateRequest update;
		try {
			update = objectMapper.treeToValue(request, IssueUpdateRequest.class);
		} catch (JsonProcessingException exception) {
			throw new HttpMessageNotReadableException("이슈 수정 요청 형식이 올바르지 않습니다.", exception);
		}
		return ResponseEntity.ok(ApiResponse.of(issueService.update(extractAccessToken(authorizationHeader), issueId, update, fields)));
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

	@PostMapping("/issues/{issueId}/trash")
	public ResponseEntity<ApiResponse<IssueData>> trash(@RequestHeader(value = "Authorization", required = false) String authorizationHeader, @PathVariable Long issueId) {
		return ResponseEntity.ok(ApiResponse.of(issueService.trash(extractAccessToken(authorizationHeader), issueId)));
	}

	@PostMapping("/issues/{issueId}/restore")
	public ResponseEntity<ApiResponse<IssueData>> restore(@RequestHeader(value = "Authorization", required = false) String authorizationHeader, @PathVariable Long issueId) {
		return ResponseEntity.ok(ApiResponse.of(issueService.restore(extractAccessToken(authorizationHeader), issueId)));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
