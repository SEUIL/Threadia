package com.threadia.backend.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threadia.backend.common.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/users")
public class UserLookupController {

	private final UserLookupService userLookupService;

	public UserLookupController(UserLookupService userLookupService) {
		this.userLookupService = userLookupService;
	}

	@GetMapping("/lookup")
	public ResponseEntity<ApiResponse<UserLookupData>> lookup(@RequestParam String email) {
		UserLookupData data = userLookupService.lookupByEmail(email);
		return ResponseEntity.ok(ApiResponse.of(data));
	}
}
