package com.threadia.backend.supportcase.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.platform.audit.AdminAction;
import com.threadia.backend.platform.audit.AdminActionAuditService;
import com.threadia.backend.platform.audit.AuditReason;
import com.threadia.backend.platform.audit.AuditTargetType;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;
import com.threadia.backend.supportcase.domain.SupportCase;
import com.threadia.backend.supportcase.domain.SupportCaseInternalNote;
import com.threadia.backend.supportcase.domain.SupportCasePublicReply;
import com.threadia.backend.supportcase.dto.InternalNoteRequest;
import com.threadia.backend.supportcase.dto.InternalNoteResponse;
import com.threadia.backend.supportcase.dto.InternalNotesData;
import com.threadia.backend.supportcase.dto.PublicReplyRequest;
import com.threadia.backend.supportcase.dto.PublicReplyResponse;
import com.threadia.backend.supportcase.dto.SupportCaseDetailData;
import com.threadia.backend.supportcase.dto.SupportCaseResponse;
import com.threadia.backend.supportcase.dto.SupportCaseStatusUpdateRequest;
import com.threadia.backend.supportcase.dto.SupportCasesData;
import com.threadia.backend.supportcase.exception.SupportCaseNotFoundException;
import com.threadia.backend.supportcase.exception.SupportCaseValidationException;
import com.threadia.backend.supportcase.repository.SupportCaseInternalNoteRepository;
import com.threadia.backend.supportcase.repository.SupportCasePublicReplyRepository;
import com.threadia.backend.supportcase.repository.SupportCaseRepository;

@Service
public class PlatformSupportCaseService {

	private final PlatformAdminAccessService platformAdminAccessService;
	private final SupportCaseRepository supportCaseRepository;
	private final SupportCasePublicReplyRepository publicReplyRepository;
	private final SupportCaseInternalNoteRepository internalNoteRepository;
	private final AdminActionAuditService auditService;
	private final Clock clock;

	@Autowired
	public PlatformSupportCaseService(
			PlatformAdminAccessService platformAdminAccessService,
			SupportCaseRepository supportCaseRepository,
			SupportCasePublicReplyRepository publicReplyRepository,
			SupportCaseInternalNoteRepository internalNoteRepository,
			AdminActionAuditService auditService) {
		this(
				platformAdminAccessService,
				supportCaseRepository,
				publicReplyRepository,
				internalNoteRepository,
				auditService,
				Clock.systemUTC());
	}

