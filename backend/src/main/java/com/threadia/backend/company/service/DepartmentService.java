package com.threadia.backend.company.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.Department;
import com.threadia.backend.company.dto.DepartmentData;
import com.threadia.backend.company.dto.DepartmentDeleteData;
import com.threadia.backend.company.dto.DepartmentRequest;
import com.threadia.backend.company.dto.DepartmentResponse;
import com.threadia.backend.company.dto.DepartmentsData;
import com.threadia.backend.company.exception.CompanyNotFoundException;
import com.threadia.backend.company.exception.DepartmentConflictException;
import com.threadia.backend.company.exception.DepartmentNotFoundException;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.repository.DepartmentRepository;

@Service
public class DepartmentService {

	private final AuthContextService authContextService;
	private final CompanyAccessService companyAccessService;
	private final CompanyRepository companyRepository;
	private final DepartmentRepository departmentRepository;
	private final CompanyMemberRepository companyMemberRepository;
	private final Clock clock;

	@Autowired
	public DepartmentService(
			AuthContextService authContextService,
			CompanyAccessService companyAccessService,
			CompanyRepository companyRepository,
			DepartmentRepository departmentRepository,
			CompanyMemberRepository companyMemberRepository) {
		this(
				authContextService,
				companyAccessService,
				companyRepository,
				departmentRepository,
				companyMemberRepository,
				Clock.systemUTC());
	}

	DepartmentService(
			AuthContextService authContextService,
			CompanyAccessService companyAccessService,
			CompanyRepository companyRepository,
			DepartmentRepository departmentRepository,
			CompanyMemberRepository companyMemberRepository,
			Clock clock) {
		this.authContextService = authContextService;
		this.companyAccessService = companyAccessService;
		this.companyRepository = companyRepository;
		this.departmentRepository = departmentRepository;
		this.companyMemberRepository = companyMemberRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DepartmentsData list(String accessToken, Long companyId) {
		AuthContext authContext = activeAuthContext(accessToken);
		company(companyId);
		companyAccessService.requireCompanyAdmin(companyId, authContext);
		return new DepartmentsData(departmentRepository.findByCompany_IdOrderByNameAsc(companyId)
				.stream()
				.map(DepartmentResponse::from)
				.toList());
	}

	@Transactional
	public DepartmentData create(String accessToken, Long companyId, DepartmentRequest request) {
		AuthContext authContext = activeAuthContext(accessToken);
		Company company = company(companyId);
		companyAccessService.requireCompanyAdmin(companyId, authContext);
		String name = normalizedName(request.name());
		if (departmentRepository.existsByCompany_IdAndName(companyId, name)) {
			throw DepartmentConflictException.duplicateName();
		}
		LocalDateTime now = now();
		Department department = departmentRepository.save(new Department(company, name, now, now));
		return new DepartmentData(DepartmentResponse.from(department));
	}

	@Transactional
	public DepartmentData update(String accessToken, Long departmentId, DepartmentRequest request) {
		AuthContext authContext = activeAuthContext(accessToken);
		Department department = departmentForUpdate(departmentId);
		Long companyId = department.getCompany().getId();
		companyAccessService.requireCompanyAdmin(companyId, authContext);
		String name = normalizedName(request.name());
		if (departmentRepository.existsByCompany_IdAndNameAndIdNot(companyId, name, departmentId)) {
			throw DepartmentConflictException.duplicateName();
		}
		department.setName(name);
		department.setUpdatedAt(now());
		return new DepartmentData(DepartmentResponse.from(department));
	}

	@Transactional
	public DepartmentDeleteData delete(String accessToken, Long departmentId) {
		AuthContext authContext = activeAuthContext(accessToken);
		Department department = departmentForUpdate(departmentId);
		companyAccessService.requireCompanyAdmin(department.getCompany().getId(), authContext);
		long affectedMemberCount = companyMemberRepository.countByDepartmentId(departmentId);
		companyMemberRepository.clearDepartmentId(departmentId, now());
		departmentRepository.delete(department);
		return new DepartmentDeleteData(true, affectedMemberCount);
	}

	private AuthContext activeAuthContext(String accessToken) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		return authContext;
	}

	private Company company(Long companyId) {
		return companyRepository.findById(companyId)
				.orElseThrow(() -> new CompanyNotFoundException(companyId));
	}

	private Department departmentForUpdate(Long departmentId) {
		return departmentRepository.findByIdForUpdate(departmentId)
				.orElseThrow(() -> new DepartmentNotFoundException(departmentId));
	}

	private static String normalizedName(String name) {
		return name.trim();
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime();
	}
}
