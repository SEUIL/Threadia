package com.threadia.backend.user;

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
import com.threadia.backend.config.jwt.JwtAuthenticationException;
import com.threadia.backend.config.jwt.JwtTokenService;

// /auth/me 로직을 curl/Postman으로 확인한 시나리오 그대로 단위 테스트로 남긴다.
@ExtendWith(MockitoExtension.class)
class MeServiceTest {

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private UserRepository userRepository;

	private MeService meService;

	@BeforeEach
	void setUp() {
		meService = new MeService(jwtTokenService, userRepository);
	}

	@Test
	void 정상_토큰이면_ACTIVE_사용자의_4개_필드만_반환한다() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "user@example.com"));
		User user = userWithStatus(UserStatus.ACTIVE);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		MeData data = meService.getMe("valid-token");

		assertThat(data.user().id()).isEqualTo(1L);
		assertThat(data.user().email()).isEqualTo("user@example.com");
		assertThat(data.user().nickname()).isEqualTo("nick");
	}

	@Test
	void 토큰의_userId에_해당하는_사용자가_없으면_AuthenticationFailedException() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(999L, "ghost@example.com"));
		when(userRepository.findById(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> meService.getMe("valid-token"))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void SUSPENDED_사용자는_토큰이_유효해도_거부한다() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "user@example.com"));
		User user = userWithStatus(UserStatus.SUSPENDED);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> meService.getMe("valid-token"))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void 토큰_검증에_실패하면_JwtAuthenticationException이_그대로_전파된다() {
		when(jwtTokenService.parseAccessToken("bad-token"))
				.thenThrow(new JwtAuthenticationException("유효하지 않은 access token입니다."));

		assertThatThrownBy(() -> meService.getMe("bad-token"))
				.isInstanceOf(JwtAuthenticationException.class);
	}

	private static User userWithStatus(UserStatus status) {
		LocalDateTime now = LocalDateTime.now();
		User user = new User("user@example.com", "hashed", "nick", null, status, now, now);
		setId(user, 1L);
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
