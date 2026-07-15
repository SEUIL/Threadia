package com.threadia.backend.company.service;

import org.springframework.stereotype.Service;

import com.threadia.backend.common.auth.AuthContext;

@Service
public class CompanyAccessService {

	private final CompanyMembershipService companyMembershipService;

	public CompanyAccessService(CompanyMembershipService companyMembershipService) {
		this.companyMembershipService = companyMembershipService;
	}

	public void requireActiveMember(Long companyId, AuthContext authContext) {
		companyMembershipService.requireActiveMember(companyId, authContext);
	}

	public void requireCompanyAdmin(Long companyId, AuthContext authContext) {
		companyMembershipService.requireCompanyAdmin(companyId, authContext);
	}
}
