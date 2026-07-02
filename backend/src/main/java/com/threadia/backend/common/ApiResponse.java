package com.threadia.backend.common;

import java.util.UUID;

public record ApiResponse<T>(T data, Meta meta) {

	public static <T> ApiResponse<T> of(T data) {
		return new ApiResponse<>(data, new Meta("req_" + UUID.randomUUID()));
	}
}
