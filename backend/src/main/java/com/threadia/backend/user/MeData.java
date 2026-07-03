package com.threadia.backend.user;

import com.threadia.backend.common.auth.UserResponse;

// GET /api/v1/auth/me 성공 응답의 data.
public record MeData(UserResponse user) {
}
