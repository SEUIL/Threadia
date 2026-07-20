package com.threadia.backend.platform.company;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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
	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 100;
	private final PlatformAdminAccessService platformAdminAccessService;
	private final CompanyRepository companyRepository;
	private final AdminActionAuditService auditService;
	private final PlatformCompanyCursorCodec cursorCodec;
	private final Clock clock;

	@Autowired
	public PlatformCompanyService(PlatformAdminAccessService platformAdminAccessService, CompanyRepository companyRepository, AdminActionAuditService auditService, PlatformCompanyCursorCodec cursorCodec) {
		this(platformAdminAccessService, companyRepository, auditService, cursorCodec, Clock.systemUTC());
	}

	PlatformCompanyService(PlatformAdminAccessService platformAdminAccessService, CompanyRepository companyRepository, AdminActionAuditService auditService, PlatformCompanyCursorCodec cursorCodec, Clock clock) {
		this.platformAdminAccessService = platformAdminAccessService;
		this.companyRepository = companyRepository;
		this.auditService = auditService;
		this.cursorCodec = cursorCodec;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public PlatformCompanyPageData list(String accessToken, String status, String q, String cursor, Integer requestedSize) {
		PlatformAdminAuthority requester = platformAdminAccessService.requireAdmin(accessToken);
		PlatformCompanyFilters filters = filters(status, q);
		int size = pageSize(requestedSize);
		PlatformCompanyCursorCodec.Cursor decoded = cursor == null || cursor.isBlank() ? null
				: cursorCodec.decode(cursor, requester.actor().getId(), filters);
		List<Company> rows = companyRepository.findPlatformCompanyPage(filters.status(), filters.q(), decoded == null ? null : decoded.id(), PageRequest.of(0, size + 1));
		boolean hasNext = rows.size() > size;
		List<Company> page = hasNext ? rows.subList(0, size) : rows;
		String nextCursor = hasNext ? cursorCodec.encode(requester.actor().getId(), filters, page.get(page.size() - 1)) : null;
		return new PlatformCompanyPageData(page.stream().map(CompanyResponse::from).toList(), nextCursor, hasNext, size);
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

	private static PlatformCompanyFilters filters(String status, String q) {
		return new PlatformCompanyFilters(parseStatus(status), normalizedQuery(q));
	}

	private static CompanyStatus parseStatus(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return CompanyStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new PlatformCompanyValidationException("status 값이 올바르지 않습니다.");
		}
	}

	private static String normalizedQuery(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		if (normalized.length() > 150) {
			throw new PlatformCompanyValidationException("q는 150자 이하여야 합니다.");
		}
		return escapeLike(normalized);
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static int pageSize(Integer requestedSize) {
		int size = requestedSize == null ? DEFAULT_SIZE : requestedSize;
		if (size < 1 || size > MAX_SIZE) {
			throw new PlatformCompanyValidationException("size는 1 이상 100 이하여야 합니다.");
		}
		return size;
	}
}
