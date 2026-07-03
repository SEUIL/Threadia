package com.threadia.backend.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.RefreshToken;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.dto.LoginData;
import com.threadia.backend.auth.dto.LoginRequest;
import com.threadia.backend.auth.dto.SignupData;
import com.threadia.backend.auth.dto.SignupRequest;
import com.threadia.backend.auth.repository.RefreshTokenRepository;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.EmailNormalizer;
import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.common.exception.EmailAlreadyExistsException;
import com.threadia.backend.config.cookie.RefreshTokenCookieProvider;
import com.threadia.backend.config.jwt.JwtTokenService;
import com.threadia.backend.config.token.RefreshTokenGenerator;

// signup/login 비즈니스 로직. AuthController가 HTTP 계층을, 이 클래스가 실제 처리를 담당한다.
@Service
public class AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final RefreshTokenCookieProvider refreshTokenCookieProvider;

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenService jwtTokenService,
			RefreshTokenGenerator refreshTokenGenerator,
			RefreshTokenCookieProvider refreshTokenCookieProvider) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.refreshTokenCookieProvider = refreshTokenCookieProvider;
	}

	/**
	 * 이메일 회원가입. 순서: email 정규화 -> 중복 체크 -> 비밀번호 해싱 -> User 저장.
	 * 토큰은 발급하지 않는다(가입과 로그인은 별개 동작).
	 *
	 * <p>existsByEmail 확인과 save 사이에 동시 가입 경합이 생기면 DB unique 제약(uq_users_email)이
	 * 최종 방어선이며, GlobalExceptionHandler가 그 DataIntegrityViolationException을 409로 변환한다.
	 * {@code @Transactional}은 그 경합 자체를 막지는 못하지만, 검증 -> 저장 사이에 이 메서드가
	 * 부분 커밋되는 상황은 방지한다.
	 */
	@Transactional
	public SignupData signup(SignupRequest request) {
		String email = EmailNormalizer.normalize(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyExistsException(email);
		}

		LocalDateTime now = nowUtc();
		// User Entity는 @PrePersist 훅이 없어 createdAt/updatedAt을 여기서 직접 채운다.
		User user = new User(
				email,
				passwordEncoder.encode(request.password()),
				request.nickname(),
				request.avatarUrl(),
				UserStatus.ACTIVE,
				now,
				now);
		userRepository.save(user);

		return new SignupData(UserResponse.from(user));
	}

	/**
	 * 이메일 로그인. 순서: email 정규화 -> 사용자 조회 및 비밀번호/ACTIVE 상태 검증
	 * -> access token 발급 -> refresh token 생성/해시 저장 -> refresh cookie 생성.
	 * 소셜 전용 계정(passwordHash == null)은 필터 체인에서 자연스럽게 걸러져 NPE 없이 거부된다.
	 */
	@Transactional
	public LoginResult login(LoginRequest request) {
		String email = EmailNormalizer.normalize(request.email());
		User user = userRepository.findByEmail(email)
				.filter(u -> u.getPasswordHash() != null)
				.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
				.filter(u -> u.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다."));

		// access token: JWT, DB에 저장하지 않는 stateless 토큰. expiresIn은 실제 발급 만료초를 그대로 응답에 반영한다.
		String accessToken = jwtTokenService.issueAccessToken(user.getId(), user.getEmail());
		long expiresIn = jwtTokenService.getAccessTokenExpirationSeconds();

		// refresh token: random raw 값을 쿠키로 내려주고, DB에는 해시만 저장(원문 저장 금지).
		// entity expiresAt은 쿠키 Max-Age와 동일한 값을 써야 하므로 refreshTokenCookieProvider에서 가져온다.
		String rawRefreshToken = refreshTokenGenerator.generateRawToken();
		String tokenHash = refreshTokenGenerator.hash(rawRefreshToken);
		LocalDateTime now = nowUtc();
		ResponseCookie cookie = refreshTokenCookieProvider.create(rawRefreshToken);
		LocalDateTime expiresAt = now.plusSeconds(refreshTokenCookieProvider.getMaxAge().toSeconds());
		RefreshToken refreshToken = new RefreshToken(user, tokenHash, expiresAt, now, now);
		refreshTokenRepository.save(refreshToken);

		LoginData data = LoginData.of(accessToken, expiresIn, UserResponse.from(user));

		return new LoginResult(data, cookie);
	}

	// 모든 시각은 UTC 기준으로 생성/저장한다(DB DEFAULT CURRENT_TIMESTAMP 사용 금지 정책).
	private static LocalDateTime nowUtc() {
		return LocalDateTime.now(Clock.system(ZoneOffset.UTC));
	}
}
