package com.threadia.backend.common.error;

import java.util.Map;

// 에러 응답의 실제 내용. code는 클라이언트가 분기 처리할 기계 판독용 문자열(VALIDATION_ERROR 등).
public record ErrorBody(String code, String message, Map<String, Object> details) {

	// details가 필요 없는 단순 에러용.
	public static ErrorBody of(String code, String message) {
		return new ErrorBody(code, message, Map.of());
	}

	// validation 실패처럼 필드별 상세 정보가 필요한 에러용.
	public static ErrorBody of(String code, String message, Map<String, Object> details) {
		return new ErrorBody(code, message, details);
	}
}
