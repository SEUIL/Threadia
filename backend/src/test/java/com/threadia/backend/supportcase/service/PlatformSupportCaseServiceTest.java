package com.threadia.backend.supportcase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.platform.audit.AdminAction;
import com.threadia.backend.platform.audit.AdminActionAuditService;
import com.threadia.backend.platform.audit.AuditTargetType;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;
import com.threadia.backend.supportcase.domain.SupportCase;
import com.threadia.backend.supportcase.domain.SupportCaseStatus;
import com.threadia.backend.supportcase.domain.SupportCaseType;
import com.threadia.backend.supportcase.dto.InternalNoteRequest;
import com.threadia.backend.supportcase.dto.PublicReplyRequest;
import com.threadia.backend.supportcase.dto.SupportCaseStatusUpdateRequest;
import com.threadia.backend.supportcase.exception.SupportCaseConflictException;
import com.threadia.backend.supportcase.exception.SupportCaseValidationException;
import com.threadia.backend.supportcase.repository.SupportCaseInternalNoteRepository;
import com.threadia.backend.supportcase.repository.SupportCasePublicReplyRepository;
import com.threadia.backend.supportcase.repository.SupportCaseRepository;

@ExtendWith(MockitoExtension.class)
class PlatformSupportCaseServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-15T03:00:00Z"), ZoneOffset.UTC);

	@Mock
	private PlatformAdminAccessService accessService;

	@Mock
	private SupportCaseRepository supportCaseRepository;

	@Mock
	private SupportCasePublicReplyRepository publicReplyRepository;

	@Mock
	private SupportCaseInternalNoteRepository internalNoteRepository;

	@Mock
	private AdminActionAuditService auditService;

	private PlatformSupportCaseService service;
	private User actor;
	private PlatformAdmin platformAdmin;
	private SupportCase supportCase;

	@BeforeEach
	void setUp() {
		service = new PlatformSupportCaseService(
				accessService,
				supportCaseRepository,
				publicReplyRepository,
				internalNoteRepository,
				auditService,
				FIXED_CLOCK);
		actor = user(1L, "admin@example.com");
		platformAdmin = platformAdmin(2L, actor, PlatformAdminRole.PLATFORM_ADMIN);
		supportCase = supportCase(10L, user(3L, "reporter@example.com"));
	}

	@Test
	void validStatusTransitionPersistsBeforeAndAfterAudit() {
		when(accessService.requireAdmin("admin-token"))
				.thenReturn(new PlatformAdminAuthority(actor, platformAdmin));
		when(supportCaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(supportCase));
		when(publicReplyRepository.findAllBySupportCase_IdOrderByCreatedAtAscIdAsc(10L)).thenReturn(List.of());

		var data = service.updateStatus(
				"admin-token",
				10L,
				new SupportCaseStatusUpdateRequest(SupportCaseStatus.IN_PROGRESS, "Begin investigation"));

		assertThat(data.supportCase().status()).isEqualTo(SupportCaseStatus.IN_PROGRESS);
		verify(auditService).recordPlatformMutation(
				eq(supportCase),
				eq(actor),
				eq(platformAdmin),
				eq(AdminAction.STATUS_CHANGED),
				eq(AuditTargetType.SUPPORT_CASE),
				eq(10L),
				eq("Begin investigation"),
				org.mockito.ArgumentMatchers.argThat(state -> state.get("status") == SupportCaseStatus.OPEN),
				org.mockito.ArgumentMatchers.argThat(state -> state.get("status") == SupportCaseStatus.IN_PROGRESS));
	}

	@Test
	void invalidStatusTransitionDoesNotWriteAudit() {
		when(accessService.requireAdmin("admin-token"))
				.thenReturn(new PlatformAdminAuthority(actor, platformAdmin));
		when(supportCaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(supportCase));

		assertThatThrownBy(() -> service.updateStatus(
				"admin-token",
				10L,
				new SupportCaseStatusUpdateRequest(SupportCaseStatus.RESOLVED, "Skip processing")))
				.isInstanceOf(SupportCaseConflictException.class);
		verifyNoInteractions(auditService);
	}

	@Test
	void publicReplyIsDeniedBeforePersistenceForNonPlatformActor() {
		when(accessService.requireAdmin("company-admin-token"))
				.thenThrow(new ForbiddenException("플랫폼 관리자 권한이 필요합니다."));

		assertThatThrownBy(() -> service.createPublicReply(
				"company-admin-token",
				10L,
				new PublicReplyRequest("Public response", "Reply to reporter")))
				.isInstanceOf(ForbiddenException.class);
		verifyNoInteractions(supportCaseRepository, publicReplyRepository, auditService);
	}

	@Test
	void internalNotesUseDedicatedAdminOnlyRepository() {
		when(accessService.requireAdmin("admin-token"))
				.thenReturn(new PlatformAdminAuthority(actor, platformAdmin));
		when(supportCaseRepository.findById(10L)).thenReturn(Optional.of(supportCase));
		when(internalNoteRepository.findAllBySupportCase_IdOrderByCreatedAtAscIdAsc(10L)).thenReturn(List.of());

		assertThat(service.listInternalNotes("admin-token", 10L).internalNotes()).isEmpty();
		verify(internalNoteRepository).findAllBySupportCase_IdOrderByCreatedAtAscIdAsc(10L);
		verifyNoInteractions(publicReplyRepository);
	}

	@Test
	void rejectsShortAuditReasonBeforeAuthorizationOrMutation() {
		when(accessService.requireAdmin("admin-token"))
				.thenReturn(new PlatformAdminAuthority(actor, platformAdmin));
		assertThatThrownBy(() -> service.createInternalNote(
				"admin-token",
				10L,
				new InternalNoteRequest("Private note", "x")))
				.isInstanceOf(SupportCaseValidationException.class);
		verifyNoInteractions(supportCaseRepository, internalNoteRepository, auditService);
	}

	@Test
	void closedCaseCannotReopen() {
		supportCase.transitionTo(SupportCaseStatus.CLOSED, LocalDateTime.now(FIXED_CLOCK));
		when(accessService.requireAdmin("admin-token"))
				.thenReturn(new PlatformAdminAuthority(actor, platformAdmin));
		when(supportCaseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(supportCase));

		assertThatThrownBy(() -> service.updateStatus(
				"admin-token",
				10L,
				new SupportCaseStatusUpdateRequest(SupportCaseStatus.OPEN, "Attempt reopen")))
				.isInstanceOf(SupportCaseConflictException.class);
		verify(auditService, never()).recordPlatformMutation(
				any(), any(), any(), any(), any(), any(), any(), any(), any());
	}

	private static User user(Long id, String email) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		User user = new User(email, "hashed", "user", null, UserStatus.ACTIVE, now, now);
		setId(user, id);
		return user;
	}

	private static PlatformAdmin platformAdmin(Long id, User user, PlatformAdminRole role) {
		PlatformAdmin platformAdmin = new PlatformAdmin(user, role, LocalDateTime.now(FIXED_CLOCK));
		setId(platformAdmin, id);
		return platformAdmin;
	}

	private static SupportCase supportCase(Long id, User reporter) {
		SupportCase supportCase = new SupportCase(
				reporter,
				SupportCaseType.INQUIRY,
				null,
				null,
				"Help",
				"Need assistance",
				LocalDateTime.now(FIXED_CLOCK));
		setId(supportCase, id);
		return supportCase;
	}

	private static void setId(Object entity, Long id) {
		try {
			Field field = entity.getClass().getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
