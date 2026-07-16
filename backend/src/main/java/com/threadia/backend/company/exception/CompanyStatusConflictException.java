package com.threadia.backend.company.exception;

public class CompanyStatusConflictException extends RuntimeException {
	public CompanyStatusConflictException(String message) {
		super(message);
	}
}
