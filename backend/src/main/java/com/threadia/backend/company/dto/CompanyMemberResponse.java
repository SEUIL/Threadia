package com.threadia.backend.company.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.company.domain.CompanyMember;

public record CompanyMemberResponse(
		Long id,
		Long companyId,
		Long departmentId,
		String role,
		String status,
		String joinedAt,
		String createdAt,
		String updatedAt,
		UserResponse user) {

	public static CompanyMemberResponse from(CompanyMember companyMember) {
		return new CompanyMemberResponse(
				companyMember.getId(),
				companyMember.getCompany().getId(),
				companyMember.getDepartmentId(),
				companyMember.getRole().name(),
				companyMember.getStatus().name(),
				formatUtc(companyMember.getJoinedAt()),
				formatUtc(companyMember.getCreatedAt()),
				formatUtc(companyMember.getUpdatedAt()),
				UserResponse.from(companyMember.getUser()));
	}

	private static String formatUtc(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
