package com.threadia.backend.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
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
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;
import com.threadia.backend.search.dto.SearchData;
import com.threadia.backend.search.repository.ProjectSearchRepository;
import com.threadia.backend.search.repository.ProjectSearchRepository.SearchCursor;
import com.threadia.backend.search.repository.ProjectSearchRepository.SearchRow;

@ExtendWith(MockitoExtension.class)
class ProjectSearchServiceTest {

	@Mock
	private AuthContextService authContextService;
	@Mock
	private CompanyMembershipService companyMembershipService;
	@Mock
	private ProjectRepository projectRepository;
	@Mock
	private ProjectMemberRepository projectMemberRepository;
	@Mock
	private ProjectSearchRepository projectSearchRepository;

	private ProjectSearchService service;
	private ProjectSearchCursorCodec projectSearchCursorCodec;
	private AuthContext context;

	@BeforeEach
	void setUp() {
		projectSearchCursorCodec = new ProjectSearchCursorCodec("test-only-project-chat-cursor-hmac-secret");
		service = new ProjectSearchService(authContextService, companyMembershipService, projectRepository, projectMemberRepository, projectSearchRepository, projectSearchCursorCodec);
		context = new AuthContext(1L, "member@threadia.com");
		User user = new User("member@threadia.com", "hash", "멤버", null, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
		setId(user, 1L);
		Company company = new Company("Threadia", "대표", null, null, null, CompanyStatus.ACTIVE, user, LocalDateTime.now(), LocalDateTime.now());
		setId(company, 10L);
		Project project = new Project(company, "검색 프로젝트", "SRC", null, ProjectStatus.ACTIVE, null, null, user, null, null, null, LocalDateTime.now(), LocalDateTime.now());
		setId(project, 20L);
		when(authContextService.fromAccessToken("token")).thenReturn(context);
		when(authContextService.requireActiveUser(context)).thenReturn(user);
		when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
		when(companyMembershipService.hasActiveRole(10L, context, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true);
	}

	@Test
	void searchReturnsAllUnifiedSourceTypes() {
		when(projectSearchRepository.search(eq(20L), eq("%release%"), eq(null), eq(21))).thenReturn(List.of(
				row(1L, "ISSUE", "이슈", "설명", "2026-07-17T04:00:00"),
				row(2L, "DOCUMENT", "문서", "내용", "2026-07-17T03:00:00"),
				row(3L, "THREAD", "스레드", null, "2026-07-17T02:00:00"),
				row(4L, "PROJECT_MESSAGE", "메시지", "메시지", "2026-07-17T01:00:00")));

		SearchData data = service.search("token", 20L, " release ", null, null);

		assertThat(data.results()).extracting("type").containsExactly("ISSUE", "DOCUMENT", "THREAD", "PROJECT_MESSAGE");
		assertThat(data.results().get(0).sortAt()).isEqualTo(LocalDateTime.parse("2026-07-17T04:00:00"));
		assertThat(data.nextCursor()).isNull();
	}

	@Test
	void searchReturnsEmptyResultWhenNoTargetsMatch() {
		when(projectSearchRepository.search(20L, "%missing%", null, 21)).thenReturn(List.of());

		SearchData data = service.search("token", 20L, "missing", null, null);

		assertThat(data.results()).isEmpty();
		assertThat(data.nextCursor()).isNull();
	}

	@Test
	void searchRejectsUnauthenticatedActorBeforeRepositorySearch() {
		reset(authContextService, companyMembershipService, projectRepository, projectMemberRepository, projectSearchRepository);
		when(authContextService.fromAccessToken("bad-token")).thenThrow(new AuthenticationFailedException("인증에 실패했습니다."));

		assertThatThrownBy(() -> service.search("bad-token", 20L, "release", null, null))
				.isInstanceOf(AuthenticationFailedException.class);

		verify(projectSearchRepository, never()).search(any(), any(), any(), anyInt());
	}

	@Test
	void searchRejectsActorWithoutProjectAccessBeforeRepositorySearch() {
		reset(companyMembershipService);
		when(companyMembershipService.hasActiveRole(10L, context, CompanyMemberRole.COMPANY_ADMIN)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.search("token", 20L, "release", null, null))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("프로젝트 접근 권한이 없습니다.");

		verify(projectSearchRepository, never()).search(any(), any(), any(), anyInt());
	}

	@Test
	void searchEscapesLiteralLikeWildcardsAndBackslashes() {
		when(projectSearchRepository.search(eq(20L), eq("%100\\%\\_\\\\done%"), eq(null), eq(21))).thenReturn(List.of());

		service.search("token", 20L, "100%_\\done", null, null);

		verify(projectSearchRepository).search(20L, "%100\\%\\_\\\\done%", null, 21);
	}

	@Test
	void searchRejectsQueryLongerThanOneHundredTrimmedCharacters() {
		assertThatThrownBy(() -> service.search("token", 20L, " " + "a".repeat(101) + " ", null, null))
				.isInstanceOf(com.threadia.backend.project.exception.ProjectValidationException.class)
				.hasMessage("q는 100자 이하여야 합니다.");

		verify(projectSearchRepository, never()).search(any(), any(), any(), anyInt());
	}

	@Test
	void cursorPaginationDoesNotRepeatPriorPageResults() {
		SearchRow first = row(11L, "ISSUE", "첫 결과", "", "2026-07-17T04:00:00");
		SearchRow second = row(12L, "DOCUMENT", "둘째 결과", "", "2026-07-17T03:00:00");
		SearchRow third = row(13L, "PROJECT_MESSAGE", "셋째 결과", "", "2026-07-17T02:00:00");
		when(projectSearchRepository.search(20L, "%release%", null, 3)).thenReturn(List.of(first, second, third));

		SearchData firstPage = service.search("token", 20L, "release", null, 2);
		when(projectSearchRepository.search(eq(20L), eq("%release%"), any(SearchCursor.class), eq(3))).thenReturn(List.of(third));
		SearchData secondPage = service.search("token", 20L, "release", firstPage.nextCursor(), 2);

		assertThat(firstPage.results()).extracting("id").containsExactly(11L, 12L);
		assertThat(secondPage.results()).extracting("id").containsExactly(13L);
		assertThat(secondPage.results()).extracting("id").doesNotContainAnyElementsOf(firstPage.results().stream().map(result -> result.id()).toList());
	}

	@Test
	void searchRejectsCursorBoundToAnotherNormalizedQueryBeforeRepositorySearch() {
		String cursor = projectSearchCursorCodec.encode(1L, 20L, "release", row(13L, "PROJECT_MESSAGE", "셋째 결과", "", "2026-07-17T02:00:00"));

		assertThatThrownBy(() -> service.search("token", 20L, "other", cursor, 20))
				.isInstanceOf(com.threadia.backend.project.exception.ProjectValidationException.class)
				.hasMessage("cursor 형식이 올바르지 않습니다.");

		verify(projectSearchRepository, never()).search(any(), any(), any(), anyInt());
	}

	@Test
	void searchCapsSizeAtFifty() {
		when(projectSearchRepository.search(20L, "%release%", null, 51)).thenReturn(List.of());

		service.search("token", 20L, "release", null, 99);

		verify(projectSearchRepository).search(20L, "%release%", null, 51);
	}

	private static SearchRow row(Long id, String type, String title, String content, String sortAt) {
		return new SearchRow(id, type, title, content, LocalDateTime.parse(sortAt));
	}

	private static void setId(Object entity, Long id) {
		try {
			Field field = entity.getClass().getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
