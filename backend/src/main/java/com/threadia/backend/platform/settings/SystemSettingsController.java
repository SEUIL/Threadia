package com.threadia.backend.platform.settings;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/platform/system-settings")
public class SystemSettingsController {
	private final SystemSettingsService settingsService;

	public SystemSettingsController(SystemSettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<SystemSettingsData>> list(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		return ResponseEntity.ok(ApiResponse.of(settingsService.list(accessToken(authorizationHeader))));
	}

	@GetMapping("/{key}")
	public ResponseEntity<ApiResponse<SystemSettingResponse>> get(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable SystemSettingKey key) {
		return ResponseEntity.ok(ApiResponse.of(settingsService.get(accessToken(authorizationHeader), key)));
	}

	@PutMapping("/{key}")
	public ResponseEntity<ApiResponse<SystemSettingResponse>> replace(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable SystemSettingKey key,
			@Valid @RequestBody ReplaceSystemSettingRequest request) {
		return ResponseEntity.ok(ApiResponse.of(settingsService.replace(accessToken(authorizationHeader), key, request)));
	}

	private static String accessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring("Bearer ".length());
	}
}
