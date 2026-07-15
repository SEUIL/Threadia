package com.threadia.backend.auth.dto;

import com.threadia.backend.common.auth.EmailNormalizer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// POST /api/v1/auth/login 요청 body.
public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank String password) {

	// SignupRequest와 동일한 이유로 validation 전에 정규화한다.
	public LoginRequest {
		email = EmailNormalizer.normalize(email);
	}
}
