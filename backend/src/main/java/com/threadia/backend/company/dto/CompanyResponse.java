package com.threadia.backend.company.dto;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.company.domain.Company;

public record CompanyResponse(
		Long id,
		String name,
		String representativeName,
		String description,
		String industryCategory,
		String logoUrl,
		String status,
		String createdAt,
		String updatedAt) {

	public static CompanyResponse from(Company company) {
		return new CompanyResponse(
				company.getId(),
				company.getName(),
				company.getRepresentativeName(),
				company.getDescription(),
				company.getIndustryCategory(),
				company.getLogoUrl(),
			company.getStatus().name(),
				formatUtc(company.getCreatedAt()),
				formatUtc(company.getUpdatedAt()));
	}

	private static String formatUtc(java.time.LocalDateTime value) {
		return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
