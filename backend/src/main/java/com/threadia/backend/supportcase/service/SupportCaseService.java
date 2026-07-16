package com.threadia.backend.supportcase.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.platform.audit.AdminAction;
import com.threadia.backend.platform.audit.AdminActionAuditService;
import com.threadia.backend.platform.audit.AuditReason;
import com.threadia.backend.platform.audit.AuditTargetType;
import com.threadia.backend.supportcase.domain.SupportCase;
import com.threadia.backend.supportcase.domain.SupportCaseType;
import com.threadia.backend.supportcase.dto.PublicReplyResponse;
import com.threadia.backend.supportcase.dto.SupportCaseCreateRequest;
import com.threadia.backend.supportcase.dto.SupportCaseDetailData;
import com.threadia.backend.supportcase.dto.SupportCaseResponse;
import com.threadia.backend.supportcase.dto.SupportCasesData;
import com.threadia.backend.supportcase.exception.SupportCaseNotFoundException;
import com.threadia.backend.supportcase.exception.SupportCaseValidationException;
import com.threadia.backend.supportcase.repository.SupportCasePublicReplyRepository;
import com.threadia.backend.supportcase.repository.SupportCaseRepository;

@Service
public class SupportCaseService {

	private final AuthContextService authContextService;
	private final SupportCaseRepository supportCaseRepository;
	private final SupportCasePublicReplyRepository publicReplyRepository;
	private final AdminActionAuditService auditService;
	private final Clock clock;

	@Autowired
	public SupportCaseService(
			AuthContextService authContextService,
			SupportCaseRepository supportCaseRepository,
			SupportCasePublicReplyRepository publicReplyRepository,
			AdminActionAuditService auditService) {
		this(authContextService, supportCaseRepository, publicReplyRepository, auditService, Clock.systemUTC());
	}

	SupportCaseService(
			AuthContextService authContextService,
			SupportCaseRepository supportCaseRepository,
			SupportCasePublicReplyRepository publicReplyRepository,
			AdminActionAuditService auditService,
			Clock clock) {
		this.authContextService = authContextService;
		this.supportCaseRepository = supportCaseRepository;
		this.publicReplyRepository = publicReplyRepository;
		this.auditService = auditService;
		this.clock = clock;
	}

	@Transactional
	public SupportCaseDetailData create(String accessToken, SupportCaseCreateRequest request) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		User reporter = authContextService.requireActiveUser(authContext);
		String reason = AuditReason.requireValid(request.reason());
		validateCreateRequest(request);
		SupportCase supportCase = supportCaseRepository.save(new SupportCase(
				reporter,
				request.type(),
				request.targetType(),
				request.targetId(),
				request.title().trim(),
				request.body(),
				LocalDateTime.now(clock)));
		auditService.recordUserMutation(
				supportCase,
				reporter,
				AdminAction.SUPPORT_CASE_CREATED,
				AuditTargetType.SUPPORT_CASE,
				supportCase.getId(),
				reason,
				null,
				SupportCaseAuditStates.supportCase(supportCase));
		return new SupportCaseDetailData(SupportCaseResponse.from(supportCase), List.of());
	}

	@Transactional(readOnly = true)
	public SupportCasesData listOwn(String accessToken) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		List<SupportCaseResponse> cases = supportCaseRepository
				.findAllByReporter_IdOrderByCreatedAtDescIdDesc(authContext.userId())
				.stream()
				.map(SupportCaseResponse::from)
				.toList();
		return new SupportCasesData(cases);
	}

	@Transactional(readOnly = true)
	public SupportCaseDetailData getOwn(String accessToken, Long supportCaseId) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		SupportCase supportCase = supportCaseRepository.findByIdAndReporter_Id(supportCaseId, authContext.userId())
				.orElseThrow(() -> new SupportCaseNotFoundException(supportCaseId));
		List<PublicReplyResponse> replies = publicReplyRepository
				.findAllBySupportCase_IdOrderByCreatedAtAscIdAsc(supportCaseId)
				.stream()
				.map(PublicReplyResponse::from)
				.toList();
		return new SupportCaseDetailData(SupportCaseResponse.from(supportCase), replies);
	}

	private static void validateCreateRequest(SupportCaseCreateRequest request) {
		if (request.type() == null) {
			throw new SupportCaseValidationException("support case type은 필수입니다.");
		}
		if (request.title() == null || request.title().trim().isEmpty() || request.title().trim().length() > 200) {
			throw new SupportCaseValidationException("title은 공백이 아니며 200자 이하여야 합니다.");
		}
		if (request.body() == null || request.body().trim().isEmpty()) {
			throw new SupportCaseValidationException("body는 공백일 수 없습니다.");
		}
		if (request.type() == SupportCaseType.INQUIRY
				&& (request.targetType() != null || request.targetId() != null)) {
			throw new SupportCaseValidationException("INQUIRY에는 report target을 지정할 수 없습니다.");
		}
		if (request.type() == SupportCaseType.REPORT && request.targetType() == null) {
			throw new SupportCaseValidationException("REPORT에는 targetType이 필요합니다.");
		}
	}
}
