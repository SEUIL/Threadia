package com.threadia.backend.projectchat.service;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;
import com.threadia.backend.projectchat.domain.Message;
import com.threadia.backend.projectchat.domain.ProjectChatMemberState;
import com.threadia.backend.projectchat.domain.ProjectChatMemberStateId;
import com.threadia.backend.projectchat.dto.MarkReadRequest;
import com.threadia.backend.projectchat.dto.MessageCreateRequest;
import com.threadia.backend.projectchat.dto.MessagesData;
import com.threadia.backend.projectchat.exception.ProjectChatConflictException;
import com.threadia.backend.projectchat.exception.ProjectChatValidationException;
import com.threadia.backend.projectchat.repository.MessageRepository;
import com.threadia.backend.projectchat.repository.ProjectChatMemberStateRepository;

@ExtendWith(MockitoExtension.class)
class ProjectChatServiceTest {
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-15T10:00:00Z"), ZoneOffset.UTC);
	@Mock private AuthContextService auth;
	@Mock private CompanyMembershipService companyMembership;
	@Mock private ProjectRepository projects;
	@Mock private ProjectMemberRepository members;
	@Mock private MessageRepository messages;
	@Mock private ProjectChatMemberStateRepository states;
	private ProjectChatService service;

	@BeforeEach
	void setUp() { service = new ProjectChatService(auth, companyMembership, projects, members, messages, states, new ProjectChatCursorCodec("test-only-project-chat-cursor-hmac-secret"), CLOCK); }

	@Test
	void listUsesDescendingKeysetAndReturnsOpaqueProjectScopedCursor() {
		Fixture fixture = fixture(); stubAccess(fixture);
		Message newest = message(fixture, 9L, "2026-07-15T09:00:02");
		Message older = message(fixture, 8L, "2026-07-15T09:00:01");
		Message extra = message(fixture, 7L, "2026-07-15T09:00:00");
		when(messages.findPageByProjectId(eq(20L), eq(null), eq(null), any())).thenReturn(List.of(newest, older, extra));

		MessagesData result = service.list("token", 20L, null, 2);

		assertThat(result.messages()).extracting("id").containsExactly(9L, 8L);
		assertThat(result.nextCursor()).isNotBlank();
		service.list("token", 20L, result.nextCursor(), 2);
		verify(messages).findPageByProjectId(eq(20L), eq(older.getCreatedAt()), eq(8L), any());
		when(projects.findById(21L)).thenReturn(Optional.of(fixture.project()));
		assertThatThrownBy(() -> service.list("token", 21L, result.nextCursor(), 2)).isInstanceOf(ProjectChatValidationException.class);
	}

	@Test
	void listRejectsTamperedSignedCursorAndLimitOutsideBoundaries() {
		Fixture fixture = fixture(); stubAccess(fixture);
		Message message = message(fixture, 9L, "2026-07-15T09:00:02");
		when(messages.findPageByProjectId(eq(20L), eq(null), eq(null), any())).thenReturn(List.of(message, message));
		String cursor = service.list("token", 20L, null, 1).nextCursor();
		String tampered = cursor.substring(0, cursor.length() - 1) + (cursor.endsWith("A") ? "B" : "A");
		assertThatThrownBy(() -> service.list("token", 20L, tampered, 1)).isInstanceOf(ProjectChatValidationException.class);
		assertThatThrownBy(() -> service.list("token", 20L, null, 0)).isInstanceOf(ProjectChatValidationException.class);
		assertThatThrownBy(() -> service.list("token", 20L, null, 101)).isInstanceOf(ProjectChatValidationException.class);
	}

	@Test
	void createTrimsContentAndRejectsThreadUntilThreadWbs() {
		Fixture fixture = fixture(); stubAccess(fixture);
		when(messages.save(any())).thenAnswer(invocation -> { Message message = invocation.getArgument(0); setId(message, 101L); return message; });

		assertThat(service.create("token", 20L, new MessageCreateRequest("  내용  ", null)).message().content()).isEqualTo("내용");
		assertThatThrownBy(() -> service.create("token", 20L, new MessageCreateRequest("내용", 77L)))
				.isInstanceOf(ProjectChatConflictException.class);
	}

	@Test
	void markReadCreatesStateWithServerTimeAndNeverMovesPointerBackward() {
		Fixture fixture = fixture(); stubAccess(fixture);
		Message old = message(fixture, 10L, "2026-07-15T09:00:00");
		Message newest = message(fixture, 11L, "2026-07-15T09:01:00");
		when(messages.findById(11L)).thenReturn(Optional.of(newest));
		ProjectChatMemberState created = new ProjectChatMemberState(fixture.project(), fixture.user(), null, LocalDateTime.now(CLOCK));
		when(states.findByIdForUpdate(new ProjectChatMemberStateId(20L, 1L))).thenReturn(Optional.of(created));
		when(messages.countUnreadAfterPointer(20L, 1L, newest.getCreatedAt(), 11L)).thenReturn(3L);

		assertThat(service.markRead("token", 20L, new MarkReadRequest(11L)).unreadCount()).isEqualTo(3L);
		verify(states).insertIfAbsent(eq(20L), eq(1L), any());
		ArgumentCaptor<ProjectChatMemberState> stateCaptor = ArgumentCaptor.forClass(ProjectChatMemberState.class);
		verify(states).save(stateCaptor.capture());
		assertThat(stateCaptor.getValue().getLastReadAt()).isEqualTo(LocalDateTime.now(CLOCK));

		ProjectChatMemberState existing = new ProjectChatMemberState(fixture.project(), fixture.user(), newest, LocalDateTime.parse("2026-07-15T09:02:00"));
		when(messages.findById(10L)).thenReturn(Optional.of(old));
		when(states.findByIdForUpdate(new ProjectChatMemberStateId(20L, 1L))).thenReturn(Optional.of(existing));
		when(messages.countUnreadAfterPointer(20L, 1L, newest.getCreatedAt(), 11L)).thenReturn(3L);
		service.markRead("token", 20L, new MarkReadRequest(10L));
		assertThat(existing.getLastReadMessage()).isSameAs(newest);
		verify(states, never()).save(existing);
	}

