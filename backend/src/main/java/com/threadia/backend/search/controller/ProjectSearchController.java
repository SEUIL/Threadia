package com.threadia.backend.search.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.project.exception.ProjectValidationException;
import com.threadia.backend.search.dto.SearchData;
import com.threadia.backend.search.service.ProjectSearchService;

@RestController
@RequestMapping("/api/v1")
public class ProjectSearchController {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final int MAX_QUERY_LENGTH = 100;

	private final ProjectSearchService projectSearchService;

	public ProjectSearchController(ProjectSearchService projectSearchService) {
		this.projectSearchService = projectSearchService;
	}

	@GetMapping("/search")
	public ResponseEntity<ApiResponse<SearchData>> search(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam Long projectId,
			@RequestParam String q,
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer size) {
		String query = q.trim();
		if (query.isEmpty()) {
			throw new ProjectValidationException("q는 공백이 아닌 값이어야 합니다.");
		}
		if (query.length() > MAX_QUERY_LENGTH) {
			throw new ProjectValidationException("q는 100자 이하여야 합니다.");
		}
		return ResponseEntity.ok(ApiResponse.of(projectSearchService.search(extractAccessToken(authorizationHeader), projectId, q, cursor, size)));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
