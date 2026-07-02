package com.threadia.backend.common;

import java.util.Map;

public record ErrorBody(String code, String message, Map<String, Object> details) {

	public static ErrorBody of(String code, String message) {
		return new ErrorBody(code, message, Map.of());
	}

	public static ErrorBody of(String code, String message, Map<String, Object> details) {
		return new ErrorBody(code, message, details);
	}
}
