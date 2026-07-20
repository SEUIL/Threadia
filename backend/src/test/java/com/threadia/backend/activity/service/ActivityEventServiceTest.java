package com.threadia.backend.activity.service;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threadia.backend.activity.domain.ActivityEvent;
import com.threadia.backend.activity.domain.ActivityEventType;
import com.threadia.backend.activity.domain.ActivityTargetType;
import com.threadia.backend.activity.dto.ActivityEventPageData;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.issue.domain.Issue;
import com.threadia.backend.issue.domain.IssueStatus;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;
import com.threadia.backend.activity.repository.ActivityEventRepository;

@ExtendWith(MockitoExtension.class)
class ActivityEventServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-19T00:00:00Z"), ZoneOffset.UTC);
	@Mock private AuthContextService auth;
	@Mock private CompanyMembershipService companyMembership;
	@Mock private ProjectRepository projects;
	@Mock private ProjectMemberRepository members;
	@Mock private ActivityEventRepository events;

	@Test
	void listUsesExactProjectViewerAuthorizationAndOffsetPageOrder() {
		Fixture fixture = fixture();
		ActivityEvent event = event(fixture, ActivityEventType.ISSUE_STATUS_CHANGED);
		when(auth.fromAccessToken("token")).thenReturn(fixture.context());
		when(auth.requireActiveUser(fixture.context())).thenReturn(fixture.user());
		when(projects.findById(20L)).thenReturn(Optional.of(fixture.project()));
		when(companyMembership.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(members.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.of(new ProjectMember(fixture.project(), fixture.user(), ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE, now(), now(), now())));
		when(events.findPageByProjectId(eq(20L), any())).thenReturn(new PageImpl<>(java.util.Collections.nCopies(11, event), org.springframework.data.domain.PageRequest.of(1, 20), 31));

		ActivityEventPageData data = service().list("token", 20L, 1, 20);

		assertThat(data.page()).isEqualTo(1);
		assertThat(data.size()).isEqualTo(20);
		assertThat(data.total()).isEqualTo(31);
		assertThat(data.events()).first().satisfies(response -> {
			assertThat(response.eventType()).isEqualTo("ISSUE_STATUS_CHANGED");
			assertThat(response.metadata().issueKey()).isEqualTo("THD-1");
			assertThat(response.metadata().fromStatus()).isEqualTo("BACKLOG");
			assertThat(response.metadata().toStatus()).isEqualTo("TODO");
		});
		verify(companyMembership).requireActiveMember(10L, fixture.context());
		ArgumentCaptor<org.springframework.data.domain.Pageable> pageable = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
		verify(events).findPageByProjectId(eq(20L), pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
		assertThat(pageable.getValue().getSort().toString()).contains("createdAt: DESC", "id: DESC");
	}

	@Test
	void listAllowsCompanyAdminWithoutProjectMembership() {
		Fixture fixture = fixture();
		when(auth.fromAccessToken("token")).thenReturn(fixture.context());
		when(auth.requireActiveUser(fixture.context())).thenReturn(fixture.user());
		when(projects.findById(20L)).thenReturn(Optional.of(fixture.project()));
		when(companyMembership.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
		when(events.findPageByProjectId(eq(20L), any())).thenReturn(org.springframework.data.domain.Page.empty());

		service().list("token", 20L, 0, 20);

		verify(members, never()).findByProject_IdAndUser_Id(any(), any());
		verify(companyMembership, never()).requireActiveMember(any(), any());
	}

	@Test
	void listRejectsNonMemberBeforeQuery() {
		Fixture fixture = fixture();
		when(auth.fromAccessToken("token")).thenReturn(fixture.context());
		when(auth.requireActiveUser(fixture.context())).thenReturn(fixture.user());
		when(projects.findById(20L)).thenReturn(Optional.of(fixture.project()));
		when(companyMembership.hasActiveRole(10L, fixture.context(), CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(members.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().list("token", 20L, 0, 20)).isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(events);
	}

	@Test
	void issueProducerStoresOnlyAllowlistedMetadata() {
		Fixture fixture = fixture();
		Issue issue = new Issue(fixture.project(), "THD-1", fixture.user(), "이슈 제목", IssueStatus.TODO, now(), now());
		setId(issue, 100L);
		User assignee = new User("assignee@example.com", "hash", "assignee", null, UserStatus.ACTIVE, now(), now());
		setId(assignee, 2L);
		issue.setAssignee(assignee);

		service().recordIssueStatusChanged(issue, fixture.user(), IssueStatus.BACKLOG, IssueStatus.TODO);

		ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
		verify(events).save(captor.capture());
		ActivityEvent saved = captor.getValue();
		assertThat(saved.getEventType()).isEqualTo(ActivityEventType.ISSUE_STATUS_CHANGED);
		assertThat(saved.getTargetType()).isEqualTo(ActivityTargetType.ISSUE);
		assertThat(saved.getMetadata().fieldNames()).toIterable().containsExactlyInAnyOrder("issueKey", "title", "assigneeUserId", "fromStatus", "toStatus");
		assertThat(saved.getMetadata().has("reporterUserId")).isFalse();
	}

	private ActivityEventService service() {
		return new ActivityEventService(auth, companyMembership, projects, members, events, new ObjectMapper(), CLOCK);
	}

	private static ActivityEvent event(Fixture fixture, ActivityEventType type) {
		ObjectNode metadata = new ObjectMapper().createObjectNode();
		metadata.put("issueKey", "THD-1"); metadata.put("title", "이슈 제목"); metadata.put("fromStatus", "BACKLOG"); metadata.put("toStatus", "TODO"); metadata.put("unexpected", "hidden");
		ActivityEvent event = new ActivityEvent(fixture.company(), fixture.project(), fixture.user(), type, ActivityTargetType.ISSUE, 100L, metadata, now());
		setId(event, 10L);
		return event;
	}

	private static Fixture fixture() {
		User user = new User("member@example.com", "hash", "member", null, UserStatus.ACTIVE, now(), now()); setId(user, 1L);
		Company company = new Company("Threadia", "Member", "workspace", "IT", null, CompanyStatus.ACTIVE, user, now(), now()); setId(company, 10L);
		Project project = new Project(company, "MVP", "THD", null, ProjectStatus.ACTIVE, LocalDate.now(), LocalDate.now(), user, null, null, null, now(), now()); setId(project, 20L);
		return new Fixture(new AuthContext(1L, "member@example.com"), user, company, project);
	}

	private static LocalDateTime now() { return LocalDateTime.now(CLOCK); }
	private static void setId(Object target, Long id) { try { Field field = target.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(target, id); } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); } }
	private record Fixture(AuthContext context, User user, Company company, Project project) { }
}
