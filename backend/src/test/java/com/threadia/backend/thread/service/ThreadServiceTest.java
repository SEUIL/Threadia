package com.threadia.backend.thread.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.document.repository.DocumentRepository;
import com.threadia.backend.issue.domain.Issue;
import com.threadia.backend.issue.domain.IssueStatus;
import com.threadia.backend.issue.repository.IssueRepository;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;
import com.threadia.backend.projectchat.repository.MessageRepository;
import com.threadia.backend.thread.domain.Thread;
import com.threadia.backend.thread.domain.ThreadComment;
import com.threadia.backend.thread.domain.ThreadTargetType;
import com.threadia.backend.thread.dto.ThreadCommentCreateRequest;
import com.threadia.backend.thread.dto.ThreadGetOrCreateRequest;
import com.threadia.backend.thread.dto.ThreadTopicUpdateRequest;
import com.threadia.backend.thread.exception.ThreadValidationException;
import com.threadia.backend.thread.repository.ThreadCommentRepository;
import com.threadia.backend.thread.repository.ThreadRepository;

@ExtendWith(MockitoExtension.class)
class ThreadServiceTest {
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-16T10:00:00Z"), ZoneOffset.UTC);
	@Mock private AuthContextService auth; @Mock private CompanyMembershipService memberships; @Mock private ProjectRepository projects; @Mock private ProjectMemberRepository members;
	@Mock private IssueRepository issues; @Mock private DocumentRepository documents; @Mock private MessageRepository messages; @Mock private ThreadRepository threads; @Mock private ThreadCommentRepository comments;
	private ThreadService service;
	@BeforeEach void setUp() { service = new ThreadService(auth, memberships, projects, members, issues, documents, messages, threads, comments, CLOCK); }

	@Test
	void getOrCreateUsesDatabaseInsertIgnoreAndReturnsTheRepresentativeThread() {
		Fixture fixture = fixture(); stubCompanyAdmin(fixture); Issue issue = issue(fixture, 100L); Thread thread = thread(fixture, 10L, fixture.user());
		when(issues.findById(100L)).thenReturn(Optional.of(issue)); when(threads.findByTargetTypeAndTargetId(ThreadTargetType.ISSUE, 100L)).thenReturn(Optional.empty(), Optional.of(thread)); when(threads.insertIgnore(eq(20L), eq("ISSUE"), eq(100L), eq("논의"), eq(1L), any(), any())).thenReturn(1);
		var result = service.getOrCreate("token", 20L, new ThreadGetOrCreateRequest(ThreadTargetType.ISSUE, 100L, " 논의 "));
		assertThat(result.created()).isTrue(); assertThat(result.data().thread().id()).isEqualTo(10L); verify(threads).insertIgnore(eq(20L), eq("ISSUE"), eq(100L), eq("논의"), eq(1L), any(), any());
	}

