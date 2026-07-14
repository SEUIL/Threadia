package com.threadia.backend.user;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenService;

@Service
public class MeService {

	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;

	public MeService(JwtTokenService jwtTokenService, UserRepository userRepository) {
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
	}

	/**
	 * access token 검증 -> DB에서 사용자 재조회 -> ACTIVE 상태 확인 -> 4필드 응답.
	 * 토큰 자체의 유효성(parseAccessToken)과 사용자 상태(ACTIVE)는 서로 다른 관심사라 단계를 분리한다:
	 * 토큰이 멀쩡해도 그 사이 계정이 SUSPENDED/DELETED로 바뀌었을 수 있기 때문에 매 요청마다 DB를 다시 확인한다.
	 */
	public MeData getMe(String accessToken) {
		AuthenticatedUser authUser = jwtTokenService.parseAccessToken(accessToken);
		User user = userRepository.findById(authUser.userId())
				.filter(u -> u.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new AuthenticationFailedException("인증에 실패했습니다."));
		return new MeData(UserResponse.from(user));
	}

	@Transactional
	public MeData updateProfile(String accessToken, ProfileUpdateRequest request) {
		AuthenticatedUser authUser = jwtTokenService.parseAccessToken(accessToken);
		LocalDateTime updatedAt = LocalDateTime.now();
		int updatedRows = userRepository.updateActiveProfile(
				authUser.userId(),
				request.hasNickname(),
				request.getNickname(),
				request.hasAvatarUrl(),
				request.getAvatarUrl(),
				updatedAt);
		if (updatedRows == 0) {
			throw new AuthenticationFailedException("인증에 실패했습니다.");
		}
		User user = userRepository.findById(authUser.userId())
				.filter(u -> u.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new AuthenticationFailedException("인증에 실패했습니다."));
		return new MeData(UserResponse.from(user));
	}
}
