package com.threadia.backend.document.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.document.domain.Document;
import com.threadia.backend.document.domain.DocumentIssueLink;
import com.threadia.backend.document.domain.DocumentType;
import com.threadia.backend.document.dto.DeletedData;
import com.threadia.backend.document.dto.DocumentCreateRequest;
import com.threadia.backend.document.dto.DocumentData;
import com.threadia.backend.document.dto.DocumentGraphData;
import com.threadia.backend.document.dto.DocumentLinkCreateRequest;
import com.threadia.backend.document.dto.DocumentLinkData;
import com.threadia.backend.document.dto.DocumentLinkResponse;
import com.threadia.backend.document.dto.DocumentLinksData;
import com.threadia.backend.document.dto.DocumentResponse;
import com.threadia.backend.document.dto.DocumentUpdateRequest;
import com.threadia.backend.document.dto.DocumentsData;
import com.threadia.backend.document.exception.DocumentConflictException;
import com.threadia.backend.document.exception.DocumentLinkNotFoundException;
import com.threadia.backend.document.exception.DocumentNotFoundException;
import com.threadia.backend.document.repository.DocumentIssueLinkRepository;
import com.threadia.backend.document.repository.DocumentRepository;
import com.threadia.backend.issue.domain.Issue;
import com.threadia.backend.issue.dto.IssueResponse;
import com.threadia.backend.issue.dto.IssuesData;
import com.threadia.backend.issue.exception.IssueNotFoundException;
import com.threadia.backend.issue.repository.IssueRepository;
import com.threadia.backend.notification.service.DocumentNotificationProducerService;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@Service
public class DocumentService {
	private final AuthContextService authContextService;
	private final CompanyMembershipService companyMembershipService;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final DocumentRepository documentRepository;
	private final IssueRepository issueRepository;
	private final DocumentIssueLinkRepository linkRepository;
	private final DocumentNotificationProducerService notificationProducer;
	private final Clock clock;

	@Autowired
	public DocumentService(AuthContextService authContextService, CompanyMembershipService companyMembershipService, ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository, DocumentRepository documentRepository, IssueRepository issueRepository, DocumentIssueLinkRepository linkRepository, DocumentNotificationProducerService notificationProducer) {
		this(authContextService, companyMembershipService, projectRepository, projectMemberRepository, documentRepository, issueRepository, linkRepository, notificationProducer, Clock.systemUTC());
	}

