package com.threadia.backend.activity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.activity.dto.ActivityEventPageData;
import com.threadia.backend.activity.service.ActivityEventService;
import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.project.exception.ProjectValidationException;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/activity-events")
public class ActivityEventController {

	private static final String BEARER_PREFIX = "Bearer ";
	private final ActivityEventService activityEvents;

	public ActivityEventController(ActivityEventService activityEvents) {
		this.activityEvents = activityEvents;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<ActivityEventPageData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new ProjectValidationException("activity event page와 size 범위가 올바르지 않습니다.");
		}
		return ResponseEntity.ok(ApiResponse.of(activityEvents.list(extractAccessToken(authorizationHeader), projectId, page, size)));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
