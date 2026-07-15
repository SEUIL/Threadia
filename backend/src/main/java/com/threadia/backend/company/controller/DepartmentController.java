package com.threadia.backend.company.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.company.dto.DepartmentData;
import com.threadia.backend.company.dto.DepartmentDeleteData;
import com.threadia.backend.company.dto.DepartmentRequest;
import com.threadia.backend.company.dto.DepartmentsData;
import com.threadia.backend.company.service.DepartmentService;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class DepartmentController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final DepartmentService departmentService;

	public DepartmentController(DepartmentService departmentService) {
		this.departmentService = departmentService;
	}

	@GetMapping("/companies/{companyId}/departments")
	public ResponseEntity<ApiResponse<DepartmentsData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId) {
		DepartmentsData data = departmentService.list(extractAccessToken(authorizationHeader), companyId);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	@PostMapping("/companies/{companyId}/departments")
	public ResponseEntity<ApiResponse<DepartmentData>> create(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId,
			@Valid @RequestBody DepartmentRequest request) {
		DepartmentData data = departmentService.create(extractAccessToken(authorizationHeader), companyId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
	}

	@PatchMapping("/departments/{departmentId}")
	public ResponseEntity<ApiResponse<DepartmentData>> update(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long departmentId,
			@Valid @RequestBody DepartmentRequest request) {
		DepartmentData data = departmentService.update(extractAccessToken(authorizationHeader), departmentId, request);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	@DeleteMapping("/departments/{departmentId}")
	public ResponseEntity<ApiResponse<DepartmentDeleteData>> delete(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long departmentId) {
		DepartmentDeleteData data = departmentService.delete(extractAccessToken(authorizationHeader), departmentId);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
