package com.threadia.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
		@NotBlank String token,

		@NotBlank
		@Size(min = 8, max = 72, message = "password는 8자 이상 72자 이하여야 합니다.")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "password는 영문+숫자 조합이어야 합니다.")
		String newPassword) {
}
