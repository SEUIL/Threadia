package com.threadia.backend.company.dto;

public record CompanyCreateData(
		CompanyResponse company,
		CompanyMemberSummaryResponse companyMember) {
}
