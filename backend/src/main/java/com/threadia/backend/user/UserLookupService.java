package com.threadia.backend.user;

import org.springframework.stereotype.Service;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.EmailNormalizer;
import com.threadia.backend.common.auth.UserResponse;
import com.threadia.backend.common.exception.UserNotFoundException;

@Service
public class UserLookupService {

	private final UserRepository userRepository;

	public UserLookupService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserLookupData lookupByEmail(String rawEmail) {
		String email = EmailNormalizer.normalize(rawEmail);
		User user = userRepository.findByEmail(email)
				.filter(u -> u.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new UserNotFoundException(email));
		return new UserLookupData(UserResponse.from(user));
	}
}
