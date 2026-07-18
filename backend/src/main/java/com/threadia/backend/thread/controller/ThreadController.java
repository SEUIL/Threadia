package com.threadia.backend.thread.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.threadia.backend.thread.domain.ThreadTargetType;
import com.threadia.backend.thread.dto.ThreadCommentCreateRequest;
import com.threadia.backend.thread.dto.ThreadCommentData;
import com.threadia.backend.thread.dto.ThreadCommentsData;
import com.threadia.backend.thread.dto.ThreadCreationResult;
import com.threadia.backend.thread.dto.ThreadData;
import com.threadia.backend.thread.dto.ThreadGetOrCreateRequest;
import com.threadia.backend.thread.dto.ThreadTopicUpdateRequest;
import com.threadia.backend.thread.service.ThreadService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ThreadController {
	private static final String BEARER_PREFIX = "Bearer ";
	private final ThreadService threadService;
	public ThreadController(ThreadService threadService) { this.threadService = threadService; }
	@GetMapping("/projects/{projectId}/threads")
	public ResponseEntity<ApiResponse<ThreadData>> findByTarget(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long projectId, @RequestParam ThreadTargetType targetType, @RequestParam Long targetId) {
		return ResponseEntity.ok(ApiResponse.of(threadService.findByTarget(token(authorization), projectId, targetType, targetId)));
	}
	@PostMapping("/projects/{projectId}/threads")
	public ResponseEntity<ApiResponse<ThreadData>> getOrCreate(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long projectId, @Valid @RequestBody ThreadGetOrCreateRequest request) {
		ThreadCreationResult result = threadService.getOrCreate(token(authorization), projectId, request);
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(ApiResponse.of(result.data()));
	}
	@GetMapping("/threads/{threadId}")
	public ResponseEntity<ApiResponse<ThreadData>> detail(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long threadId) { return ResponseEntity.ok(ApiResponse.of(threadService.detail(token(authorization), threadId))); }
	@PatchMapping("/threads/{threadId}")
	public ResponseEntity<ApiResponse<ThreadData>> updateTopic(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long threadId, @RequestBody ThreadTopicUpdateRequest request) { return ResponseEntity.ok(ApiResponse.of(threadService.updateTopic(token(authorization), threadId, request))); }
	@GetMapping("/threads/{threadId}/comments")
	public ResponseEntity<ApiResponse<ThreadCommentsData>> listComments(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long threadId) { return ResponseEntity.ok(ApiResponse.of(threadService.listComments(token(authorization), threadId))); }
	@PostMapping("/threads/{threadId}/comments")
	public ResponseEntity<ApiResponse<ThreadCommentData>> createComment(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long threadId, @Valid @RequestBody ThreadCommentCreateRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(threadService.createComment(token(authorization), threadId, request))); }
	private static String token(String authorization) { if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다."); return authorization.substring(BEARER_PREFIX.length()); }
}
