package com.threadia.backend.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// JwtAuthenticationException(401) 매핑은 config.jwt 패키지 구현 시 여기에 추가한다.
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, Object> details = new LinkedHashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			details.put(fieldError.getField(), fieldError.getDefaultMessage());
		}
		ErrorBody body = ErrorBody.of("VALIDATION_ERROR", "요청 형식이 올바르지 않습니다.", details);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(body));
	}

	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
		ErrorBody body = ErrorBody.of("CONFLICT", ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(body));
	}
}
