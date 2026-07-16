package com.threadia.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.common.exception.UserNotFoundException;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.config.jwt.JwtTokenService;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceTest {

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private JdbcTemplate jdbcTemplate;

	private UserLookupService userLookupService;

	@BeforeEach
	void setUp() {
		userLookupService = new UserLookupService(jwtTokenService, userRepository, jdbcTemplate);
	}

	@Test
	void lookupByEmailNormalizesEmailAndReturnsActiveUser() {
		stubActiveRequester();
		User user = user(2L, "member@example.com", "member", UserStatus.ACTIVE);
		when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

		UserLookupData data = userLookupService.lookupByEmail("valid-token", "  MEMBER@example.com  ");

		assertThat(data.user().id()).isEqualTo(2L);
		assertThat(data.user().email()).isEqualTo("member@example.com");
		assertThat(data.user().nickname()).isEqualTo("member");
		verify(userRepository).findByEmail("member@example.com");
	}

	@Test
	void lookupByEmailThrowsWhenUserDoesNotExist() {
		stubActiveRequester();
		when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userLookupService.lookupByEmail("valid-token", "ghost@example.com"))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void lookupByEmailThrowsWhenUserIsNotActive() {
		stubActiveRequester();
		User user = user(2L, "member@example.com", "member", UserStatus.SUSPENDED);
		when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> userLookupService.lookupByEmail("valid-token", "member@example.com"))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void lookupByEmailThrowsWhenTokenIsInvalid() {
		when(jwtTokenService.parseAccessToken("invalid-token"))
				.thenThrow(new JwtAuthenticationException("유효하지 않은 access token입니다."));

		assertThatThrownBy(() -> userLookupService.lookupByEmail("invalid-token", "member@example.com"))
				.isInstanceOf(JwtAuthenticationException.class);
		verify(userRepository, never()).findByEmail("member@example.com");
	}

	@Test
	void lookupByEmailThrowsWhenRequesterDoesNotExist() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "requester@example.com"));
		when(userRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userLookupService.lookupByEmail("valid-token", "member@example.com"))
				.isInstanceOf(AuthenticationFailedException.class);
		verify(userRepository, never()).findByEmail("member@example.com");
	}

	@Test
	void lookupByEmailThrowsWhenRequesterIsNotActive() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "requester@example.com"));
		User requester = user(1L, "requester@example.com", "requester", UserStatus.SUSPENDED);
		when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

		assertThatThrownBy(() -> userLookupService.lookupByEmail("valid-token", "member@example.com"))
				.isInstanceOf(AuthenticationFailedException.class);
		verify(userRepository, never()).findByEmail("member@example.com");
	}

	@Test
	void lookupByEmailThrowsWhenRequesterIsNotCompanyAdmin() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "requester@example.com"));
		User requester = user(1L, "requester@example.com", "requester", UserStatus.ACTIVE);
		when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
		when(jdbcTemplate.queryForObject(companyAdminQuery(), Integer.class, 1L)).thenReturn(0);

		assertThatThrownBy(() -> userLookupService.lookupByEmail("valid-token", "member@example.com"))
				.isInstanceOf(ForbiddenException.class);
		verify(userRepository, never()).findByEmail("member@example.com");
	}

	@Test
	void lookupByEmailDeniesCompanyAdminWhenNoActiveCompanyExists() {
		when(jwtTokenService.parseAccessToken("valid-token")).thenReturn(new AuthenticatedUser(1L, "requester@example.com"));
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "requester@example.com", "requester", UserStatus.ACTIVE)));
		when(jdbcTemplate.queryForObject(companyAdminQuery(), Integer.class, 1L)).thenReturn(0);

		assertThatThrownBy(() -> userLookupService.lookupByEmail("valid-token", "member@example.com"))
				.isInstanceOf(ForbiddenException.class);
		verify(userRepository, never()).findByEmail("member@example.com");
	}

	private void stubActiveRequester() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "requester@example.com"));
		User requester = user(1L, "requester@example.com", "requester", UserStatus.ACTIVE);
		when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
		when(jdbcTemplate.queryForObject(companyAdminQuery(), Integer.class, 1L)).thenReturn(1);
	}

	private static String companyAdminQuery() {
		return """
				SELECT COUNT(*)
				FROM company_members company_member
				JOIN companies company ON company.id = company_member.company_id
				WHERE company_member.user_id = ?
				  AND company_member.role = 'COMPANY_ADMIN'
				  AND company_member.status = 'ACTIVE'
				  AND company.status = 'ACTIVE'
				""";
	}

	private static User user(Long id, String email, String nickname, UserStatus status) {
		LocalDateTime now = LocalDateTime.now();
		User user = new User(email, "hashed", nickname, null, status, now, now);
		setId(user, id);
		return user;
	}

	private static void setId(User user, Long id) {
		try {
			Field field = User.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(user, id);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
