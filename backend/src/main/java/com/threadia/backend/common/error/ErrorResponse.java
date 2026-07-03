package com.threadia.backend.common.error;

import java.util.UUID;

import com.threadia.backend.common.response.Meta;

// 모든 에러 응답의 공통 envelope: { "error": {...}, "meta": { "requestId": "..." } }
public record ErrorResponse(ErrorBody error, Meta meta) {

	public static ErrorResponse of(ErrorBody error) {
		return new ErrorResponse(error, new Meta("req_" + UUID.randomUUID()));
	}
}
