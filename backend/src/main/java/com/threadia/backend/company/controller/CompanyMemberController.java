package com.threadia.backend.company.controller;

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
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.dto.CompanyMemberData;
import com.threadia.backend.company.dto.CompanyMemberRemoveData;
import com.threadia.backend.company.dto.CompanyMemberUpdateRequest;
import com.threadia.backend.company.dto.CompanyMembersData;
import com.threadia.backend.company.service.CompanyMemberService;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class CompanyMemberController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final CompanyMemberService companyMemberService;

	public CompanyMemberController(CompanyMemberService companyMemberService) {
		this.companyMemberService = companyMemberService;
	}

	@GetMapping("/companies/{companyId}/company-members")
	public ResponseEntity<ApiResponse<CompanyMembersData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId,
			@RequestParam(required = false) CompanyMemberStatus status,
			@RequestParam(required = false) Long departmentId,
			@RequestParam(required = false) String q) {
		CompanyMembersData data = companyMemberService.list(
				extractAccessToken(authorizationHeader),
				companyId,
				status,
				departmentId,
				q);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	@PatchMapping("/companies/{companyId}/company-members/{companyMemberId}")
	public ResponseEntity<ApiResponse<CompanyMemberData>> update(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId,
			@PathVariable Long companyMemberId,
			@Valid @RequestBody CompanyMemberUpdateRequest request) {
		CompanyMemberData data = companyMemberService.update(
				extractAccessToken(authorizationHeader),
				companyId,
				companyMemberId,
				request);
		return ResponseEntity.ok(ApiResponse.of(data));
	}


	@DeleteMapping("/companies/{companyId}/company-members/{companyMemberId}")
	public ResponseEntity<ApiResponse<CompanyMemberRemoveData>> remove(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long companyId,
			@PathVariable Long companyMemberId) {
		CompanyMemberRemoveData data = companyMemberService.remove(
				extractAccessToken(authorizationHeader),
				companyId,
				companyMemberId);
		return ResponseEntity.ok(ApiResponse.of(data));
	}

	private static String extractAccessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
