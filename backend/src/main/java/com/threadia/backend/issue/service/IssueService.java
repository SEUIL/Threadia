package com.threadia.backend.issue.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.issue.domain.Issue;
import com.threadia.backend.issue.domain.IssueStatus;
import com.threadia.backend.issue.domain.IssueStatusHistory;
import com.threadia.backend.issue.dto.IssueCreateRequest;
import com.threadia.backend.issue.dto.IssueData;
import com.threadia.backend.issue.dto.IssueResponse;
import com.threadia.backend.issue.dto.IssueStatusUpdateRequest;
import com.threadia.backend.issue.dto.IssuesData;
import com.threadia.backend.issue.exception.IssueNotFoundException;
import com.threadia.backend.issue.repository.IssueRepository;
import com.threadia.backend.issue.repository.IssueStatusHistoryRepository;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@Service
public class IssueService {

	private final AuthContextService authContextService;
	private final CompanyMembershipService companyMembershipService;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final IssueRepository issueRepository;
	private final IssueStatusHistoryRepository issueStatusHistoryRepository;
	private final Clock clock;

	@Autowired
	public IssueService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			IssueRepository issueRepository,
			IssueStatusHistoryRepository issueStatusHistoryRepository) {
		this(
				authContextService,
				companyMembershipService,
				projectRepository,
				projectMemberRepository,
				issueRepository,
				issueStatusHistoryRepository,
				Clock.systemUTC());
	}

	IssueService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			IssueRepository issueRepository,
			IssueStatusHistoryRepository issueStatusHistoryRepository,
			Clock clock) {
		this.authContextService = authContextService;
		this.companyMembershipService = companyMembershipService;
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.issueRepository = issueRepository;
		this.issueStatusHistoryRepository = issueStatusHistoryRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public IssuesData list(String accessToken, Long projectId) {
		AuthContext authContext = activeAuthContext(accessToken);
		Project project = project(projectId);
		requireProjectAccess(project, authContext);
		return new IssuesData(issueRepository.findActiveByProjectIdInDisplayOrder(projectId)
				.stream()
				.map(IssueResponse::from)
				.toList());
	}

	@Transactional
	public IssueData create(String accessToken, Long projectId, IssueCreateRequest request) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		User reporter = authContextService.requireActiveUser(authContext);
		Project project = projectForUpdate(projectId);
		requireProjectAccess(project, authContext);
		long nextSequence = nextIssueSequence(project.getProjectKey(), issueRepository.findByProjectIdForSequenceUpdate(projectId));
		LocalDateTime now = now();
		Issue issue = issueRepository.save(new Issue(
				project,
				project.getProjectKey() + "-" + nextSequence,
				reporter,
				request.title().trim(),
				IssueStatus.BACKLOG,
				now,
				now));
		issueStatusHistoryRepository.save(new IssueStatusHistory(issue, reporter, null, IssueStatus.BACKLOG, now));
		return new IssueData(IssueResponse.from(issue));
	}

	@Transactional
	public IssueData updateStatus(String accessToken, Long issueId, IssueStatusUpdateRequest request) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		User actor = authContextService.requireActiveUser(authContext);
		Issue issue = issueForUpdate(issueId);
		ProjectMemberRole role = requireProjectAccess(issue.getProject(), authContext);
		if (request.status() == IssueStatus.DONE && role == ProjectMemberRole.MEMBER) {
			throw new ForbiddenException("일반 프로젝트 멤버는 이슈를 DONE 상태로 변경할 수 없습니다.");
		}
		IssueStatus previousStatus = issue.getStatus();
		if (previousStatus == request.status()) {
			return new IssueData(IssueResponse.from(issue));
		}
		LocalDateTime now = now();
		issue.setStatus(request.status());
		issue.setUpdatedAt(now);
		issueStatusHistoryRepository.save(new IssueStatusHistory(issue, actor, previousStatus, request.status(), now));
		return new IssueData(IssueResponse.from(issue));
	}

	private ProjectMemberRole requireProjectAccess(Project project, AuthContext authContext) {
		Long companyId = project.getCompany().getId();
		if (companyMembershipService.hasActiveRole(companyId, authContext, CompanyMemberRole.COMPANY_ADMIN)) {
			return null;
		}
		companyMembershipService.requireActiveMember(companyId, authContext);
		ProjectMember projectMember = projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), authContext.userId())
				.filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE)
				.orElseThrow(() -> new ForbiddenException("프로젝트 접근 권한이 없습니다."));
		return projectMember.getRole();
	}

	private AuthContext activeAuthContext(String accessToken) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(authContext);
		return authContext;
	}

	private Project project(Long projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new ProjectNotFoundException(projectId));
	}

	private Project projectForUpdate(Long projectId) {
		return projectRepository.findByIdForUpdate(projectId)
				.orElseThrow(() -> new ProjectNotFoundException(projectId));
	}

	private Issue issueForUpdate(Long issueId) {
		return issueRepository.findByIdForUpdate(issueId)
				.orElseThrow(() -> new IssueNotFoundException(issueId));
	}

	private static long nextIssueSequence(String projectKey, List<Issue> issues) {
		String prefix = projectKey + "-";
		long maxSequence = 0;
		for (Issue issue : issues) {
			String issueKey = issue.getIssueKey();
			if (issueKey == null || !issueKey.startsWith(prefix)) {
				continue;
			}
			String suffix = issueKey.substring(prefix.length());
			if (suffix.isEmpty() || !suffix.chars().allMatch(Character::isDigit)) {
				continue;
			}
			try {
				long sequence = Long.parseLong(suffix);
				if (sequence > maxSequence) {
					maxSequence = sequence;
				}
			} catch (NumberFormatException exception) {
				continue;
			}
		}
		return Math.incrementExact(maxSequence);
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock).atOffset(ZoneOffset.UTC).toLocalDateTime();
	}
}
