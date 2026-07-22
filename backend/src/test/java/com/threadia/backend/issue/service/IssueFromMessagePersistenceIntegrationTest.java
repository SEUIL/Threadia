package com.threadia.backend.issue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.threadia.backend.activity.domain.ActivityEventType;
import com.threadia.backend.activity.repository.ActivityEventRepository;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.issue.dto.IssueData;
import com.threadia.backend.issue.dto.IssueFromMessageRequest;
import com.threadia.backend.issue.repository.IssueRepository;
import com.threadia.backend.issue.repository.IssueStatusHistoryRepository;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;
import com.threadia.backend.projectchat.domain.Message;
import com.threadia.backend.projectchat.exception.MessageNotFoundException;
import com.threadia.backend.projectchat.repository.MessageRepository;
import com.threadia.backend.thread.repository.ThreadRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:issue-from-message-persistence;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
class IssueFromMessagePersistenceIntegrationTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 21, 10, 0);

	@Autowired private IssueService issueService;
	@Autowired private JdbcTemplate jdbcTemplate;
	@Autowired private UserRepository users;
	@Autowired private CompanyRepository companies;
	@Autowired private CompanyMemberRepository companyMembers;
	@Autowired private ProjectRepository projects;
	@Autowired private ProjectMemberRepository projectMembers;
	@Autowired private MessageRepository messages;
	@Autowired private IssueRepository issues;
	@Autowired private IssueStatusHistoryRepository histories;
	@Autowired private ActivityEventRepository activityEvents;
	@Autowired private ThreadRepository threads;

	@MockitoBean private AuthContextService auth;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;

	private User actor;
	private Project project;
	private Message message;

	@BeforeEach
	void setUp() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		actor = users.save(new User("from-message-" + suffix + "@example.com", "hash", "actor", null, UserStatus.ACTIVE, NOW, NOW));
		Company company = companies.save(new Company("From Message Co " + suffix, null, null, null, null, CompanyStatus.ACTIVE, actor, NOW, NOW));
		companyMembers.save(new CompanyMember(
				company,
				actor,
				null,
				CompanyMemberRole.COMPANY_ADMIN,
				CompanyMemberStatus.ACTIVE,
				NOW,
				NOW,
				NOW));
		project = projects.save(new Project(company, "From Message Project", "FM" + suffix.toUpperCase(), null, ProjectStatus.ACTIVE, null, null, actor, null, null, null, NOW, NOW));
		projectMembers.save(new ProjectMember(project, actor, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE, NOW, NOW, NOW));
		message = messages.save(new Message(project, actor, "원본 메시지", NOW));
		AuthContext context = new AuthContext(actor.getId(), actor.getEmail());
		when(auth.fromAccessToken(anyString())).thenReturn(context);
		when(auth.requireActiveUser(any())).thenReturn(actor);
	}

	@Test
	void createFromMessagePersistsSourceHistoryActivityAndDoesNotCreateThread() {
		IssueData data = issueService.createFromMessage("token", project.getId(), new IssueFromMessageRequest(
				message.getId(), "메시지 이슈", "원본 메시지에서 생성", null, null, null, null));

		assertThat(jdbcTemplate.queryForObject("select source_type from issues where id = ?", String.class, data.issue().id()))
				.isEqualTo("MESSAGE");
		assertThat(jdbcTemplate.queryForObject("select source_message_id from issues where id = ?", Long.class, data.issue().id()))
				.isEqualTo(message.getId());
		assertThat(data.issue().projectId()).isEqualTo(project.getId());
		assertThat(data.issue().sourceType()).isEqualTo("MESSAGE");
		assertThat(data.issue().sourceMessageId()).isEqualTo(message.getId());
		assertThat(jdbcTemplate.queryForObject("select count(*) from issue_status_history where issue_id = ?", Long.class, data.issue().id()))
				.isEqualTo(1L);
		assertThat(jdbcTemplate.queryForObject("select event_type from activity_events where target_id = ?", String.class, data.issue().id()))
				.isEqualTo(ActivityEventType.ISSUE_CREATED.name());
		assertThat(jdbcTemplate.queryForObject("select count(*) from threads where target_id = ?", Long.class, data.issue().id()))
				.isZero();
	}

	@Test
	void createFromMessageRejectsMessageFromAnotherProjectWithoutPersistingIssue() {
		Project otherProject = projects.save(new Project(project.getCompany(), "Other Project", "OT" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(), null, ProjectStatus.ACTIVE, null, null, actor, null, null, null, NOW, NOW));
		Message otherMessage = messages.save(new Message(otherProject, actor, "다른 프로젝트 메시지", NOW));
		long beforeIssueCount = issues.count();
		long beforeHistoryCount = histories.count();
		long beforeActivityCount = activityEvents.count();

		assertThatThrownBy(() -> issueService.createFromMessage("token", project.getId(), new IssueFromMessageRequest(
				otherMessage.getId(), "메시지 이슈", null, null, null, null, null)))
				.isInstanceOf(MessageNotFoundException.class);

		assertThat(issues.count()).isEqualTo(beforeIssueCount);
		assertThat(histories.count()).isEqualTo(beforeHistoryCount);
		assertThat(activityEvents.count()).isEqualTo(beforeActivityCount);
	}

	@Test
	void createFromMessageAllowsDuplicateConversionWithSequentialKeys() {
		IssueData first = issueService.createFromMessage("token", project.getId(), new IssueFromMessageRequest(
				message.getId(), "첫 번째 메시지 이슈", null, null, null, null, null));
		IssueData second = issueService.createFromMessage("token", project.getId(), new IssueFromMessageRequest(
				message.getId(), "두 번째 메시지 이슈", null, null, null, null, null));

		assertThat(List.of(first.issue().issueKey(), second.issue().issueKey()))
				.containsExactly(project.getProjectKey() + "-1", project.getProjectKey() + "-2");
		assertThat(first.issue().sourceMessageId()).isEqualTo(message.getId());
		assertThat(second.issue().sourceMessageId()).isEqualTo(message.getId());
		assertThat(jdbcTemplate.queryForObject("select count(*) from issues where project_id = ?", Long.class, project.getId()))
				.isEqualTo(2L);
	}
}
