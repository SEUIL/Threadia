package com.threadia.backend.common.exception;

// 로그인 credential 실패, /me에서 유효한 토큰이지만 사용자가 ACTIVE가 아닌 경우 등에 던진다.
// GlobalExceptionHandler가 401 UNAUTHORIZED로 변환한다.
public class AuthenticationFailedException extends RuntimeException {

	public AuthenticationFailedException(String message) {
		super(message);
	}
}
