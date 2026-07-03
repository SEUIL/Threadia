package com.threadia.backend.auth.service;

import org.springframework.http.ResponseCookie;

import com.threadia.backend.auth.dto.LoginData;

public record OAuthLoginResult(LoginData data, ResponseCookie refreshCookie) {
}
