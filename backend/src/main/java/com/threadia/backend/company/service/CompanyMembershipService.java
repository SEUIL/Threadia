package com.threadia.backend.company.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;

@Service
@Transactional(readOnly = true)
public class CompanyMembershipService {

	private final CompanyMemberRepository companyMemberRepository;
	private final CompanyRepository companyRepository;

	@Autowired
	public CompanyMembershipService(CompanyMemberRepository companyMemberRepository, CompanyRepository companyRepository) {
		this.companyMemberRepository = companyMemberRepository;
		this.companyRepository = companyRepository;
	}

	public Optional<CompanyMember> findMembership(Long companyId, Long userId) {
		if (!isCompanyActive(companyId)) {
			return Optional.empty();
		}
		return companyMemberRepository.findByCompany_IdAndUser_Id(companyId, userId);
	}

	public Optional<CompanyMember> findMembership(Long companyId, AuthContext authContext) {
		return findMembership(companyId, authContext.userId());
	}

	public boolean isActiveMember(Long companyId, Long userId) {
		if (!isCompanyActive(companyId)) {
			return false;
		}
		return companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(
				companyId,
				userId,
				CompanyMemberStatus.ACTIVE);
	}

	public boolean isActiveMember(Long companyId, AuthContext authContext) {
		return isActiveMember(companyId, authContext.userId());
	}

	public boolean isActiveMemberForUpdate(Long companyId, Long userId) {
		if (!isCompanyActive(companyId)) {
			return false;
		}
		return companyMemberRepository.findByCompany_IdAndUser_IdForUpdate(companyId, userId)
				.filter(companyMember -> companyMember.getStatus() == CompanyMemberStatus.ACTIVE)
				.isPresent();
	}

	public boolean hasRole(Long companyId, Long userId, CompanyMemberRole role, CompanyMemberStatus status) {
		if (!isCompanyActive(companyId)) {
			return false;
		}
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

	public void requireCompanyAdminForUpdate(Long companyId, AuthContext authContext) {
		if (!isCompanyActive(companyId)) {
			throw new ForbiddenException("회사 관리자 권한이 필요합니다.");
		}
		boolean companyAdmin = companyMemberRepository.findByCompany_IdAndUser_IdForUpdate(companyId, authContext.userId())
				.filter(companyMember -> companyMember.getRole() == CompanyMemberRole.COMPANY_ADMIN)
				.filter(companyMember -> companyMember.getStatus() == CompanyMemberStatus.ACTIVE)
				.isPresent();
		if (!companyAdmin) {
			throw new ForbiddenException("회사 관리자 권한이 필요합니다.");
		}
	}

	private boolean isCompanyActive(Long companyId) {
		return companyRepository.findById(companyId)
				.map(company -> company.getStatus() == CompanyStatus.ACTIVE)
				.orElse(false);
	}
}
