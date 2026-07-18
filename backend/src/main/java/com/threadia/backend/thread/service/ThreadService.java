package com.threadia.backend.thread.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.document.exception.DocumentNotFoundException;
import com.threadia.backend.document.repository.DocumentRepository;
import com.threadia.backend.issue.exception.IssueNotFoundException;
import com.threadia.backend.issue.repository.IssueRepository;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;
import com.threadia.backend.projectchat.exception.MessageNotFoundException;
import com.threadia.backend.projectchat.repository.MessageRepository;
import com.threadia.backend.thread.domain.Thread;
import com.threadia.backend.thread.domain.ThreadComment;
import com.threadia.backend.thread.domain.ThreadTargetType;
import com.threadia.backend.thread.dto.ThreadCommentCreateRequest;
import com.threadia.backend.thread.dto.ThreadCommentData;
import com.threadia.backend.thread.dto.ThreadCommentResponse;
import com.threadia.backend.thread.dto.ThreadCommentsData;
import com.threadia.backend.thread.dto.ThreadCreationResult;
import com.threadia.backend.thread.dto.ThreadData;
import com.threadia.backend.thread.dto.ThreadGetOrCreateRequest;
import com.threadia.backend.thread.dto.ThreadResponse;
import com.threadia.backend.thread.dto.ThreadTopicUpdateRequest;
import com.threadia.backend.thread.exception.ThreadNotFoundException;
import com.threadia.backend.thread.exception.ThreadValidationException;
import com.threadia.backend.thread.repository.ThreadCommentRepository;
import com.threadia.backend.thread.repository.ThreadRepository;

