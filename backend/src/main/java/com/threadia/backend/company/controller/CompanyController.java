package com.threadia.backend.company.controller;

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
import com.threadia.backend.company.dto.CompanyCreateData;
import com.threadia.backend.company.dto.CompanyCreateRequest;
import com.threadia.backend.company.dto.CompanyData;
import com.threadia.backend.company.dto.CompanyLogoRequest;
import com.threadia.backend.company.dto.CompanyStatsData;
import com.threadia.backend.company.dto.CompanyUpdateRequest;
import com.threadia.backend.company.service.CompanyService;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final CompanyService companyService;

	public CompanyController(CompanyService companyService) {
		this.companyService = companyService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<CompanyCreateData>> create(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@Valid @RequestBody CompanyCreateRequest request) {
		CompanyCreateData data = companyService.create(extractAccessToken(authorizationHeader), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
	}

	@GetMapping("/{companyId}")
	public ResponseEntity<ApiResponse<CompanyData>> get(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId) {
		CompanyData data = companyService.get(extractAccessToken(authorizationHeader), companyId);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	@PatchMapping("/{companyId}")
	public ResponseEntity<ApiResponse<CompanyData>> update(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId,
			@Valid @RequestBody CompanyUpdateRequest request) {
		CompanyData data = companyService.update(extractAccessToken(authorizationHeader), companyId, request);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	@PostMapping("/{companyId}/logo")
	public ResponseEntity<ApiResponse<CompanyData>> updateLogo(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId,
			@Valid @RequestBody CompanyLogoRequest request) {
		CompanyData data = companyService.updateLogo(extractAccessToken(authorizationHeader), companyId, request);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	@GetMapping("/{companyId}/stats")
	public ResponseEntity<ApiResponse<CompanyStatsData>> stats(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId) {
		CompanyStatsData data = companyService.stats(extractAccessToken(authorizationHeader), companyId);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
