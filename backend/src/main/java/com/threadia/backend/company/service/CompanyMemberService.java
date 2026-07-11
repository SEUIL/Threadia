package com.threadia.backend.company.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.dto.CompanyMemberData;
import com.threadia.backend.company.dto.CompanyMemberResponse;
import com.threadia.backend.company.dto.CompanyMemberUpdateRequest;
import com.threadia.backend.company.dto.CompanyMembersData;
import com.threadia.backend.company.exception.CompanyMemberConflictException;
import com.threadia.backend.company.exception.CompanyMemberNotFoundException;
import com.threadia.backend.company.exception.CompanyNotFoundException;
import com.threadia.backend.company.exception.DepartmentNotFoundException;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.repository.DepartmentRepository;

@Service
public class CompanyMemberService {

	private final AuthContextService authContextService;
	private final CompanyAccessService companyAccessService;
	private final CompanyRepository companyRepository;
	private final CompanyMemberRepository companyMemberRepository;
	private final DepartmentRepository departmentRepository;
	private final Clock clock;

	@Autowired
	public CompanyMemberService(
			AuthContextService authContextService,
			CompanyAccessService companyAccessService,
			CompanyRepository companyRepository,
			CompanyMemberRepository companyMemberRepository,
			DepartmentRepository departmentRepository) {
		this(
				authContextService,
				companyAccessService,
				companyRepository,
				companyMemberRepository,
				departmentRepository,
				Clock.systemUTC());
	}

	CompanyMemberService(
			AuthContextService authContextService,
			CompanyAccessService companyAccessService,
			CompanyRepository companyRepository,
			CompanyMemberRepository companyMemberRepository,
			DepartmentRepository departmentRepository,
			Clock clock) {
		this.authContextService = authContextService;
		this.companyAccessService = companyAccessService;
		this.companyRepository = companyRepository;
		this.companyMemberRepository = companyMemberRepository;
		this.departmentRepository = departmentRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public CompanyMembersData list(
			String accessToken,
			Long companyId,
			CompanyMemberStatus status,
			Long departmentId,
			String q) {
		AuthContext authContext = activeAuthContext(accessToken);
		company(companyId);
		companyAccessService.requireCompanyAdmin(companyId, authContext);
		String normalizedQ = normalizeQ(q);
		return new CompanyMembersData(companyMemberRepository.findByCompanyIdWithFiltersOrderByIdAsc(
					companyId,
					status,
					departmentId,
					normalizedQ)
				.stream()
				.map(CompanyMemberResponse::from)
				.toList());
	}

	private static String normalizeQ(String q) {
		if (q == null || q.trim().isEmpty()) {
			return null;
		}
		return q.trim().toLowerCase(Locale.ROOT);
	}

	@Transactional
	public CompanyMemberData update(String accessToken, Long companyId, Long companyMemberId, CompanyMemberUpdateRequest request) {
		AuthContext authContext = activeAuthContext(accessToken);
		companyForUpdate(companyId);
		companyAccessService.requireCompanyAdmin(companyId, authContext);
		CompanyMember companyMember = companyMemberForUpdate(companyMemberId, companyId);
		validateDepartment(companyId, request.departmentId());
		validateLeftMemberReactivation(companyMember, request.status());
		validateLastActiveCompanyAdmin(companyMember, request.role(), request.status());
		companyMember.setRole(request.role());
		companyMember.setStatus(request.status());
		companyMember.setDepartmentId(request.departmentId());
		companyMember.setUpdatedAt(now());
		return new CompanyMemberData(CompanyMemberResponse.from(companyMember));
	}

	private AuthContext activeAuthContext(String accessToken) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		return authContext;
	}

	private void validateDepartment(Long companyId, Long departmentId) {
		if (departmentId != null && !departmentRepository.existsByIdAndCompany_Id(departmentId, companyId)) {
			throw new DepartmentNotFoundException(departmentId);
		}
	}

	private static void validateLeftMemberReactivation(CompanyMember companyMember, CompanyMemberStatus requestedStatus) {
		if (companyMember.getStatus() == CompanyMemberStatus.LEFT && requestedStatus == CompanyMemberStatus.ACTIVE) {
			throw CompanyMemberConflictException.leftMemberReactivation();
		}
	}

	private void validateLastActiveCompanyAdmin(
			CompanyMember companyMember,
			CompanyMemberRole requestedRole,
			CompanyMemberStatus requestedStatus) {
		if (!isActiveCompanyAdmin(companyMember) || isActiveCompanyAdmin(requestedRole, requestedStatus)) {
			return;
		}
		long activeAdminCount = companyMemberRepository.countByCompany_IdAndRoleAndStatus(
				companyMember.getCompany().getId(),
				CompanyMemberRole.COMPANY_ADMIN,
				CompanyMemberStatus.ACTIVE);
		if (activeAdminCount <= 1) {
			throw CompanyMemberConflictException.lastActiveCompanyAdmin();
		}
	}

	private static boolean isActiveCompanyAdmin(CompanyMember companyMember) {
		return isActiveCompanyAdmin(companyMember.getRole(), companyMember.getStatus());
	}

	private static boolean isActiveCompanyAdmin(CompanyMemberRole role, CompanyMemberStatus status) {
		return role == CompanyMemberRole.COMPANY_ADMIN && status == CompanyMemberStatus.ACTIVE;
	}

	private void company(Long companyId) {
		companyRepository.findById(companyId)
				.orElseThrow(() -> new CompanyNotFoundException(companyId));
	}

	private void companyForUpdate(Long companyId) {
		companyRepository.findByIdForUpdate(companyId)
				.orElseThrow(() -> new CompanyNotFoundException(companyId));
	}

	private CompanyMember companyMemberForUpdate(Long companyMemberId, Long companyId) {
		return companyMemberRepository.findByIdAndCompanyIdForUpdate(companyMemberId, companyId)
				.orElseThrow(() -> new CompanyMemberNotFoundException(companyMemberId));
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime();
	}
}
