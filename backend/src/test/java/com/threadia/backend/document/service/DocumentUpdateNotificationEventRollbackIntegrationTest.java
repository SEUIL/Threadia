package com.threadia.backend.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

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
import com.threadia.backend.notification.repository.NotificationEventRepository;
import com.threadia.backend.notification.repository.NotificationRecipientRepository;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:document-update-notifications-event-rollback;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
class DocumentUpdateNotificationEventRollbackIntegrationTest {

	@Autowired private UserRepository users;
	@Autowired private CompanyRepository companies;
	@Autowired private ProjectRepository projects;
	@Autowired private DocumentRepository documents;
	@Autowired private DocumentService documentService;
	@MockitoBean private AuthContextService authContextService;
	@MockitoBean private CompanyMembershipService companyMembershipService;
	@MockitoBean private ProjectMemberRepository projectMembers;
	@MockitoBean private DocumentIssueLinkRepository links;
	@MockitoBean private NotificationEventRepository events;
	@MockitoBean private NotificationRecipientRepository recipients;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void eventPersistenceFailureRollsBackDocumentUpdate() {
		LocalDateTime now = LocalDateTime.parse("2026-07-22T13:00:00");
		User actor = users.save(new User("event-failure@example.com", "hash", "actor", null, UserStatus.ACTIVE, now, now));
		Company company = companies.save(new Company("Threadia", "대표", null, null, null, CompanyStatus.ACTIVE, actor, now, now));
		Project project = projects.save(new Project(company, "프로젝트", "THD", null, ProjectStatus.ACTIVE, null, null, actor, null, null, null, now, now));
		Document document = documents.save(new Document(project, actor, "원본 문서", DocumentType.DOC, "old content", now));
		AuthContext context = new AuthContext(actor.getId(), actor.getEmail());
		when(authContextService.fromAccessToken("token")).thenReturn(context);
		when(authContextService.requireActiveUser(context)).thenReturn(actor);
		when(companyMembershipService.hasActiveRole(company.getId(), context, CompanyMemberRole.COMPANY_ADMIN))
				.thenReturn(true);
		when(events.save(any())).thenThrow(new IllegalStateException("notification event write failed"));

		assertThatThrownBy(() -> documentService.update("token", document.getId(),
				new DocumentUpdateRequest("수정 문서", DocumentType.DOC, "new content")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("notification event write failed");

		assertThat(documents.findById(document.getId())).get()
				.satisfies(rolledBack -> {
					assertThat(rolledBack.getTitle()).isEqualTo("원본 문서");
					assertThat(rolledBack.getContent()).isEqualTo("old content");
				});
	}
}
