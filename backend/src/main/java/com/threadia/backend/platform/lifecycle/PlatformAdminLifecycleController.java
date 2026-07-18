package com.threadia.backend.platform.lifecycle;

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
import com.threadia.backend.config.jwt.JwtAuthenticationException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/platform")
public class PlatformAdminLifecycleController {

	private final PlatformAdminLifecycleService lifecycleService;

	public PlatformAdminLifecycleController(PlatformAdminLifecycleService lifecycleService) {
		this.lifecycleService = lifecycleService;
	}

	@GetMapping("/actor")
	public ResponseEntity<ApiResponse<PlatformActorData>> actor(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		return ResponseEntity.ok(ApiResponse.of(lifecycleService.actor(accessToken(authorizationHeader))));
	}

	@GetMapping("/admins")
	public ResponseEntity<ApiResponse<PlatformAdminsData>> list(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		return ResponseEntity.ok(ApiResponse.of(lifecycleService.list(accessToken(authorizationHeader))));
	}

	@PostMapping("/admins")
	public ResponseEntity<ApiResponse<PlatformAdminResponse>> grant(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@Valid @RequestBody GrantPlatformAdminRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(lifecycleService.grant(accessToken(authorizationHeader), request)));
	}

	@PostMapping("/admins/{platformAdminId}/revoke")
	public ResponseEntity<ApiResponse<PlatformAdminResponse>> revoke(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long platformAdminId,
			@Valid @RequestBody PlatformAdminLifecycleRequest request) {
		return ResponseEntity.ok(ApiResponse.of(lifecycleService.revoke(accessToken(authorizationHeader), platformAdminId, request)));
	}

	@PostMapping("/admins/{platformAdminId}/reactivate")
	public ResponseEntity<ApiResponse<PlatformAdminResponse>> reactivate(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long platformAdminId,
			@Valid @RequestBody PlatformAdminLifecycleRequest request) {
		return ResponseEntity.ok(ApiResponse.of(lifecycleService.reactivate(accessToken(authorizationHeader), platformAdminId, request)));
	}

	@PatchMapping("/admins/{platformAdminId}/role")
	public ResponseEntity<ApiResponse<PlatformAdminResponse>> changeRole(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable Long platformAdminId,
			@Valid @RequestBody ChangePlatformAdminRoleRequest request) {
		return ResponseEntity.ok(ApiResponse.of(lifecycleService.changeRole(accessToken(authorizationHeader), platformAdminId, request)));
	}

	private static String accessToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			throw new JwtAuthenticationException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
		}
		return authorizationHeader.substring("Bearer ".length());
	}
}
