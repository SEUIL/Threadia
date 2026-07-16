package com.threadia.backend.platform.company;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.dto.CompanyData;
import com.threadia.backend.company.dto.CompanyResponse;
import com.threadia.backend.company.exception.CompanyNotFoundException;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.exception.CompanyStatusConflictException;
import com.threadia.backend.platform.audit.AdminAction;
import com.threadia.backend.platform.audit.AdminActionAuditService;
import com.threadia.backend.platform.audit.AuditReason;
import com.threadia.backend.platform.audit.AuditTargetType;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;

@Service
public class PlatformCompanyService {
	private final PlatformAdminAccessService platformAdminAccessService;
	private final CompanyRepository companyRepository;
	private final AdminActionAuditService auditService;
	private final Clock clock;

	@Autowired
	public PlatformCompanyService(PlatformAdminAccessService platformAdminAccessService, CompanyRepository companyRepository, AdminActionAuditService auditService) {
		this(platformAdminAccessService, companyRepository, auditService, Clock.systemUTC());
	}

	PlatformCompanyService(PlatformAdminAccessService platformAdminAccessService, CompanyRepository companyRepository, AdminActionAuditService auditService, Clock clock) {
		this.platformAdminAccessService = platformAdminAccessService;
		this.companyRepository = companyRepository;
		this.auditService = auditService;
		this.clock = clock;
	}

	@Transactional
	public CompanyData updateStatus(String accessToken, Long companyId, CompanyStatusUpdateRequest request) {
		PlatformAdminAuthority authority = platformAdminAccessService.requireAdmin(accessToken);
		String reason = AuditReason.requireValid(request.reason());
		Company company = companyRepository.findByIdForUpdate(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
		if (company.getStatus() == request.status()) {
			throw new CompanyStatusConflictException("회사 상태가 이미 요청한 상태입니다.");
		}
		CompanyStatus before = company.getStatus();
		company.setStatus(request.status());
		company.setUpdatedAt(LocalDateTime.now(clock));
		auditService.recordPlatformMutation(null, authority.actor(), authority.platformAdmin(), AdminAction.COMPANY_STATUS_CHANGED,
				AuditTargetType.COMPANY, company.getId(), reason, Map.of("status", before.name()), Map.of("status", company.getStatus().name()));
		return new CompanyData(CompanyResponse.from(company));
	}
}
