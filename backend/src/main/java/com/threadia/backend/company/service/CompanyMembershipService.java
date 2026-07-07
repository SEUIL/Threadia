package com.threadia.backend.company.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;

@Service
@Transactional(readOnly = true)
public class CompanyMembershipService {

	private final CompanyMemberRepository companyMemberRepository;

	public CompanyMembershipService(CompanyMemberRepository companyMemberRepository) {
		this.companyMemberRepository = companyMemberRepository;
	}

	public Optional<CompanyMember> findMembership(Long companyId, Long userId) {
		return companyMemberRepository.findByCompany_IdAndUser_Id(companyId, userId);
	}

	public Optional<CompanyMember> findMembership(Long companyId, AuthContext authContext) {
		return findMembership(companyId, authContext.userId());
	}

	public boolean isActiveMember(Long companyId, Long userId) {
		return companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(
				companyId,
				userId,
				CompanyMemberStatus.ACTIVE);
	}

	public boolean isActiveMember(Long companyId, AuthContext authContext) {
		return isActiveMember(companyId, authContext.userId());
	}

	public boolean hasRole(Long companyId, Long userId, CompanyMemberRole role, CompanyMemberStatus status) {
		return companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(
				companyId,
				userId,
				role,
				status);
	}

	public boolean hasActiveRole(Long companyId, Long userId, CompanyMemberRole role) {
		return hasRole(companyId, userId, role, CompanyMemberStatus.ACTIVE);
	}

	public boolean hasActiveRole(Long companyId, AuthContext authContext, CompanyMemberRole role) {
		return hasActiveRole(companyId, authContext.userId(), role);
	}

	public void requireActiveMember(Long companyId, AuthContext authContext) {
		if (!isActiveMember(companyId, authContext)) {
			throw new ForbiddenException("회사 접근 권한이 없습니다.");
		}
	}

	public void requireCompanyAdmin(Long companyId, AuthContext authContext) {
		if (!hasActiveRole(companyId, authContext, CompanyMemberRole.COMPANY_ADMIN)) {
			throw new ForbiddenException("회사 관리자 권한이 필요합니다.");
		}
	}
}
