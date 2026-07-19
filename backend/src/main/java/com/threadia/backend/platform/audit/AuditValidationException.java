package com.threadia.backend.platform.audit;

public class AuditValidationException extends RuntimeException {
	public AuditValidationException(String message) {
		super(message);
	}
}
