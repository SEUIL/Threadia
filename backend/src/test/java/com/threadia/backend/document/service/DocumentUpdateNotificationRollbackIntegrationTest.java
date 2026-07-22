package com.threadia.backend.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.document.domain.Document;
import com.threadia.backend.document.domain.DocumentType;
import com.threadia.backend.document.dto.DocumentUpdateRequest;
import com.threadia.backend.document.repository.DocumentIssueLinkRepository;
import com.threadia.backend.document.repository.DocumentRepository;
import com.threadia.backend.notification.domain.NotificationRecipient;
import com.threadia.backend.notification.repository.NotificationEventRepository;
import com.threadia.backend.notification.repository.NotificationRecipientRepository;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:document-update-notifications-rollback;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
class DocumentUpdateNotificationRollbackIntegrationTest {

	@Autowired private UserRepository users;
	@Autowired private CompanyRepository companies;
	@Autowired private ProjectRepository projects;
	@Autowired private DocumentRepository documents;
	@Autowired private NotificationEventRepository events;
	@Autowired private DocumentService documentService;
	@MockitoBean private AuthContextService authContextService;
	@MockitoBean private CompanyMembershipService companyMembershipService;
	@MockitoBean private ProjectMemberRepository projectMembers;
	@MockitoBean private DocumentIssueLinkRepository links;
	@MockitoBean private NotificationRecipientRepository recipients;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void recipientLookupFailureRollsBackDocumentUpdateAndNotificationEvent() {
		Fixture fixture = fixture("lookup-failure@example.com", "원본 문서", "old content");
		when(links.findActiveNotificationRecipientsByDocumentIdExcludingUserId(fixture.document.getId(), fixture.actor.getId()))
				.thenThrow(new IllegalStateException("recipient lookup failed"));

		assertThatThrownBy(() -> documentService.update("token", fixture.document.getId(),
				new DocumentUpdateRequest("수정 문서", DocumentType.DOC, "new content")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("recipient lookup failed");

		assertRolledBack(fixture.document.getId(), "원본 문서", "old content");
		assertThat(events.count()).isZero();
	}

	@Test
	void recipientPersistenceFailureRollsBackDocumentUpdateAndNotificationEvent() {
		Fixture fixture = fixture("persistence-failure@example.com", "원본 회의록", "old meeting");
		User recipient = users.save(user("recipient-failure@example.com", "recipient", UserStatus.ACTIVE, fixture.now));
		when(links.findActiveNotificationRecipientsByDocumentIdExcludingUserId(fixture.document.getId(), fixture.actor.getId()))
				.thenReturn(List.of(recipient));
		when(recipients.saveAll(any())).thenThrow(new IllegalStateException("notification recipient write failed"));

		assertThatThrownBy(() -> documentService.update("token", fixture.document.getId(),
				new DocumentUpdateRequest("수정 회의록", DocumentType.MEETING_NOTE, "new meeting")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("notification recipient write failed");

		assertRolledBack(fixture.document.getId(), "원본 회의록", "old meeting");
		assertThat(events.count()).isZero();
		verify(recipients).saveAll(any());
	}

	private Fixture fixture(String email, String title, String content) {
		LocalDateTime now = LocalDateTime.parse("2026-07-22T12:00:00");
		User actor = users.save(user(email, "actor", UserStatus.ACTIVE, now));
		Company company = companies.save(new Company("Threadia", "대표", null, null, null, CompanyStatus.ACTIVE, actor, now, now));
		Project project = projects.save(new Project(company, "프로젝트", "THD", null, ProjectStatus.ACTIVE, null, null, actor, null, null, null, now, now));
		Document document = documents.save(new Document(project, actor, title, DocumentType.DOC, content, now));
		AuthContext context = new AuthContext(actor.getId(), actor.getEmail());
		when(authContextService.fromAccessToken("token")).thenReturn(context);
		when(authContextService.requireActiveUser(context)).thenReturn(actor);
		when(companyMembershipService.hasActiveRole(company.getId(), context, CompanyMemberRole.COMPANY_ADMIN))
				.thenReturn(true);
		return new Fixture(actor, document, now);
	}

	private void assertRolledBack(Long documentId, String title, String content) {
		assertThat(documents.findById(documentId)).get()
				.satisfies(document -> {
					assertThat(document.getTitle()).isEqualTo(title);
					assertThat(document.getContent()).isEqualTo(content);
				});
	}

	private static User user(String email, String nickname, UserStatus status, LocalDateTime now) {
		return new User(email, "hash", nickname, null, status, now, now);
	}

	private record Fixture(User actor, Document document, LocalDateTime now) {
	}
}
