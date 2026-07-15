package com.threadia.backend.issue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.issue.domain.Issue;
import com.threadia.backend.issue.domain.IssuePriority;
import com.threadia.backend.issue.domain.IssueStatus;
import com.threadia.backend.issue.domain.IssueStatusHistory;
import com.threadia.backend.issue.dto.IssueCreateRequest;
import com.threadia.backend.issue.dto.IssueData;
import com.threadia.backend.issue.dto.IssueStatusUpdateRequest;
import com.threadia.backend.issue.dto.IssueUpdateRequest;
import com.threadia.backend.issue.dto.IssuesData;
import com.threadia.backend.issue.exception.IssueNotFoundException;
import com.threadia.backend.issue.repository.IssueRepository;
import com.threadia.backend.issue.repository.IssueStatusHistoryRepository;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.exception.ProjectValidationException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;
import com.threadia.backend.sprint.domain.Sprint;
import com.threadia.backend.sprint.domain.SprintStatus;
import com.threadia.backend.sprint.repository.SprintRepository;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-13T00:00:00Z"), ZoneOffset.UTC);

	@Mock
	private AuthContextService authContextService;
	@Mock
	private CompanyMembershipService companyMembershipService;
	@Mock
	private ProjectRepository projectRepository;
	@Mock
	private ProjectMemberRepository projectMemberRepository;
	@Mock
	private IssueRepository issueRepository;
	@Mock
	private IssueStatusHistoryRepository historyRepository;
	@Mock
	private SprintRepository sprintRepository;

	private IssueService issueService;

	@BeforeEach
	void setUp() {
		issueService = new IssueService(
				authContextService,
				companyMembershipService,
				projectRepository,
				projectMemberRepository,
				issueRepository,
				historyRepository,
				null,
				sprintRepository,
				FIXED_CLOCK);
	}

	@Test
	void listReturnsOnlyRepositoryActiveIssuesInDisplayOrderForCompanyAdmin() {
		Fixtures fixtures = fixtures();
		Issue backlog = issue(100L, fixtures.project(), fixtures.user(), "THD-1", "첫 이슈", IssueStatus.BACKLOG);
		Issue done = issue(101L, fixtures.project(), fixtures.user(), "THD-2", "완료 이슈", IssueStatus.DONE);
		stubAuth(fixtures);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(fixtures.project()));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
		when(issueRepository.findActiveByProjectIdInDisplayOrder(20L)).thenReturn(List.of(backlog, done));

		IssuesData data = issueService.list("token", 20L);

		assertThat(data.issues()).extracting("issueKey").containsExactly("THD-1", "THD-2");
		verify(projectMemberRepository, never()).findByProject_IdAndUser_Id(any(), any());
	}

	@Test
	void listAllowsActiveRegularProjectMember() {
		Fixtures fixtures = fixtures();
		stubAuth(fixtures);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(fixtures.project()));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L))
				.thenReturn(Optional.of(projectMember(fixtures, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)));
		when(issueRepository.findActiveByProjectIdInDisplayOrder(20L)).thenReturn(List.of());

		issueService.list("token", 20L);

		verify(companyMembershipService).requireActiveMember(10L, fixtures.authContext());
	}

	@Test
	void listRejectsInvalidTokenBeforeValidation() {
		when(authContextService.fromAccessToken("bad-token")).thenThrow(new AuthenticationFailedException("토큰이 유효하지 않습니다."));

		assertThatThrownBy(() -> issueService.list("bad-token", 20L, null, 30L, null, true, false))
				.isInstanceOf(AuthenticationFailedException.class)
				.hasMessage("토큰이 유효하지 않습니다.");

		verifyNoInteractions(projectRepository, companyMembershipService, projectMemberRepository, issueRepository);
	}

	@Test
	void listRejectsUnauthorizedActorBeforeValidation() {
		Fixtures fixtures = fixtures();
		stubAuth(fixtures);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(fixtures.project()));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> issueService.list("token", 20L, null, 30L, null, true, false))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("프로젝트 접근 권한이 없습니다.");

		InOrder order = inOrder(authContextService, projectRepository, companyMembershipService, projectMemberRepository);
		order.verify(authContextService).fromAccessToken("token");
		order.verify(authContextService).requireActiveUser(fixtures.authContext());
		order.verify(projectRepository).findById(20L);
		order.verify(companyMembershipService).hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN);
		order.verify(companyMembershipService).requireActiveMember(10L, fixtures.authContext());
		order.verify(projectMemberRepository).findByProject_IdAndUser_Id(20L, 1L);
		verifyNoInteractions(issueRepository);
	}

	@Test
	void listUsesUnassignedOnlyRepositoryFilter() {
		Fixtures fixtures = fixtures();
		stubAuth(fixtures);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(fixtures.project()));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
		when(issueRepository.findByFiltersInDisplayOrder(20L, null, null, null, true, false)).thenReturn(List.of());

		issueService.list("token", 20L, null, null, null, true, false);

		verify(issueRepository).findByFiltersInDisplayOrder(20L, null, null, null, true, false);
	}

	@Test
	void listRejectsUnassignedOnlyCombinedWithSprintIdAfterAuthAndAccess() {
		Fixtures fixtures = fixtures();
		stubAuth(fixtures);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(fixtures.project()));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		assertThatThrownBy(() -> issueService.list("token", 20L, null, 30L, null, true, false))
				.isInstanceOf(ProjectValidationException.class)
				.hasMessage("unassignedOnly와 sprintId는 함께 사용할 수 없습니다.");

		InOrder order = inOrder(authContextService, projectRepository, companyMembershipService);
		order.verify(authContextService).fromAccessToken("token");
		order.verify(authContextService).requireActiveUser(fixtures.authContext());
		order.verify(projectRepository).findById(20L);
		order.verify(companyMembershipService).hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN);
		verifyNoInteractions(issueRepository);
	}

	@Test
	void createLocksProjectThenUsesMaximumSequenceAndCreatesInitialHistory() {
		Fixtures fixtures = fixtures();
		stubAuth(fixtures);
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(fixtures.project()));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
		when(issueRepository.findByProjectIdForSequenceUpdate(20L)).thenReturn(List.of(
				issue(99L, fixtures.project(), fixtures.user(), "THD-7", "기존 이슈", IssueStatus.DONE)));
		when(issueRepository.save(any())).thenAnswer(invocation -> {
			Issue issue = invocation.getArgument(0);
			setId(issue, 100L);
			return issue;
		});

		IssueData data = issueService.create("token", 20L, new IssueCreateRequest("  새 이슈  "));

		InOrder order = inOrder(authContextService, projectRepository, issueRepository, historyRepository);
		order.verify(authContextService).fromAccessToken("token");
		order.verify(authContextService).requireActiveUser(fixtures.authContext());
		order.verify(projectRepository).findByIdForUpdate(20L);
		order.verify(issueRepository).findByProjectIdForSequenceUpdate(20L);
		ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
		order.verify(issueRepository).save(issueCaptor.capture());
		ArgumentCaptor<IssueStatusHistory> historyCaptor = ArgumentCaptor.forClass(IssueStatusHistory.class);
		order.verify(historyRepository).save(historyCaptor.capture());
		assertThat(issueCaptor.getValue().getIssueKey()).isEqualTo("THD-8");
		assertThat(issueCaptor.getValue().getTitle()).isEqualTo("새 이슈");
		assertThat(issueCaptor.getValue().getStatus()).isEqualTo(IssueStatus.BACKLOG);
		assertThat(historyCaptor.getValue().getIssue()).isSameAs(issueCaptor.getValue());
		assertThat(historyCaptor.getValue().getFromStatus()).isNull();
		assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(IssueStatus.BACKLOG);
		assertThat(historyCaptor.getValue().getChangedBy()).isSameAs(fixtures.user());
		assertThat(data.issue().issueKey()).isEqualTo("THD-8");
	}

	@Test
	void createSelectsHighestNumericSuffixAndSkipsMalformedKeys() {
		Fixtures fixtures = fixtures();
		stubAuth(fixtures);
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(fixtures.project()));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
		when(issueRepository.findByProjectIdForSequenceUpdate(20L)).thenReturn(List.of(
				issue(91L, fixtures.project(), fixtures.user(), "THD-9", "기존 이슈", IssueStatus.DONE),
				issue(92L, fixtures.project(), fixtures.user(), "THD-invalid", "잘못된 키", IssueStatus.DONE),
				issue(93L, fixtures.project(), fixtures.user(), "OTHER-999", "다른 prefix", IssueStatus.DONE),
				issue(94L, fixtures.project(), fixtures.user(), "THD-12-extra", "복합 suffix", IssueStatus.DONE),
				issue(95L, fixtures.project(), fixtures.user(), "THD-999999999999999999999", "범위 초과", IssueStatus.DONE),
				issue(96L, fixtures.project(), fixtures.user(), "THD-12", "최대 sequence", IssueStatus.DONE)));
		when(issueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		IssueData data = issueService.create("token", 20L, new IssueCreateRequest("새 이슈"));

		assertThat(data.issue().issueKey()).isEqualTo("THD-13");
	}

	@Test
	void createForRegularMemberAlwaysUsesBacklog() {
		Fixtures fixtures = fixtures();
		stubAuth(fixtures);
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(fixtures.project()));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L))
				.thenReturn(Optional.of(projectMember(fixtures, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)));
		when(issueRepository.findByProjectIdForSequenceUpdate(20L)).thenReturn(List.of());
		when(issueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		IssueData data = issueService.create("token", 20L, new IssueCreateRequest("멤버 생성 이슈"));

		assertThat(data.issue().status()).isEqualTo("BACKLOG");
	}

	@Test
	void createPersistsDescriptionForRegularMember() {
		Fixtures fixtures = fixtures();
		stubAuth(fixtures);
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(fixtures.project()));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L))
				.thenReturn(Optional.of(projectMember(fixtures, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)));
		when(issueRepository.findByProjectIdForSequenceUpdate(20L)).thenReturn(List.of());
		when(issueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		IssueData data = issueService.create("token", 20L, new IssueCreateRequest("멤버 생성 이슈", "설명", null, null, null, null));

		assertThat(data.issue().description()).isEqualTo("설명");
	}

	@Test
	void createRejectsManagerOnlyFieldsForRegularMember() {
		Fixtures fixtures = fixtures();
		stubAuth(fixtures);
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(fixtures.project()));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L))
				.thenReturn(Optional.of(projectMember(fixtures, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)));

		assertThatThrownBy(() -> issueService.create("token", 20L, new IssueCreateRequest("멤버 생성 이슈", "설명", IssuePriority.HIGH, null, null, null)))
				.isInstanceOf(ForbiddenException.class);
		verify(issueRepository, never()).findByProjectIdForSequenceUpdate(any());
	}

	@Test
	void updateRejectsTitleLongerThanTwoHundredCharacters() {
		Fixtures fixtures = fixtures();
		Issue issue = issue(100L, fixtures.project(), fixtures.user(), "THD-1", "첫 이슈", IssueStatus.BACKLOG);
		stubAuth(fixtures);
		when(issueRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(issue));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		assertThatThrownBy(() -> issueService.update("token", 100L, new IssueUpdateRequest("x".repeat(201), null, null, null, null, null), Set.of("title")))
				.isInstanceOf(com.threadia.backend.project.exception.ProjectValidationException.class);
		assertThat(issue.getTitle()).isEqualTo("첫 이슈");
	}

	@Test
	void updateDetachesSprintWhenSprintIdIsExplicitlyNull() {
		Fixtures fixtures = fixtures();
		Issue issue = issue(100L, fixtures.project(), fixtures.user(), "THD-1", "첫 이슈", IssueStatus.BACKLOG);
		Sprint sprint = new Sprint(fixtures.project(), "현재 Sprint", null, null, null, LocalDateTime.now(FIXED_CLOCK));
		issue.setSprint(sprint);
		stubAuth(fixtures);
		when(issueRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(issue));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		issueService.update("token", 100L, new IssueUpdateRequest(null, null, null, null, null, null), Set.of("sprintId"));

		assertThat(issue.getSprint()).isNull();
	}

	@Test
	void updateLeavesSprintUnchangedWhenSprintIdIsOmitted() {
		Fixtures fixtures = fixtures();
		Issue issue = issue(100L, fixtures.project(), fixtures.user(), "THD-1", "첫 이슈", IssueStatus.BACKLOG);
		Sprint sprint = new Sprint(fixtures.project(), "현재 Sprint", null, null, null, LocalDateTime.now(FIXED_CLOCK));
		issue.setSprint(sprint);
		stubAuth(fixtures);
		when(issueRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(issue));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		issueService.update("token", 100L, new IssueUpdateRequest("수정 제목", null, null, null, null, null), Set.of("title"));

		assertThat(issue.getSprint()).isSameAs(sprint);
	}

	@Test
	void updateRejectsNonNullSprintIdWithoutMutatingSprintRelation() {
		Fixtures fixtures = fixtures();
		Issue issue = issue(100L, fixtures.project(), fixtures.user(), "THD-1", "첫 이슈", IssueStatus.BACKLOG);
		Sprint sprint = new Sprint(fixtures.project(), "현재 Sprint", null, null, null, LocalDateTime.now(FIXED_CLOCK));
		issue.setSprint(sprint);
		stubAuth(fixtures);
		when(issueRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(issue));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		assertThatThrownBy(() -> issueService.update("token", 100L, new IssueUpdateRequest(null, null, null, null, null, 30L), Set.of("sprintId")))
				.isInstanceOf(ProjectValidationException.class)
				.hasMessage("Sprint 할당은 POST /sprints/{id}/issues API를 사용해야 합니다.");

		assertThat(issue.getSprint()).isSameAs(sprint);
	}

	@Test
	void createRejectsMissingProjectBeforeSequenceLookup() {
		Fixtures fixtures = fixtures();
		stubAuth(fixtures);
		when(projectRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> issueService.create("token", 404L, new IssueCreateRequest("새 이슈")))
				.isInstanceOf(ProjectNotFoundException.class);
		verify(issueRepository, never()).findByProjectIdForSequenceUpdate(any());
	}

	@Test
	void createLocksSprintBeforeIssueSequenceAndRejectsCompletedSprint() {
		Fixtures fixtures = fixtures();
		Sprint completedSprint = new Sprint(fixtures.project(), "완료 Sprint", null, null, null, LocalDateTime.now(FIXED_CLOCK));
		setId(completedSprint, 30L);
		completedSprint.start(LocalDateTime.now(FIXED_CLOCK));
		completedSprint.complete(null, LocalDateTime.now(FIXED_CLOCK));
		stubAuth(fixtures);
		when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(fixtures.project()));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
		when(sprintRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(completedSprint));

		assertThatThrownBy(() -> issueService.create("token", 20L, new IssueCreateRequest("새 이슈", null, null, null, null, 30L)))
				.isInstanceOf(com.threadia.backend.project.exception.ProjectValidationException.class);
		verify(issueRepository, never()).findByProjectIdForSequenceUpdate(any());
	}

	@Test
	void updateStatusChangesStateAndCreatesHistoryForRegularMemberWhenTargetIsNotDone() {
		Fixtures fixtures = fixtures();
		Issue issue = issue(100L, fixtures.project(), fixtures.user(), "THD-1", "첫 이슈", IssueStatus.BACKLOG);
		stubAuth(fixtures);
		when(issueRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(issue));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L))
				.thenReturn(Optional.of(projectMember(fixtures, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)));

		IssueData data = issueService.updateStatus("token", 100L, new IssueStatusUpdateRequest(IssueStatus.TODO));

		ArgumentCaptor<IssueStatusHistory> historyCaptor = ArgumentCaptor.forClass(IssueStatusHistory.class);
		verify(historyRepository).save(historyCaptor.capture());
		assertThat(historyCaptor.getValue().getFromStatus()).isEqualTo(IssueStatus.BACKLOG);
		assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(IssueStatus.TODO);
		assertThat(issue.getUpdatedAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
		assertThat(data.issue().status()).isEqualTo("TODO");
	}

	@Test
	void updateStatusRejectsDoneForRegularMember() {
		Fixtures fixtures = fixtures();
		Issue issue = issue(100L, fixtures.project(), fixtures.user(), "THD-1", "첫 이슈", IssueStatus.IN_REVIEW);
		stubAuth(fixtures);
		when(issueRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(issue));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L))
				.thenReturn(Optional.of(projectMember(fixtures, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)));

		assertThatThrownBy(() -> issueService.updateStatus("token", 100L, new IssueStatusUpdateRequest(IssueStatus.DONE)))
				.isInstanceOf(ForbiddenException.class);
		assertThat(issue.getStatus()).isEqualTo(IssueStatus.IN_REVIEW);
		verify(historyRepository, never()).save(any());
	}

	@Test
	void updateStatusAllowsDoneForProjectAdmin() {
		Fixtures fixtures = fixtures();
		Issue issue = issue(100L, fixtures.project(), fixtures.user(), "THD-1", "첫 이슈", IssueStatus.IN_REVIEW);
		stubAuth(fixtures);
		when(issueRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(issue));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L))
				.thenReturn(Optional.of(projectMember(fixtures, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE)));

		IssueData data = issueService.updateStatus("token", 100L, new IssueStatusUpdateRequest(IssueStatus.DONE));

		assertThat(data.issue().status()).isEqualTo("DONE");
		verify(historyRepository).save(any());
	}

	@Test
	void updateStatusAllowsDoneForCompanyAdminWithoutProjectMembership() {
		Fixtures fixtures = fixtures();
		Issue issue = issue(100L, fixtures.project(), fixtures.user(), "THD-1", "첫 이슈", IssueStatus.IN_REVIEW);
		stubAuth(fixtures);
		when(issueRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(issue));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		IssueData data = issueService.updateStatus("token", 100L, new IssueStatusUpdateRequest(IssueStatus.DONE));

		assertThat(data.issue().status()).isEqualTo("DONE");
		verify(projectMemberRepository, never()).findByProject_IdAndUser_Id(any(), any());
	}

	@Test
	void updateStatusDoesNotCreateHistoryWhenStatusIsUnchanged() {
		Fixtures fixtures = fixtures();
		Issue issue = issue(100L, fixtures.project(), fixtures.user(), "THD-1", "첫 이슈", IssueStatus.TODO);
		LocalDateTime originalUpdatedAt = issue.getUpdatedAt();
		stubAuth(fixtures);
		when(issueRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(issue));
		when(companyMembershipService.hasActiveRole(10L, fixtures.authContext(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);

		IssueData data = issueService.updateStatus("token", 100L, new IssueStatusUpdateRequest(IssueStatus.TODO));

		assertThat(data.issue().status()).isEqualTo("TODO");
		assertThat(issue.getUpdatedAt()).isEqualTo(originalUpdatedAt);
		verify(historyRepository, never()).save(any());
	}

	@Test
	void updateStatusRejectsMissingIssue() {
		Fixtures fixtures = fixtures();
		stubAuth(fixtures);
		when(issueRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> issueService.updateStatus("token", 404L, new IssueStatusUpdateRequest(IssueStatus.TODO)))
				.isInstanceOf(IssueNotFoundException.class);
	}

	private void stubAuth(Fixtures fixtures) {
		when(authContextService.fromAccessToken("token")).thenReturn(fixtures.authContext());
		when(authContextService.requireActiveUser(fixtures.authContext())).thenReturn(fixtures.user());
	}

	private static Fixtures fixtures() {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		User user = new User("member@example.com", "hashed", "member", null, UserStatus.ACTIVE, now, now);
		setId(user, 1L);
		Company company = new Company("Threadia", "Member", "workspace", "IT", null, "ACTIVE", user, now, now);
		setId(company, 10L);
		Project project = new Project(
				company,
				"Threadia MVP",
				"THD",
				null,
				ProjectStatus.ACTIVE,
				LocalDate.parse("2026-06-17"),
				LocalDate.parse("2026-07-31"),
				user,
				null,
				null,
				null,
				now,
				now);
		setId(project, 20L);
		return new Fixtures(new AuthContext(1L, "member@example.com"), user, project);
	}

	private static ProjectMember projectMember(Fixtures fixtures, ProjectMemberRole role, ProjectMemberStatus status) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		return new ProjectMember(fixtures.project(), fixtures.user(), role, status, now, now, now);
	}

	private static Issue issue(
			Long id,
			Project project,
			User reporter,
			String issueKey,
			String title,
			IssueStatus status) {
		LocalDateTime created = LocalDateTime.parse("2026-07-12T00:00:00");
		Issue issue = new Issue(project, issueKey, reporter, title, status, created, created);
		setId(issue, id);
		return issue;
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

	private record Fixtures(AuthContext authContext, User user, Project project) {
	}
}
