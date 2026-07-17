package com.threadia.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.notification.domain.NotificationEvent;
import com.threadia.backend.notification.domain.NotificationRecipient;
import com.threadia.backend.notification.domain.NotificationTargetType;
import com.threadia.backend.notification.domain.NotificationType;
import com.threadia.backend.notification.dto.NotificationPageData;
import com.threadia.backend.notification.dto.NotificationReadAllRequest;
import com.threadia.backend.notification.exception.NotificationRecipientNotFoundException;
import com.threadia.backend.notification.exception.NotificationValidationException;
import com.threadia.backend.notification.repository.NotificationRecipientRepository;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-17T10:05:00Z"), ZoneOffset.UTC);
	private static final String SECRET = "test-only-notification-cursor-hmac-secret";

	@Mock private AuthContextService authContextService;
	@Mock private CompanyMembershipService companyMembershipService;
	@Mock private ProjectRepository projectRepository;
	@Mock private ProjectMemberRepository projectMemberRepository;
	@Mock private NotificationRecipientRepository recipientRepository;
	private NotificationService service;

	@BeforeEach
	void setUp() {
		service = new NotificationService(
				authContextService,
				companyMembershipService,
				projectRepository,
				projectMemberRepository,
				recipientRepository,
				new NotificationCursorCodec(SECRET),
				CLOCK);
	}

	@Test
	void listUsesOwnedDescendingKeysetAndBindsCursorToNormalizedFilters() {
		Fixture fixture = fixture();
		stubAuth(fixture);
		NotificationRecipient newest = recipient(fixture, 83L, 93L, "2026-07-17T10:00:02");
		NotificationRecipient older = recipient(fixture, 82L, 92L, "2026-07-17T10:00:01");
		NotificationRecipient extra = recipient(fixture, 81L, 91L, "2026-07-17T10:00:00");
		when(recipientRepository.findPage(
				eq(1L), eq(null), eq(null), eq(NotificationType.MENTIONED), eq(true),
				eq(null), eq(null), any())).thenReturn(List.of(newest, older, extra));

		NotificationPageData page = service.list("token", null, null, " mentioned ", true, null, 2);

		assertThat(page.notifications()).extracting("notificationRecipientId").containsExactly(83L, 82L);
		assertThat(page.notifications().get(0).target().type()).isEqualTo("THREAD");
		assertThat(page.hasNext()).isTrue();
		assertThat(page.nextCursor()).isNotBlank();

		when(recipientRepository.findPage(
				eq(1L), eq(null), eq(null), eq(NotificationType.MENTIONED), eq(true),
				eq(older.getCreatedAt()), eq(82L), any())).thenReturn(List.of());
		service.list("token", null, null, "MENTIONED", true, page.nextCursor(), 2);

		assertThatThrownBy(() -> service.list("token", null, null, "MENTIONED", false, page.nextCursor(), 2))
				.isInstanceOf(NotificationValidationException.class);
	}

	@Test
	void listUsesDefaultSizeAndRejectsInvalidSizeTypeAndCursor() {
		Fixture fixture = fixture();
		stubAuth(fixture);
		when(recipientRepository.findPage(
				eq(1L), eq(null), eq(null), eq(null), eq(false), eq(null), eq(null), any()))
				.thenReturn(List.of());

		assertThat(service.list("token", null, null, null, null, null, null).size()).isEqualTo(20);
		assertThatThrownBy(() -> service.list("token", null, null, "UNKNOWN", false, null, 20))
				.isInstanceOf(NotificationValidationException.class);
		assertThatThrownBy(() -> service.list("token", null, null, null, false, null, 0))
				.isInstanceOf(NotificationValidationException.class);
		assertThatThrownBy(() -> service.list("token", null, null, null, false, null, 101))
				.isInstanceOf(NotificationValidationException.class);
		assertThatThrownBy(() -> service.list("token", null, null, null, false, "invalid", 20))
				.isInstanceOf(NotificationValidationException.class);
	}

	@Test
	void companyAndProjectFiltersRequireActiveMembershipAndCompanyConsistency() {
		Fixture fixture = fixture();
		stubAuth(fixture);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(fixture.project()));
		when(companyMembershipService.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN))
				.thenReturn(false);
		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(20L, 1L, ProjectMemberStatus.ACTIVE))
				.thenReturn(true);
		when(recipientRepository.countUnread(1L, 10L, 20L, NotificationType.ISSUE_ASSIGNED)).thenReturn(4L);

		assertThat(service.unreadCount("token", 10L, 20L, "issue_assigned").unreadCount()).isEqualTo(4L);
		verify(companyMembershipService).requireActiveMember(10L, fixture.context());

		assertThatThrownBy(() -> service.unreadCount("token", 11L, 20L, null))
				.isInstanceOf(NotificationValidationException.class);
	}

	@Test
	void projectFilterAllowsActiveCompanyAdminAndRejectsMissingProjectMember() {
		Fixture fixture = fixture();
		stubAuth(fixture);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(fixture.project()));
		when(companyMembershipService.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN))
				.thenReturn(true);
		when(recipientRepository.countUnread(1L, null, 20L, null)).thenReturn(2L);

		assertThat(service.unreadCount("token", null, 20L, null).unreadCount()).isEqualTo(2L);
		verify(companyMembershipService, never()).requireActiveMember(10L, fixture.context());

		when(companyMembershipService.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN))
				.thenReturn(false);
		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(20L, 1L, ProjectMemberStatus.ACTIVE))
				.thenReturn(false);
		assertThatThrownBy(() -> service.unreadCount("token", null, 20L, null))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void markReadIsOwnershipSafeAndPreservesFirstReadAt() {
		Fixture fixture = fixture();
		stubAuth(fixture);
		NotificationRecipient recipient = recipient(fixture, 80L, 90L, "2026-07-17T10:00:00");
		when(recipientRepository.findOwnedByIdForUpdate(80L, 1L)).thenReturn(Optional.of(recipient));

		assertThat(service.markRead("token", 80L).readAt()).isEqualTo("2026-07-17T10:05:00Z");
		assertThat(service.markRead("token", 80L).readAt()).isEqualTo("2026-07-17T10:05:00Z");
		assertThat(recipient.getReadAt()).isEqualTo(LocalDateTime.parse("2026-07-17T10:05:00"));

		when(recipientRepository.findOwnedByIdForUpdate(81L, 1L)).thenReturn(Optional.empty());
		when(recipientRepository.existsById(81L)).thenReturn(true);
		assertThatThrownBy(() -> service.markRead("token", 81L)).isInstanceOf(ForbiddenException.class);

		when(recipientRepository.findOwnedByIdForUpdate(82L, 1L)).thenReturn(Optional.empty());
		when(recipientRepository.existsById(82L)).thenReturn(false);
		assertThatThrownBy(() -> service.markRead("token", 82L))
				.isInstanceOf(NotificationRecipientNotFoundException.class);
	}

	@Test
	void markAllReadUsesCurrentUserOptionalFiltersAndOnlyServerUtcTime() {
		Fixture fixture = fixture();
		stubAuth(fixture);
		when(recipientRepository.markAllRead(
				1L, 10L, null, "ROLE_CHANGED", LocalDateTime.parse("2026-07-17T10:05:00")))
				.thenReturn(5);

		assertThat(service.markAllRead(
				"token", new NotificationReadAllRequest(" role_changed ", 10L, null)).updatedCount()).isEqualTo(5);
		verify(companyMembershipService).requireActiveMember(10L, fixture.context());
	}

	private void stubAuth(Fixture fixture) {
		when(authContextService.fromAccessToken("token")).thenReturn(fixture.context());
		when(authContextService.requireActiveUser(fixture.context())).thenReturn(fixture.user());
	}

	private static Fixture fixture() {
		LocalDateTime now = LocalDateTime.parse("2026-07-17T09:00:00");
		User user = new User("member@example.com", "hash", "member", null, UserStatus.ACTIVE, now, now);
		setId(user, 1L);
		Company company = new Company(
				"Threadia", "member", "workspace", "IT", null, CompanyStatus.ACTIVE, user, now, now);
		setId(company, 10L);
		Project project = new Project(
				company, "MVP", "THD", null, ProjectStatus.ACTIVE, null, null, user,
				null, null, null, now, now);
		setId(project, 20L);
		return new Fixture(new AuthContext(1L, "member@example.com"), user, company, project);
	}

	private static NotificationRecipient recipient(Fixture fixture, Long recipientId, Long eventId, String createdAt) {
		LocalDateTime timestamp = LocalDateTime.parse(createdAt);
		NotificationEvent event = new NotificationEvent(
				null,
				fixture.company(),
				fixture.project(),
				fixture.user(),
				NotificationType.MENTIONED,
				NotificationTargetType.THREAD,
				6000L,
				"새 멘션이 있습니다.",
				"본문",
				null,
				timestamp);
		setId(event, eventId);
		NotificationRecipient recipient = new NotificationRecipient(event, fixture.user(), null, timestamp);
		setId(recipient, recipientId);
		return recipient;
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

	private record Fixture(AuthContext context, User user, Company company, Project project) {
	}
}