	@Test
	void getOrCreateReturnsExistingThreadWithoutChangingItsTopic() {
		Fixture fixture = fixture(); stubCompanyAdmin(fixture); Issue issue = issue(fixture, 100L); Thread existing = thread(fixture, 10L, fixture.user());
		when(issues.findById(100L)).thenReturn(Optional.of(issue)); when(threads.findByTargetTypeAndTargetId(ThreadTargetType.ISSUE, 100L)).thenReturn(Optional.of(existing));
		assertThat(service.getOrCreate("token", 20L, new ThreadGetOrCreateRequest(ThreadTargetType.ISSUE, 100L, "새 주제")).created()).isFalse();
		org.mockito.Mockito.verifyNoInteractions(comments); org.mockito.Mockito.verify(threads, org.mockito.Mockito.never()).insertIgnore(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void rejectsTargetFromAnotherProjectBeforeCreatingThread() {
		Fixture fixture = fixture(); stubCompanyAdmin(fixture); Issue issue = issue(fixture, 100L); Project other = otherProject(fixture); setField(issue, "project", other); when(issues.findById(100L)).thenReturn(Optional.of(issue));
		assertThatThrownBy(() -> service.getOrCreate("token", 20L, new ThreadGetOrCreateRequest(ThreadTargetType.ISSUE, 100L, null))).isInstanceOf(ThreadValidationException.class);
		org.mockito.Mockito.verify(threads, org.mockito.Mockito.never()).insertIgnore(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void onlyAuthorOrAdministratorCanUpdateTopicAndCommentsAreTrimmedAppendOnly() {
		Fixture fixture = fixture(); stubCompanyAdminWithoutProject(fixture); Thread thread = thread(fixture, 10L, fixture.user()); when(threads.findByIdWithProject(10L)).thenReturn(Optional.of(thread));
		assertThat(service.updateTopic("token", 10L, new ThreadTopicUpdateRequest(" 새 주제 ")).thread().topic()).isEqualTo("새 주제");
		when(comments.save(any())).thenAnswer(invocation -> { ThreadComment comment = invocation.getArgument(0); setId(comment, 3L); return comment; });
		assertThat(service.createComment("token", 10L, new ThreadCommentCreateRequest(" 댓글 ")).comment().content()).isEqualTo("댓글");
		ArgumentCaptor<ThreadComment> captor = ArgumentCaptor.forClass(ThreadComment.class); verify(comments).save(captor.capture()); assertThat(captor.getValue().getCreatedAt()).isEqualTo(LocalDateTime.now(CLOCK));
	}

	@Test
	void regularMemberCannotUpdateAnotherAuthorsTopicButCanReadAndComment() {
		Fixture fixture = fixture(); stubActiveMember(fixture); User author = fixture.user(); setId(author, 2L); Thread thread = thread(fixture, 10L, author); when(threads.findByIdWithProject(10L)).thenReturn(Optional.of(thread));
		assertThatThrownBy(() -> service.updateTopic("token", 10L, new ThreadTopicUpdateRequest("변경"))).isInstanceOf(ForbiddenException.class);
		when(comments.findByThreadIdOrderByIdAsc(10L)).thenReturn(List.of()); assertThat(service.listComments("token", 10L).comments()).isEmpty();
	}

	private void stubCompanyAdmin(Fixture fixture) { when(auth.fromAccessToken("token")).thenReturn(fixture.context()); when(auth.requireActiveUser(fixture.context())).thenReturn(fixture.user()); when(projects.findById(20L)).thenReturn(Optional.of(fixture.project())); when(memberships.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true); }
	private void stubCompanyAdminWithoutProject(Fixture fixture) { when(auth.fromAccessToken("token")).thenReturn(fixture.context()); when(auth.requireActiveUser(fixture.context())).thenReturn(fixture.user()); when(memberships.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true); }
	private void stubActiveMember(Fixture fixture) { when(auth.fromAccessToken("token")).thenReturn(fixture.context()); when(auth.requireActiveUser(fixture.context())).thenReturn(fixture.user()); when(memberships.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false); when(members.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.of(new ProjectMember(fixture.project(), fixture.user(), ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK)))); }
	private static Fixture fixture() { LocalDateTime now = LocalDateTime.now(CLOCK); User user = new User("member@example.com", "hash", "member", null, UserStatus.ACTIVE, now, now); setId(user, 1L); Company company = new Company("Threadia", "member", "workspace", "IT", null, CompanyStatus.ACTIVE, user, now, now); setId(company, 10L); Project project = new Project(company, "MVP", "THD", null, ProjectStatus.ACTIVE, LocalDate.now(), LocalDate.now(), user, null, null, null, now, now); setId(project, 20L); return new Fixture(new AuthContext(1L, "member@example.com"), user, project); }
	private static Issue issue(Fixture fixture, Long id) { Issue issue = new Issue(fixture.project(), "THD-1", fixture.user(), "이슈", IssueStatus.BACKLOG, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK)); setId(issue, id); return issue; }
	private static Project otherProject(Fixture fixture) { Project project = new Project(fixture.project().getCompany(), "Other", "OTH", null, ProjectStatus.ACTIVE, LocalDate.now(), LocalDate.now(), fixture.user(), null, null, null, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK)); setId(project, 21L); return project; }
	private static Thread thread(Fixture fixture, Long id, User author) { Thread thread = new Thread(fixture.project(), ThreadTargetType.ISSUE, 100L, "논의", author, LocalDateTime.now(CLOCK)); setId(thread, id); return thread; }
	private static void setId(Object entity, Long id) { setField(entity, "id", id); }
	private static void setField(Object entity, String name, Object value) { try { Field field = entity.getClass().getDeclaredField(name); field.setAccessible(true); field.set(entity, value); } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); } }
	private record Fixture(AuthContext context, User user, Project project) { }
}
