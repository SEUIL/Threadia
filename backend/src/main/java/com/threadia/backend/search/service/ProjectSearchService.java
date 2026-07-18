package com.threadia.backend.search.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.exception.ProjectNotFoundException;
import com.threadia.backend.project.exception.ProjectValidationException;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;
import com.threadia.backend.search.dto.SearchData;
import com.threadia.backend.search.dto.SearchResult;
import com.threadia.backend.search.repository.ProjectSearchRepository;
import com.threadia.backend.search.repository.ProjectSearchRepository.SearchCursor;
import com.threadia.backend.search.repository.ProjectSearchRepository.SearchRow;

@Service
public class ProjectSearchService {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 50;
	private static final int MAX_QUERY_LENGTH = 100;

	private final AuthContextService authContextService;
	private final CompanyMembershipService companyMembershipService;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectSearchRepository projectSearchRepository;
	private final ProjectSearchCursorCodec projectSearchCursorCodec;

	public ProjectSearchService(
			AuthContextService authContextService,
			CompanyMembershipService companyMembershipService,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			ProjectSearchRepository projectSearchRepository,
			ProjectSearchCursorCodec projectSearchCursorCodec) {
		this.authContextService = authContextService;
		this.companyMembershipService = companyMembershipService;
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.projectSearchRepository = projectSearchRepository;
		this.projectSearchCursorCodec = projectSearchCursorCodec;
	}

	@Transactional(readOnly = true)
	public SearchData search(String token, Long projectId, String q, String cursorToken, Integer requestedSize) {
		AuthContext context = activeAuthContext(token);
		requireProjectAccess(project(projectId), context);
		String query = validateQuery(q);
		SearchCursor cursor = cursorToken == null || cursorToken.isBlank()
				? null
				: projectSearchCursorCodec.decode(cursorToken, context.userId(), projectId, query);
		int size = normalizeSize(requestedSize);
		List<SearchRow> rows = projectSearchRepository.search(projectId, toLikePattern(query), cursor, size + 1);
		boolean hasNext = rows.size() > size;
		List<SearchRow> page = hasNext ? rows.subList(0, size) : rows;
		String nextCursor = hasNext ? projectSearchCursorCodec.encode(context.userId(), projectId, query, page.get(page.size() - 1)) : null;
		return new SearchData(page.stream().map(ProjectSearchService::toResult).toList(), nextCursor);
	}

	private AuthContext activeAuthContext(String token) {
		AuthContext context = authContextService.fromAccessToken(token);
		authContextService.requireActiveUser(context);
		return context;
	}

	private Project project(Long projectId) {
		return projectRepository.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
	}

	private void requireProjectAccess(Project project, AuthContext context) {
		Long companyId = project.getCompany().getId();
		if (companyMembershipService.hasActiveRole(companyId, context, CompanyMemberRole.COMPANY_ADMIN)) {
			return;
		}
		companyMembershipService.requireActiveMember(companyId, context);
		projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), context.userId())
				.filter(member -> member.getStatus() == ProjectMemberStatus.ACTIVE)
				.orElseThrow(() -> new ForbiddenException("프로젝트 접근 권한이 없습니다."));
	}

	private static String validateQuery(String q) {
		if (q == null || q.trim().isEmpty()) {
			throw new ProjectValidationException("q는 공백이 아닌 값이어야 합니다.");
		}
		String query = q.trim();
		if (query.length() > MAX_QUERY_LENGTH) {
			throw new ProjectValidationException("q는 100자 이하여야 합니다.");
		}
		return query;
	}

	private static int normalizeSize(Integer requestedSize) {
		if (requestedSize == null) {
			return DEFAULT_SIZE;
		}
		if (requestedSize < 1) {
			throw new ProjectValidationException("size는 1 이상이어야 합니다.");
		}
		return Math.min(requestedSize, MAX_SIZE);
	}

	private static String toLikePattern(String query) {
		return "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
	}

	private static SearchResult toResult(SearchRow row) {
		return new SearchResult(row.sourceId(), row.sourceType(), row.title(), snippet(row.content()), row.sortAt());
	}

	private static String snippet(String content) {
		if (content == null) {
			return null;
		}
		return content.length() <= 200 ? content : content.substring(0, 200);
	}

}
