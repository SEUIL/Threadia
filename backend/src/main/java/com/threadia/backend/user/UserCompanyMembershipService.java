package com.threadia.backend.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;

@Service
public class UserCompanyMembershipService {

	private final AuthContextService authContextService;
	private final CompanyMemberRepository companyMemberRepository;

	public UserCompanyMembershipService(
			AuthContextService authContextService,
			CompanyMemberRepository companyMemberRepository) {
		this.authContextService = authContextService;
		this.companyMemberRepository = companyMemberRepository;
	}

	@Transactional(readOnly = true)
	public UserCompanyMembershipsData list(String accessToken) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		return new UserCompanyMembershipsData(companyMemberRepository
				.findByUserIdAndStatusWithCompanyOrderByCompanyIdAscCompanyMemberIdAsc(
						authContext.userId(),
						CompanyMemberStatus.ACTIVE)
				.stream()
				.filter(membership -> membership.getCompany().getStatus() == CompanyStatus.ACTIVE)
				.map(UserCompanyMembershipResponse::from)
				.toList());
	}
}
