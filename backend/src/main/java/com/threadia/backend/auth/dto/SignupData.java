package com.threadia.backend.auth.dto;

import com.threadia.backend.common.auth.UserResponse;

// POST /api/v1/auth/signup 성공 응답의 data. signup은 토큰을 발급하지 않는다(자동 로그인 아님).
public record SignupData(UserResponse user) {
}
