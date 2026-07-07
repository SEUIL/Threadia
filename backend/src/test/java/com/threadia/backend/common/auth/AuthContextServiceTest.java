package com.threadia.backend.common.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenService;

@ExtendWith(MockitoExtension.class)
class AuthContextServiceTest {

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private UserRepository userRepository;

	private AuthContextService authContextService;

	@BeforeEach
	void setUp() {
		authContextService = new AuthContextService(jwtTokenService, userRepository);
	}

	@Test
	void fromAccessTokenReturnsAuthContextForActiveUser() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "user@example.com"));
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserStatus.ACTIVE)));

		AuthContext authContext = authContextService.fromAccessToken("valid-token");

		assertThat(authContext.userId()).isEqualTo(1L);
		assertThat(authContext.email()).isEqualTo("user@example.com");
	}

	@Test
	void fromAccessTokenThrowsWhenUserIsNotActive() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "user@example.com"));
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserStatus.SUSPENDED)));

		assertThatThrownBy(() -> authContextService.fromAccessToken("valid-token"))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void requireActiveUserReturnsActiveUser() {
		AuthContext authContext = new AuthContext(1L, "user@example.com");
		User user = user(1L, UserStatus.ACTIVE);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		assertThat(authContextService.requireActiveUser(authContext)).isSameAs(user);
	}

	private static User user(Long id, UserStatus status) {
		LocalDateTime now = LocalDateTime.now();
		User user = new User("user@example.com", "hashed", "nick", null, status, now, now);
		setId(user, id);
		return user;
	}

	private static void setId(User user, Long id) {
		try {
			Field field = User.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(user, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
