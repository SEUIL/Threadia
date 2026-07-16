package com.threadia.backend.platform.bootstrap;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.EmailNormalizer;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminBootstrapLock;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.repository.PlatformAdminBootstrapLockRepository;
import com.threadia.backend.platform.repository.PlatformAdminRepository;

@Service
public class PlatformAdminBootstrapService {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private final PlatformAdminBootstrapLockRepository bootstrapLockRepository;
	private final PlatformAdminRepository platformAdminRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	@Autowired
	public PlatformAdminBootstrapService(
			PlatformAdminBootstrapLockRepository bootstrapLockRepository,
			PlatformAdminRepository platformAdminRepository,
			UserRepository userRepository) {
		this(bootstrapLockRepository, platformAdminRepository, userRepository, Clock.systemUTC());
	}

	PlatformAdminBootstrapService(
			PlatformAdminBootstrapLockRepository bootstrapLockRepository,
			PlatformAdminRepository platformAdminRepository,
			UserRepository userRepository,
			Clock clock) {
		this.bootstrapLockRepository = bootstrapLockRepository;
		this.platformAdminRepository = platformAdminRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Transactional
	public void bootstrap(String bootstrapEmail) {
		bootstrapLockRepository.findByIdForUpdate(PlatformAdminBootstrapLock.SINGLETON_ID)
				.orElseThrow(() -> new IllegalStateException(
						"platform admin bootstrap lock row가 없습니다. canonical schema를 적용해야 합니다."));
		if (platformAdminRepository.count() > 0) {
			return;
		}

		String normalizedEmail = EmailNormalizer.normalize(bootstrapEmail);
		if (normalizedEmail == null || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
			throw new IllegalStateException(
					"platform_admins가 비어 있어 유효한 THREADIA_PLATFORM_ADMIN_BOOTSTRAP_EMAIL이 필요합니다.");
		}

		User bootstrapUser = userRepository.findByEmail(normalizedEmail)
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.orElseThrow(() -> new IllegalStateException(
						"platform admin bootstrap email과 일치하는 ACTIVE 사용자가 없습니다."));
		platformAdminRepository.saveAndFlush(new PlatformAdmin(
				bootstrapUser,
				PlatformAdminRole.SUPER_ADMIN,
				LocalDateTime.now(clock)));
	}
}
