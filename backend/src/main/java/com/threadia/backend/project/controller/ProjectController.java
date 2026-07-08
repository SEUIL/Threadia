package com.threadia.backend.project.controller;

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
import com.threadia.backend.project.dto.ProjectCreateData;
import com.threadia.backend.project.dto.ProjectCreateRequest;
import com.threadia.backend.project.dto.ProjectsData;
import com.threadia.backend.project.service.ProjectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ProjectController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping("/companies/{companyId}/projects")
	public ResponseEntity<ApiResponse<ProjectsData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId) {
		ProjectsData data = projectService.list(extractAccessToken(authorizationHeader), companyId);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	@PostMapping("/companies/{companyId}/projects")
	public ResponseEntity<ApiResponse<ProjectCreateData>> create(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId,
			@Valid @RequestBody ProjectCreateRequest request) {
		ProjectCreateData data = projectService.create(extractAccessToken(authorizationHeader), companyId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
