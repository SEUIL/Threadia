package com.threadia.backend.platform.company;

import java.util.List;

import com.threadia.backend.company.dto.CompanyResponse;

public record PlatformCompanyPageData(List<CompanyResponse> companies, String nextCursor, boolean hasNext, int size) {
}
