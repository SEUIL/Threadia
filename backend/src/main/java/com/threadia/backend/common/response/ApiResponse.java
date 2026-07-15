package com.threadia.backend.common.response;

import java.util.UUID;

// 모든 성공 응답의 공통 envelope: { "data": {...}, "meta": { "requestId": "..." } }
public record ApiResponse<T>(T data, Meta meta) {

	// requestId를 매번 직접 채우지 않도록 여기서 자동 생성한다.
	public static <T> ApiResponse<T> of(T data) {
		return new ApiResponse<>(data, new Meta("req_" + UUID.randomUUID()));
	}
}
