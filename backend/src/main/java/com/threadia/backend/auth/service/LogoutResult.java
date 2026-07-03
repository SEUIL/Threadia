package com.threadia.backend.auth.service;

import org.springframework.http.ResponseCookie;

import com.threadia.backend.auth.dto.LogoutData;

public record LogoutResult(LogoutData data, ResponseCookie clearCookie) {
}
