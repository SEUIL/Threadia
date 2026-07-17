package com.threadia.backend.notification.controller;

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
import com.threadia.backend.notification.dto.NotificationPageResponse;
import com.threadia.backend.notification.dto.NotificationReadAllData;
import com.threadia.backend.notification.dto.NotificationReadAllRequest;
import com.threadia.backend.notification.dto.NotificationReadData;
import com.threadia.backend.notification.dto.NotificationUnreadCountData;
import com.threadia.backend.notification.service.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private static final String BEARER_PREFIX = "Bearer ";
	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public ResponseEntity<NotificationPageResponse> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam(required = false) Long companyId,
			@RequestParam(required = false) Long projectId,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) Boolean unreadOnly,
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer size) {
		return ResponseEntity.ok(NotificationPageResponse.from(notificationService.list(
				extractAccessToken(authorizationHeader),
				companyId,
				projectId,
				type,
				unreadOnly,
				cursor,
				size)));
	}

	@GetMapping("/unread-count")
	public ResponseEntity<ApiResponse<NotificationUnreadCountData>> unreadCount(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam(required = false) Long companyId,
			@RequestParam(required = false) Long projectId,
			@RequestParam(required = false) String type) {
		return ResponseEntity.ok(ApiResponse.of(notificationService.unreadCount(
				extractAccessToken(authorizationHeader), companyId, projectId, type)));
	}

	@PostMapping("/{notificationRecipientId}/read")
	public ResponseEntity<ApiResponse<NotificationReadData>> markRead(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long notificationRecipientId) {
		return ResponseEntity.ok(ApiResponse.of(notificationService.markRead(
				extractAccessToken(authorizationHeader), notificationRecipientId)));
	}

	@PostMapping("/read-all")
	public ResponseEntity<ApiResponse<NotificationReadAllData>> markAllRead(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody NotificationReadAllRequest request) {
		return ResponseEntity.ok(ApiResponse.of(notificationService.markAllRead(
				extractAccessToken(authorizationHeader), request)));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
