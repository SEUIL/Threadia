package com.threadia.backend.activity.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threadia.backend.activity.domain.ActivityEvent;
import com.threadia.backend.activity.domain.ActivityEventType;
import com.threadia.backend.activity.domain.ActivityTargetType;
import com.threadia.backend.activity.dto.ActivityEventPageData;
import com.threadia.backend.activity.dto.ActivityEventResponse;
import com.threadia.backend.activity.repository.ActivityEventRepository;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.issue.domain.Issue;
import com.threadia.backend.issue.domain.IssueStatus;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@Service
public class ActivityEventService {

	private final AuthContextService auth;
	private final CompanyMembershipService companyMembership;
	private final ProjectRepository projects;
	private final ProjectMemberRepository members;
	private final ActivityEventRepository events;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	@Autowired
	public ActivityEventService(AuthContextService auth, CompanyMembershipService companyMembership,
			ProjectRepository projects, ProjectMemberRepository members, ActivityEventRepository events,
			ObjectMapper objectMapper) {
		this(auth, companyMembership, projects, members, events, objectMapper, Clock.systemUTC());
	}

	ActivityEventService(AuthContextService auth, CompanyMembershipService companyMembership,
			ProjectRepository projects, ProjectMemberRepository members, ActivityEventRepository events,
			ObjectMapper objectMapper, Clock clock) {
		this.auth = auth;
		this.companyMembership = companyMembership;
		this.projects = projects;
		this.members = members;
		this.events = events;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public ActivityEventPageData list(String token, Long projectId, int page, int size) {
		AuthContext context = auth.fromAccessToken(token);
		auth.requireActiveUser(context);
		Project project = projects.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
		requireProjectViewer(project, context);
		Page<ActivityEvent> result = events.findPageByProjectId(projectId,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		return new ActivityEventPageData(result.getContent().stream().map(ActivityEventResponse::from).toList(), page, size,
				result.getTotalElements());
	}

	public void recordIssueCreated(Issue issue, User actor) {
		record(issue, actor, ActivityEventType.ISSUE_CREATED, null, issue.getStatus());
	}

	public void recordIssueStatusChanged(Issue issue, User actor, IssueStatus fromStatus, IssueStatus toStatus) {
		record(issue, actor, ActivityEventType.ISSUE_STATUS_CHANGED, fromStatus, toStatus);
	}

	public void recordIssueTrashed(Issue issue, User actor) {
		record(issue, actor, ActivityEventType.ISSUE_TRASHED, null, null);
	}

	public void recordIssueRestored(Issue issue, User actor) {
		record(issue, actor, ActivityEventType.ISSUE_RESTORED, null, null);
	}

	private void record(Issue issue, User actor, ActivityEventType type, IssueStatus fromStatus, IssueStatus toStatus) {
		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("issueKey", issue.getIssueKey());
		metadata.put("title", issue.getTitle());
		if (issue.getAssignee() != null) metadata.put("assigneeUserId", issue.getAssignee().getId());
		if (fromStatus != null) metadata.put("fromStatus", fromStatus.name());
		if (toStatus != null) metadata.put("toStatus", toStatus.name());
		events.save(new ActivityEvent(issue.getProject().getCompany(), issue.getProject(), actor, type,
				ActivityTargetType.ISSUE, issue.getId(), metadata, now()));
	}

	private void requireProjectViewer(Project project, AuthContext context) {
		Long companyId = project.getCompany().getId();
		if (companyMembership.hasActiveRole(companyId, context, CompanyMemberRole.COMPANY_ADMIN)) return;
		companyMembership.requireActiveMember(companyId, context);
		members.findByProject_IdAndUser_Id(project.getId(), context.userId())
				.filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE)
				.map(ProjectMember::getRole)
				.orElseThrow(() -> new ForbiddenException("프로젝트 접근 권한이 없습니다."));
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime();
	}
}
