package com.threadia.backend.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {

	private final MeService meService;

	public UserProfileController(MeService meService) {
		this.meService = meService;
	}

	@PatchMapping("/profile")
	public ResponseEntity<ApiResponse<MeData>> updateProfile(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@Valid @RequestBody ProfileUpdateRequest request) {
		MeData data = meService.updateProfile(MeController.extractAccessToken(authorizationHeader), request);
		return ResponseEntity.ok(ApiResponse.of(data));
	}
}