@Service
public class ThreadService {
	private final AuthContextService auth; private final CompanyMembershipService companyMembership; private final ProjectRepository projects; private final ProjectMemberRepository members;
	private final IssueRepository issues; private final DocumentRepository documents; private final MessageRepository messages; private final ThreadRepository threads; private final ThreadCommentRepository comments; private final Clock clock;
	@Autowired
	public ThreadService(AuthContextService auth, CompanyMembershipService companyMembership, ProjectRepository projects, ProjectMemberRepository members, IssueRepository issues, DocumentRepository documents, MessageRepository messages, ThreadRepository threads, ThreadCommentRepository comments) {
		this(auth, companyMembership, projects, members, issues, documents, messages, threads, comments, Clock.systemUTC());
	}
	ThreadService(AuthContextService auth, CompanyMembershipService companyMembership, ProjectRepository projects, ProjectMemberRepository members, IssueRepository issues, DocumentRepository documents, MessageRepository messages, ThreadRepository threads, ThreadCommentRepository comments, Clock clock) {
		this.auth = auth; this.companyMembership = companyMembership; this.projects = projects; this.members = members; this.issues = issues; this.documents = documents; this.messages = messages; this.threads = threads; this.comments = comments; this.clock = clock;
	}
	@Transactional(readOnly = true)
	public ThreadData findByTarget(String token, Long projectId, ThreadTargetType targetType, Long targetId) {
		AuthContext context = active(token); Project project = project(projectId); requireAccess(project, context); validateTargetProject(project, targetType, targetId);
		Thread thread = threads.findByTargetTypeAndTargetId(targetType, targetId).orElseThrow(() -> new ThreadNotFoundException(targetId));
		return new ThreadData(ThreadResponse.from(thread));
	}
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public ThreadCreationResult getOrCreate(String token, Long projectId, ThreadGetOrCreateRequest request) {
		AuthContext context = active(token); User actor = auth.requireActiveUser(context); Project project = project(projectId); requireAccess(project, context); validateTargetProject(project, request.targetType(), request.targetId());
		Thread existing = threads.findByTargetTypeAndTargetId(request.targetType(), request.targetId()).orElse(null);
		if (existing != null) return new ThreadCreationResult(new ThreadData(ThreadResponse.from(existing)), false);
		String topic = normalizedTopic(request.topic()); LocalDateTime now = now();
		boolean created = threads.insertIgnore(project.getId(), request.targetType().name(), request.targetId(), topic, actor.getId(), now, now) == 1;
		Thread thread = threads.findByTargetTypeAndTargetId(request.targetType(), request.targetId()).orElseThrow();
		return new ThreadCreationResult(new ThreadData(ThreadResponse.from(thread)), created);
	}
	@Transactional(readOnly = true)
	public ThreadData detail(String token, Long threadId) {
		AuthContext context = active(token); Thread thread = thread(threadId); requireAccess(thread.getProject(), context); return new ThreadData(ThreadResponse.from(thread));
	}
	@Transactional
	public ThreadData updateTopic(String token, Long threadId, ThreadTopicUpdateRequest request) {
		AuthContext context = active(token); Thread thread = thread(threadId); requireAccess(thread.getProject(), context);
		if (!thread.getCreatedBy().getId().equals(context.userId()) && !canManage(thread.getProject(), context)) throw new ForbiddenException("쓰레드 주제를 수정할 권한이 없습니다.");
		thread.updateTopic(normalizedTopic(request.topic()), now()); return new ThreadData(ThreadResponse.from(thread));
	}
	@Transactional(readOnly = true)
	public ThreadCommentsData listComments(String token, Long threadId) {
		AuthContext context = active(token); Thread thread = thread(threadId); requireAccess(thread.getProject(), context);
		return new ThreadCommentsData(comments.findByThreadIdOrderByIdAsc(threadId).stream().map(ThreadCommentResponse::from).toList());
	}
	@Transactional
	public ThreadCommentData createComment(String token, Long threadId, ThreadCommentCreateRequest request) {
		AuthContext context = active(token); User author = auth.requireActiveUser(context); Thread thread = thread(threadId); requireAccess(thread.getProject(), context);
		ThreadComment comment = comments.save(new ThreadComment(thread, author, request.content().trim(), now())); return new ThreadCommentData(ThreadCommentResponse.from(comment));
	}
	private void validateTargetProject(Project project, ThreadTargetType targetType, Long targetId) {
		Long targetProjectId = switch (targetType) {
			case ISSUE -> issues.findById(targetId).orElseThrow(() -> new IssueNotFoundException(targetId)).getProject().getId();
			case DOCUMENT -> documents.findByIdWithProject(targetId).orElseThrow(() -> new DocumentNotFoundException(targetId)).getProject().getId();
			case MESSAGE -> messages.findById(targetId).orElseThrow(() -> new MessageNotFoundException(targetId)).getProject().getId();
		};
		if (!project.getId().equals(targetProjectId)) throw new ThreadValidationException("쓰레드 대상은 요청 프로젝트에 속해야 합니다.");
	}
	private String normalizedTopic(String topic) { if (topic == null) return null; String value = topic.trim(); if (value.isEmpty() || value.length() > 200) throw new ThreadValidationException("topic은 trim 후 1~200자여야 합니다."); return value; }
	private AuthContext active(String token) { AuthContext context = auth.fromAccessToken(token); auth.requireActiveUser(context); return context; }
	private Project project(Long projectId) { return projects.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId)); }
	private Thread thread(Long threadId) { return threads.findByIdWithProject(threadId).orElseThrow(() -> new ThreadNotFoundException(threadId)); }
	private void requireAccess(Project project, AuthContext context) { if (companyMembership.hasActiveRole(project.getCompany().getId(), context, CompanyMemberRole.COMPANY_ADMIN)) return; companyMembership.requireActiveMember(project.getCompany().getId(), context); members.findByProject_IdAndUser_Id(project.getId(), context.userId()).filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE).orElseThrow(() -> new ForbiddenException("프로젝트 접근 권한이 없습니다.")); }
	private boolean canManage(Project project, AuthContext context) { if (companyMembership.hasActiveRole(project.getCompany().getId(), context, CompanyMemberRole.COMPANY_ADMIN)) return true; companyMembership.requireActiveMember(project.getCompany().getId(), context); return members.findByProject_IdAndUser_Id(project.getId(), context.userId()).filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE && member.getRole() == ProjectMemberRole.PROJECT_ADMIN).isPresent(); }
	private LocalDateTime now() { return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime(); }
}
