package com.threadia.backend.issue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.threadia.backend.activity.service.ActivityEventService;
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
import com.threadia.backend.issue.domain.Issue;
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
import com.threadia.backend.projectchat.repository.MessageRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:issue-from-message-rollback;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
class IssueFromMessageRollbackIntegrationTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 21, 10, 0);

	@Autowired private IssueService issueService;
	@Autowired private UserRepository users;
	@Autowired private CompanyRepository companies;
	@Autowired private CompanyMemberRepository companyMembers;
	@Autowired private ProjectRepository projects;
	@Autowired private ProjectMemberRepository projectMembers;
	@Autowired private MessageRepository messages;
	@Autowired private IssueRepository issues;
	@Autowired private IssueStatusHistoryRepository histories;

	@MockitoBean private AuthContextService auth;
	@MockitoBean private ActivityEventService activityEvents;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;

	private Project project;
	private Message message;
	private User actor;

	@BeforeEach
	void setUp() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		actor = users.save(new User("from-message-rollback-" + suffix + "@example.com", "hash", "actor", null, UserStatus.ACTIVE, NOW, NOW));
		Company company = companies.save(new Company("From Message Rollback Co " + suffix, null, null, null, null, CompanyStatus.ACTIVE, actor, NOW, NOW));
		companyMembers.save(new CompanyMember(
				company,
				actor,
				null,
				CompanyMemberRole.COMPANY_ADMIN,
				CompanyMemberStatus.ACTIVE,
				NOW,
				NOW,
				NOW));
		project = projects.save(new Project(company, "From Message Rollback Project", "FR" + suffix.toUpperCase(), null, ProjectStatus.ACTIVE, null, null, actor, null, null, null, NOW, NOW));
		projectMembers.save(new ProjectMember(project, actor, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE, NOW, NOW, NOW));
		message = messages.save(new Message(project, actor, "원본 메시지", NOW));
		AuthContext context = new AuthContext(actor.getId(), actor.getEmail());
		when(auth.fromAccessToken(anyString())).thenReturn(context);
		when(auth.requireActiveUser(any())).thenReturn(actor);
	}

	@Test
	void activityFailureRollsBackIssueAndInitialHistory() {
		doThrow(new IllegalStateException("activity write failed"))
				.when(activityEvents).recordIssueCreated(any(Issue.class), any(User.class));

		assertThatThrownBy(() -> issueService.createFromMessage("token", project.getId(), new IssueFromMessageRequest(
				message.getId(), "메시지 이슈", null, null, null, null, null)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("activity write failed");

		assertThat(issues.count()).isZero();
		assertThat(histories.count()).isZero();
	}
}
