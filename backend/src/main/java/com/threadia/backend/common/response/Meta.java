package com.threadia.backend.common.response;

// 모든 응답(성공/에러 공통)에 붙는 요청 추적용 식별자. 로그와 매칭해 특정 요청을 추적할 때 쓴다.
public record Meta(String requestId) {
}
