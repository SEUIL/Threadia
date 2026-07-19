package com.threadia.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.config.jwt.JwtTokenService;
import com.threadia.backend.invitation.domain.Invitation;
import com.threadia.backend.invitation.domain.InvitationScopeType;
import com.threadia.backend.invitation.domain.InvitationStatus;
import com.threadia.backend.invitation.dto.InvitationActionData;
import com.threadia.backend.invitation.repository.InvitationRepository;

@ExtendWith(MockitoExtension.class)
class InvitationActionServiceTest {

	@Mock private InvitationRepository invitationRepository;
	@Mock private JwtTokenService jwtTokenService;
	@Mock private UserRepository userRepository;
	@Mock private CompanyInvitationService companyInvitationService;
	@Mock private ProjectInvitationService projectInvitationService;
	@InjectMocks private InvitationActionService invitationActionService;

	@Test
	void routesCompanyAcceptAndDeclineToExistingCompanyService() {
		stubActiveRequester();
		when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation(1L, InvitationScopeType.COMPANY)));
		when(companyInvitationService.accept("token", 1L)).thenReturn(new InvitationActionData(1L, "ACCEPTED", true));
		when(companyInvitationService.decline("token", 1L)).thenReturn(new InvitationActionData(1L, "DECLINED", false));
		when(companyInvitationService.cancel("token", 1L)).thenReturn(new InvitationActionData(1L, "CANCELED", false));

		var data = invitationActionService.accept("token", 1L);

		assertThat(data.membershipCreated()).isTrue();
		verify(companyInvitationService).accept("token", 1L);
		assertThat(invitationActionService.decline("token", 1L).status()).isEqualTo("DECLINED");
		verify(companyInvitationService).decline("token", 1L);
		assertThat(invitationActionService.cancel("token", 1L).status()).isEqualTo("CANCELED");
		verify(companyInvitationService).cancel("token", 1L);
	}

	@Test
	void routesProjectAcceptDeclineAndCancelToProjectService() {
		stubActiveRequester();
		when(invitationRepository.findById(2L)).thenReturn(Optional.of(invitation(2L, InvitationScopeType.PROJECT)));
		when(projectInvitationService.accept("token", 2L)).thenReturn(new InvitationActionData(2L, "ACCEPTED", true));
		when(projectInvitationService.decline("token", 2L)).thenReturn(new InvitationActionData(2L, "DECLINED", false));
		when(projectInvitationService.cancel("token", 2L)).thenReturn(new InvitationActionData(2L, "CANCELED", false));

		var data = invitationActionService.cancel("token", 2L);

		assertThat(data.status()).isEqualTo("CANCELED");
		assertThat(invitationActionService.accept("token", 2L).status()).isEqualTo("ACCEPTED");
		assertThat(invitationActionService.decline("token", 2L).status()).isEqualTo("DECLINED");
		verify(projectInvitationService).accept("token", 2L);
		verify(projectInvitationService).decline("token", 2L);
		verify(projectInvitationService).cancel("token", 2L);
	}

	@Test
	void rejectsInvalidTokenBeforeLookingUpExistingInvitation() {
		when(jwtTokenService.parseAccessToken("invalid-token"))
				.thenThrow(new JwtAuthenticationException("유효하지 않은 access token입니다."));

		assertThatThrownBy(() -> invitationActionService.accept("invalid-token", 1L))
				.isInstanceOf(JwtAuthenticationException.class);
		verify(invitationRepository, never()).findById(1L);
	}

	@Test
	void rejectsInvalidTokenBeforeLookingUpNonexistentInvitation() {
		when(jwtTokenService.parseAccessToken("invalid-token"))
				.thenThrow(new JwtAuthenticationException("유효하지 않은 access token입니다."));

		assertThatThrownBy(() -> invitationActionService.cancel("invalid-token", 999L))
				.isInstanceOf(JwtAuthenticationException.class);
		verify(invitationRepository, never()).findById(999L);
	}

	private void stubActiveRequester() {
		User user = user(1L);
		when(jwtTokenService.parseAccessToken("token")).thenReturn(new AuthenticatedUser(1L, user.getEmail()));
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
	}

	private static User user(Long id) {
		LocalDateTime now = LocalDateTime.of(2026, 7, 7, 10, 15, 30);
		User user = new User("user@example.com", "hashed", "user", null, UserStatus.ACTIVE, now, now);
		setId(user, id);
		return user;
	}

	private static Invitation invitation(Long id, InvitationScopeType scopeType) {
		LocalDateTime now = LocalDateTime.of(2026, 7, 7, 10, 15, 30);
		User user = user(1L);
		Company company = new Company("Threadia", "대표", null, "IT", null, com.threadia.backend.company.domain.CompanyStatus.ACTIVE, user, now, now);
		setId(company, 1L);
		Invitation invitation = new Invitation(company, scopeType == InvitationScopeType.PROJECT ? 10L : null, user, user, scopeType, "MEMBER", InvitationStatus.PENDING, now.plusDays(7), now, now);
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
