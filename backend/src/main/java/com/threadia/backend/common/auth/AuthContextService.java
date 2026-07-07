package com.threadia.backend.common.auth;

import org.springframework.stereotype.Service;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.config.jwt.JwtTokenService;

@Service
public class AuthContextService {

	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;

	public AuthContextService(JwtTokenService jwtTokenService, UserRepository userRepository) {
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
	}

	public AuthContext fromAccessToken(String accessToken) {
		AuthContext authContext = AuthContext.from(jwtTokenService.parseAccessToken(accessToken));
		requireActiveUser(authContext);
		return authContext;
	}

	public User requireActiveUser(AuthContext authContext) {
		return userRepository.findById(authContext.userId())
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new AuthenticationFailedException("인증에 실패했습니다."));
	}
}
