package com.threadia.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

@ExtendWith(MockitoExtension.class)
class MeServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-14T12:34:56Z"), ZoneOffset.UTC);
	private static final LocalDateTime FIXED_UPDATED_AT = LocalDateTime.of(2026, 7, 14, 12, 34, 56);

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private UserRepository userRepository;

	private MeService meService;

	@BeforeEach
	void setUp() {
		meService = new MeService(jwtTokenService, userRepository, FIXED_CLOCK);
	}

	@Test
	void 정상_토큰이면_ACTIVE_사용자의_4개_필드만_반환한다() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "user@example.com"));
		when(userRepository.findById(1L)).thenReturn(Optional.of(user("user@example.com", "nick", null, UserStatus.ACTIVE)));

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
		when(userRepository.findById(1L)).thenReturn(Optional.of(user("user@example.com", "nick", null, UserStatus.SUSPENDED)));

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

	@Test
	void nickname만_수정하면_생략한_avatarUrl은_DB에_맡기고_재조회_응답을_반환한다() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "user@example.com"));
		when(userRepository.updateActiveProfile(eq(1L), eq(true), eq("new-nick"), eq(false), isNull(),
				eq(FIXED_UPDATED_AT))).thenReturn(1);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(
				"user@example.com", "new-nick", "https://example.com/concurrent-avatar.png", UserStatus.ACTIVE)));
		ProfileUpdateRequest request = new ProfileUpdateRequest();
		request.setNickname("new-nick");

		MeData data = meService.updateProfile("valid-token", request);

		assertThat(data.user().nickname()).isEqualTo("new-nick");
		assertThat(data.user().avatarUrl()).isEqualTo("https://example.com/concurrent-avatar.png");
		verify(userRepository).updateActiveProfile(eq(1L), eq(true), eq("new-nick"), eq(false), isNull(),
				eq(FIXED_UPDATED_AT));
		verify(userRepository).findById(1L);
	}

	@Test
	void avatarUrl만_수정하면_생략한_nickname은_DB에_맡기고_재조회_응답을_반환한다() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "user@example.com"));
		when(userRepository.updateActiveProfile(eq(1L), eq(false), isNull(), eq(true),
				eq("https://example.com/new.png"), eq(FIXED_UPDATED_AT))).thenReturn(1);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(
				"user@example.com", "concurrent-nick", "https://example.com/new.png", UserStatus.ACTIVE)));
		ProfileUpdateRequest request = new ProfileUpdateRequest();
		request.setAvatarUrl("https://example.com/new.png");

		MeData data = meService.updateProfile("valid-token", request);

		assertThat(data.user().nickname()).isEqualTo("concurrent-nick");
		assertThat(data.user().avatarUrl()).isEqualTo("https://example.com/new.png");
		verify(userRepository).updateActiveProfile(eq(1L), eq(false), isNull(), eq(true),
				eq("https://example.com/new.png"), eq(FIXED_UPDATED_AT));
	}

	@Test
	void nickname과_avatarUrl을_함께_수정하고_재조회한_응답을_반환한다() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "user@example.com"));
		when(userRepository.updateActiveProfile(eq(1L), eq(true), eq("new-nick"), eq(true),
				eq("https://example.com/new.png"), eq(FIXED_UPDATED_AT))).thenReturn(1);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(
				"user@example.com", "new-nick", "https://example.com/new.png", UserStatus.ACTIVE)));
		ProfileUpdateRequest request = new ProfileUpdateRequest();
		request.setNickname("new-nick");
		request.setAvatarUrl("https://example.com/new.png");

		MeData data = meService.updateProfile("valid-token", request);

		assertThat(data.user().nickname()).isEqualTo("new-nick");
		assertThat(data.user().avatarUrl()).isEqualTo("https://example.com/new.png");
	}

	@Test
	void avatarUrl_null은_명시적_clear_flag와_함께_단일_update로_전달한다() {
		when(jwtTokenService.parseAccessToken("valid-token"))
				.thenReturn(new AuthenticatedUser(1L, "user@example.com"));
		when(userRepository.updateActiveProfile(eq(1L), eq(false), isNull(), eq(true), isNull(),
				eq(FIXED_UPDATED_AT))).thenReturn(1);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user("user@example.com", "nick", null, UserStatus.ACTIVE)));
		ProfileUpdateRequest request = new ProfileUpdateRequest();
		request.setAvatarUrl(null);

		MeData data = meService.updateProfile("valid-token", request);

		assertThat(data.user().avatarUrl()).isNull();
		verify(userRepository).updateActiveProfile(eq(1L), eq(false), isNull(), eq(true), isNull(),
				eq(FIXED_UPDATED_AT));
	}

	@Test
	void guarded_update가_0행이거나_재조회_시_ACTIVE_사용자가_없으면_인증에_실패한다() {
		when(jwtTokenService.parseAccessToken("race-token"))
				.thenReturn(new AuthenticatedUser(1L, "user@example.com"));
		ProfileUpdateRequest request = new ProfileUpdateRequest();
		request.setNickname("new-nick");
		when(userRepository.updateActiveProfile(eq(1L), eq(true), eq("new-nick"), eq(false), isNull(),
				eq(FIXED_UPDATED_AT))).thenReturn(0);

		assertThatThrownBy(() -> meService.updateProfile("race-token", request))
				.isInstanceOf(AuthenticationFailedException.class);
		verify(userRepository, never()).findById(1L);

		when(userRepository.updateActiveProfile(eq(1L), eq(true), eq("new-nick"), eq(false), isNull(),
				eq(FIXED_UPDATED_AT))).thenReturn(1);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user("user@example.com", "new-nick", null, UserStatus.SUSPENDED)));
		assertThatThrownBy(() -> meService.updateProfile("race-token", request))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	private static User user(String email, String nickname, String avatarUrl, UserStatus status) {
		LocalDateTime now = LocalDateTime.now();
		User user = new User(email, "hashed", nickname, avatarUrl, status, now, now);
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
