package com.threadia.backend.issue.service;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.activity.service.ActivityEventService;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.*;
import com.threadia.backend.common.exception.*;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.issue.domain.*;
import com.threadia.backend.issue.dto.*;
import com.threadia.backend.issue.exception.*;
import com.threadia.backend.issue.repository.*;
import com.threadia.backend.project.domain.*;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.exception.ProjectValidationException;
import com.threadia.backend.project.repository.*;
import com.threadia.backend.projectchat.domain.Message;
import com.threadia.backend.projectchat.exception.MessageNotFoundException;
import com.threadia.backend.projectchat.repository.MessageRepository;
import com.threadia.backend.sprint.domain.*;
import com.threadia.backend.sprint.repository.SprintRepository;

@Service
public class IssueService {
	private final AuthContextService auth; private final CompanyMembershipService companyMembership;
	private final ProjectRepository projects; private final ProjectMemberRepository members; private final IssueRepository issues;
	private final IssueStatusHistoryRepository history; private final UserRepository users; private final SprintRepository sprints; private final Clock clock;
	private final MessageRepository messages; private final ActivityEventService activityEvents;

	public IssueService(AuthContextService auth, CompanyMembershipService companyMembership, ProjectRepository projects, ProjectMemberRepository members, IssueRepository issues, IssueStatusHistoryRepository history, UserRepository users, SprintRepository sprints) {
		this(auth, companyMembership, projects, members, issues, history, users, sprints, null, null, Clock.systemUTC());
	}
	@Autowired
	public IssueService(AuthContextService auth, CompanyMembershipService companyMembership, ProjectRepository projects, ProjectMemberRepository members, IssueRepository issues, IssueStatusHistoryRepository history, UserRepository users, SprintRepository sprints, MessageRepository messages, ActivityEventService activityEvents) {
		this(auth, companyMembership, projects, members, issues, history, users, sprints, messages, activityEvents, Clock.systemUTC());
	}
	IssueService(AuthContextService auth, CompanyMembershipService companyMembership, ProjectRepository projects, ProjectMemberRepository members, IssueRepository issues, IssueStatusHistoryRepository history, Clock clock) {
		this(auth, companyMembership, projects, members, issues, history, null, null, null, null, clock);
	}
	IssueService(AuthContextService auth, CompanyMembershipService companyMembership, ProjectRepository projects, ProjectMemberRepository members, IssueRepository issues, IssueStatusHistoryRepository history, UserRepository users, SprintRepository sprints, Clock clock) {
		this(auth, companyMembership, projects, members, issues, history, users, sprints, null, null, clock);
	}
	IssueService(AuthContextService auth, CompanyMembershipService companyMembership, ProjectRepository projects, ProjectMemberRepository members, IssueRepository issues, IssueStatusHistoryRepository history, UserRepository users, SprintRepository sprints, ActivityEventService activityEvents, Clock clock) {
		this(auth, companyMembership, projects, members, issues, history, users, sprints, null, activityEvents, clock);
	}
	IssueService(AuthContextService auth, CompanyMembershipService companyMembership, ProjectRepository projects, ProjectMemberRepository members, IssueRepository issues, IssueStatusHistoryRepository history, UserRepository users, SprintRepository sprints, MessageRepository messages, ActivityEventService activityEvents, Clock clock) {
		this.auth=auth; this.companyMembership=companyMembership; this.projects=projects; this.members=members; this.issues=issues; this.history=history; this.users=users; this.sprints=sprints; this.messages=messages; this.activityEvents=activityEvents; this.clock=clock;
	}

