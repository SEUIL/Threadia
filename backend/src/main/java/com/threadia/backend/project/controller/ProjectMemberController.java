package com.threadia.backend.project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.dto.ProjectMemberData;
import com.threadia.backend.project.dto.ProjectMemberRemoveData;
import com.threadia.backend.project.dto.ProjectMemberUpdateRequest;
import com.threadia.backend.project.dto.ProjectMembersData;
import com.threadia.backend.project.service.ProjectMemberService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ProjectMemberController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final ProjectMemberService projectMemberService;

	public ProjectMemberController(ProjectMemberService projectMemberService) {
		this.projectMemberService = projectMemberService;
	}

	@GetMapping("/projects/{projectId}/project-members")
	public ResponseEntity<ApiResponse<ProjectMembersData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectId,
			@RequestParam(required = false) ProjectMemberRole role,
			@RequestParam(required = false) ProjectMemberStatus status,
			@RequestParam(required = false) String q) {
		ProjectMembersData data = projectMemberService.list(
				extractAccessToken(authorizationHeader),
				projectId,
				role,
				status,
				q);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	@PatchMapping("/project-members/{projectMemberId}")
	public ResponseEntity<ApiResponse<ProjectMemberData>> update(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectMemberId,
			@Valid @RequestBody ProjectMemberUpdateRequest request) {
		ProjectMemberData data = projectMemberService.update(
				extractAccessToken(authorizationHeader),
				projectMemberId,
				request);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	@DeleteMapping("/project-members/{projectMemberId}")
	public ResponseEntity<ApiResponse<ProjectMemberRemoveData>> remove(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long projectMemberId) {
		ProjectMemberRemoveData data = projectMemberService.remove(
				extractAccessToken(authorizationHeader),
				projectMemberId);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
