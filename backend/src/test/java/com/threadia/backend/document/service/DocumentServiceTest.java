package com.threadia.backend.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
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
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.document.domain.Document;
import com.threadia.backend.document.domain.DocumentType;
import com.threadia.backend.document.domain.DocumentIssueLink;
import com.threadia.backend.document.dto.DocumentLinkCreateRequest;
import com.threadia.backend.document.dto.DocumentUpdateRequest;
import com.threadia.backend.document.exception.DocumentConflictException;
import com.threadia.backend.document.repository.DocumentIssueLinkRepository;
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

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
	@Mock private AuthContextService auth;
	@Mock private CompanyMembershipService memberships;
	@Mock private ProjectRepository projects;
	@Mock private ProjectMemberRepository projectMembers;
	@Mock private DocumentRepository documents;
	@Mock private IssueRepository issues;
	@Mock private DocumentIssueLinkRepository links;
	private DocumentService service;
	private User user;
	private Project project;
	private Document document;
	private AuthContext context;
	@BeforeEach void setUp() {
		service = new DocumentService(auth, memberships, projects, projectMembers, documents, issues, links, Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneOffset.UTC));
		LocalDateTime now = LocalDateTime.parse("2026-07-14T00:00:00"); user = new User("user@threadia.com", "hash", "사용자", null, UserStatus.ACTIVE, now, now); id(user, 1L);
		Company company = new Company("Threadia", "대표", null, null, null, com.threadia.backend.company.domain.CompanyStatus.ACTIVE, user, now, now); id(company, 10L);
		project = new Project(company, "프로젝트", "THD", null, ProjectStatus.ACTIVE, null, null, user, null, null, null, now, now); id(project, 20L);
		document = new Document(project, user, "문서", DocumentType.DOC, "# 내용", now); id(document, 30L); context = new AuthContext(1L, "user@threadia.com");
		when(auth.fromAccessToken("token")).thenReturn(context); when(auth.requireActiveUser(context)).thenReturn(user);
		when(memberships.hasActiveRole(10L, context, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
	}
	@Test void trashAndRestoreAreIdempotentForAuthor() {
		when(documents.findByIdForUpdate(30L)).thenReturn(Optional.of(document));
		service.trash("token", 30L); LocalDateTime trashedAt = document.getTrashedAt(); service.trash("token", 30L);
		assertThat(document.getTrashedAt()).isEqualTo(trashedAt);
		service.restore("token", 30L); LocalDateTime restoredAt = document.getRestoredAt(); service.restore("token", 30L);
		assertThat(document.getTrashedAt()).isNull(); assertThat(document.getRestoredAt()).isEqualTo(restoredAt);
	}
	@Test void createLinkRejectsEndpointsOutsideRequestedProject() {
		Project otherProject = new Project(project.getCompany(), "다른 프로젝트", "OTH", null, ProjectStatus.ACTIVE, null, null, user, null, null, null, LocalDateTime.now(), LocalDateTime.now()); id(otherProject, 21L);
		Issue issue = new Issue(otherProject, "OTHER-1", user, "이슈", IssueStatus.BACKLOG, LocalDateTime.now(), LocalDateTime.now()); id(issue, 40L);
		when(projects.findById(20L)).thenReturn(Optional.of(project));
		when(documents.findByIdWithProject(30L)).thenReturn(Optional.of(document)); when(issues.findById(40L)).thenReturn(Optional.of(issue));
		assertThatThrownBy(() -> service.createLink("token", 20L, new DocumentLinkCreateRequest(30L, 40L))).isInstanceOf(DocumentConflictException.class);
		verify(links, never()).save(any());
	}
	@Test void graphUsesProjectScopedBatchRepositoriesAndHidesTrashedEndpoints() {
		when(projects.findById(20L)).thenReturn(Optional.of(project));
		when(documents.findActiveByProjectId(20L)).thenReturn(List.of(document)); when(issues.findActiveByProjectIdInDisplayOrder(20L)).thenReturn(List.of()); when(links.findByProjectIdWithEndpoints(20L)).thenReturn(List.of());
		assertThat(service.graph("token", 20L).nodes()).extracting("type").containsExactly("DOCUMENT");
	}
	@Test void updateRejectsRemovedDocumentAuthorBeforeAuthorShortcut() {
		when(memberships.hasActiveRole(10L, context, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(documents.findByIdForUpdate(30L)).thenReturn(Optional.of(document));
		when(projectMembers.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.of(projectMember(ProjectMemberRole.MEMBER, ProjectMemberStatus.REMOVED)));
		assertThatThrownBy(() -> service.update("token", 30L, new DocumentUpdateRequest("수정", DocumentType.DOC, "내용"))).isInstanceOf(ForbiddenException.class);
	}
	@Test void updateAllowsActiveDocumentAuthorMember() {
		when(memberships.hasActiveRole(10L, context, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(documents.findByIdForUpdate(30L)).thenReturn(Optional.of(document));
		when(projectMembers.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.of(projectMember(ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)));
		assertThat(service.update("token", 30L, new DocumentUpdateRequest("수정", DocumentType.DOC, "내용")).document().title()).isEqualTo("수정");
	}
	@Test void trashRejectsInactiveCompanyDocumentAuthorBeforeAuthorShortcut() {
		when(memberships.hasActiveRole(10L, context, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(documents.findByIdForUpdate(30L)).thenReturn(Optional.of(document));
		doThrow(new ForbiddenException("회사 접근 권한이 없습니다.")).when(memberships).requireActiveMember(10L, context);
		assertThatThrownBy(() -> service.trash("token", 30L)).isInstanceOf(ForbiddenException.class);
		verify(projectMembers, never()).findByProject_IdAndUser_Id(any(), any());
	}
	@Test void updateAllowsActiveProjectAdminWhoIsNotAuthor() {
		User author = new User("author@threadia.com", "hash", "작성자", null, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now()); id(author, 2L);
		Document authoredByOther = new Document(project, author, "문서", DocumentType.DOC, "내용", LocalDateTime.now()); id(authoredByOther, 31L);
		when(memberships.hasActiveRole(10L, context, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(documents.findByIdForUpdate(31L)).thenReturn(Optional.of(authoredByOther));
		when(projectMembers.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.of(projectMember(ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE)));
		assertThat(service.update("token", 31L, new DocumentUpdateRequest("수정", DocumentType.MEETING_NOTE, "내용")).document().type()).isEqualTo("MEETING_NOTE");
	}
	@Test void deleteLinkRejectsRemovedLinkCreatorBeforeCreatorShortcut() {
		Issue issue = issue(40L);
		DocumentIssueLink link = new DocumentIssueLink(project, document, issue, user, LocalDateTime.now()); id(link, 50L);
		when(memberships.hasActiveRole(10L, context, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(links.findByIdWithProject(50L)).thenReturn(Optional.of(link));
		when(projectMembers.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.of(projectMember(ProjectMemberRole.MEMBER, ProjectMemberStatus.REMOVED)));
		assertThatThrownBy(() -> service.deleteLink("token", 50L)).isInstanceOf(ForbiddenException.class);
		verify(links, never()).delete(any());
	}
	@Test void deleteLinkAllowsActiveCreatorMember() {
		Issue issue = issue(40L);
		DocumentIssueLink link = new DocumentIssueLink(project, document, issue, user, LocalDateTime.now()); id(link, 50L);
		when(memberships.hasActiveRole(10L, context, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(links.findByIdWithProject(50L)).thenReturn(Optional.of(link));
		when(projectMembers.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.of(projectMember(ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE)));
		assertThat(service.deleteLink("token", 50L).deleted()).isTrue();
		verify(links).delete(link);
	}
	@Test void createLinkRejectsDuplicateAndTrashedEndpoints() {
		Issue issue = issue(40L);
		when(projects.findById(20L)).thenReturn(Optional.of(project)); when(documents.findByIdWithProject(30L)).thenReturn(Optional.of(document)); when(issues.findById(40L)).thenReturn(Optional.of(issue));
		when(links.existsByDocument_IdAndIssue_Id(30L, 40L)).thenReturn(true);
		assertThatThrownBy(() -> service.createLink("token", 20L, new DocumentLinkCreateRequest(30L, 40L))).isInstanceOf(DocumentConflictException.class);
		document.trash(user, LocalDateTime.now());
		assertThatThrownBy(() -> service.createLink("token", 20L, new DocumentLinkCreateRequest(30L, 40L))).isInstanceOf(DocumentConflictException.class);
		verify(links, never()).save(any());
	}
	@Test void documentReverseReadHidesLinksWhenDocumentIsTrashed() {
		document.trash(user, LocalDateTime.now());
		when(documents.findByIdWithProject(30L)).thenReturn(Optional.of(document));
		assertThat(service.issuesForDocument("token", 30L).issues()).isEmpty();
		verify(links, never()).findVisibleByDocumentId(any());
	}
	@Test void issueReverseReadHidesLinksWhenIssueIsTrashed() {
		Issue issue = issue(40L); set(issue, "trashedAt", LocalDateTime.now());
		when(issues.findById(40L)).thenReturn(Optional.of(issue));
		assertThat(service.documentsForIssue("token", 40L).documents()).isEmpty();
		verify(links, never()).findVisibleByIssueId(any());
	}
	private ProjectMember projectMember(ProjectMemberRole role, ProjectMemberStatus status) { LocalDateTime now = LocalDateTime.now(); return new ProjectMember(project, user, role, status, now, now, now); }
	private Issue issue(Long id) { Issue issue = new Issue(project, "THD-1", user, "이슈", IssueStatus.BACKLOG, LocalDateTime.now(), LocalDateTime.now()); id(issue, id); return issue; }
	private static void set(Object value, String name, Object fieldValue) { try { Field field = value.getClass().getDeclaredField(name); field.setAccessible(true); field.set(value, fieldValue); } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); } }
	private static void id(Object value, Long id) { try { Field field = value.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(value, id); } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); } }
}