	@Test
	void markReadRejectsMessageFromAnotherProjectAndUnreadExcludesOwnMessages() {
		Fixture fixture = fixture(); stubAccess(fixture);
		Project otherProject = fixture.project(); setId(otherProject, 21L);
		Message other = new Message(otherProject, fixture.user(), "다른 프로젝트", LocalDateTime.now(CLOCK)); setId(other, 99L);
		when(messages.findById(99L)).thenReturn(Optional.of(other));
		assertThatThrownBy(() -> service.markRead("token", 20L, new MarkReadRequest(99L))).isInstanceOf(ForbiddenException.class);

		when(states.findById(new ProjectChatMemberStateId(20L, 1L))).thenReturn(Optional.empty());
		when(messages.countUnreadWithoutPointer(20L, 1L)).thenReturn(4L);
		assertThat(service.unreadCount("token", 20L).unreadCount()).isEqualTo(4L);
		verify(messages).countUnreadWithoutPointer(20L, 1L);
	}

	@Test
	void listAllowsActiveRegularMemberButRejectsInactiveOrMissingProjectMember() {
		Fixture fixture = fixture();
		when(auth.fromAccessToken("token")).thenReturn(fixture.context());
		when(auth.requireActiveUser(fixture.context())).thenReturn(fixture.user());
		when(projects.findById(20L)).thenReturn(Optional.of(fixture.project()));
		when(companyMembership.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(members.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.of(new ProjectMember(
				fixture.project(), fixture.user(), ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE,
				LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK))));
		when(messages.findPageByProjectId(eq(20L), eq(null), eq(null), any())).thenReturn(List.of());
		assertThat(service.list("token", 20L, null, 50).messages()).isEmpty();
		verify(companyMembership).requireActiveMember(10L, fixture.context());

		when(members.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.list("token", 20L, null, 50)).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void detailRejectsUserWithoutProjectAccess() {
		Fixture fixture = fixture();
		Message message = message(fixture, 9L, "2026-07-15T09:00:02");
		when(auth.fromAccessToken("token")).thenReturn(fixture.context());
		when(auth.requireActiveUser(fixture.context())).thenReturn(fixture.user());
		when(messages.findById(9L)).thenReturn(Optional.of(message));
		when(companyMembership.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(members.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.detail("token", 9L)).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void activeProjectMemberCanCreateReadDetailAndReadUnreadCount() {
		Fixture fixture = fixture();
		stubActiveProjectMemberAccess(fixture);
		Message message = message(fixture, 9L, "2026-07-15T09:00:02");
		when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(messages.findById(9L)).thenReturn(Optional.of(message));
		ProjectChatMemberState state = new ProjectChatMemberState(fixture.project(), fixture.user(), null, LocalDateTime.now(CLOCK));
		when(states.findByIdForUpdate(new ProjectChatMemberStateId(20L, 1L))).thenReturn(Optional.of(state));
		when(messages.countUnreadAfterPointer(20L, 1L, message.getCreatedAt(), 9L)).thenReturn(0L);
		when(states.findById(new ProjectChatMemberStateId(20L, 1L))).thenReturn(Optional.of(state));

		service.create("token", 20L, new MessageCreateRequest("member message", null));
		assertThat(service.detail("token", 9L).message().id()).isEqualTo(9L);
		assertThat(service.markRead("token", 20L, new MarkReadRequest(9L)).lastReadMessageId()).isEqualTo(9L);
		assertThat(service.unreadCount("token", 20L).unreadCount()).isZero();
	}

	private void stubAccess(Fixture fixture) {
		when(auth.fromAccessToken("token")).thenReturn(fixture.context());
		when(auth.requireActiveUser(fixture.context())).thenReturn(fixture.user());
		when(projects.findById(20L)).thenReturn(Optional.of(fixture.project()));
		when(companyMembership.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
	}

	private void stubActiveProjectMemberAccess(Fixture fixture) {
		when(auth.fromAccessToken("token")).thenReturn(fixture.context());
		when(auth.requireActiveUser(fixture.context())).thenReturn(fixture.user());
		when(projects.findById(20L)).thenReturn(Optional.of(fixture.project()));
		when(companyMembership.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(members.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.of(new ProjectMember(
				fixture.project(), fixture.user(), ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE,
				LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK))));
	}

	private static Fixture fixture() {
		LocalDateTime now = LocalDateTime.now(CLOCK);
		User user = new User("member@example.com", "hash", "member", null, UserStatus.ACTIVE, now, now); setId(user, 1L);
		Company company = new Company("Threadia", "member", "workspace", "IT", null, "ACTIVE", user, now, now); setId(company, 10L);
		Project project = new Project(company, "MVP", "THD", null, ProjectStatus.ACTIVE, LocalDate.now(), LocalDate.now(), user, null, null, null, now, now); setId(project, 20L);
		return new Fixture(new AuthContext(1L, "member@example.com"), user, project);
	}

	private static Message message(Fixture fixture, Long id, String createdAt) {
		Message message = new Message(fixture.project(), fixture.user(), "내용", LocalDateTime.parse(createdAt)); setId(message, id); return message;
	}
	private static void setId(Object entity, Long id) { try { Field field = entity.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(entity, id); } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); } }
	private record Fixture(AuthContext context, User user, Project project) { }
}
