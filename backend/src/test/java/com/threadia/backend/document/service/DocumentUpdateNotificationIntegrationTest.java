package com.threadia.backend.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

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
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.document.domain.Document;
import com.threadia.backend.document.domain.DocumentIssueLink;
import com.threadia.backend.document.domain.DocumentType;
import com.threadia.backend.document.dto.DocumentUpdateRequest;
import com.threadia.backend.document.repository.DocumentIssueLinkRepository;
import com.threadia.backend.document.repository.DocumentRepository;
import com.threadia.backend.issue.domain.Issue;
import com.threadia.backend.issue.domain.IssueStatus;
import com.threadia.backend.issue.repository.IssueRepository;
import com.threadia.backend.notification.domain.NotificationEvent;
import com.threadia.backend.notification.domain.NotificationTargetType;
import com.threadia.backend.notification.domain.NotificationType;
import com.threadia.backend.notification.repository.NotificationEventRepository;
import com.threadia.backend.notification.repository.NotificationRecipientRepository;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:document-update-notifications;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
@Transactional
class DocumentUpdateNotificationIntegrationTest {

	@Autowired private UserRepository users;
	@Autowired private CompanyRepository companies;
	@Autowired private CompanyMemberRepository companyMembers;
	@Autowired private ProjectRepository projects;
	@Autowired private ProjectMemberRepository projectMembers;
	@Autowired private DocumentRepository documents;
	@Autowired private DocumentIssueLinkRepository links;
	@Autowired private IssueRepository issues;
	@Autowired private NotificationEventRepository events;
	@Autowired private NotificationRecipientRepository recipients;
	@Autowired private DocumentService documentService;
	@MockitoBean private AuthContextService authContextService;
	@MockitoBean private CompanyMembershipService companyMembershipService;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void updateCreatesOneDocumentUpdatedEventAndRecipientsWithoutRawContentLeak() {
		LocalDateTime now = LocalDateTime.parse("2026-07-22T10:00:00");
		User actor = users.save(user("actor@example.com", "actor", UserStatus.ACTIVE, now));
		User linkedReporter = users.save(user("reporter@example.com", "reporter", UserStatus.ACTIVE, now));
		User linkedAssignee = users.save(user("assignee@example.com", "assignee", UserStatus.ACTIVE, now));
		User unrelated = users.save(user("unrelated@example.com", "unrelated", UserStatus.ACTIVE, now));
		User removed = users.save(user("removed@example.com", "removed", UserStatus.ACTIVE, now));
		User suspended = users.save(user("suspended@example.com", "suspended", UserStatus.SUSPENDED, now));
		User companySuspended = users.save(user("company-suspended@example.com", "companySuspended", UserStatus.ACTIVE, now));
		Company company = companies.save(company(actor, now));
		companyMembers.save(companyMember(company, actor, CompanyMemberStatus.ACTIVE, now));
		companyMembers.save(companyMember(company, linkedReporter, CompanyMemberStatus.ACTIVE, now));
		companyMembers.save(companyMember(company, linkedAssignee, CompanyMemberStatus.ACTIVE, now));
		companyMembers.save(companyMember(company, unrelated, CompanyMemberStatus.ACTIVE, now));
		companyMembers.save(companyMember(company, removed, CompanyMemberStatus.ACTIVE, now));
		companyMembers.save(companyMember(company, suspended, CompanyMemberStatus.ACTIVE, now));
		companyMembers.save(companyMember(company, companySuspended, CompanyMemberStatus.SUSPENDED, now));
		Project project = projects.save(project(company, actor, now));
		projectMembers.save(member(project, actor, ProjectMemberStatus.ACTIVE, now));
		projectMembers.save(member(project, linkedReporter, ProjectMemberStatus.ACTIVE, now));
		projectMembers.save(member(project, linkedAssignee, ProjectMemberStatus.ACTIVE, now));
		projectMembers.save(member(project, unrelated, ProjectMemberStatus.ACTIVE, now));
		projectMembers.save(member(project, removed, ProjectMemberStatus.REMOVED, now));
		projectMembers.save(member(project, suspended, ProjectMemberStatus.ACTIVE, now));
		projectMembers.save(member(project, companySuspended, ProjectMemberStatus.ACTIVE, now));
		Document document = documents.save(new Document(project, actor, "원본 문서", DocumentType.DOC, "old content", now));
		Issue linkedIssue = issues.save(issue(project, linkedReporter, linkedAssignee, "THD-1", now));
		Issue actorIssue = issues.save(issue(project, actor, actor, "THD-2", now));
		Issue removedIssue = issues.save(issue(project, removed, removed, "THD-3", now));
		Issue suspendedIssue = issues.save(issue(project, suspended, suspended, "THD-4", now));
		Issue companySuspendedIssue = issues.save(issue(project, companySuspended, companySuspended, "THD-5", now));
		links.save(new DocumentIssueLink(project, document, linkedIssue, actor, now));
		links.save(new DocumentIssueLink(project, document, actorIssue, actor, now));
		links.save(new DocumentIssueLink(project, document, removedIssue, actor, now));
		links.save(new DocumentIssueLink(project, document, suspendedIssue, actor, now));
		links.save(new DocumentIssueLink(project, document, companySuspendedIssue, actor, now));
		AuthContext context = new AuthContext(actor.getId(), actor.getEmail());
		when(authContextService.fromAccessToken("token")).thenReturn(context);
		when(authContextService.requireActiveUser(context)).thenReturn(actor);
		when(companyMembershipService.hasActiveRole(company.getId(), context, CompanyMemberRole.COMPANY_ADMIN))
				.thenReturn(false);

		String rawContent = "raw-secret-document-content-should-not-leak";
		documentService.update("token", document.getId(), new DocumentUpdateRequest("수정 문서", DocumentType.DOC, rawContent));

		List<NotificationEvent> notificationEvents = events.findAll();
		assertThat(notificationEvents).hasSize(1);
		NotificationEvent event = notificationEvents.get(0);
		assertThat(event.getActivityEventId()).isNull();
		assertThat(event.getCompany().getId()).isEqualTo(company.getId());
		assertThat(event.getProject().getId()).isEqualTo(project.getId());
		assertThat(event.getActor().getId()).isEqualTo(actor.getId());
		assertThat(event.getType()).isEqualTo(NotificationType.DOCUMENT_UPDATED);
		assertThat(event.getTargetType()).isEqualTo(NotificationTargetType.DOCUMENT);
		assertThat(event.getTargetId()).isEqualTo(document.getId());
		assertThat(event.getTitle()).doesNotContain(rawContent);
		assertThat(event.getBody()).isNull();
		assertThat(event.getMetadata()).isNull();
		assertThat(recipients.findAll()).extracting(recipientRow -> recipientRow.getRecipient().getId())
				.containsExactly(linkedReporter.getId(), linkedAssignee.getId());
	}

	private static User user(String email, String nickname, UserStatus status, LocalDateTime now) {
		return new User(email, "hash", nickname, null, status, now, now);
	}

	private static Company company(User creator, LocalDateTime now) {
		return new Company("Threadia", "대표", null, null, null, CompanyStatus.ACTIVE, creator, now, now);
	}

	private static CompanyMember companyMember(Company company, User user, CompanyMemberStatus status, LocalDateTime now) {
		return new CompanyMember(company, user, null, CompanyMemberRole.MEMBER, status, now, now, now);
	}

	private static Project project(Company company, User creator, LocalDateTime now) {
		return new Project(company, "프로젝트", "THD", null, ProjectStatus.ACTIVE, null, null, creator, null, null, null, now, now);
	}

	private static Issue issue(Project project, User reporter, User assignee, String issueKey, LocalDateTime now) {
		Issue issue = new Issue(project, issueKey, reporter, "연결 이슈", IssueStatus.TODO, now, now);
		issue.setAssignee(assignee);
		return issue;
	}

	private static ProjectMember member(Project project, User user, ProjectMemberStatus status, LocalDateTime now) {
		return new ProjectMember(project, user, ProjectMemberRole.MEMBER, status, now, now, now);
	}
}