	PlatformSupportCaseService(
			PlatformAdminAccessService platformAdminAccessService,
			SupportCaseRepository supportCaseRepository,
			SupportCasePublicReplyRepository publicReplyRepository,
			SupportCaseInternalNoteRepository internalNoteRepository,
			AdminActionAuditService auditService,
			Clock clock) {
		this.platformAdminAccessService = platformAdminAccessService;
		this.supportCaseRepository = supportCaseRepository;
		this.publicReplyRepository = publicReplyRepository;
		this.internalNoteRepository = internalNoteRepository;
		this.auditService = auditService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public SupportCasesData listAll(String accessToken) {
		platformAdminAccessService.requireAdmin(accessToken);
		return new SupportCasesData(supportCaseRepository.findAllByOrderByCreatedAtDescIdDesc()
				.stream()
				.map(SupportCaseResponse::from)
				.toList());
	}

	@Transactional(readOnly = true)
	public SupportCaseDetailData get(String accessToken, Long supportCaseId) {
		platformAdminAccessService.requireAdmin(accessToken);
		return detail(supportCase(supportCaseId));
	}

	@Transactional
	public PublicReplyResponse createPublicReply(
			String accessToken,
			Long supportCaseId,
			PublicReplyRequest request) {
		PlatformAdminAuthority authority = platformAdminAccessService.requireAdmin(accessToken);
		String reason = AuditReason.requireValid(request.reason());
		requireBody(request.body());
		SupportCase supportCase = supportCase(supportCaseId);
		SupportCasePublicReply reply = publicReplyRepository.save(new SupportCasePublicReply(
				supportCase,
				authority.platformAdmin(),
				request.body(),
				LocalDateTime.now(clock)));
		auditService.recordPlatformMutation(
				supportCase,
				authority.actor(),
				authority.platformAdmin(),
				AdminAction.PUBLIC_REPLY_CREATED,
				AuditTargetType.PUBLIC_REPLY,
				reply.getId(),
				reason,
				null,
				SupportCaseAuditStates.publicReply(reply));
		return PublicReplyResponse.from(reply);
	}

	@Transactional
	public InternalNoteResponse createInternalNote(
			String accessToken,
			Long supportCaseId,
			InternalNoteRequest request) {
		PlatformAdminAuthority authority = platformAdminAccessService.requireAdmin(accessToken);
		String reason = AuditReason.requireValid(request.reason());
		requireBody(request.body());
		SupportCase supportCase = supportCase(supportCaseId);
		SupportCaseInternalNote note = internalNoteRepository.save(new SupportCaseInternalNote(
				supportCase,
				authority.platformAdmin(),
				request.body(),
				LocalDateTime.now(clock)));
		auditService.recordPlatformMutation(
				supportCase,
				authority.actor(),
				authority.platformAdmin(),
				AdminAction.INTERNAL_NOTE_CREATED,
				AuditTargetType.INTERNAL_NOTE,
				note.getId(),
				reason,
				null,
				SupportCaseAuditStates.internalNote(note));
		return InternalNoteResponse.from(note);
	}

	@Transactional(readOnly = true)
	public InternalNotesData listInternalNotes(String accessToken, Long supportCaseId) {
		platformAdminAccessService.requireAdmin(accessToken);
		supportCase(supportCaseId);
		List<InternalNoteResponse> notes = internalNoteRepository
				.findAllBySupportCase_IdOrderByCreatedAtAscIdAsc(supportCaseId)
				.stream()
				.map(InternalNoteResponse::from)
				.toList();
		return new InternalNotesData(notes);
	}

	@Transactional
	public SupportCaseDetailData updateStatus(
			String accessToken,
			Long supportCaseId,
			SupportCaseStatusUpdateRequest request) {
		PlatformAdminAuthority authority = platformAdminAccessService.requireAdmin(accessToken);
		String reason = AuditReason.requireValid(request.reason());
		if (request.status() == null) {
			throw new SupportCaseValidationException("status는 필수입니다.");
		}
		SupportCase supportCase = supportCaseRepository.findByIdForUpdate(supportCaseId)
				.orElseThrow(() -> new SupportCaseNotFoundException(supportCaseId));
		Map<String, Object> beforeState = SupportCaseAuditStates.status(supportCase);
		supportCase.transitionTo(request.status(), LocalDateTime.now(clock));
		auditService.recordPlatformMutation(
				supportCase,
				authority.actor(),
				authority.platformAdmin(),
				AdminAction.STATUS_CHANGED,
				AuditTargetType.SUPPORT_CASE,
				supportCase.getId(),
				reason,
				beforeState,
				SupportCaseAuditStates.status(supportCase));
		return detail(supportCase);
	}

	private SupportCaseDetailData detail(SupportCase supportCase) {
		List<PublicReplyResponse> replies = publicReplyRepository
				.findAllBySupportCase_IdOrderByCreatedAtAscIdAsc(supportCase.getId())
				.stream()
				.map(PublicReplyResponse::from)
				.toList();
		return new SupportCaseDetailData(SupportCaseResponse.from(supportCase), replies);
	}

	private SupportCase supportCase(Long supportCaseId) {
		return supportCaseRepository.findById(supportCaseId)
				.orElseThrow(() -> new SupportCaseNotFoundException(supportCaseId));
	}

	private static void requireBody(String body) {
		if (body == null || body.trim().isEmpty()) {
			throw new SupportCaseValidationException("body는 공백일 수 없습니다.");
		}
	}
}
