package com.threadia.backend.config.jwt;

// 토큰 누락, 만료, 변조, 형식 오류 시 던진다. GlobalExceptionHandler가 401로 변환한다.
public class JwtAuthenticationException extends RuntimeException {

	public JwtAuthenticationException(String message) {
		super(message);
	}

	public JwtAuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}
}
