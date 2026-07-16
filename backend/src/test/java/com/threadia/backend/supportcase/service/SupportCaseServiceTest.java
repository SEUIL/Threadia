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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.platform.audit.AdminAction;
import com.threadia.backend.platform.audit.AdminActionAuditService;
import com.threadia.backend.platform.audit.AuditTargetType;
import com.threadia.backend.supportcase.domain.SupportCase;
import com.threadia.backend.supportcase.domain.SupportCaseTargetType;
import com.threadia.backend.supportcase.domain.SupportCaseType;
import com.threadia.backend.supportcase.dto.SupportCaseCreateRequest;
import com.threadia.backend.supportcase.dto.SupportCaseDetailData;
import com.threadia.backend.supportcase.exception.SupportCaseNotFoundException;
import com.threadia.backend.supportcase.exception.SupportCaseValidationException;
import com.threadia.backend.supportcase.repository.SupportCasePublicReplyRepository;
import com.threadia.backend.supportcase.repository.SupportCaseRepository;

@ExtendWith(MockitoExtension.class)
class SupportCaseServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-15T02:00:00Z"), ZoneOffset.UTC);

	@Mock
	private AuthContextService authContextService;

	@Mock
	private SupportCaseRepository supportCaseRepository;

	@Mock
	private SupportCasePublicReplyRepository publicReplyRepository;

	@Mock
	private AdminActionAuditService auditService;

	private SupportCaseService service;
	private User reporter;
	private AuthContext authContext;

	@BeforeEach
	void setUp() {
		service = new SupportCaseService(
				authContextService,
				supportCaseRepository,
				publicReplyRepository,
				auditService,
				FIXED_CLOCK);
		reporter = user(1L, "reporter@example.com");
		authContext = new AuthContext(1L, reporter.getEmail());
	}

	@Test
	void createsCaseAndPersistsIndependentAuditReason() {
		when(authContextService.fromAccessToken("token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(reporter);
		when(supportCaseRepository.save(any())).thenAnswer(invocation -> {
			SupportCase supportCase = invocation.getArgument(0);
			setId(supportCase, 10L);
			return supportCase;
		});
		UUID targetId = UUID.randomUUID();

		SupportCaseDetailData data = service.create("token", new SupportCaseCreateRequest(
				SupportCaseType.REPORT,
				"Abuse report",
				"reported content",
				SupportCaseTargetType.CONTENT,
				targetId,
				"Initial report intake"));

		assertThat(data.supportCase().id()).isEqualTo(10L);
		assertThat(data.supportCase().targetId()).isEqualTo(targetId);
		assertThat(data.publicReplies()).isEmpty();
		verify(auditService).recordUserMutation(
				any(SupportCase.class),
				eq(reporter),
				eq(AdminAction.SUPPORT_CASE_CREATED),
				eq(AuditTargetType.SUPPORT_CASE),
				eq(10L),
				eq("Initial report intake"),
				eq(null),
				any());
	}

	@Test
	void listsOnlyCasesOwnedByAuthenticatedReporter() {
		when(authContextService.fromAccessToken("token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(reporter);
		when(supportCaseRepository.findAllByReporter_IdOrderByCreatedAtDescIdDesc(1L))
				.thenReturn(List.of(supportCase(10L, reporter)));

		assertThat(service.listOwn("token").supportCases())
				.extracting(response -> response.reporterUserId())
				.containsExactly(1L);
		verify(supportCaseRepository).findAllByReporter_IdOrderByCreatedAtDescIdDesc(1L);
		verify(supportCaseRepository, never()).findAllByOrderByCreatedAtDescIdDesc();
	}

	@Test
	void hidesAnotherReportersCaseAsNotFound() {
		when(authContextService.fromAccessToken("token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(reporter);
		when(supportCaseRepository.findByIdAndReporter_Id(99L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getOwn("token", 99L))
				.isInstanceOf(SupportCaseNotFoundException.class);
		verifyNoInteractions(publicReplyRepository);
	}

	@Test
	void regularDetailContractHasNoInternalNoteComponent() {
		assertThat(SupportCaseDetailData.class.getRecordComponents())
				.extracting(component -> component.getName())
				.containsExactly("supportCase", "publicReplies")
				.doesNotContain("internalNotes");
	}

	@Test
	void rejectsShortReasonBeforeAnyMutation() {
		when(authContextService.fromAccessToken("token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(reporter);
		SupportCaseCreateRequest request = new SupportCaseCreateRequest(
				SupportCaseType.INQUIRY,
				"Help",
				"Need assistance",
				null,
				null,
				" x ");

		assertThatThrownBy(() -> service.create("token", request))
				.isInstanceOf(SupportCaseValidationException.class)
				.hasMessageContaining("audit reason");
		verifyNoInteractions(supportCaseRepository, auditService);
	}

	@Test
	void trimsReasonBeforeAcceptingExactly500Characters() {
		when(authContextService.fromAccessToken("token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(reporter);
		when(supportCaseRepository.save(any())).thenAnswer(invocation -> {
			SupportCase supportCase = invocation.getArgument(0);
			setId(supportCase, 10L);
			return supportCase;
		});
		String normalizedReason = "r".repeat(500);

		service.create("token", new SupportCaseCreateRequest(
				SupportCaseType.INQUIRY,
				"Help",
				"Need assistance",
				null,
				null,
				"  " + normalizedReason + "  "));

		verify(auditService).recordUserMutation(
				any(SupportCase.class),
				eq(reporter),
				eq(AdminAction.SUPPORT_CASE_CREATED),
				eq(AuditTargetType.SUPPORT_CASE),
				eq(10L),
				eq(normalizedReason),
				eq(null),
				any());
	}

	@Test
	void rejectsReasonLongerThan500CharactersAfterTrim() {
		when(authContextService.fromAccessToken("token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(reporter);

		assertThatThrownBy(() -> service.create("token", new SupportCaseCreateRequest(
				SupportCaseType.INQUIRY,
				"Help",
				"Need assistance",
				null,
				null,
				"  " + "r".repeat(501) + "  ")))
				.isInstanceOf(SupportCaseValidationException.class)
				.hasMessageContaining("audit reason");
		verifyNoInteractions(supportCaseRepository, auditService);
	}

	@Test
	void rejectsInquiryWithReportTarget() {
		when(authContextService.fromAccessToken("token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(reporter);
		SupportCaseCreateRequest request = new SupportCaseCreateRequest(
				SupportCaseType.INQUIRY,
				"Help",
				"Need assistance",
				SupportCaseTargetType.USER,
				null,
				"Create inquiry");

		assertThatThrownBy(() -> service.create("token", request))
				.isInstanceOf(SupportCaseValidationException.class)
				.hasMessageContaining("INQUIRY");
		verifyNoInteractions(supportCaseRepository, auditService);
	}

	private static SupportCase supportCase(Long id, User user) {
		SupportCase supportCase = new SupportCase(
				user,
				SupportCaseType.INQUIRY,
				null,
				null,
				"Help",
				"Need assistance",
				LocalDateTime.now(FIXED_CLOCK));
		setId(supportCase, id);
		return supportCase;
	}

	private static User user(Long id, String email) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		User user = new User(email, "hashed", "reporter", null, UserStatus.ACTIVE, now, now);
		setId(user, id);
		return user;
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