	DocumentService(AuthContextService authContextService, CompanyMembershipService companyMembershipService, ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository, DocumentRepository documentRepository, IssueRepository issueRepository, DocumentIssueLinkRepository linkRepository, DocumentNotificationProducerService notificationProducer, Clock clock) {
		this.authContextService = authContextService; this.companyMembershipService = companyMembershipService; this.projectRepository = projectRepository; this.projectMemberRepository = projectMemberRepository; this.documentRepository = documentRepository; this.issueRepository = issueRepository; this.linkRepository = linkRepository; this.notificationProducer = notificationProducer; this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DocumentsData list(String token, Long projectId, DocumentType type, String q, boolean includeTrashed) {
		requireProjectAccess(project(projectId), activeAuthContext(token));
		String query = q == null || q.isBlank() ? null : q.trim();
		return new DocumentsData(documentRepository.findByProjectFilters(projectId, type, query, includeTrashed).stream().map(DocumentResponse::from).toList());
	}
	@Transactional
	public DocumentData create(String token, Long projectId, DocumentCreateRequest request) {
		AuthContext context = authContextService.fromAccessToken(token); User author = authContextService.requireActiveUser(context); Project project = project(projectId); requireProjectAccess(project, context);
		Document document = documentRepository.save(new Document(project, author, request.title().trim(), request.type(), request.content(), now()));
		return new DocumentData(DocumentResponse.from(document));
	}
	@Transactional(readOnly = true)
	public DocumentData detail(String token, Long documentId) {
		Document document = document(documentId); requireProjectAccess(document.getProject(), activeAuthContext(token)); return new DocumentData(DocumentResponse.from(document));
	}
	@Transactional
	public DocumentData update(String token, Long documentId, DocumentUpdateRequest request) {
		AuthContext context = authContextService.fromAccessToken(token); User actor = authContextService.requireActiveUser(context); Document document = documentForUpdate(documentId); requireProjectAccess(document.getProject(), context); requireAuthorOrManage(document, context);
		LocalDateTime updatedAt = now(); document.update(request.title().trim(), request.type(), request.content(), updatedAt); notificationProducer.documentUpdated(document, actor, updatedAt); return new DocumentData(DocumentResponse.from(document));
	}
	@Transactional
	public DocumentData trash(String token, Long documentId) {
		AuthContext context = authContextService.fromAccessToken(token); User actor = authContextService.requireActiveUser(context); Document document = documentForUpdate(documentId); requireProjectAccess(document.getProject(), context); requireAuthorOrManage(document, context);
		document.trash(actor, now()); return new DocumentData(DocumentResponse.from(document));
	}
	@Transactional
	public DocumentData restore(String token, Long documentId) {
		AuthContext context = activeAuthContext(token); Document document = documentForUpdate(documentId); requireProjectAccess(document.getProject(), context); requireAuthorOrManage(document, context); document.restore(now()); return new DocumentData(DocumentResponse.from(document));
	}
	@Transactional(readOnly = true)
	public DocumentGraphData graph(String token, Long projectId) {
		requireProjectAccess(project(projectId), activeAuthContext(token));
		List<DocumentGraphData.GraphNode> nodes = new ArrayList<>();
		documentRepository.findActiveByProjectId(projectId).forEach(d -> nodes.add(new DocumentGraphData.GraphNode(d.getId(), "DOCUMENT", d.getTitle(), null, null)));
		issueRepository.findActiveByProjectIdInDisplayOrder(projectId).forEach(i -> nodes.add(new DocumentGraphData.GraphNode(i.getId(), "ISSUE", i.getTitle(), i.getIssueKey(), i.getStatus().name())));
		List<DocumentGraphData.GraphEdge> edges = linkRepository.findByProjectIdWithEndpoints(projectId).stream().filter(link -> link.getDocument().getTrashedAt() == null && link.getIssue().getTrashedAt() == null).map(link -> new DocumentGraphData.GraphEdge(link.getId(), link.getDocument().getId(), link.getIssue().getId())).toList();
		return new DocumentGraphData(nodes, edges);
	}
	@Transactional(readOnly = true)
	public DocumentLinksData listLinks(String token, Long projectId) {
		requireProjectAccess(project(projectId), activeAuthContext(token));
		return new DocumentLinksData(linkRepository.findByProjectIdWithEndpoints(projectId).stream().filter(link -> link.getDocument().getTrashedAt() == null && link.getIssue().getTrashedAt() == null).map(DocumentLinkResponse::from).toList());
	}
	@Transactional
	public DocumentLinkData createLink(String token, Long projectId, DocumentLinkCreateRequest request) {
		AuthContext context = authContextService.fromAccessToken(token); User actor = authContextService.requireActiveUser(context); Project project = project(projectId); requireProjectAccess(project, context);
		Document document = document(request.documentId()); Issue issue = issue(request.issueId());
		if (!document.getProject().getId().equals(projectId) || !issue.getProject().getId().equals(projectId)) throw new DocumentConflictException("문서와 이슈는 요청 프로젝트에 속해야 합니다.");
		if (document.getTrashedAt() != null || issue.getTrashedAt() != null) throw new DocumentConflictException("휴지통 상태의 문서 또는 이슈는 연결할 수 없습니다.");
		if (linkRepository.existsByDocument_IdAndIssue_Id(document.getId(), issue.getId())) throw new DocumentConflictException("문서와 이슈는 이미 연결되어 있습니다.");
		DocumentIssueLink link = linkRepository.save(new DocumentIssueLink(project, document, issue, actor, now())); return new DocumentLinkData(DocumentLinkResponse.from(link));
	}
	@Transactional
	public DeletedData deleteLink(String token, Long linkId) {
		AuthContext context = activeAuthContext(token); DocumentIssueLink link = linkRepository.findByIdWithProject(linkId).orElseThrow(() -> new DocumentLinkNotFoundException(linkId));
		requireProjectAccess(link.getProject(), context);
		if (!link.getCreatedBy().getId().equals(context.userId()) && !canManage(link.getProject(), context)) throw new ForbiddenException("문서-이슈 연결을 삭제할 권한이 없습니다.");
		linkRepository.delete(link); return new DeletedData(true);
	}
	@Transactional(readOnly = true)
	public DocumentsData documentsForIssue(String token, Long issueId) {
		Issue issue = issue(issueId); requireProjectAccess(issue.getProject(), activeAuthContext(token));
		if (issue.getTrashedAt() != null) return new DocumentsData(List.of());
		return new DocumentsData(linkRepository.findVisibleByIssueId(issueId).stream().map(link -> DocumentResponse.from(link.getDocument())).toList());
	}
	@Transactional(readOnly = true)
	public IssuesData issuesForDocument(String token, Long documentId) {
		Document document = document(documentId); requireProjectAccess(document.getProject(), activeAuthContext(token));
		if (document.getTrashedAt() != null) return new IssuesData(List.of());
		return new IssuesData(linkRepository.findVisibleByDocumentId(documentId).stream().map(link -> IssueResponse.from(link.getIssue())).toList());
	}

	private AuthContext activeAuthContext(String token) { AuthContext context = authContextService.fromAccessToken(token); authContextService.requireActiveUser(context); return context; }
	private Project project(Long id) { return projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id)); }
	private Document document(Long id) { return documentRepository.findByIdWithProject(id).orElseThrow(() -> new DocumentNotFoundException(id)); }
	private Document documentForUpdate(Long id) { return documentRepository.findByIdForUpdate(id).orElseThrow(() -> new DocumentNotFoundException(id)); }
	private Issue issue(Long id) { return issueRepository.findById(id).orElseThrow(() -> new IssueNotFoundException(id)); }
	private void requireAuthorOrManage(Document document, AuthContext context) { if (!document.getAuthor().getId().equals(context.userId()) && !canManage(document.getProject(), context)) throw new ForbiddenException("문서를 수정할 권한이 없습니다."); }
	private void requireProjectAccess(Project project, AuthContext context) { if (companyMembershipService.hasActiveRole(project.getCompany().getId(), context, CompanyMemberRole.COMPANY_ADMIN)) return; companyMembershipService.requireActiveMember(project.getCompany().getId(), context); projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), context.userId()).filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE).orElseThrow(() -> new ForbiddenException("프로젝트 접근 권한이 없습니다.")); }
	private boolean canManage(Project project, AuthContext context) { if (companyMembershipService.hasActiveRole(project.getCompany().getId(), context, CompanyMemberRole.COMPANY_ADMIN)) return true; companyMembershipService.requireActiveMember(project.getCompany().getId(), context); return projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), context.userId()).filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE && member.getRole() == ProjectMemberRole.PROJECT_ADMIN).isPresent(); }
	private LocalDateTime now() { return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime(); }
}
