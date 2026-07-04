package com.threadia.backend.auth.dto;

import com.threadia.backend.common.auth.EmailNormalizer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(@NotBlank @Email String email) {

	public PasswordResetRequest {
		email = EmailNormalizer.normalize(email);
	}
}