	@Transactional(readOnly=true)
	public IssuesData list(String token, Long projectId) { return list(token, projectId, null, null, null, false, false); }
	@Transactional(readOnly=true)
	public IssuesData list(String token, Long projectId, IssueStatus status, Long sprintId, Long assigneeId, boolean includeTrashed) { return list(token, projectId, status, sprintId, assigneeId, false, includeTrashed); }
	@Transactional(readOnly=true)
	public IssuesData list(String token, Long projectId, IssueStatus status, Long sprintId, Long assigneeId, boolean unassignedOnly, boolean includeTrashed) {
		AuthContext context=active(token); requireAccess(project(projectId), context);
		if (unassignedOnly && sprintId != null) throw new ProjectValidationException("unassignedOnly와 sprintId는 함께 사용할 수 없습니다.");
		List<Issue> result = status == null && sprintId == null && assigneeId == null && !unassignedOnly && !includeTrashed
				? issues.findActiveByProjectIdInDisplayOrder(projectId)
				: issues.findByFiltersInDisplayOrder(projectId,status,sprintId,assigneeId,unassignedOnly,includeTrashed);
		return new IssuesData(result.stream().map(IssueResponse::from).toList());
	}
	@Transactional(readOnly=true)
	public IssueData detail(String token, Long issueId) { AuthContext context=active(token); Issue issue=issue(issueId); requireAccess(issue.getProject(),context); return new IssueData(IssueResponse.from(issue)); }
	@Transactional
	public IssueData create(String token, Long projectId, IssueCreateRequest request) {
		AuthContext context=auth.fromAccessToken(token); User actor=auth.requireActiveUser(context); Project project=projectForUpdate(projectId); ProjectMemberRole role=requireAccess(project,context);
		if (!canManage(role) && hasManagerOnlyFields(request)) throw new ForbiddenException("이슈 생성 권한이 없습니다.");
		Sprint sprint = canManage(role) && request.sprintId()!=null ? activeSprintForUpdate(project,request.sprintId()) : null;
		long sequence=nextSequence(project.getProjectKey(),issues.findByProjectIdForSequenceUpdate(projectId)); LocalDateTime now=now();
		Issue issue=new Issue(project,project.getProjectKey()+"-"+sequence,actor,request.title().trim(),IssueStatus.BACKLOG,now,now);
		issue.setDescription(request.description());
		if (canManage(role)) { applyCreateManagementExtras(issue, project, request, sprint); }
		issues.save(issue); history.save(new IssueStatusHistory(issue,actor,null,IssueStatus.BACKLOG,now)); recordCreated(issue, actor); return new IssueData(IssueResponse.from(issue));
	}
	@Transactional
	public IssueData createFromMessage(String token, Long projectId, IssueFromMessageRequest request) {
		if (messages==null) throw new IllegalStateException("Message repository is unavailable");
		AuthContext context=auth.fromAccessToken(token); User actor=auth.requireActiveUser(context); Project project=projectForUpdate(projectId); ProjectMemberRole role=requireAccess(project,context);
		if (!canManage(role)) throw new ForbiddenException("이슈 생성 권한이 없습니다.");
		Message message=messages.findByIdAndProject_Id(request.messageId(), projectId).orElseThrow(() -> new MessageNotFoundException(request.messageId()));
		Sprint sprint = canManage(role) && request.sprintId()!=null ? activeSprintForUpdate(project,request.sprintId()) : null;
		long sequence=nextSequence(project.getProjectKey(),issues.findByProjectIdForSequenceUpdate(projectId)); LocalDateTime now=now();
		Issue issue=new Issue(project,project.getProjectKey()+"-"+sequence,actor,request.title().trim(),IssueStatus.BACKLOG,now,now);
		issue.setDescription(request.description()); issue.setSourceMessage(message);
		if (canManage(role)) { applyCreateManagementExtras(issue, project, request, sprint); }
		issues.save(issue); history.save(new IssueStatusHistory(issue,actor,null,IssueStatus.BACKLOG,now)); recordCreated(issue, actor); return new IssueData(IssueResponse.from(issue));
	}
	@Transactional
	public IssueData update(String token, Long issueId, IssueUpdateRequest request, Set<String> fields) {
		AuthContext context=auth.fromAccessToken(token); auth.requireActiveUser(context); Issue issue=issueForUpdate(issueId); ProjectMemberRole role=requireAccess(issue.getProject(),context); rejectTrashed(issue);
		if (!canManage(role) && fields.stream().anyMatch(field -> !field.equals("title") && !field.equals("description"))) throw new ForbiddenException("이슈 수정 권한이 없습니다.");
		if (fields.contains("title")) { if (request.title()==null || request.title().isBlank()) throw new ProjectValidationException("title은 필수입니다."); if (request.title().trim().length()>200) throw new ProjectValidationException("title은 200자 이하여야 합니다."); issue.setTitle(request.title().trim()); }
		if (fields.contains("description")) issue.setDescription(request.description());
		if (fields.contains("priority")) { if (request.priority()==null) throw new ProjectValidationException("priority는 필수입니다."); issue.setPriority(request.priority()); }
		if (fields.contains("assigneeUserId")) issue.setAssignee(request.assigneeUserId()==null ? null : activeProjectUser(issue.getProject(),request.assigneeUserId()));
		if (fields.contains("dueDate")) issue.setDueDate(request.dueDate());
		if (fields.contains("sprintId")) { if (request.sprintId()!=null) throw new ProjectValidationException("Sprint 할당은 POST /sprints/{id}/issues API를 사용해야 합니다."); issue.setSprint(null); }
		issue.setUpdatedAt(now()); return new IssueData(IssueResponse.from(issue));
	}
	@Transactional
	public IssueData updateStatus(String token, Long issueId, IssueStatusUpdateRequest request) {
		AuthContext context=auth.fromAccessToken(token); User actor=auth.requireActiveUser(context); Issue issue=issueForUpdate(issueId); ProjectMemberRole role=requireAccess(issue.getProject(),context); rejectTrashed(issue);
		if (!canManage(role) && request.status()==IssueStatus.DONE) throw new ForbiddenException("일반 프로젝트 멤버는 이슈를 DONE 상태로 변경할 수 없습니다.");
		if (issue.getStatus()==request.status()) return new IssueData(IssueResponse.from(issue));
		IssueStatus before=issue.getStatus(); LocalDateTime now=now(); issue.setStatus(request.status()); issue.setUpdatedAt(now); history.save(new IssueStatusHistory(issue,actor,before,request.status(),now)); recordStatusChanged(issue, actor, before, request.status()); return new IssueData(IssueResponse.from(issue));
	}
	@Transactional public IssueData trash(String token, Long issueId) { return trashOrRestore(token,issueId,true); }
	@Transactional public IssueData restore(String token, Long issueId) { return trashOrRestore(token,issueId,false); }
	private IssueData trashOrRestore(String token, Long issueId, boolean trash) { AuthContext context=auth.fromAccessToken(token); User actor=auth.requireActiveUser(context); Issue issue=issueForUpdate(issueId); ProjectMemberRole role=requireAccess(issue.getProject(),context); if(!canManage(role)&&!issue.getReporter().getId().equals(actor.getId())) throw new ForbiddenException("이슈 등록자 또는 관리자만 처리할 수 있습니다."); if(trash&&issue.getTrashedAt()==null) { issue.trash(actor,now()); recordTrashed(issue, actor); } if(!trash&&issue.getTrashedAt()!=null) { issue.restore(now()); recordRestored(issue, actor); } return new IssueData(IssueResponse.from(issue)); }
	private void recordCreated(Issue issue, User actor) { if (activityEvents != null) activityEvents.recordIssueCreated(issue, actor); }
	private void recordStatusChanged(Issue issue, User actor, IssueStatus fromStatus, IssueStatus toStatus) { if (activityEvents != null) activityEvents.recordIssueStatusChanged(issue, actor, fromStatus, toStatus); }
	private void recordTrashed(Issue issue, User actor) { if (activityEvents != null) activityEvents.recordIssueTrashed(issue, actor); }
	private void recordRestored(Issue issue, User actor) { if (activityEvents != null) activityEvents.recordIssueRestored(issue, actor); }
	private void applyCreateManagementExtras(Issue issue, Project project, IssueCreateRequest request, Sprint sprint) { if(request.priority()!=null) issue.setPriority(request.priority()); if(request.assigneeUserId()!=null) issue.setAssignee(activeProjectUser(project,request.assigneeUserId())); if(request.dueDate()!=null) issue.setDueDate(request.dueDate()); if(sprint!=null) issue.setSprint(sprint); }
	private boolean hasManagerOnlyFields(IssueCreateRequest request) { return request.priority()!=null || request.assigneeUserId()!=null || request.dueDate()!=null || request.sprintId()!=null; }
	private void applyCreateManagementExtras(Issue issue, Project project, IssueFromMessageRequest request, Sprint sprint) { if(request.priority()!=null) issue.setPriority(request.priority()); if(request.assigneeUserId()!=null) issue.setAssignee(activeProjectUser(project,request.assigneeUserId())); if(request.dueDate()!=null) issue.setDueDate(request.dueDate()); if(sprint!=null) issue.setSprint(sprint); }
	private boolean hasManagerOnlyFields(IssueFromMessageRequest request) { return request.priority()!=null || request.assigneeUserId()!=null || request.dueDate()!=null || request.sprintId()!=null; }
	private Sprint activeSprintForUpdate(Project project, Long sprintId) { if(sprints==null) throw new IllegalStateException("Sprint repository is unavailable"); Sprint sprint=sprints.findByIdForUpdate(sprintId).orElseThrow(() -> new ProjectValidationException("Sprint를 찾을 수 없습니다.")); if(!sprint.getProject().getId().equals(project.getId())||sprint.getStatus()==SprintStatus.COMPLETED) throw new ProjectValidationException("같은 프로젝트의 완료되지 않은 Sprint만 지정할 수 있습니다."); return sprint; }
	private User activeProjectUser(Project project, Long userId) { if(!members.existsByProject_IdAndUser_IdAndStatus(project.getId(),userId,ProjectMemberStatus.ACTIVE)) throw new ProjectValidationException("담당자는 활성 프로젝트 구성원이어야 합니다."); if(users==null) throw new IllegalStateException("User repository is unavailable"); return users.findById(userId).orElseThrow(() -> new ProjectValidationException("사용자를 찾을 수 없습니다.")); }
	private void rejectTrashed(Issue issue) { if(issue.getTrashedAt()!=null) throw IssueConflictException.trashed(); }
	private boolean canManage(ProjectMemberRole role) { return role==null || role==ProjectMemberRole.PROJECT_ADMIN; }
	private ProjectMemberRole requireAccess(Project project, AuthContext context) { Long companyId=project.getCompany().getId(); if(companyMembership.hasActiveRole(companyId,context,CompanyMemberRole.COMPANY_ADMIN)) return null; companyMembership.requireActiveMember(companyId,context); return members.findByProject_IdAndUser_Id(project.getId(),context.userId()).filter(member -> member.getStatus()==ProjectMemberStatus.ACTIVE).map(ProjectMember::getRole).orElseThrow(() -> new ForbiddenException("프로젝트 접근 권한이 없습니다.")); }
	private AuthContext active(String token) { AuthContext context=auth.fromAccessToken(token); auth.requireActiveUser(context); return context; }
	private Project project(Long id) { return projects.findById(id).orElseThrow(() -> new ProjectNotFoundException(id)); }
	private Project projectForUpdate(Long id) { return projects.findByIdForUpdate(id).orElseThrow(() -> new ProjectNotFoundException(id)); }
	private Issue issue(Long id) { return issues.findById(id).orElseThrow(() -> new IssueNotFoundException(id)); }
	private Issue issueForUpdate(Long id) { return issues.findByIdForUpdate(id).orElseThrow(() -> new IssueNotFoundException(id)); }
	private static long nextSequence(String key,List<Issue> all) { String prefix=key+"-"; long max=0; for(Issue issue:all) { String value=issue.getIssueKey(); if(value==null||!value.startsWith(prefix))continue; try { String suffix=value.substring(prefix.length()); if(!suffix.isEmpty()&&suffix.chars().allMatch(Character::isDigit)) max=Math.max(max,Long.parseLong(suffix)); } catch(NumberFormatException ignored) {} } return Math.incrementExact(max); }
	private LocalDateTime now() { return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime(); }
}
