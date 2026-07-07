package com.threadia.backend.company.dto;

import java.util.Map;

public record CompanyStatsData(
		Long companyId,
		long memberCount,
		long departmentCount,
		long projectCount,
		Map<String, Long> projectStatusCounts) {
}
