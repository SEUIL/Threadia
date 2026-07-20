package com.threadia.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.common.exception.UserNotFoundException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenService;
import com.threadia.backend.invitation.domain.Invitation;
import com.threadia.backend.invitation.domain.InvitationScopeType;
import com.threadia.backend.invitation.domain.InvitationStatus;
import com.threadia.backend.invitation.dto.ProjectInvitationRequest;
import com.threadia.backend.invitation.exception.InvitationConflictException;
import com.threadia.backend.invitation.repository.InvitationRepository;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectMember;
import com.threadia.backend.project.domain.ProjectMemberRole;
import com.threadia.backend.project.domain.ProjectMemberStatus;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.repository.ProjectMemberRepository;
import com.threadia.backend.project.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class ProjectInvitationServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-07T10:15:30Z"), ZoneOffset.UTC);

	@Mock private JwtTokenService jwtTokenService;
	@Mock private UserRepository userRepository;
	@Mock private CompanyMemberRepository companyMemberRepository;
	@Mock private ProjectRepository projectRepository;
	@Mock private ProjectMemberRepository projectMemberRepository;
	@Mock private InvitationRepository invitationRepository;

	private ProjectInvitationService service;

	@BeforeEach
	void setUp() {
		service = new ProjectInvitationService(
				jwtTokenService, userRepository, companyMemberRepository, projectRepository,
				projectMemberRepository, invitationRepository, FIXED_CLOCK);
	}

	@Test
	void createLocksProjectAndCreatesProjectScopedInvitationForActiveCompanyMember() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		stubProjectAdmin(requester, project);
		when(userRepository.findById(invitee.getId())).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, invitee.getId(), CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(10L, invitee.getId(), ProjectMemberStatus.ACTIVE)).thenReturn(false);
		when(invitationRepository.findAllByProjectIdAndInvitee_IdAndScopeTypeAndStatus(10L, 2L, InvitationScopeType.PROJECT, InvitationStatus.PENDING)).thenReturn(List.of());
		when(invitationRepository.save(any())).thenAnswer(invocation -> {
			Invitation invitation = invocation.getArgument(0);
			setId(invitation, 100L);
			return invitation;
		});

		var data = service.create("token", 10L, new ProjectInvitationRequest(2L, ProjectMemberRole.MEMBER));

		assertThat(data.invitation().scopeType()).isEqualTo("PROJECT");
		assertThat(data.invitation().projectId()).isEqualTo(10L);
		assertThat(data.invitation().expiresAt()).isEqualTo("2026-07-14T10:15:30Z");
		ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
		verify(invitationRepository).save(captor.capture());
		assertThat(captor.getValue().getRole()).isEqualTo("MEMBER");
		verify(projectRepository).findByIdForUpdate(10L);
	}

	@Test
	void createAllowsActiveCompanyAdminWithoutProjectMembership() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findById(2L)).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(10L, 2L, ProjectMemberStatus.ACTIVE)).thenReturn(false);
		when(invitationRepository.findAllByProjectIdAndInvitee_IdAndScopeTypeAndStatus(10L, 2L, InvitationScopeType.PROJECT, InvitationStatus.PENDING)).thenReturn(List.of());
		when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.create("token", 10L, new ProjectInvitationRequest(2L, ProjectMemberRole.MEMBER));

		verify(invitationRepository).save(any());
	}

	@Test
	void createRejectsNonAdminAndInactiveCompanyInvitee() {
		User requester = user(1L);
		Project project = project(requester);
		stubRequester(requester);
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 1L, CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(1L, 1L, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(10L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create("token", 10L, new ProjectInvitationRequest(2L, ProjectMemberRole.MEMBER)))
				.isInstanceOf(ForbiddenException.class);
		verify(userRepository, never()).findById(2L);
	}

	@Test
	void createRejectsInviteeWhoIsNotAnActiveCompanyMember() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findById(2L)).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE)).thenReturn(false);

		assertThatThrownBy(() -> service.create("token", 10L, new ProjectInvitationRequest(2L, ProjectMemberRole.MEMBER)))
				.isInstanceOf(InvitationConflictException.class)
				.hasMessageContaining("활성 회사 구성원");
		verify(invitationRepository, never()).save(any());
	}

	@Test
	void createRejectsExistingInactiveInviteeWithCanonicalConflictBeforeMembershipCheck() {
		User requester = user(1L);
		User invitee = user(2L, UserStatus.SUSPENDED);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findById(2L)).thenReturn(Optional.of(invitee));

		Throwable thrown = catchThrowable(() -> service.create("token", 10L, new ProjectInvitationRequest(2L, ProjectMemberRole.MEMBER)));
		assertThat(thrown).isInstanceOf(InvitationConflictException.class);
		InvitationConflictException conflict = (InvitationConflictException) thrown;
		assertThat(conflict.getCode()).isEqualTo("TARGET_NOT_ACTIVE_COMPANY_MEMBER");
		assertThat(conflict).hasMessage("대상 사용자가 활성 회사 구성원이 아닙니다.");
		verify(companyMemberRepository, never()).existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE);
		verify(invitationRepository, never()).save(any());
	}

	@Test
	void createRejectsMissingInviteeWith404UserNotFound() {
		User requester = user(1L);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findById(2L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create("token", 10L, new ProjectInvitationRequest(2L, ProjectMemberRole.MEMBER)))
				.isInstanceOf(UserNotFoundException.class);
		verify(companyMemberRepository, never()).existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE);
		verify(invitationRepository, never()).save(any());
	}

	@Test
	void createRejectsSelfInvitationBeforePersistence() {
		User requester = user(1L);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);

		assertThatThrownBy(() -> service.create("token", 10L, new ProjectInvitationRequest(1L, ProjectMemberRole.MEMBER)))
				.isInstanceOf(InvitationConflictException.class)
				.hasMessageContaining("자기 자신");
		verify(invitationRepository, never()).save(any());
	}

	@Test
	void createRejectsActiveProjectMemberAndOnlySameProjectPendingDuplicate() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findById(2L)).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(10L, 2L, ProjectMemberStatus.ACTIVE)).thenReturn(true);

		assertThatThrownBy(() -> service.create("token", 10L, new ProjectInvitationRequest(2L, ProjectMemberRole.MEMBER)))
				.isInstanceOf(InvitationConflictException.class).hasMessageContaining("이미 프로젝트 구성원");

		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(10L, 2L, ProjectMemberStatus.ACTIVE)).thenReturn(false);
		when(invitationRepository.findAllByProjectIdAndInvitee_IdAndScopeTypeAndStatus(10L, 2L, InvitationScopeType.PROJECT, InvitationStatus.PENDING))
				.thenReturn(List.of(projectInvitation(100L, project, requester, invitee)));

		assertThatThrownBy(() -> service.create("token", 10L, new ProjectInvitationRequest(2L, ProjectMemberRole.MEMBER)))
				.isInstanceOf(InvitationConflictException.class).hasMessageContaining("대기 중인 초대");
	}

	@Test
	void createExpiresStalePendingInvitationBeforeCreatingReplacement() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		Invitation stale = projectInvitation(100L, project, requester, invitee);
		stale.setExpiresAt(now().minusSeconds(1));
		stubCompanyAdmin(requester, project);
		when(userRepository.findById(2L)).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(10L, 2L, ProjectMemberStatus.ACTIVE)).thenReturn(false);
		when(invitationRepository.findAllByProjectIdAndInvitee_IdAndScopeTypeAndStatus(10L, 2L, InvitationScopeType.PROJECT, InvitationStatus.PENDING)).thenReturn(List.of(stale));
		when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.create("token", 10L, new ProjectInvitationRequest(2L, ProjectMemberRole.MEMBER));

		assertThat(stale.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
	}

	@Test
	void findInvitableMemberNormalizesEmailAndReturnsActiveSameCompanyUserForProjectAdmin() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		stubProjectAdmin(requester, project);
		when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(10L, 2L, ProjectMemberStatus.ACTIVE)).thenReturn(false);
		when(invitationRepository.findAllByProjectIdAndInvitee_IdAndScopeTypeAndStatus(10L, 2L, InvitationScopeType.PROJECT, InvitationStatus.PENDING)).thenReturn(List.of());

		var data = service.findInvitableMember("token", 10L, "  USER2@example.com  ");

		assertThat(data.user().id()).isEqualTo(2L);
		assertThat(data.user().email()).isEqualTo("user2@example.com");
		assertThat(data.user().nickname()).isEqualTo("user2");
		assertThat(data.user().avatarUrl()).isNull();
		verify(userRepository).findByEmail("user2@example.com");
	}

	@Test
	void findInvitableMemberAllowsActiveCompanyAdminWithoutProjectMembership() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findByEmail(invitee.getEmail())).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(10L, 2L, ProjectMemberStatus.ACTIVE)).thenReturn(false);
		when(invitationRepository.findAllByProjectIdAndInvitee_IdAndScopeTypeAndStatus(10L, 2L, InvitationScopeType.PROJECT, InvitationStatus.PENDING)).thenReturn(List.of());

		var data = service.findInvitableMember("token", 10L, invitee.getEmail());

		assertThat(data.user().id()).isEqualTo(2L);
	}

	@Test
	void findInvitableMemberChecksExactProjectManagePermissionBeforeEmailLookup() {
		User requester = user(1L);
		Project project = project(requester);
		stubRequester(requester);
		stubProjectLookup(project);
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 1L, CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(1L, 1L, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(10L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findInvitableMember("token", 10L, "user2@example.com"))
				.isInstanceOf(ForbiddenException.class);
		verify(userRepository, never()).findByEmail("user2@example.com");
	}

	@Test
	void findInvitableMemberRejectsMissingUser() {
		User requester = user(1L);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findInvitableMember("token", 10L, "ghost@example.com"))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void findInvitableMemberRejectsInactiveUserAsNotFound() {
		User requester = user(1L);
		User invitee = user(2L, UserStatus.SUSPENDED);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findByEmail(invitee.getEmail())).thenReturn(Optional.of(invitee));

		// 비활성 사용자도 "찾을 수 없음"으로 통일한다 — 계정 상태를 노출하지 않는다.
		assertThatThrownBy(() -> service.findInvitableMember("token", 10L, invitee.getEmail()))
				.isInstanceOf(UserNotFoundException.class);
	}

	/**
	 * ★ enumeration 방지의 핵심: 미가입 email과 "가입했지만 다른 회사" email이
	 * 호출자 입장에서 구분되지 않아야 한다. 둘이 갈리면 PROJECT_ADMIN이 임의의
	 * email로 서비스 가입 여부를 알아낼 수 있다.
	 */
	@Test
	void findInvitableMemberMakesUnregisteredAndOutsideCompanyIndistinguishable() {
		User requester = user(1L);
		User outsider = user(2L);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
		when(userRepository.findByEmail(outsider.getEmail())).thenReturn(Optional.of(outsider));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE))
				.thenReturn(false);

		Throwable unregistered = catchThrowable(() -> service.findInvitableMember("token", 10L, "ghost@example.com"));
		Throwable outsideCompany = catchThrowable(() -> service.findInvitableMember("token", 10L, outsider.getEmail()));

		assertThat(unregistered).isInstanceOf(UserNotFoundException.class);
		assertThat(outsideCompany).isInstanceOf(UserNotFoundException.class);
		assertThat(outsideCompany.getClass()).isEqualTo(unregistered.getClass());
	}

	@Test
	void findInvitableMemberRejectsSelf() {
		User requester = user(1L);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));

		Throwable thrown = catchThrowable(() -> service.findInvitableMember("token", 10L, requester.getEmail()));

		assertThat(thrown).isInstanceOf(InvitationConflictException.class);
		assertThat(((InvitationConflictException) thrown).getCode()).isEqualTo("INVITATION_SELF_NOT_ALLOWED");
		verify(projectMemberRepository, never()).existsByProject_IdAndUser_IdAndStatus(10L, 1L, ProjectMemberStatus.ACTIVE);
	}

	@Test
	void findInvitableMemberRejectsUserOutsideActiveProjectCompanyAsNotFound() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findByEmail(invitee.getEmail())).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE)).thenReturn(false);

		// 회사 밖 사용자는 존재 여부조차 노출하지 않는다(위 enumeration 테스트 참고).
		assertThatThrownBy(() -> service.findInvitableMember("token", 10L, invitee.getEmail()))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void findInvitableMemberDoesNotLockProjectRow() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findByEmail(invitee.getEmail())).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(10L, 2L, ProjectMemberStatus.ACTIVE)).thenReturn(false);
		when(invitationRepository.findAllByProjectIdAndInvitee_IdAndScopeTypeAndStatus(10L, 2L, InvitationScopeType.PROJECT, InvitationStatus.PENDING)).thenReturn(List.of());

		service.findInvitableMember("token", 10L, invitee.getEmail());

		// 아무것도 쓰지 않는 조회라 비관적 락을 잡지 않는다 — 초대 화면에서 이메일을
		// 칠 때마다 호출되는 경로여서 같은 프로젝트의 쓰기 작업과 경합하면 안 된다.
		verify(projectRepository).findById(10L);
		verify(projectRepository, never()).findByIdForUpdate(10L);
	}

	@Test
	void findInvitableMemberRejectsActiveProjectMember() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		stubCompanyAdmin(requester, project);
		when(userRepository.findByEmail(invitee.getEmail())).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(10L, 2L, ProjectMemberStatus.ACTIVE)).thenReturn(true);

		Throwable thrown = catchThrowable(() -> service.findInvitableMember("token", 10L, invitee.getEmail()));

		assertThat(thrown).isInstanceOf(InvitationConflictException.class);
		assertThat(((InvitationConflictException) thrown).getCode()).isEqualTo("ALREADY_PROJECT_MEMBER");
		verify(invitationRepository, never()).findAllByProjectIdAndInvitee_IdAndScopeTypeAndStatus(
				10L, 2L, InvitationScopeType.PROJECT, InvitationStatus.PENDING);
	}

	@Test
	void findInvitableMemberRejectsLivePendingProjectInvitation() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		Invitation pending = projectInvitation(100L, project, requester, invitee);
		stubCompanyAdmin(requester, project);
		when(userRepository.findByEmail(invitee.getEmail())).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(10L, 2L, ProjectMemberStatus.ACTIVE)).thenReturn(false);
		when(invitationRepository.findAllByProjectIdAndInvitee_IdAndScopeTypeAndStatus(10L, 2L, InvitationScopeType.PROJECT, InvitationStatus.PENDING)).thenReturn(List.of(pending));

		Throwable thrown = catchThrowable(() -> service.findInvitableMember("token", 10L, invitee.getEmail()));

		assertThat(thrown).isInstanceOf(InvitationConflictException.class);
		assertThat(((InvitationConflictException) thrown).getCode()).isEqualTo("INVITATION_ALREADY_PENDING");
		assertThat(pending.getStatus()).isEqualTo(InvitationStatus.PENDING);
	}

	@Test
	void findInvitableMemberExpiresStalePendingInvitationAndReturnsCandidate() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		Invitation stale = projectInvitation(100L, project, requester, invitee);
		stale.setExpiresAt(now().minusSeconds(1));
		stubCompanyAdmin(requester, project);
		when(userRepository.findByEmail(invitee.getEmail())).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, 2L, CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(projectMemberRepository.existsByProject_IdAndUser_IdAndStatus(10L, 2L, ProjectMemberStatus.ACTIVE)).thenReturn(false);
		when(invitationRepository.findAllByProjectIdAndInvitee_IdAndScopeTypeAndStatus(10L, 2L, InvitationScopeType.PROJECT, InvitationStatus.PENDING)).thenReturn(List.of(stale));

		var data = service.findInvitableMember("token", 10L, invitee.getEmail());

		assertThat(data.user().id()).isEqualTo(2L);
		assertThat(stale.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
		assertThat(stale.getUpdatedAt()).isEqualTo(now());
	}

	@Test
	void acceptCreatesMembershipAndAcceptsInvitation() {
		User invitee = user(2L);
		User inviter = user(1L);
		Project project = project(inviter);
		Invitation invitation = projectInvitation(100L, project, inviter, invitee);
		stubRequester(invitee);
		when(invitationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(invitation));
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(companyMemberRepository.findByCompany_IdAndUser_IdForUpdate(1L, 2L)).thenReturn(Optional.of(companyMember(project.getCompany(), invitee, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE)));
		when(projectMemberRepository.findByProject_IdAndUser_IdForUpdate(10L, 2L)).thenReturn(Optional.empty());

		var data = service.accept("token", 100L);

		assertThat(data.membershipCreated()).isTrue();
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
		ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
		verify(projectMemberRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(ProjectMemberStatus.ACTIVE);
	}

	@Test
	void acceptReactivatesRemovedMembershipWithoutReplacingIdentity() {
		User invitee = user(2L);
		User inviter = user(1L);
		Project project = project(inviter);
		Invitation invitation = projectInvitation(100L, project, inviter, invitee);
		ProjectMember removed = new ProjectMember(project, invitee, ProjectMemberRole.MEMBER, ProjectMemberStatus.REMOVED, now().minusDays(2), now().minusDays(5), now().minusDays(2));
		setId(removed, 77L);
		stubRequester(invitee);
		when(invitationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(invitation));
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(companyMemberRepository.findByCompany_IdAndUser_IdForUpdate(1L, 2L)).thenReturn(Optional.of(companyMember(project.getCompany(), invitee, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE)));
		when(projectMemberRepository.findByProject_IdAndUser_IdForUpdate(10L, 2L)).thenReturn(Optional.of(removed));

		var data = service.accept("token", 100L);

		assertThat(data.membershipCreated()).isFalse();
		assertThat(removed.getId()).isEqualTo(77L);
		assertThat(removed.getCreatedAt()).isEqualTo(now().minusDays(5));
		assertThat(removed.getStatus()).isEqualTo(ProjectMemberStatus.ACTIVE);
		assertThat(removed.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
		assertThat(removed.getJoinedAt()).isEqualTo(now());
		verify(projectMemberRepository).findByProject_IdAndUser_IdForUpdate(10L, 2L);
		verify(projectMemberRepository, never()).save(any());
	}

	@Test
	void acceptRejectsActiveMembershipWithoutChangingInvitation() {
		User invitee = user(2L);
		User inviter = user(1L);
		Project project = project(inviter);
		Invitation invitation = projectInvitation(100L, project, inviter, invitee);
		ProjectMember active = new ProjectMember(project, invitee, ProjectMemberRole.MEMBER, ProjectMemberStatus.ACTIVE, now(), now(), now());
		stubRequester(invitee);
		when(invitationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(invitation));
		when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
		when(companyMemberRepository.findByCompany_IdAndUser_IdForUpdate(1L, 2L)).thenReturn(Optional.of(companyMember(project.getCompany(), invitee, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE)));
		when(projectMemberRepository.findByProject_IdAndUser_IdForUpdate(10L, 2L)).thenReturn(Optional.of(active));

		assertThatThrownBy(() -> service.accept("token", 100L)).isInstanceOf(InvitationConflictException.class);
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
	}

	@Test
	void acceptRejectsOtherInviteeAndExpiredInvitationAndCancelRequiresProjectAdmin() {
		User inviter = user(1L);
		User invitee = user(2L);
		Project project = project(inviter);
		Invitation invitation = projectInvitation(100L, project, inviter, invitee);
		stubRequester(user(3L));
		when(invitationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(invitation));
		assertThatThrownBy(() -> service.accept("token", 100L)).isInstanceOf(ForbiddenException.class);

		stubRequester(invitee);
		invitation.setExpiresAt(now().minusSeconds(1));
		assertThatThrownBy(() -> service.accept("token", 100L)).isInstanceOf(InvitationConflictException.class);
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
	}

	@Test
	void listReturnsOnlyProjectScopeAndExpiresStalePendingInvitation() {
		User requester = user(1L);
		Project project = project(requester);
		Invitation stale = projectInvitation(100L, project, requester, user(2L));
		stale.setExpiresAt(now().minusMinutes(1));
		stubCompanyAdmin(requester, project);
		when(invitationRepository.findByProjectIdAndScopeTypeOrderByCreatedAtDesc(10L, InvitationScopeType.PROJECT)).thenReturn(List.of(stale));

		var data = service.list("token", 10L);

		assertThat(data.invitations()).singleElement().extracting(invitation -> invitation.scopeType()).isEqualTo("PROJECT");
		assertThat(stale.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
	}

	@Test
	void cancelRequiresProjectManagerAndChangesOnlyPendingProjectInvitation() {
		User requester = user(1L);
		User invitee = user(2L);
		Project project = project(requester);
		Invitation invitation = projectInvitation(100L, project, requester, invitee);
		stubCompanyAdmin(requester, project);
		when(invitationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(invitation));

		var data = service.cancel("token", 100L);

		assertThat(data.status()).isEqualTo("CANCELED");
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.CANCELED);
	}

	@Test
	void projectActionsRejectCompanyScopedInvitation() {
		User requester = user(1L);
		Project project = project(requester);
		Invitation companyInvitation = new Invitation(
				project.getCompany(), null, requester, user(2L), InvitationScopeType.COMPANY,
				"MEMBER", InvitationStatus.PENDING, now().plusDays(7), now(), now());
		setId(companyInvitation, 100L);
		stubRequester(requester);
		when(invitationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(companyInvitation));

		assertThatThrownBy(() -> service.decline("token", 100L)).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void declineMarksPendingProjectInvitationDeclinedWithoutCreatingMembership() {
		User invitee = user(2L);
		User inviter = user(1L);
		Project project = project(inviter);
		Invitation invitation = projectInvitation(100L, project, inviter, invitee);
		stubRequester(invitee);
		when(invitationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(invitation));

		var data = service.decline("token", 100L);

		assertThat(data.status()).isEqualTo("DECLINED");
		assertThat(data.membershipCreated()).isFalse();
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.DECLINED);
		verify(projectMemberRepository, never()).save(any());
	}

	@Test
	void acceptAndDeclineRejectInactiveCompanyBeforeInvitationStateMutation() {
		User invitee = user(2L);
		User inviter = user(1L);
		Project project = project(inviter);
		project.getCompany().setStatus(com.threadia.backend.company.domain.CompanyStatus.INACTIVE);
		Invitation invitation = projectInvitation(100L, project, inviter, invitee);
		invitation.setExpiresAt(now().minusSeconds(1));
		stubRequester(invitee);
		when(invitationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(invitation));

		assertThatThrownBy(() -> service.accept("token", 100L)).isInstanceOf(ForbiddenException.class);
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
		assertThatThrownBy(() -> service.decline("token", 100L)).isInstanceOf(ForbiddenException.class);
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
		verify(projectRepository, never()).findByIdForUpdate(any());
	}

	@Test
	void acceptRejectsInviteeWhoIsNoLongerAnActiveCompanyMember() {
		User invitee = user(2L);
		User inviter = user(1L);
		Project project = project(inviter);
		Invitation invitation = projectInvitation(100L, project, inviter, invitee);
		stubRequester(invitee);
		when(invitationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(invitation));
		when(companyMemberRepository.findByCompany_IdAndUser_IdForUpdate(1L, 2L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.accept("token", 100L))
				.isInstanceOf(InvitationConflictException.class)
				.hasMessageContaining("활성 회사 구성원");
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
		verify(projectMemberRepository, never()).save(any());
	}

	private void stubCompanyAdmin(User requester, Project project) {
		stubRequester(requester);
		stubProjectLookup(project);
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, requester.getId(), CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(1L, requester.getId(), CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE)).thenReturn(true);
	}

	/**
	 * 쓰기 경로(create/list 등)는 프로젝트 행을 잠그고(findByIdForUpdate),
	 * 조회 전용인 findInvitableMember는 락 없이 읽는다(findById). 어느 쪽을 쓰든
	 * 같은 helper로 stub할 수 있게 둘 다 열어두고, 쓰이지 않은 쪽은 lenient로 넘긴다.
	 */
	private void stubProjectLookup(Project project) {
		lenient().when(projectRepository.findByIdForUpdate(project.getId())).thenReturn(Optional.of(project));
		lenient().when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
	}

	private void stubProjectAdmin(User requester, Project project) {
		stubRequester(requester);
		stubProjectLookup(project);
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(1L, requester.getId(), CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(1L, requester.getId(), CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE)).thenReturn(false);
		when(projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), requester.getId()))
				.thenReturn(Optional.of(new ProjectMember(project, requester, ProjectMemberRole.PROJECT_ADMIN, ProjectMemberStatus.ACTIVE, now(), now(), now())));
	}

	private void stubRequester(User requester) {
		when(jwtTokenService.parseAccessToken("token")).thenReturn(new AuthenticatedUser(requester.getId(), requester.getEmail()));
		when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
	}

	private static User user(Long id) {
		return user(id, UserStatus.ACTIVE);
	}

	private static User user(Long id, UserStatus status) {
		User user = new User("user" + id + "@example.com", "hashed", "user" + id, null, status, now(), now());
		setId(user, id);
		return user;
	}

	private static Project project(User creator) {
		Company company = new Company("Threadia", "대표", null, "IT", null, com.threadia.backend.company.domain.CompanyStatus.ACTIVE, creator, now(), now());
		setId(company, 1L);
		Project project = new Project(company, "Project", "PROJECT", null, ProjectStatus.PLANNING, null, null, creator, null, null, null, now(), now());
		setId(project, 10L);
		return project;
	}

	private static CompanyMember companyMember(Company company, User user, CompanyMemberRole role, CompanyMemberStatus status) {
		return new CompanyMember(company, user, null, role, status, now(), now(), now());
	}

	private static Invitation projectInvitation(Long id, Project project, User inviter, User invitee) {
		Invitation invitation = new Invitation(project.getCompany(), project.getId(), inviter, invitee, InvitationScopeType.PROJECT, ProjectMemberRole.MEMBER.name(), InvitationStatus.PENDING, now().plusDays(7), now(), now());
		setId(invitation, id);
		return invitation;
	}

	private static LocalDateTime now() {
		return LocalDateTime.ofInstant(FIXED_CLOCK.instant(), ZoneOffset.UTC);
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
