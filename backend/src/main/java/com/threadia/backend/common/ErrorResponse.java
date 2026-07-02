package com.threadia.backend.common;

import java.util.UUID;

public record ErrorResponse(ErrorBody error, Meta meta) {

	public static ErrorResponse of(ErrorBody error) {
		return new ErrorResponse(error, new Meta("req_" + UUID.randomUUID()));
	}
}
