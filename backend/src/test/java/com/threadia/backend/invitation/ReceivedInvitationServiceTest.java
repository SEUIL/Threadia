package com.threadia.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;
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

@ExtendWith(MockitoExtension.class)
class ReceivedInvitationServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-07T10:15:30Z"), ZoneOffset.UTC);

	@Mock private JwtTokenService jwtTokenService;
	@Mock private UserRepository userRepository;
	@Mock private InvitationRepository invitationRepository;

	@Test
	void listMineReturnsCompanyAndProjectInvitationsAndExpiresStalePendingInvitation() {
		User user = user(2L);
		Invitation companyInvitation = invitation(1L, user, InvitationScopeType.COMPANY);
		Invitation projectInvitation = invitation(2L, user, InvitationScopeType.PROJECT);
		projectInvitation.setExpiresAt(now().minusSeconds(1));
		ReceivedInvitationService service = new ReceivedInvitationService(jwtTokenService, userRepository, invitationRepository, CLOCK);
		when(jwtTokenService.parseAccessToken("token")).thenReturn(new AuthenticatedUser(2L, user.getEmail()));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(invitationRepository.findByInvitee_IdOrderByCreatedAtDesc(2L)).thenReturn(List.of(projectInvitation, companyInvitation));

		var data = service.listMine("token");

		assertThat(data.invitations()).extracting(invitation -> invitation.scopeType())
				.containsExactly("PROJECT", "COMPANY");
		assertThat(data.invitations().get(0).status()).isEqualTo("EXPIRED");
		assertThat(projectInvitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
	}

	private static User user(Long id) {
		User user = new User("member@example.com", "hashed", "member", null, UserStatus.ACTIVE, now(), now());
		setId(user, id);
		return user;
	}

	private static Invitation invitation(Long id, User invitee, InvitationScopeType scopeType) {
		Company company = new Company("Threadia", "대표", null, "IT", null, "ACTIVE", invitee, now(), now());
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
