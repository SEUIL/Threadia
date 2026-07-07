package com.threadia.backend.user;

import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.EmailNormalizer;
import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.common.exception.AuthenticationFailedException;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.common.exception.UserNotFoundException;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenService;

@Service
public class UserLookupService {

	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final JdbcTemplate jdbcTemplate;

	public UserLookupService(JwtTokenService jwtTokenService, UserRepository userRepository, JdbcTemplate jdbcTemplate) {
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	public UserLookupData lookupByEmail(String accessToken, String rawEmail) {
		validateRequester(accessToken);
		String email = EmailNormalizer.normalize(rawEmail);
		User user = userRepository.findByEmail(email)
				.filter(u -> u.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new UserNotFoundException(email));
		return new UserLookupData(UserResponse.from(user));
	}

	private void validateRequester(String accessToken) {
		AuthenticatedUser authUser = jwtTokenService.parseAccessToken(accessToken);
		userRepository.findById(authUser.userId())
				.filter(u -> u.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new AuthenticationFailedException("인증에 실패했습니다."));
		Integer companyAdminCount = jdbcTemplate.queryForObject(
				"""
						SELECT COUNT(*)
						FROM company_members
						WHERE user_id = ?
						  AND role = 'COMPANY_ADMIN'
						  AND status = 'ACTIVE'
						""",
				Integer.class,
				authUser.userId());
		if (companyAdminCount == null || companyAdminCount == 0) {
			throw new ForbiddenException("회사 관리자 권한이 필요합니다.");
		}
	}
}
