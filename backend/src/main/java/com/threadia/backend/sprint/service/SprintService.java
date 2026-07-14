package com.threadia.backend.sprint.service;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.*;
import com.threadia.backend.common.exception.*;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.issue.domain.*;
import com.threadia.backend.issue.repository.*;
import com.threadia.backend.project.domain.*;
import com.threadia.backend.project.exception.*;
import com.threadia.backend.project.repository.*;
import com.threadia.backend.sprint.domain.*;
import com.threadia.backend.sprint.dto.*;
import com.threadia.backend.sprint.exception.*;
import com.threadia.backend.sprint.repository.SprintRepository;

@Service
public class SprintService {
	private final AuthContextService auth; private final CompanyMembershipService companyMembership; private final ProjectRepository projects; private final ProjectMemberRepository members; private final SprintRepository sprints; private final IssueRepository issues; private final IssueStatusHistoryRepository history; private final Clock clock;
	@Autowired
	public SprintService(AuthContextService auth, CompanyMembershipService companyMembership, ProjectRepository projects, ProjectMemberRepository members, SprintRepository sprints, IssueRepository issues, IssueStatusHistoryRepository history) { this(auth,companyMembership,projects,members,sprints,issues,history,Clock.systemUTC()); }
	SprintService(AuthContextService auth, CompanyMembershipService companyMembership, ProjectRepository projects, ProjectMemberRepository members, SprintRepository sprints, IssueRepository issues, IssueStatusHistoryRepository history, Clock clock) { this.auth=auth; this.companyMembership=companyMembership; this.projects=projects; this.members=members; this.sprints=sprints; this.issues=issues; this.history=history; this.clock=clock; }
	@Transactional(readOnly=true) public SprintsData list(String token, Long projectId) { ActiveUser active=active(token); requireAccess(project(projectId),active.context()); return new SprintsData(sprints.findByProject_IdOrderByIdAsc(projectId).stream().map(SprintResponse::from).toList()); }
	@Transactional public SprintData create(String token, Long projectId, SprintCreateRequest request) { ActiveUser active=active(token); Project project=projectForUpdate(projectId); requireManage(project,active.context()); dates(request.startDate(),request.endDate()); Sprint sprint=sprints.save(new Sprint(project,request.name().trim(),request.goal(),request.startDate(),request.endDate(),now())); return new SprintData(SprintResponse.from(sprint)); }
	@Transactional(readOnly=true) public SprintData detail(String token, Long id) { ActiveUser active=active(token); Sprint sprint=sprint(id); requireAccess(sprint.getProject(),active.context()); return new SprintData(SprintResponse.from(sprint)); }
	@Transactional public SprintData update(String token, Long id, SprintUpdateRequest request) { ActiveUser active=active(token); Sprint sprint=lockSprintInProjectOrder(id); requireManage(sprint.getProject(),active.context()); if(sprint.getStatus()!=SprintStatus.PLANNED) throw new SprintConflictException("PLANNED Sprint만 수정할 수 있습니다."); dates(request.startDate(),request.endDate()); sprint.update(request.name().trim(),request.goal(),request.startDate(),request.endDate(),now()); return new SprintData(SprintResponse.from(sprint)); }
	@Transactional public SprintData start(String token, Long id) { ActiveUser active=active(token); Sprint sprint=lockSprintInProjectOrder(id); requireManage(sprint.getProject(),active.context()); if(sprint.getStatus()!=SprintStatus.PLANNED) throw new SprintConflictException("PLANNED Sprint만 시작할 수 있습니다."); if(sprints.existsByProject_IdAndStatus(sprint.getProject().getId(),SprintStatus.ACTIVE)) throw new SprintConflictException("프로젝트에는 ACTIVE Sprint가 하나만 있을 수 있습니다."); sprint.start(now()); return new SprintData(SprintResponse.from(sprint)); }
	@Transactional public SprintData assignIssues(String token, Long id, SprintIssueAssignmentRequest request) { ActiveUser active=active(token); Sprint sprint=lockSprintInProjectOrder(id); requireManage(sprint.getProject(),active.context()); if(sprint.getStatus()==SprintStatus.COMPLETED) throw new SprintConflictException("COMPLETED Sprint에는 이슈를 배정할 수 없습니다."); List<Long> ids=request.issueIds(); if(new HashSet<>(ids).size()!=ids.size()) throw new ProjectValidationException("중복된 이슈 ID가 있습니다."); List<Issue> selected=issues.findAllByIdInForUpdate(ids); if(selected.size()!=ids.size()) throw new ProjectValidationException("이슈를 찾을 수 없습니다."); for(Issue issue:selected) { if(!issue.getProject().getId().equals(sprint.getProject().getId())||issue.getTrashedAt()!=null) throw new ProjectValidationException("같은 프로젝트의 활성 이슈만 배정할 수 있습니다."); issue.setSprint(sprint); issue.setUpdatedAt(now()); } return new SprintData(SprintResponse.from(sprint)); }
	@Transactional public SprintData complete(String token, Long id, SprintCompleteRequest request) { ActiveUser active=active(token); Sprint sprint=lockSprintInProjectOrder(id); requireManage(sprint.getProject(),active.context()); if(sprint.getStatus()!=SprintStatus.ACTIVE) throw new SprintConflictException("ACTIVE Sprint만 완료할 수 있습니다."); LocalDateTime now=now(); for(Issue issue:issues.findBySprintIdForUpdate(id)) { issue.setSprint(null); if(issue.getStatus()!=IssueStatus.DONE && issue.getStatus()!=IssueStatus.BACKLOG) { IssueStatus before=issue.getStatus(); issue.setStatus(IssueStatus.BACKLOG); history.save(new IssueStatusHistory(issue,active.user(),before,IssueStatus.BACKLOG,now)); } issue.setUpdatedAt(now); } sprint.complete(request.summary(),now); return new SprintData(SprintResponse.from(sprint)); }
	private void dates(LocalDate start,LocalDate end) { if(start!=null&&end!=null&&start.isAfter(end)) throw ProjectValidationException.invalidDateRange(); }
	private void requireManage(Project project,AuthContext context) { ProjectMemberRole role=requireAccess(project,context); if(role==ProjectMemberRole.MEMBER) throw new ForbiddenException("프로젝트 관리 권한이 없습니다."); }
	private ProjectMemberRole requireAccess(Project project,AuthContext context) { Long companyId=project.getCompany().getId(); if(companyMembership.hasActiveRole(companyId,context,CompanyMemberRole.COMPANY_ADMIN))return null; companyMembership.requireActiveMember(companyId,context); return members.findByProject_IdAndUser_Id(project.getId(),context.userId()).filter(member->member.getStatus()==ProjectMemberStatus.ACTIVE).map(ProjectMember::getRole).orElseThrow(()->new ForbiddenException("프로젝트 접근 권한이 없습니다.")); }
	private ActiveUser active(String token) { AuthContext context=auth.fromAccessToken(token); return new ActiveUser(context,auth.requireActiveUser(context)); }
	private Project project(Long id) { return projects.findById(id).orElseThrow(()->new ProjectNotFoundException(id)); }
	private Project projectForUpdate(Long id) { return projects.findByIdForUpdate(id).orElseThrow(()->new ProjectNotFoundException(id)); }
	private Sprint sprint(Long id) { return sprints.findById(id).orElseThrow(()->new SprintNotFoundException(id)); }
	private Sprint sprintForUpdate(Long id) { return sprints.findByIdForUpdate(id).orElseThrow(()->new SprintNotFoundException(id)); }
	private Sprint lockSprintInProjectOrder(Long id) { Sprint preview=sprint(id); projectForUpdate(preview.getProject().getId()); return sprintForUpdate(id); }
	private LocalDateTime now() { return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime(); }
	private record ActiveUser(AuthContext context, User user) {}
}
