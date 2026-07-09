package com.threadia.backend.company.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.dto.CompanyCreateData;
import com.threadia.backend.company.dto.CompanyCreateRequest;
import com.threadia.backend.company.dto.CompanyData;
import com.threadia.backend.company.dto.CompanyLogoRequest;
import com.threadia.backend.company.dto.CompanyMemberSummaryResponse;
import com.threadia.backend.company.dto.CompanyResponse;
import com.threadia.backend.company.dto.CompanyStatsData;
import com.threadia.backend.company.dto.CompanyUpdateRequest;
import com.threadia.backend.company.exception.CompanyNotFoundException;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.repository.CompanyStatsRepository;

@Service
public class CompanyService {

	private final AuthContextService authContextService;
	private final CompanyAccessService companyAccessService;
	private final CompanyRepository companyRepository;
	private final CompanyMemberRepository companyMemberRepository;
	private final CompanyStatsRepository companyStatsRepository;
	private final Clock clock;

	@Autowired
	public CompanyService(
			AuthContextService authContextService,
			CompanyAccessService companyAccessService,
			CompanyRepository companyRepository,
			CompanyMemberRepository companyMemberRepository,
			CompanyStatsRepository companyStatsRepository) {
		this(authContextService, companyAccessService, companyRepository, companyMemberRepository, companyStatsRepository, Clock.systemUTC());
	}

	CompanyService(
			AuthContextService authContextService,
			CompanyAccessService companyAccessService,
			CompanyRepository companyRepository,
			CompanyMemberRepository companyMemberRepository,
			CompanyStatsRepository companyStatsRepository,
			Clock clock) {
		this.authContextService = authContextService;
		this.companyAccessService = companyAccessService;
		this.companyRepository = companyRepository;
		this.companyMemberRepository = companyMemberRepository;
		this.companyStatsRepository = companyStatsRepository;
		this.clock = clock;
	}

	@Transactional
	public CompanyCreateData create(String accessToken, CompanyCreateRequest request) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		User creator = authContextService.requireActiveUser(authContext);
		LocalDateTime now = now();
		Company company = companyRepository.save(new Company(
				request.name(),
				request.representativeName(),
				request.description(),
				request.industryCategory(),
				request.logoUrl(),
				"ACTIVE",
				creator,
				now,
				now));
		CompanyMember companyMember = companyMemberRepository.save(new CompanyMember(
				company,
				creator,
				null,
				CompanyMemberRole.COMPANY_ADMIN,
				CompanyMemberStatus.ACTIVE,
				now,
				now,
				now));
		return new CompanyCreateData(CompanyResponse.from(company), CompanyMemberSummaryResponse.from(companyMember));
	}

	@Transactional(readOnly = true)
	public CompanyData get(String accessToken, Long companyId) {
		Company company = company(companyId);
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		companyAccessService.requireActiveMember(companyId, authContext);
		return new CompanyData(CompanyResponse.from(company));
	}

	@Transactional
	public CompanyData update(String accessToken, Long companyId, CompanyUpdateRequest request) {
		Company company = companyForUpdate(companyId);
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		companyAccessService.requireCompanyAdmin(companyId, authContext);
		company.setName(request.name());
		company.setRepresentativeName(request.representativeName());
		company.setDescription(request.description());
		company.setIndustryCategory(request.industryCategory());
		company.setLogoUrl(request.logoUrl());
		company.setUpdatedAt(now());
		return new CompanyData(CompanyResponse.from(company));
	}

	@Transactional
	public CompanyData updateLogo(String accessToken, Long companyId, CompanyLogoRequest request) {
		Company company = companyForUpdate(companyId);
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		companyAccessService.requireCompanyAdmin(companyId, authContext);
		company.setLogoUrl(request.logoUrl());
		company.setUpdatedAt(now());
		return new CompanyData(CompanyResponse.from(company));
	}

	@Transactional(readOnly = true)
	public CompanyStatsData stats(String accessToken, Long companyId) {
		company(companyId);
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		companyAccessService.requireCompanyAdmin(companyId, authContext);
		long memberCount = companyMemberRepository.countByCompany_IdAndStatus(companyId, CompanyMemberStatus.ACTIVE);
		long departmentCount = companyStatsRepository.countDepartments(companyId);
		long projectCount = companyStatsRepository.countProjects(companyId);
		return new CompanyStatsData(
				companyId,
				memberCount,
				departmentCount,
				projectCount,
				companyStatsRepository.countProjectsByStatus(companyId));
	}

	private Company company(Long companyId) {
		return companyRepository.findById(companyId)
				.orElseThrow(() -> new CompanyNotFoundException(companyId));
	}

	private Company companyForUpdate(Long companyId) {
		return companyRepository.findByIdForUpdate(companyId)
				.orElseThrow(() -> new CompanyNotFoundException(companyId));
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime();
	}
}
