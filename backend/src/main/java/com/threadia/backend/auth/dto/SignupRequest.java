package com.threadia.backend.auth.dto;

import com.threadia.backend.common.auth.EmailNormalizer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// POST /api/v1/auth/signup 요청 body.
public record SignupRequest(
		@NotBlank @Email String email,

		@NotBlank
		// max 72: BCrypt는 72바이트를 넘는 입력을 조용히 절삭해 해싱하므로, 그 이상은 무의미할 뿐 아니라
		// 앞 72바이트가 같은 비밀번호가 서로 같아지는 문제가 있어 요청 단계에서 막는다.
		// (@Size는 byte가 아닌 char 기준이라 ASCII 비밀번호에서 정확히 72byte와 일치한다.)
		@Size(min = 8, max = 72, message = "password는 8자 이상 72자 이하여야 합니다.")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "password는 영문+숫자 조합이어야 합니다.")
		String password,

		@NotBlank @Size(min = 2, max = 100) String nickname,

		// User.avatarUrl 컬럼 length(500)과 동일한 상한.
		@Size(max = 500) String avatarUrl) {

	// email 형식 검증(@Email)보다 trim+lowercase 정규화가 먼저 적용돼야
	// 앞뒤 공백이 있는 email이 validation에서 잘못 거부되지 않는다.
	public SignupRequest {
		email = EmailNormalizer.normalize(email);
	}
}
