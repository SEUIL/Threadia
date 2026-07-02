package com.threadia.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotBlank @Email String email,

		@NotBlank
		@Size(min = 8, message = "password는 8자 이상이어야 합니다.")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "password는 영문+숫자 조합이어야 합니다.")
		String password,

		@NotBlank @Size(min = 2, max = 100) String nickname) {
}
