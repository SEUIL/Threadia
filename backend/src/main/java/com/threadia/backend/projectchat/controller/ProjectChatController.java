package com.threadia.backend.projectchat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.projectchat.dto.MarkReadData;
import com.threadia.backend.projectchat.dto.MarkReadRequest;
import com.threadia.backend.projectchat.dto.MessageCreateRequest;
import com.threadia.backend.projectchat.dto.MessageData;
import com.threadia.backend.projectchat.dto.MessagesData;
import com.threadia.backend.projectchat.dto.UnreadCountData;
import com.threadia.backend.projectchat.service.ProjectChatService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ProjectChatController {
	private static final String BEARER_PREFIX = "Bearer ";
	private final ProjectChatService projectChatService;

	public ProjectChatController(ProjectChatService projectChatService) { this.projectChatService = projectChatService; }

	@GetMapping("/projects/{projectId}/messages")
	public ResponseEntity<ApiResponse<MessagesData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectId,
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer limit) {
		return ResponseEntity.ok(ApiResponse.of(projectChatService.list(extractAccessToken(authorizationHeader), projectId, cursor, limit)));
	}

	@PostMapping("/projects/{projectId}/messages")
	public ResponseEntity<ApiResponse<MessageData>> create(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectId,
			@Valid @RequestBody MessageCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.of(projectChatService.create(extractAccessToken(authorizationHeader), projectId, request)));
	}

	@GetMapping("/messages/{messageId}")
	public ResponseEntity<ApiResponse<MessageData>> detail(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long messageId) {
		return ResponseEntity.ok(ApiResponse.of(projectChatService.detail(extractAccessToken(authorizationHeader), messageId)));
	}

	@PostMapping("/projects/{projectId}/messages/mark-read")
	public ResponseEntity<ApiResponse<MarkReadData>> markRead(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectId,
			@Valid @RequestBody MarkReadRequest request) {
		return ResponseEntity.ok(ApiResponse.of(projectChatService.markRead(extractAccessToken(authorizationHeader), projectId, request)));
	}

	@GetMapping("/projects/{projectId}/messages/unread-count")
	public ResponseEntity<ApiResponse<UnreadCountData>> unreadCount(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectId) {
		return ResponseEntity.ok(ApiResponse.of(projectChatService.unreadCount(extractAccessToken(authorizationHeader), projectId)));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
