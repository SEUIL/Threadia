package com.threadia.backend.auth;

import com.threadia.backend.common.UserResponse;

// signup은 토큰을 발급하지 않는다 (자동 로그인 아님). user 정보만 반환.
public record SignupData(UserResponse user) {
}
