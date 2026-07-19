package com.threadia.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenService;
import com.threadia.backend.invitation.domain.Invitation;
import com.threadia.backend.invitation.domain.InvitationScopeType;
import com.threadia.backend.invitation.domain.InvitationStatus;
import com.threadia.backend.invitation.repository.InvitationRepository;
import com.threadia.backend.project.domain.Project;
import com.threadia.backend.project.domain.ProjectStatus;
import com.threadia.backend.project.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class ReceivedInvitationServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-07T10:15:30Z"), ZoneOffset.UTC);

	@Mock private JwtTokenService jwtTokenService;
	@Mock private UserRepository userRepository;
	@Mock private InvitationRepository invitationRepository;
	@Mock private ProjectRepository projectRepository;

	@Test
	void listMineReturnsCompanyAndProjectInvitationsAndExpiresStalePendingInvitation() {
		User user = user(2L);
		Invitation companyInvitation = invitation(1L, user, InvitationScopeType.COMPANY);
		Invitation projectInvitation = invitation(2L, user, InvitationScopeType.PROJECT);
		projectInvitation.setExpiresAt(now().minusSeconds(1));
		ReceivedInvitationService service = newService();
		when(jwtTokenService.parseAccessToken("token")).thenReturn(new AuthenticatedUser(2L, user.getEmail()));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(invitationRepository.findByInvitee_IdOrderByCreatedAtDesc(2L)).thenReturn(List.of(projectInvitation, companyInvitation));
		when(projectRepository.findAllById(List.of(10L))).thenReturn(List.of(project(10L, "Threadia 웹")));

		var data = service.listMine("token");

		assertThat(data.invitations()).extracting(invitation -> invitation.scopeType())
				.containsExactly("PROJECT", "COMPANY");
		assertThat(data.invitations().get(0).status()).isEqualTo("EXPIRED");
		assertThat(projectInvitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
	}

	@Test
	void listMineFillsProjectNameOnlyForProjectScope() {
		User user = user(2L);
		Invitation companyInvitation = invitation(1L, user, InvitationScopeType.COMPANY);
		Invitation projectInvitation = invitation(2L, user, InvitationScopeType.PROJECT);
		ReceivedInvitationService service = newService();
		when(jwtTokenService.parseAccessToken("token")).thenReturn(new AuthenticatedUser(2L, user.getEmail()));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(invitationRepository.findByInvitee_IdOrderByCreatedAtDesc(2L)).thenReturn(List.of(projectInvitation, companyInvitation));
		when(projectRepository.findAllById(List.of(10L))).thenReturn(List.of(project(10L, "Threadia 웹")));

		var data = service.listMine("token");

		// 초대 대상자는 아직 프로젝트 member가 아니라 프로젝트 상세를 못 읽는다 —
		// 어느 프로젝트 초대인지 보여주려면 이 필드가 채워져 있어야 한다.
		assertThat(data.invitations().get(0).projectName()).isEqualTo("Threadia 웹");
		assertThat(data.invitations().get(1).projectName()).isNull();
	}

	/**
	 * 회사 초대만 받은 사용자가 대부분이라 이 경로가 깨지면 영향이 크다.
	 * projectId가 null인데 Map.of()에 get(null)을 하면 NPE가 나 목록 전체가 500이 된다.
	 */
	@Test
	void listMineWithOnlyCompanyInvitationsDoesNotQueryProjectsAndSucceeds() {
		User user = user(2L);
		ReceivedInvitationService service = newService();
		when(jwtTokenService.parseAccessToken("token")).thenReturn(new AuthenticatedUser(2L, user.getEmail()));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(invitationRepository.findByInvitee_IdOrderByCreatedAtDesc(2L))
				.thenReturn(List.of(invitation(1L, user, InvitationScopeType.COMPANY)));

		var data = service.listMine("token");

		assertThat(data.invitations()).hasSize(1);
		assertThat(data.invitations().get(0).projectName()).isNull();
		verifyNoInteractions(projectRepository);
	}

	private ReceivedInvitationService newService() {
		return new ReceivedInvitationService(jwtTokenService, userRepository, invitationRepository, projectRepository, CLOCK);
	}

	private static Project project(Long id, String name) {
		Company company = new Company("Threadia", "대표", null, "IT", null, com.threadia.backend.company.domain.CompanyStatus.ACTIVE, null, now(), now());
		setId(company, 1L);
		Project project = new Project(
				company, name, "THR", null, ProjectStatus.ACTIVE, null, null, null, null, null, null, now(), now());
		setId(project, id);
		return project;
	}

	private static User user(Long id) {
		User user = new User("member@example.com", "hashed", "member", null, UserStatus.ACTIVE, now(), now());
		setId(user, id);
		return user;
	}

	private static Invitation invitation(Long id, User invitee, InvitationScopeType scopeType) {
		Company company = new Company("Threadia", "대표", null, "IT", null, com.threadia.backend.company.domain.CompanyStatus.ACTIVE, invitee, now(), now());
		setId(company, 1L);
		Invitation invitation = new Invitation(company, scopeType == InvitationScopeType.PROJECT ? 10L : null, invitee, invitee, scopeType, "MEMBER", InvitationStatus.PENDING, now().plusDays(7), now(), now());
		setId(invitation, id);
		return invitation;
	}

	private static LocalDateTime now() {
		return LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC);
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
