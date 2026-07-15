package com.threadia.backend.sprint.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.threadia.backend.auth.domain.*;
import com.threadia.backend.common.auth.*;
import com.threadia.backend.company.domain.*;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.issue.domain.*;
import com.threadia.backend.issue.repository.*;
import com.threadia.backend.project.domain.*;
import com.threadia.backend.project.repository.*;
import com.threadia.backend.sprint.domain.*;
import com.threadia.backend.sprint.dto.*;
import com.threadia.backend.sprint.exception.SprintConflictException;
import com.threadia.backend.sprint.repository.SprintRepository;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

@ExtendWith(MockitoExtension.class)
class SprintServiceTest {
	private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"),ZoneOffset.UTC);
	@Mock private AuthContextService auth; @Mock private CompanyMembershipService companyMembership; @Mock private ProjectRepository projects;
	@Mock private ProjectMemberRepository members; @Mock private SprintRepository sprints; @Mock private IssueRepository issues; @Mock private IssueStatusHistoryRepository history;
	private SprintService service; private User user; private Project project; private AuthContext context;
	@BeforeEach void setUp() { service=new SprintService(auth,companyMembership,projects,members,sprints,issues,history,CLOCK); LocalDateTime now=LocalDateTime.now(CLOCK); user=new User("member@example.com","hash","member",null,UserStatus.ACTIVE,now,now); id(user,1L); Company company=new Company("Threadia",null,null,null,null,"ACTIVE",user,now,now); id(company,10L); project=new Project(company,"MVP","THD",null,ProjectStatus.ACTIVE,null,null,user,null,null,null,now,now); id(project,20L); context=new AuthContext(1L,"member@example.com"); lenient().when(auth.fromAccessToken("token")).thenReturn(context); lenient().when(auth.requireActiveUser(context)).thenReturn(user); lenient().when(companyMembership.hasActiveRole(10L,context,CompanyMemberRole.COMPANY_ADMIN)).thenReturn(true); }
	@Test void startRejectsSecondActiveSprintAfterProjectLock() {
		Sprint sprint=sprint(SprintStatus.PLANNED); when(sprints.findById(30L)).thenReturn(Optional.of(sprint)); when(sprints.findByIdForUpdate(30L)).thenReturn(Optional.of(sprint)); when(projects.findByIdForUpdate(20L)).thenReturn(Optional.of(project)); when(sprints.existsByProject_IdAndStatus(20L,SprintStatus.ACTIVE)).thenReturn(true);
		assertThatThrownBy(()->service.start("token",30L)).isInstanceOf(SprintConflictException.class);
		verify(projects).findByIdForUpdate(20L); assertThat(sprint.getStatus()).isEqualTo(SprintStatus.PLANNED);
	}
	@Test void completeClearsSprintAndRollsOnlyNonDoneIssuesBackToBacklog() {
		Sprint sprint=sprint(SprintStatus.ACTIVE); Issue inProgress=issue("THD-1",IssueStatus.IN_PROGRESS); Issue backlog=issue("THD-2",IssueStatus.BACKLOG); Issue done=issue("THD-3",IssueStatus.DONE);
		when(sprints.findById(30L)).thenReturn(Optional.of(sprint)); when(projects.findByIdForUpdate(20L)).thenReturn(Optional.of(project)); when(sprints.findByIdForUpdate(30L)).thenReturn(Optional.of(sprint)); when(issues.findBySprintIdForUpdate(30L)).thenReturn(List.of(inProgress,backlog,done));
		SprintData data=service.complete("token",30L,new SprintCompleteRequest("closed"));
		assertThat(data.sprint().status()).isEqualTo("COMPLETED"); assertThat(inProgress.getStatus()).isEqualTo(IssueStatus.BACKLOG); assertThat(inProgress.getSprint()).isNull(); assertThat(backlog.getStatus()).isEqualTo(IssueStatus.BACKLOG); assertThat(backlog.getSprint()).isNull(); assertThat(done.getStatus()).isEqualTo(IssueStatus.DONE); assertThat(done.getSprint()).isNull(); verify(history,times(1)).save(any(IssueStatusHistory.class));
	}
	@Test void invalidAuthenticationPreventsEverySprintResourceLookupOrLock() {
		when(auth.fromAccessToken("invalid-token")).thenThrow(new JwtAuthenticationException("유효하지 않은 access token입니다."));

		assertThatThrownBy(()->service.list("invalid-token",20L)).isInstanceOf(JwtAuthenticationException.class);
		assertThatThrownBy(()->service.create("invalid-token",20L,new SprintCreateRequest("Sprint",null,null,null))).isInstanceOf(JwtAuthenticationException.class);
		assertThatThrownBy(()->service.detail("invalid-token",30L)).isInstanceOf(JwtAuthenticationException.class);
		assertThatThrownBy(()->service.update("invalid-token",30L,new SprintUpdateRequest("Sprint",null,null,null))).isInstanceOf(JwtAuthenticationException.class);
		assertThatThrownBy(()->service.start("invalid-token",30L)).isInstanceOf(JwtAuthenticationException.class);
		assertThatThrownBy(()->service.assignIssues("invalid-token",30L,new SprintIssueAssignmentRequest(List.of(100L)))).isInstanceOf(JwtAuthenticationException.class);
		assertThatThrownBy(()->service.complete("invalid-token",30L,new SprintCompleteRequest(null))).isInstanceOf(JwtAuthenticationException.class);

		verifyNoInteractions(projects,sprints,issues);
	}
	private Sprint sprint(SprintStatus status) { Sprint sprint=new Sprint(project,"Sprint",null,null,null,LocalDateTime.now(CLOCK)); id(sprint,30L); if(status==SprintStatus.ACTIVE)sprint.start(LocalDateTime.now(CLOCK)); return sprint; }
	private Issue issue(String key,IssueStatus status) { Issue issue=new Issue(project,key,user,key,status,LocalDateTime.now(CLOCK),LocalDateTime.now(CLOCK)); issue.setSprint(sprint(SprintStatus.ACTIVE)); return issue; }
	private static void id(Object value,Long id) { try { Field field=value.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(value,id); } catch(ReflectiveOperationException exception) { throw new AssertionError(exception); } }
}
