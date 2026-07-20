package com.threadia.backend.platform.company;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import com.threadia.backend.company.domain.CompanyStatus;

record PlatformCompanyFilters(CompanyStatus status, String q) {

	String statusBinding() {
		return status == null ? "-" : status.name();
	}

	String qBinding() {
		if (q == null) {
			return "-";
		}
		try {
			return Base64.getUrlEncoder().withoutPadding()
					.encodeToString(MessageDigest.getInstance("SHA-256").digest(q.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("company cursor query binding을 생성할 수 없습니다.", exception);
		}
	}
}
