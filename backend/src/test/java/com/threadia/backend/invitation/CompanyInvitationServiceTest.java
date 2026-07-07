package com.threadia.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.common.exception.UserNotFoundException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.config.jwt.JwtTokenService;
import com.threadia.backend.invitation.domain.Invitation;
import com.threadia.backend.invitation.domain.InvitationScopeType;
import com.threadia.backend.invitation.domain.InvitationStatus;
import com.threadia.backend.invitation.dto.CompanyInvitationData;
import com.threadia.backend.invitation.dto.CompanyInvitationRequest;
import com.threadia.backend.invitation.dto.CompanyInvitationsData;
import com.threadia.backend.invitation.exception.CompanyNotFoundException;
import com.threadia.backend.invitation.exception.InvitationConflictException;
import com.threadia.backend.invitation.repository.InvitationRepository;

@ExtendWith(MockitoExtension.class)
class CompanyInvitationServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-07T10:15:30Z"), ZoneOffset.UTC);

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CompanyRepository companyRepository;

	@Mock
	private CompanyMemberRepository companyMemberRepository;

	@Mock
	private InvitationRepository invitationRepository;

	private CompanyInvitationService companyInvitationService;

	@BeforeEach
	void setUp() {
		companyInvitationService = new CompanyInvitationService(
				jwtTokenService,
				userRepository,
				companyRepository,
				companyMemberRepository,
				invitationRepository,
				FIXED_CLOCK);
	}

	@Test
	void createCompanyInvitationCreatesPendingCompanyInvitationWithoutMembership() {
		User requester = user(1L, "admin@example.com", UserStatus.ACTIVE);
		User invitee = user(2L, "member@example.com", UserStatus.ACTIVE);
		Company company = company(1L, requester);
		stubAuthorizedRequester(requester, company);
		when(userRepository.findById(2L)).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_Id(1L, 2L))
				.thenReturn(false);
		when(invitationRepository.existsByCompany_IdAndInvitee_IdAndScopeTypeAndStatus(
				1L, 2L, InvitationScopeType.COMPANY, InvitationStatus.PENDING)).thenReturn(false);
		when(invitationRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
			Invitation invitation = invocation.getArgument(0);
			setId(invitation, 10L);
			return invitation;
		});

		CompanyInvitationData data = companyInvitationService.create(
				"valid-token",
				1L,
				new CompanyInvitationRequest(2L, CompanyMemberRole.MEMBER));

		assertThat(data.invitation().id()).isEqualTo(10L);
		assertThat(data.invitation().scopeType()).isEqualTo("COMPANY");
		assertThat(data.invitation().companyId()).isEqualTo(1L);
		assertThat(data.invitation().projectId()).isNull();
		assertThat(data.invitation().inviteeUserId()).isEqualTo(2L);
		assertThat(data.invitation().status()).isEqualTo("PENDING");
		assertThat(data.invitation().expiresAt()).isEqualTo("2026-07-14T10:15:30Z");
		ArgumentCaptor<Invitation> invitationCaptor = ArgumentCaptor.forClass(Invitation.class);
		verify(invitationRepository).save(invitationCaptor.capture());
		assertThat(invitationCaptor.getValue().getProjectId()).isNull();
		assertThat(invitationCaptor.getValue().getStatus()).isEqualTo(InvitationStatus.PENDING);
		verify(companyMemberRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void listCompanyInvitationsReturnsCompanyScopeInvitations() {
		User requester = user(1L, "admin@example.com", UserStatus.ACTIVE);
		User invitee = user(2L, "member@example.com", UserStatus.ACTIVE);
		Company company = company(1L, requester);
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(requester.getId(), requester.getEmail()));
		when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
		when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(
				company.getId(), requester.getId(), CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE))
				.thenReturn(true);
		Invitation invitation = invitation(10L, company, requester, invitee);
		when(invitationRepository.findByCompany_IdAndScopeTypeOrderByCreatedAtDesc(1L, InvitationScopeType.COMPANY))
				.thenReturn(List.of(invitation));

		CompanyInvitationsData data = companyInvitationService.list("valid-token", 1L);

		assertThat(data.invitations()).hasSize(1);
		assertThat(data.invitations().get(0).id()).isEqualTo(10L);
		assertThat(data.invitations().get(0).scopeType()).isEqualTo("COMPANY");
		assertThat(data.invitations().get(0).inviteeUserId()).isEqualTo(2L);
	}

	@Test
	void createCompanyInvitationThrowsWhenTokenIsInvalid() {
		when(jwtTokenService.parseAccessToken("invalid-token"))
				.thenThrow(new JwtAuthenticationException("유효하지 않은 access token입니다."));

		assertThatThrownBy(() -> companyInvitationService.create(
				"invalid-token", 1L, new CompanyInvitationRequest(2L, CompanyMemberRole.MEMBER)))
				.isInstanceOf(JwtAuthenticationException.class);
		verify(invitationRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void createCompanyInvitationThrowsWhenRequesterIsNotActive() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "admin@example.com"));
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin@example.com", UserStatus.SUSPENDED)));

		assertThatThrownBy(() -> companyInvitationService.create(
				"valid-token", 1L, new CompanyInvitationRequest(2L, CompanyMemberRole.MEMBER)))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void createCompanyInvitationThrowsWhenCompanyDoesNotExist() {
		User requester = user(1L, "admin@example.com", UserStatus.ACTIVE);
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "admin@example.com"));
		when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(
				1L, 1L, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE)).thenReturn(true);
		when(companyRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> companyInvitationService.create(
				"valid-token", 1L, new CompanyInvitationRequest(2L, CompanyMemberRole.MEMBER)))
				.isInstanceOf(CompanyNotFoundException.class);
	}

	@Test
	void createCompanyInvitationThrowsWhenRequesterIsNotCompanyAdmin() {
		User requester = user(1L, "admin@example.com", UserStatus.ACTIVE);
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "admin@example.com"));
		when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(
				1L, 1L, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE)).thenReturn(false);

		assertThatThrownBy(() -> companyInvitationService.create(
				"valid-token", 1L, new CompanyInvitationRequest(2L, CompanyMemberRole.MEMBER)))
				.isInstanceOf(ForbiddenException.class);
		verify(companyRepository, never()).findByIdForUpdate(1L);
		verify(userRepository, never()).findById(2L);
		verify(invitationRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void listCompanyInvitationsThrowsWhenRequesterIsNotCompanyAdminWithoutQueryingInvitations() {
		User requester = user(1L, "admin@example.com", UserStatus.ACTIVE);
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "admin@example.com"));
		when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(
				1L, 1L, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE)).thenReturn(false);

		assertThatThrownBy(() -> companyInvitationService.list("valid-token", 1L))
				.isInstanceOf(ForbiddenException.class);
		verify(companyRepository, never()).findById(1L);
		verify(invitationRepository, never()).findByCompany_IdAndScopeTypeOrderByCreatedAtDesc(
				1L,
				InvitationScopeType.COMPANY);
	}

	@Test
	void createCompanyInvitationThrowsWhenInviteeIsNotActive() {
		User requester = user(1L, "admin@example.com", UserStatus.ACTIVE);
		Company company = company(1L, requester);
		stubAuthorizedRequester(requester, company);
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "member@example.com", UserStatus.SUSPENDED)));

		assertThatThrownBy(() -> companyInvitationService.create(
				"valid-token", 1L, new CompanyInvitationRequest(2L, CompanyMemberRole.MEMBER)))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void createCompanyInvitationThrowsWhenInviteeIsAlreadyCompanyMember() {
		User requester = user(1L, "admin@example.com", UserStatus.ACTIVE);
		User invitee = user(2L, "member@example.com", UserStatus.ACTIVE);
		Company company = company(1L, requester);
		stubAuthorizedRequester(requester, company);
		when(userRepository.findById(2L)).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_Id(1L, 2L))
				.thenReturn(true);

		assertThatThrownBy(() -> companyInvitationService.create(
				"valid-token", 1L, new CompanyInvitationRequest(2L, CompanyMemberRole.MEMBER)))
				.isInstanceOf(InvitationConflictException.class)
				.hasMessageContaining("이미 회사 구성원입니다.");
	}

	@Test
	void createCompanyInvitationThrowsWhenPendingInvitationAlreadyExists() {
		User requester = user(1L, "admin@example.com", UserStatus.ACTIVE);
		User invitee = user(2L, "member@example.com", UserStatus.ACTIVE);
		Company company = company(1L, requester);
		stubAuthorizedRequester(requester, company);
		when(userRepository.findById(2L)).thenReturn(Optional.of(invitee));
		when(companyMemberRepository.existsByCompany_IdAndUser_Id(1L, 2L))
				.thenReturn(false);
		when(invitationRepository.existsByCompany_IdAndInvitee_IdAndScopeTypeAndStatus(
				1L, 2L, InvitationScopeType.COMPANY, InvitationStatus.PENDING)).thenReturn(true);

		assertThatThrownBy(() -> companyInvitationService.create(
				"valid-token", 1L, new CompanyInvitationRequest(2L, CompanyMemberRole.MEMBER)))
				.isInstanceOf(InvitationConflictException.class)
				.hasMessageContaining("이미 대기 중인 초대가 있습니다.");
	}

	private void stubAuthorizedRequester(User requester, Company company) {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(requester.getId(), requester.getEmail()));
		when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
		when(companyRepository.findByIdForUpdate(company.getId())).thenReturn(Optional.of(company));
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(
				company.getId(), requester.getId(), CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE))
				.thenReturn(true);
	}

	private static User user(Long id, String email, UserStatus status) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		User user = new User(email, "hashed", email, null, status, now, now);
		setId(user, id);
		return user;
	}

	private static Company company(Long id, User createdBy) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		Company company = new Company("Threadia", "대표", null, "IT", null, "ACTIVE", createdBy, now, now);
		setId(company, id);
		return company;
	}

	private static Invitation invitation(Long id, Company company, User inviter, User invitee) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		Invitation invitation = new Invitation(
				company,
				null,
				inviter,
				invitee,
				InvitationScopeType.COMPANY,
				CompanyMemberRole.MEMBER.name(),
				InvitationStatus.PENDING,
				now.plusDays(7),
				now,
				now);
		setId(invitation, id);
		return invitation;
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
