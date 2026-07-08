package com.threadia.backend.user;

import com.threadia.backend.company.domain.CompanyMember;

public record UserCompanyMembershipResponse(
		Long companyMemberId,
		Long companyId,
		String companyName,
		String role,
		String status) {

	public static UserCompanyMembershipResponse from(CompanyMember companyMember) {
		return new UserCompanyMembershipResponse(
				companyMember.getId(),
				companyMember.getCompany().getId(),
				companyMember.getCompany().getName(),
				companyMember.getRole().name(),
				companyMember.getStatus().name());
	}
}
