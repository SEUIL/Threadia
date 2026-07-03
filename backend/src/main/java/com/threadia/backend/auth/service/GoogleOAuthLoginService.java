package com.threadia.backend.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.OAuthAccount;
import com.threadia.backend.auth.domain.OAuthProvider;
import com.threadia.backend.auth.domain.RefreshToken;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.dto.LoginData;
import com.threadia.backend.auth.repository.OAuthAccountRepository;
import com.threadia.backend.auth.repository.RefreshTokenRepository;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.config.cookie.RefreshTokenCookieProvider;
import com.threadia.backend.config.jwt.JwtTokenService;
import com.threadia.backend.config.token.RefreshTokenGenerator;

@Service
public class GoogleOAuthLoginService {

	private final OAuthAccountRepository oauthAccountRepository;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenService jwtTokenService;
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final RefreshTokenCookieProvider refreshTokenCookieProvider;

	public GoogleOAuthLoginService(
			OAuthAccountRepository oauthAccountRepository,
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			JwtTokenService jwtTokenService,
			RefreshTokenGenerator refreshTokenGenerator,
			RefreshTokenCookieProvider refreshTokenCookieProvider) {
		this.oauthAccountRepository = oauthAccountRepository;
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtTokenService = jwtTokenService;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.refreshTokenCookieProvider = refreshTokenCookieProvider;
	}

	@Transactional
	public OAuthLoginResult login(OAuth2User oauth2User) {
		GoogleOAuthUserInfo userInfo = GoogleOAuthUserInfo.from(oauth2User);
		User user = oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, userInfo.providerUserId())
				.map(OAuthAccount::getUser)
				.orElseGet(() -> createOAuthUser(userInfo));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new OAuthLoginException("활성 상태의 계정만 Google OAuth 로그인이 가능합니다.");
		}
		return issueTokens(user);
	}

	private User createOAuthUser(GoogleOAuthUserInfo userInfo) {
		if (userRepository.findByEmail(userInfo.email()).isPresent()) {
			throw new OAuthLoginException("이미 가입된 이메일은 자동으로 Google 계정에 연결할 수 없습니다.");
		}
		LocalDateTime now = nowUtc();
		User user = new User(
				userInfo.email(), null, userInfo.nickname(), userInfo.avatarUrl(), UserStatus.ACTIVE, now, now);
		User savedUser = userRepository.save(user);
		OAuthAccount oauthAccount = new OAuthAccount(
				savedUser, OAuthProvider.GOOGLE, userInfo.providerUserId(), userInfo.email(), now, now);
		oauthAccountRepository.save(oauthAccount);
		return savedUser;
	}

	private OAuthLoginResult issueTokens(User user) {
		String accessToken = jwtTokenService.issueAccessToken(user.getId(), user.getEmail());
		long expiresIn = jwtTokenService.getAccessTokenExpirationSeconds();
		String rawRefreshToken = refreshTokenGenerator.generateRawToken();
		String tokenHash = refreshTokenGenerator.hash(rawRefreshToken);
		LocalDateTime now = nowUtc();
		ResponseCookie cookie = refreshTokenCookieProvider.create(rawRefreshToken);
		LocalDateTime expiresAt = now.plusSeconds(refreshTokenCookieProvider.getMaxAge().toSeconds());
		refreshTokenRepository.save(new RefreshToken(user, tokenHash, expiresAt, now, now));
		LoginData data = LoginData.of(accessToken, expiresIn, UserResponse.from(user));
		return new OAuthLoginResult(data, cookie);
	}

	private static LocalDateTime nowUtc() {
		return LocalDateTime.now(Clock.system(ZoneOffset.UTC));
	}
}
