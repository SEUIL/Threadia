package com.threadia.backend.auth.service;

import org.springframework.http.ResponseCookie;

import com.threadia.backend.auth.dto.LoginData;

// AuthService -> AuthController 전달용 내부 객체. 응답 body(data)와 Set-Cookie 헤더 값을 함께 넘긴다.
public record LoginResult(LoginData data, ResponseCookie refreshCookie) {
}
