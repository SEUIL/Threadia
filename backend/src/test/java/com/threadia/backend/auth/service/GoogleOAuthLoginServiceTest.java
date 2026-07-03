package com.threadia.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.threadia.backend.auth.domain.OAuthAccount;
import com.threadia.backend.auth.domain.OAuthProvider;
import com.threadia.backend.auth.domain.RefreshToken;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.OAuthAccountRepository;
import com.threadia.backend.auth.repository.RefreshTokenRepository;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.config.cookie.RefreshTokenCookieProviderImpl;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenServiceImpl;
import com.threadia.backend.config.token.RefreshTokenGenerator;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthLoginServiceTest {

	private static final String JWT_SECRET = "01234567890123456789012345678901";
	private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 1800L;
	private static final Duration REFRESH_TOKEN_MAX_AGE = Duration.ofDays(14);

	@Mock
	private OAuthAccountRepository oauthAccountRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private RefreshTokenGenerator refreshTokenGenerator;

	private JwtTokenServiceImpl jwtTokenService;
	private GoogleOAuthLoginService service;

	@BeforeEach
	void setUp() {
		jwtTokenService = new JwtTokenServiceImpl(JWT_SECRET, ACCESS_TOKEN_EXPIRATION_SECONDS);
		service = new GoogleOAuthLoginService(
				oauthAccountRepository,
				userRepository,
				refreshTokenRepository,
				jwtTokenService,
				refreshTokenGenerator,
				new RefreshTokenCookieProviderImpl(false, 14));
	}

	@Test
	void oauth_account가_없으면_새_소셜전용_user와_oauth_account를_만들고_토큰을_발급한다() {
		OAuth2User oauth2User = googleUser("google-sub", " USER@Example.com ", "구글사용자", "https://example.com/avatar.png");
		when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub"))
				.thenReturn(Optional.empty());
		when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
		when(userRepository.save(any())).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			setId(user, 1L);
			return user;
		});
		when(refreshTokenGenerator.generateRawToken()).thenReturn("raw-token");
		when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed-token");

		OAuthLoginResult result = service.login(oauth2User);

		AuthenticatedUser parsed = jwtTokenService.parseAccessToken(result.data().accessToken());
		assertThat(parsed.userId()).isEqualTo(1L);
		assertThat(parsed.email()).isEqualTo("user@example.com");
		assertThat(result.data().user().email()).isEqualTo("user@example.com");
		assertThat(result.refreshCookie().getValue()).isEqualTo("raw-token");
		assertThat(result.refreshCookie().isHttpOnly()).isTrue();
		assertThat(result.refreshCookie().getSameSite()).isEqualTo("Lax");

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
		assertThat(savedUser.getPasswordHash()).isNull();
		assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

		ArgumentCaptor<OAuthAccount> oauthCaptor = ArgumentCaptor.forClass(OAuthAccount.class);
		verify(oauthAccountRepository).save(oauthCaptor.capture());
		assertThat(oauthCaptor.getValue().getProvider()).isEqualTo(OAuthProvider.GOOGLE);
		assertThat(oauthCaptor.getValue().getProviderUserId()).isEqualTo("google-sub");
		assertThat(oauthCaptor.getValue().getProviderEmail()).isEqualTo("user@example.com");

		ArgumentCaptor<RefreshToken> refreshCaptor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(refreshCaptor.capture());
		RefreshToken refreshToken = refreshCaptor.getValue();
		assertThat(refreshToken.getTokenHash()).isEqualTo("hashed-token");
		assertThat(Duration.between(refreshToken.getCreatedAt(), refreshToken.getExpiresAt()))
				.isCloseTo(REFRESH_TOKEN_MAX_AGE, Duration.ofSeconds(1));
	}

	@Test
	void 기존_oauth_account가_있으면_연결된_ACTIVE_user로_토큰을_발급한다() {
		User user = user("user@example.com", UserStatus.ACTIVE);
		OAuthAccount account = new OAuthAccount(user, OAuthProvider.GOOGLE, "google-sub", "user@example.com", now(), now());
		when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub"))
				.thenReturn(Optional.of(account));
		when(refreshTokenGenerator.generateRawToken()).thenReturn("raw-token");
		when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed-token");

		OAuthLoginResult result = service.login(googleUser("google-sub", "user@example.com", "name", null));

		assertThat(jwtTokenService.parseAccessToken(result.data().accessToken()).userId()).isEqualTo(1L);
		verify(userRepository, never()).findByEmail(any());
		verify(userRepository, never()).save(any());
		verify(oauthAccountRepository, never()).save(any());
	}

	@Test
	void 같은_email의_local_user가_있으면_blind_auto_link하지_않는다() {
		when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub"))
				.thenReturn(Optional.empty());
		when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user("user@example.com", UserStatus.ACTIVE)));

		assertThatThrownBy(() -> service.login(googleUser("google-sub", "user@example.com", "name", null)))
				.isInstanceOf(OAuthLoginException.class);

		verify(userRepository, never()).save(any());
		verify(oauthAccountRepository, never()).save(any());
		verify(refreshTokenRepository, never()).save(any());
	}


	@Test
	void Google_email_verified가_false이면_거부한다() {
		assertThatThrownBy(() -> service.login(googleUser("google-sub", "user@example.com", "name", null, false)))
				.isInstanceOf(OAuthLoginException.class);

		verify(oauthAccountRepository, never()).findByProviderAndProviderUserId(any(), any());
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void Google_email_verified가_없으면_거부한다() {
		assertThatThrownBy(() -> service.login(googleUserWithoutEmailVerified()))
				.isInstanceOf(OAuthLoginException.class);

		verify(oauthAccountRepository, never()).findByProviderAndProviderUserId(any(), any());
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void Google_email_verified가_boolean이_아니면_거부한다() {
		assertThatThrownBy(() -> service.login(googleUser("google-sub", "user@example.com", "name", null, "true")))
				.isInstanceOf(OAuthLoginException.class);

		verify(oauthAccountRepository, never()).findByProviderAndProviderUserId(any(), any());
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	void 연결된_user가_ACTIVE가_아니면_거부한다() {
		User user = user("user@example.com", UserStatus.SUSPENDED);
		OAuthAccount account = new OAuthAccount(user, OAuthProvider.GOOGLE, "google-sub", "user@example.com", now(), now());
		when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub"))
				.thenReturn(Optional.of(account));

		assertThatThrownBy(() -> service.login(googleUser("google-sub", "user@example.com", "name", null)))
				.isInstanceOf(OAuthLoginException.class);

		verify(refreshTokenRepository, never()).save(any());
	}

	private static OAuth2User googleUser(String sub, String email, String name, String picture) {
		return googleUser(sub, email, name, picture, true);
	}

	private static OAuth2User googleUser(String sub, String email, String name, String picture, Object emailVerified) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("sub", sub);
		attributes.put("email", email);
		attributes.put("name", name);
		attributes.put("picture", picture == null ? "" : picture);
		attributes.put("email_verified", emailVerified);
		return new DefaultOAuth2User(java.util.List.of(), attributes, "sub");
	}

	private static OAuth2User googleUserWithoutEmailVerified() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("sub", "google-sub");
		attributes.put("email", "user@example.com");
		attributes.put("name", "name");
		attributes.put("picture", "");
		return new DefaultOAuth2User(java.util.List.of(), attributes, "sub");
	}

	private static User user(String email, UserStatus status) {
		User user = new User(email, null, "nick", null, status, now(), now());
		setId(user, 1L);
		return user;
	}

	private static LocalDateTime now() {
		return LocalDateTime.now();
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
