package com.threadia.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.threadia.backend.auth.domain.PasswordResetToken;
import com.threadia.backend.auth.domain.RefreshToken;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.dto.PasswordResetConfirmRequest;
import com.threadia.backend.auth.dto.PasswordResetRequest;
import com.threadia.backend.auth.repository.PasswordResetTokenRepository;
import com.threadia.backend.auth.repository.RefreshTokenRepository;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.email.EmailSender;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.config.token.RefreshTokenGenerator;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

	private static final long RESET_EXPIRATION_MINUTES = 30L;
	private static final String RESET_URL_PREFIX = "http://localhost:3000/reset-password?token=";

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordResetTokenRepository passwordResetTokenRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private RefreshTokenGenerator tokenGenerator;

	@Mock
	private EmailSender emailSender;

	private PasswordResetService service;

	@BeforeEach
	void setUp() {
		service = new PasswordResetService(
				userRepository,
				passwordResetTokenRepository,
				refreshTokenRepository,
				passwordEncoder,
				tokenGenerator,
				emailSender,
				RESET_EXPIRATION_MINUTES,
				RESET_URL_PREFIX);
	}

	@Test
	void request_가입된_ACTIVE_local_user면_token_hash를_저장하고_reset_email을_보낸다() {
		User user = user("user@example.com", "old-hash", UserStatus.ACTIVE);
		when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
		when(tokenGenerator.generateRawToken()).thenReturn("raw-reset-token");
		when(tokenGenerator.hash("raw-reset-token")).thenReturn("hashed-reset-token");
		when(passwordResetTokenRepository.findByUser_Id(user.getId())).thenReturn(List.of());

		service.request(new PasswordResetRequest(" USER@Example.com "));

		ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
		verify(passwordResetTokenRepository).save(captor.capture());
		PasswordResetToken saved = captor.getValue();
		assertThat(saved.getUser()).isSameAs(user);
		assertThat(saved.getTokenHash()).isEqualTo("hashed-reset-token");
		assertThat(Duration.between(saved.getCreatedAt(), saved.getExpiresAt()).toMinutes())
				.isEqualTo(RESET_EXPIRATION_MINUTES);
		assertThat(saved.getUsedAt()).isNull();
		verify(emailSender).send(
				eq("user@example.com"),
				eq("Threadia 비밀번호 재설정 안내"),
				contains(RESET_URL_PREFIX + "raw-reset-token"));
	}

	@Test
	void request_새_token을_발급하기_전에_기존_미사용_resetToken을_used처리한다() {
		User user = user("user@example.com", "old-hash", UserStatus.ACTIVE);
		PasswordResetToken oldUnusedToken = new PasswordResetToken(
				user, "old-unused-token", nowUtc().plusMinutes(10), nowUtc().minusMinutes(10));
		PasswordResetToken oldUsedToken = new PasswordResetToken(
				user, "old-used-token", nowUtc().plusMinutes(10), nowUtc().minusMinutes(10));
		LocalDateTime oldUsedAt = nowUtc().minusMinutes(5);
		oldUsedToken.markUsed(oldUsedAt);
		when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
		when(passwordResetTokenRepository.findByUser_Id(user.getId()))
				.thenReturn(List.of(oldUnusedToken, oldUsedToken));
		when(tokenGenerator.generateRawToken()).thenReturn("raw-reset-token");
		when(tokenGenerator.hash("raw-reset-token")).thenReturn("hashed-reset-token");

		service.request(new PasswordResetRequest("user@example.com"));

		assertThat(oldUnusedToken.getUsedAt()).isNotNull();
		assertThat(oldUsedToken.getUsedAt()).isEqualTo(oldUsedAt);
	}

	@Test
	void request_emailSender가_실패해도_success를_반환해_계정_존재를_노출하지_않는다() {
		User user = user("user@example.com", "old-hash", UserStatus.ACTIVE);
		when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
		when(passwordResetTokenRepository.findByUser_Id(user.getId())).thenReturn(List.of());
		when(tokenGenerator.generateRawToken()).thenReturn("raw-reset-token");
		when(tokenGenerator.hash("raw-reset-token")).thenReturn("hashed-reset-token");
		doThrow(new IllegalStateException("smtp failed"))
				.when(emailSender).send(eq("user@example.com"), any(), any());

		assertThat(service.request(new PasswordResetRequest("user@example.com")).success()).isTrue();

		verify(passwordResetTokenRepository).save(any());
	}

	@Test
	void request_없는_email이어도_success를_반환하고_token이나_email을_만들지_않는다() {
		when(userRepository.findByEmail("none@example.com")).thenReturn(Optional.empty());

		assertThat(service.request(new PasswordResetRequest("none@example.com")).success()).isTrue();

		verify(tokenGenerator, never()).generateRawToken();
		verify(passwordResetTokenRepository, never()).save(any());
		verify(emailSender, never()).send(any(), any(), any());
	}

	@Test
	void request_소셜전용_user이면_success만_반환하고_email을_보내지_않는다() {
		User user = user("social@example.com", null, UserStatus.ACTIVE);
		when(userRepository.findByEmail("social@example.com")).thenReturn(Optional.of(user));

		assertThat(service.request(new PasswordResetRequest("social@example.com")).success()).isTrue();

		verify(tokenGenerator, never()).generateRawToken();
		verify(passwordResetTokenRepository, never()).save(any());
		verify(emailSender, never()).send(any(), any(), any());
	}

	@Test
	void confirm_유효한_token이면_password를_변경하고_resetToken을_used처리하고_refreshToken을_revoke한다() {
		User user = user("user@example.com", "old-hash", UserStatus.ACTIVE);
		PasswordResetToken resetToken = new PasswordResetToken(
				user, "hashed-reset-token", nowUtc().plusMinutes(10), nowUtc().minusMinutes(20));
		PasswordResetToken otherUnusedResetToken = new PasswordResetToken(
				user, "other-reset-token", nowUtc().plusMinutes(10), nowUtc().minusMinutes(5));
		PasswordResetToken alreadyUsedResetToken = new PasswordResetToken(
				user, "used-reset-token", nowUtc().plusMinutes(10), nowUtc().minusMinutes(5));
		LocalDateTime alreadyUsedAt = nowUtc().minusMinutes(3);
		alreadyUsedResetToken.markUsed(alreadyUsedAt);
		RefreshToken activeRefreshToken = new RefreshToken(
				user, "active-refresh", nowUtc().plusDays(14), nowUtc(), nowUtc());
		RefreshToken alreadyRevokedToken = new RefreshToken(
				user, "revoked-refresh", nowUtc().plusDays(14), nowUtc(), nowUtc());
		LocalDateTime alreadyRevokedAt = nowUtc().minusMinutes(5);
		alreadyRevokedToken.revoke(alreadyRevokedAt);
		when(tokenGenerator.hash("raw-reset-token")).thenReturn("hashed-reset-token");
		when(passwordResetTokenRepository.findByTokenHash("hashed-reset-token")).thenReturn(Optional.of(resetToken));
		when(passwordResetTokenRepository.findByUser_Id(user.getId()))
				.thenReturn(List.of(resetToken, otherUnusedResetToken, alreadyUsedResetToken));
		when(passwordEncoder.encode("newpass1234")).thenReturn("new-hash");
		when(refreshTokenRepository.findByUser_Id(user.getId()))
				.thenReturn(List.of(activeRefreshToken, alreadyRevokedToken));

		service.confirm(new PasswordResetConfirmRequest("raw-reset-token", "newpass1234"));

		assertThat(user.getPasswordHash()).isEqualTo("new-hash");
		assertThat(user.getUpdatedAt()).isNotNull();
		assertThat(resetToken.getUsedAt()).isNotNull();
		assertThat(otherUnusedResetToken.getUsedAt()).isEqualTo(resetToken.getUsedAt());
		assertThat(alreadyUsedResetToken.getUsedAt()).isEqualTo(alreadyUsedAt);
		assertThat(activeRefreshToken.getRevokedAt()).isEqualTo(resetToken.getUsedAt());
		assertThat(alreadyRevokedToken.getRevokedAt()).isEqualTo(alreadyRevokedAt);
		verify(passwordResetTokenRepository).save(resetToken);
		verify(userRepository).save(user);
	}

	@Test
	void confirm_없는_token이면_AuthenticationFailedException을_던지고_상태를_바꾸지_않는다() {
		when(tokenGenerator.hash("bad-token")).thenReturn("hashed-bad-token");
		when(passwordResetTokenRepository.findByTokenHash("hashed-bad-token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.confirm(new PasswordResetConfirmRequest("bad-token", "newpass1234")))
				.isInstanceOf(AuthenticationFailedException.class);

		verify(passwordEncoder, never()).encode(any());
		verify(refreshTokenRepository, never()).findByUser_Id(any());
		verify(passwordResetTokenRepository, never()).save(any());
	}

	@Test
	void confirm_만료된_token이면_AuthenticationFailedException을_던진다() {
		User user = user("user@example.com", "old-hash", UserStatus.ACTIVE);
		PasswordResetToken resetToken = new PasswordResetToken(
				user, "hashed-reset-token", nowUtc().minusSeconds(1), nowUtc().minusMinutes(31));
		when(tokenGenerator.hash("raw-reset-token")).thenReturn("hashed-reset-token");
		when(passwordResetTokenRepository.findByTokenHash("hashed-reset-token")).thenReturn(Optional.of(resetToken));

		assertThatThrownBy(() -> service.confirm(new PasswordResetConfirmRequest("raw-reset-token", "newpass1234")))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void confirm_이미_사용된_token이면_AuthenticationFailedException을_던진다() {
		User user = user("user@example.com", "old-hash", UserStatus.ACTIVE);
		PasswordResetToken resetToken = new PasswordResetToken(
				user, "hashed-reset-token", nowUtc().plusMinutes(10), nowUtc().minusMinutes(10));
		resetToken.markUsed(nowUtc().minusMinutes(5));
		when(tokenGenerator.hash("raw-reset-token")).thenReturn("hashed-reset-token");
		when(passwordResetTokenRepository.findByTokenHash("hashed-reset-token")).thenReturn(Optional.of(resetToken));

		assertThatThrownBy(() -> service.confirm(new PasswordResetConfirmRequest("raw-reset-token", "newpass1234")))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void confirm_비활성_user의_token이면_AuthenticationFailedException을_던진다() {
		User user = user("user@example.com", "old-hash", UserStatus.SUSPENDED);
		PasswordResetToken resetToken = new PasswordResetToken(
				user, "hashed-reset-token", nowUtc().plusMinutes(10), nowUtc().minusMinutes(10));
		when(tokenGenerator.hash("raw-reset-token")).thenReturn("hashed-reset-token");
		when(passwordResetTokenRepository.findByTokenHash("hashed-reset-token")).thenReturn(Optional.of(resetToken));

		assertThatThrownBy(() -> service.confirm(new PasswordResetConfirmRequest("raw-reset-token", "newpass1234")))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	private static User user(String email, String passwordHash, UserStatus status) {
		LocalDateTime now = nowUtc();
		User user = new User(email, passwordHash, "nick", null, status, now, now);
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

	private static LocalDateTime nowUtc() {
		return LocalDateTime.now(Clock.system(ZoneOffset.UTC));
	}
}
