package com.threadia.backend.company.dto;

import com.threadia.backend.company.domain.CompanyMember;

public record CompanyMemberSummaryResponse(
		Long id,
		String role,
		String status) {

	public static CompanyMemberSummaryResponse from(CompanyMember companyMember) {
		return new CompanyMemberSummaryResponse(
				companyMember.getId(),
				companyMember.getRole().name(),
				companyMember.getStatus().name());
	}
}
