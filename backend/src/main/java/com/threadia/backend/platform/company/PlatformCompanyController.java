package com.threadia.backend.platform.company;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.company.dto.CompanyData;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/platform/companies")
public class PlatformCompanyController {
	private final PlatformCompanyService companyService;

	public PlatformCompanyController(PlatformCompanyService companyService) {
		this.companyService = companyService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PlatformCompanyPageData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer size) {
		return ResponseEntity.ok(ApiResponse.of(companyService.list(accessToken(authorizationHeader), status, q, cursor, size)));
	}

	@PatchMapping("/{companyId}/status")
	public ResponseEntity<ApiResponse<CompanyData>> updateStatus(@RequestHeader(value = "Authorization", required = false) String authorizationHeader, @PathVariable Long companyId, @Valid @RequestBody CompanyStatusUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.of(companyService.updateStatus(accessToken(authorizationHeader), companyId, request)));
	}

	private static String accessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring("Bearer ".length());
	}
}
