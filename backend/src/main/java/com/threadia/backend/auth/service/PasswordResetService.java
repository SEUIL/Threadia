package com.threadia.backend.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.PasswordResetToken;
import com.threadia.backend.auth.domain.RefreshToken;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.dto.PasswordResetConfirmRequest;
import com.threadia.backend.auth.dto.PasswordResetData;
import com.threadia.backend.auth.dto.PasswordResetRequest;
import com.threadia.backend.auth.repository.PasswordResetTokenRepository;
import com.threadia.backend.auth.repository.RefreshTokenRepository;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.email.EmailSender;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.config.token.RefreshTokenGenerator;

@Service
public class PasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
	private static final String RESET_EMAIL_SUBJECT = "Threadia 비밀번호 재설정 안내";

	private final UserRepository userRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenGenerator tokenGenerator;
	private final EmailSender emailSender;
	private final long expirationMinutes;
	private final String resetUrlPrefix;

	public PasswordResetService(
			UserRepository userRepository,
			PasswordResetTokenRepository passwordResetTokenRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			RefreshTokenGenerator tokenGenerator,
			EmailSender emailSender,
			@Value("${threadia.password-reset.expiration-minutes}") long expirationMinutes,
			@Value("${threadia.password-reset.url-prefix}") String resetUrlPrefix) {
		this.userRepository = userRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenGenerator = tokenGenerator;
		this.emailSender = emailSender;
		this.expirationMinutes = expirationMinutes;
		this.resetUrlPrefix = resetUrlPrefix;
	}

	@Transactional
	public PasswordResetData request(PasswordResetRequest request) {
		LocalDateTime now = nowUtc();
		userRepository.findByEmail(request.email())
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.filter(user -> user.getPasswordHash() != null)
				.ifPresent(user -> issueResetToken(user, now));

		return PasswordResetData.ofSuccess();
	}

	@Transactional
	public PasswordResetData confirm(PasswordResetConfirmRequest request) {
		LocalDateTime now = nowUtc();
		String tokenHash = tokenGenerator.hash(request.token());
		PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
				.filter(token -> token.getUsedAt() == null)
				.filter(token -> token.getExpiresAt().isAfter(now))
				.filter(token -> token.getUser().getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new AuthenticationFailedException("password reset token is invalid."));

		User user = resetToken.getUser();
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		user.setUpdatedAt(now);
		resetToken.markUsed(now);
		markUnusedResetTokensUsed(user, now);

		for (RefreshToken refreshToken : refreshTokenRepository.findByUser_Id(user.getId())) {
			if (refreshToken.getRevokedAt() == null) {
				refreshToken.revoke(now);
			}
		}

		passwordResetTokenRepository.save(resetToken);
		userRepository.save(user);

		return PasswordResetData.ofSuccess();
	}

	private void issueResetToken(User user, LocalDateTime now) {
		markUnusedResetTokensUsed(user, now);
		String rawToken = tokenGenerator.generateRawToken();
		String tokenHash = tokenGenerator.hash(rawToken);
		LocalDateTime expiresAt = now.plusMinutes(expirationMinutes);
		PasswordResetToken resetToken = new PasswordResetToken(user, tokenHash, expiresAt, now);
		passwordResetTokenRepository.save(resetToken);
		sendResetEmail(user.getEmail(), rawToken);
	}

	private void markUnusedResetTokensUsed(User user, LocalDateTime usedAt) {
		for (PasswordResetToken token : passwordResetTokenRepository.findByUser_Id(user.getId())) {
			if (token.getUsedAt() == null) {
				token.markUsed(usedAt);
			}
		}
	}

	private void sendResetEmail(String email, String rawToken) {
		try {
			emailSender.send(email, RESET_EMAIL_SUBJECT, resetEmailBody(rawToken));
		} catch (RuntimeException exception) {
			log.warn("password reset email delivery failed");
		}
	}

	private String resetEmailBody(String rawToken) {
		return "아래 링크에서 비밀번호를 재설정해 주세요.\n" + resetUrlPrefix + rawToken;
	}

	private static LocalDateTime nowUtc() {
		return LocalDateTime.now(Clock.system(ZoneOffset.UTC));
	}
}
