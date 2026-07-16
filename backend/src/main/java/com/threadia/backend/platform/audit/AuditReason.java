package com.threadia.backend.platform.audit;

import com.threadia.backend.supportcase.exception.SupportCaseValidationException;

public final class AuditReason {

	private AuditReason() {
	}

	public static String requireValid(String reason) {
		String normalized = reason == null ? "" : reason.trim();
		if (normalized.length() < 3 || normalized.length() > 500) {
			throw new SupportCaseValidationException("audit reason은 공백 제외 3자 이상 500자 이하여야 합니다.");
		}
		return normalized;
	}
}
