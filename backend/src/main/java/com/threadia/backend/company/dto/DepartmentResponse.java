package com.threadia.backend.company.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.company.domain.Department;

public record DepartmentResponse(
		Long id,
		Long companyId,
		String name,
		String createdAt,
		String updatedAt) {

	public static DepartmentResponse from(Department department) {
		return new DepartmentResponse(
				department.getId(),
				department.getCompany().getId(),
				department.getName(),
				formatUtc(department.getCreatedAt()),
				formatUtc(department.getUpdatedAt()));
	}

	private static String formatUtc(LocalDateTime value) {
		return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
