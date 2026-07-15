package com.threadia.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.threadia.backend.auth.domain.RefreshToken;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.dto.LoginRequest;
import com.threadia.backend.auth.dto.SignupData;
import com.threadia.backend.auth.dto.SignupRequest;
import com.threadia.backend.auth.repository.RefreshTokenRepository;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.common.exception.EmailAlreadyExistsException;
import com.threadia.backend.config.cookie.RefreshTokenCookieProviderImpl;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenServiceImpl;
import com.threadia.backend.config.token.RefreshTokenGenerator;

/**
 * signup/login 로직을 curl/Postman으로 확인한 시나리오 그대로 단위 테스트로 남긴다.
 * DB 구현체 없이 순수 비즈니스 로직(분기)만 검증하므로 THREADIA_DB_PASSWORD 없이도 항상 실행된다.
 *
 * <p>JwtTokenService/RefreshTokenCookieProvider는 mock이 아니라 실제 구현체(JwtTokenServiceImpl,
 * RefreshTokenCookieProviderImpl)를 사용한다. I/O가 없는 순수 설정 기반 클래스라 mock으로 값을
 * stub하면 "AuthService가 값을 잘 옮겨 담는지"만 검증되고 실제 서명/Duration 계산 로직은
 * 전혀 실행되지 않기 때문이다. 실제 구현체를 쓰면 expiresIn/refresh token 만료가
 * 실제로 같은 출처에서 나오는지까지 검증된다. Mock은 DB(UserRepository, RefreshTokenRepository)
 * 처럼 진짜 I/O 경계에만 남긴다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final String JWT_SECRET = "01234567890123456789012345678901";
	private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 1800L;
	private static final Duration REFRESH_TOKEN_MAX_AGE = Duration.ofDays(14);

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private RefreshTokenGenerator refreshTokenGenerator;

	private JwtTokenServiceImpl jwtTokenService;
	private RefreshTokenCookieProviderImpl refreshTokenCookieProvider;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		jwtTokenService = new JwtTokenServiceImpl(JWT_SECRET, ACCESS_TOKEN_EXPIRATION_SECONDS);
		refreshTokenCookieProvider = new RefreshTokenCookieProviderImpl(false, 14);
		authService = new AuthService(
				userRepository, refreshTokenRepository, passwordEncoder,
				jwtTokenService, refreshTokenGenerator, refreshTokenCookieProvider);
	}

	@Test
	void signup_성공시_email을_정규화해서_ACTIVE_상태로_저장하고_토큰없이_반환한다() {
		when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password1234")).thenReturn("hashed");

		SignupRequest request = new SignupRequest(
				" USER@Example.com ", "password1234", "nick", "https://example.com/avatar.png");
		SignupData data = authService.signup(request);

		assertThat(data.user().email()).isEqualTo("user@example.com");
		assertThat(data.user().nickname()).isEqualTo("nick");

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User saved = captor.getValue();
		assertThat(saved.getEmail()).isEqualTo("user@example.com");
		assertThat(saved.getPasswordHash()).isEqualTo("hashed");
		assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(saved.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
	}

	@Test
	void signup_이미_가입된_email이면_EmailAlreadyExistsException을_던지고_저장하지_않는다() {
		when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

		SignupRequest request = new SignupRequest("dup@example.com", "password1234", "nick", null);

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOf(EmailAlreadyExistsException.class);

		verify(userRepository, never()).save(any());
	}

	@Test
	void login_성공시_accessToken과_refresh_cookie를_발급하고_refreshToken을_저장한다() {
		User user = userWithStatus("hashed", UserStatus.ACTIVE);
		when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password1234", "hashed")).thenReturn(true);
		when(refreshTokenGenerator.generateRawToken()).thenReturn("raw-token");
		when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed-token");

		LoginRequest request = new LoginRequest("USER@Example.com", "password1234");
		LoginResult result = authService.login(request);

		// accessToken이 실제로 유효한 JWT인지, 그리고 그 안의 userId/email이 로그인한 사용자와 일치하는지 실제 파싱으로 확인한다.
		AuthenticatedUser parsed = jwtTokenService.parseAccessToken(result.data().accessToken());
		assertThat(parsed.userId()).isEqualTo(user.getId());
		assertThat(parsed.email()).isEqualTo("user@example.com");

		assertThat(result.data().tokenType()).isEqualTo("Bearer");
		// expiresIn이 JwtTokenServiceImpl 생성 시 넣은 실제 만료초와 동일한 출처에서 나오는지 검증한다(하드코딩 값과의 우연한 일치가 아님).
		assertThat(result.data().expiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRATION_SECONDS);
		assertThat(result.data().companies()).isEmpty();

		// refresh 쿠키가 실제 RefreshTokenCookieProviderImpl 정책(HttpOnly/SameSite=Lax/Path=/, 14일)을 그대로 따르는지 확인한다.
		ResponseCookie cookie = result.refreshCookie();
		assertThat(cookie.getName()).isEqualTo("refreshToken");
		assertThat(cookie.getValue()).isEqualTo("raw-token");
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
		assertThat(cookie.getPath()).isEqualTo("/");
		assertThat(cookie.getMaxAge()).isEqualTo(REFRESH_TOKEN_MAX_AGE);

		// DB에 저장된 RefreshToken의 만료시각도 쿠키와 같은 Duration(14일)에서 계산됐는지 확인한다.
		ArgumentCaptor<com.threadia.backend.auth.domain.RefreshToken> captor =
				ArgumentCaptor.forClass(com.threadia.backend.auth.domain.RefreshToken.class);
		verify(refreshTokenRepository).save(captor.capture());
		com.threadia.backend.auth.domain.RefreshToken savedToken = captor.getValue();
		assertThat(savedToken.getTokenHash()).isEqualTo("hashed-token");
		Duration actualLifetime = Duration.between(savedToken.getCreatedAt(), savedToken.getExpiresAt());
		assertThat(actualLifetime).isCloseTo(REFRESH_TOKEN_MAX_AGE, Duration.ofSeconds(1));
	}

	@Test
	void login_비밀번호가_틀리면_AuthenticationFailedException() {
		User user = userWithStatus("hashed", UserStatus.ACTIVE);
		when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

		LoginRequest request = new LoginRequest("user@example.com", "wrong-password");

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void login_존재하지_않는_email이면_AuthenticationFailedException() {
		when(userRepository.findByEmail("none@example.com")).thenReturn(Optional.empty());

		LoginRequest request = new LoginRequest("none@example.com", "password1234");

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void login_SUSPENDED_사용자는_비밀번호가_맞아도_거부한다() {
		User user = userWithStatus("hashed", UserStatus.SUSPENDED);
		when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password1234", "hashed")).thenReturn(true);

		LoginRequest request = new LoginRequest("user@example.com", "password1234");

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void login_소셜전용_계정은_비밀번호_대조없이_거부하고_NPE가_나지_않는다() {
		User user = userWithStatus(null, UserStatus.ACTIVE);
		when(userRepository.findByEmail("social@example.com")).thenReturn(Optional.of(user));

		LoginRequest request = new LoginRequest("social@example.com", "password1234");

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(AuthenticationFailedException.class);

		verify(passwordEncoder, never()).matches(any(), any());
	}

	@Test
	void refresh_성공시_기존_refreshToken을_rotate하고_새_accessToken과_refresh_cookie를_발급한다() {
		User user = userWithStatus("hashed", UserStatus.ACTIVE);
		LocalDateTime now = nowUtc();
		RefreshToken oldRefreshToken = new RefreshToken(
				user,
				"hashed-old-refresh-token",
				now.plusDays(14),
				now,
				now);
		when(refreshTokenGenerator.hash("old-raw-refresh-token")).thenReturn("hashed-old-refresh-token");
		when(refreshTokenRepository.findByTokenHash("hashed-old-refresh-token")).thenReturn(Optional.of(oldRefreshToken));
		when(refreshTokenGenerator.generateRawToken()).thenReturn("new-raw-refresh-token");
		when(refreshTokenGenerator.hash("new-raw-refresh-token")).thenReturn("hashed-new-refresh-token");

		LoginResult result = authService.refresh("old-raw-refresh-token");

		AuthenticatedUser parsed = jwtTokenService.parseAccessToken(result.data().accessToken());
		assertThat(parsed.userId()).isEqualTo(user.getId());
		assertThat(parsed.email()).isEqualTo("user@example.com");
		assertThat(result.data().expiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRATION_SECONDS);
		assertThat(result.data().companies()).isEmpty();

		ResponseCookie cookie = result.refreshCookie();
		assertThat(cookie.getName()).isEqualTo("refreshToken");
		assertThat(cookie.getValue()).isEqualTo("new-raw-refresh-token");
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
		assertThat(cookie.getPath()).isEqualTo("/");
		assertThat(cookie.getMaxAge()).isEqualTo(REFRESH_TOKEN_MAX_AGE);

		assertThat(oldRefreshToken.getRotatedAt()).isNotNull();
		assertThat(oldRefreshToken.getRevokedAt()).isEqualTo(oldRefreshToken.getRotatedAt());
		assertThat(oldRefreshToken.getUpdatedAt()).isEqualTo(oldRefreshToken.getRotatedAt());

		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository, times(2)).save(captor.capture());
		assertThat(captor.getAllValues().get(0)).isSameAs(oldRefreshToken);
		RefreshToken newRefreshToken = captor.getAllValues().get(1);
		assertThat(newRefreshToken).isNotSameAs(oldRefreshToken);
		assertThat(newRefreshToken.getUser()).isSameAs(user);
		assertThat(newRefreshToken.getTokenHash()).isEqualTo("hashed-new-refresh-token");
		assertThat(newRefreshToken.getRevokedAt()).isNull();
		assertThat(newRefreshToken.getRotatedAt()).isNull();
		Duration actualLifetime = Duration.between(newRefreshToken.getCreatedAt(), newRefreshToken.getExpiresAt());
		assertThat(actualLifetime).isEqualTo(REFRESH_TOKEN_MAX_AGE);
	}

	@Test
	void refresh_missing_cookie_throws_AuthenticationFailedException_without_hash_lookup_or_save() {
		assertThatThrownBy(() -> authService.refresh(null))
				.isInstanceOf(AuthenticationFailedException.class);

		verify(refreshTokenGenerator, never()).hash(any());
		verify(refreshTokenRepository, never()).findByTokenHash(any());
		verify(refreshTokenGenerator, never()).generateRawToken();
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void refresh_DB에_없는_refreshToken이면_AuthenticationFailedException을_던지고_새_토큰을_발급하지_않는다() {
		when(refreshTokenGenerator.hash("unknown-raw-refresh-token")).thenReturn("hashed-unknown-refresh-token");
		when(refreshTokenRepository.findByTokenHash("hashed-unknown-refresh-token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.refresh("unknown-raw-refresh-token"))
				.isInstanceOf(AuthenticationFailedException.class);

		verify(refreshTokenGenerator, never()).generateRawToken();
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void refresh_이미_revoke된_refreshToken이면_AuthenticationFailedException을_던지고_새_토큰을_발급하지_않는다() {
		User user = userWithStatus("hashed", UserStatus.ACTIVE);
		LocalDateTime now = nowUtc();
		RefreshToken refreshToken = new RefreshToken(
				user,
				"hashed-refresh-token",
				now.plusDays(14),
				now,
				now);
		LocalDateTime alreadyRevokedAt = now.minusMinutes(5);
		refreshToken.revoke(alreadyRevokedAt);
		when(refreshTokenGenerator.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
		when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(refreshToken));

		assertThatThrownBy(() -> authService.refresh("raw-refresh-token"))
				.isInstanceOf(AuthenticationFailedException.class);

		assertThat(refreshToken.getRevokedAt()).isEqualTo(alreadyRevokedAt);
		assertThat(refreshToken.getRotatedAt()).isNull();
		verify(refreshTokenGenerator, never()).generateRawToken();
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void refresh_만료된_refreshToken이면_AuthenticationFailedException을_던지고_새_토큰을_발급하지_않는다() {
		User user = userWithStatus("hashed", UserStatus.ACTIVE);
		LocalDateTime now = nowUtc();
		RefreshToken refreshToken = new RefreshToken(
				user,
				"hashed-refresh-token",
				now.minusSeconds(1),
				now.minusDays(14),
				now.minusDays(14));
		when(refreshTokenGenerator.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
		when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(refreshToken));

		assertThatThrownBy(() -> authService.refresh("raw-refresh-token"))
				.isInstanceOf(AuthenticationFailedException.class);

		assertThat(refreshToken.getRevokedAt()).isNull();
		assertThat(refreshToken.getRotatedAt()).isNull();
		verify(refreshTokenGenerator, never()).generateRawToken();
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void refresh_비활성_사용자의_refreshToken이면_AuthenticationFailedException을_던지고_새_토큰을_발급하지_않는다() {
		User user = userWithStatus("hashed", UserStatus.SUSPENDED);
		LocalDateTime now = nowUtc();
		RefreshToken refreshToken = new RefreshToken(
				user,
				"hashed-refresh-token",
				now.plusDays(14),
				now,
				now);
		when(refreshTokenGenerator.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
		when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(refreshToken));

		assertThatThrownBy(() -> authService.refresh("raw-refresh-token"))
				.isInstanceOf(AuthenticationFailedException.class);

		assertThat(refreshToken.getRevokedAt()).isNull();
		assertThat(refreshToken.getRotatedAt()).isNull();
		verify(refreshTokenGenerator, never()).generateRawToken();
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void logout_success_revokes_refresh_token_and_returns_clear_cookie() {
		User user = userWithStatus("hashed", UserStatus.ACTIVE);
		String accessToken = jwtTokenService.issueAccessToken(user.getId(), user.getEmail());
		LocalDateTime now = nowUtc();
		RefreshToken refreshToken = new RefreshToken(
				user,
				"hashed-refresh-token",
				now.plusDays(14),
				now,
				now);
		when(refreshTokenGenerator.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
		when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(refreshToken));

		LogoutResult result = authService.logout(accessToken, "raw-refresh-token");

		assertThat(result.data().success()).isTrue();
		assertThat(result.clearCookie().getName()).isEqualTo("refreshToken");
		assertThat(result.clearCookie().getValue()).isEmpty();
		assertThat(result.clearCookie().getMaxAge()).isEqualTo(Duration.ZERO);
		assertThat(refreshToken.getRevokedAt()).isNotNull();
		assertThat(refreshToken.getUpdatedAt()).isEqualTo(refreshToken.getRevokedAt());
		verify(refreshTokenRepository).save(refreshToken);
	}

	@Test
	void logout_missing_refresh_cookie_throws_AuthenticationFailedException() {
		User user = userWithStatus("hashed", UserStatus.ACTIVE);
		String accessToken = jwtTokenService.issueAccessToken(user.getId(), user.getEmail());

		assertThatThrownBy(() -> authService.logout(accessToken, null))
				.isInstanceOf(AuthenticationFailedException.class);

		verify(refreshTokenRepository, never()).findByTokenHash(any());
	}

	@Test
	void logout_이미_revoke된_refreshToken이면_AuthenticationFailedException을_던지고_다시_revoke하지_않는다() {
		User user = userWithStatus("hashed", UserStatus.ACTIVE);
		String accessToken = jwtTokenService.issueAccessToken(user.getId(), user.getEmail());
		LocalDateTime now = nowUtc();
		RefreshToken refreshToken = new RefreshToken(
				user,
				"hashed-refresh-token",
				now.plusDays(14),
				now,
				now);
		LocalDateTime alreadyRevokedAt = now.minusMinutes(5);
		refreshToken.revoke(alreadyRevokedAt);
		when(refreshTokenGenerator.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
		when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(refreshToken));

		assertThatThrownBy(() -> authService.logout(accessToken, "raw-refresh-token"))
				.isInstanceOf(AuthenticationFailedException.class);

		// revoked_at이 재요청 시각으로 덮어써지지 않아야 감사 로그가 "최초 로그아웃 시각" 그대로 보존된다.
		assertThat(refreshToken.getRevokedAt()).isEqualTo(alreadyRevokedAt);
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void logout_refresh_token_for_different_user_throws_AuthenticationFailedException() {
		User loginUser = userWithStatus("hashed", UserStatus.ACTIVE);
		String accessToken = jwtTokenService.issueAccessToken(loginUser.getId(), loginUser.getEmail());
		User otherUser = new User("other@example.com", "hashed", "other", null, UserStatus.ACTIVE,
				nowUtc(), nowUtc());
		setId(otherUser, 2L);
		RefreshToken refreshToken = new RefreshToken(
				otherUser,
				"hashed-refresh-token",
				nowUtc().plusDays(14),
				nowUtc(),
				nowUtc());
		when(refreshTokenGenerator.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
		when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(refreshToken));

		assertThatThrownBy(() -> authService.logout(accessToken, "raw-refresh-token"))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void logout_DB에_없는_refreshToken이면_AuthenticationFailedException을_던진다() {
		User user = userWithStatus("hashed", UserStatus.ACTIVE);
		String accessToken = jwtTokenService.issueAccessToken(user.getId(), user.getEmail());
		when(refreshTokenGenerator.hash("unknown-refresh-token")).thenReturn("hashed-unknown");
		when(refreshTokenRepository.findByTokenHash("hashed-unknown")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.logout(accessToken, "unknown-refresh-token"))
				.isInstanceOf(AuthenticationFailedException.class);

		verify(refreshTokenRepository, never()).save(any());
	}

	private static User userWithStatus(String passwordHash, UserStatus status) {
		LocalDateTime now = nowUtc();
		User user = new User("user@example.com", passwordHash, "nick", null, status, now, now);
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

	// AuthService(nowUtc())와 동일한 기준으로 시각을 맞춘다. LocalDateTime.now()(로컬 timezone)를 쓰면
	// KST 등 UTC+9 환경에서 만료/미만료 경계값이 서비스 쪽 UTC 판정과 어긋나 테스트가 로컬에서만 실패한다.
	private static LocalDateTime nowUtc() {
		return LocalDateTime.now(Clock.system(ZoneOffset.UTC));
	}
}
