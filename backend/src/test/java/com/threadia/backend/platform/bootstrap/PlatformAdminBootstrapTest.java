package com.threadia.backend.platform.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminBootstrapLock;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.repository.PlatformAdminBootstrapLockRepository;
import com.threadia.backend.platform.repository.PlatformAdminRepository;

@ExtendWith(MockitoExtension.class)
class PlatformAdminBootstrapTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-15T01:02:03Z"), ZoneOffset.UTC);

	@Mock
	private PlatformAdminBootstrapLockRepository bootstrapLockRepository;

	@Mock
	private PlatformAdminRepository platformAdminRepository;

	@Mock
	private UserRepository userRepository;

	@Test
	void createsFirstSuperAdminFromNormalizedExistingActiveUser() {
		User activeUser = user("bootstrap@example.com", UserStatus.ACTIVE);
		when(platformAdminRepository.count()).thenReturn(0L);
		when(userRepository.findByEmail("bootstrap@example.com")).thenReturn(Optional.of(activeUser));
		PlatformAdminBootstrapService bootstrap = bootstrapService();

		bootstrap.bootstrap("  Bootstrap@Example.com  ");

		ArgumentCaptor<PlatformAdmin> captor = ArgumentCaptor.forClass(PlatformAdmin.class);
		verify(platformAdminRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getUser()).isSameAs(activeUser);
		assertThat(captor.getValue().getRole()).isEqualTo(PlatformAdminRole.SUPER_ADMIN);
		assertThat(captor.getValue().getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 15, 1, 2, 3));
	}

	@Test
	void isIdempotentWhenAnyPlatformAdminAlreadyExists() {
		when(platformAdminRepository.count()).thenReturn(1L);
		PlatformAdminBootstrapService bootstrap = bootstrapService();

		bootstrap.bootstrap("changed@example.com");

		verifyNoInteractions(userRepository);
		verify(platformAdminRepository, never()).saveAndFlush(any());
	}

	@Test
	void failsStartupWhenNoAdminsAndBootstrapEmailIsMissing() {
		when(platformAdminRepository.count()).thenReturn(0L);

		assertThatThrownBy(() -> bootstrapService().bootstrap(" "))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("THREADIA_PLATFORM_ADMIN_BOOTSTRAP_EMAIL");
	}

	@Test
	void failsStartupWhenNoAdminsAndBootstrapEmailIsInvalid() {
		when(platformAdminRepository.count()).thenReturn(0L);

		assertThatThrownBy(() -> bootstrapService().bootstrap("not-an-email"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("THREADIA_PLATFORM_ADMIN_BOOTSTRAP_EMAIL");
	}

	@Test
	void failsStartupWhenNoMatchingActiveUserExists() {
		when(platformAdminRepository.count()).thenReturn(0L);
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> bootstrapService().bootstrap("missing@example.com"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ACTIVE 사용자");
	}

	@Test
	void failsStartupWhenMatchingUserIsInactive() {
		when(platformAdminRepository.count()).thenReturn(0L);
		when(userRepository.findByEmail("inactive@example.com"))
				.thenReturn(Optional.of(user("inactive@example.com", UserStatus.SUSPENDED)));

		assertThatThrownBy(() -> bootstrapService().bootstrap("inactive@example.com"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ACTIVE 사용자");
	}

	@Test
	void failsStartupWhenCanonicalBootstrapLockRowIsMissing() {
		when(bootstrapLockRepository.findByIdForUpdate(PlatformAdminBootstrapLock.SINGLETON_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> newBootstrapService().bootstrap("bootstrap@example.com"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("bootstrap lock row");
		verify(platformAdminRepository, never()).count();
	}

	private PlatformAdminBootstrapService bootstrapService() {
		when(bootstrapLockRepository.findByIdForUpdate(PlatformAdminBootstrapLock.SINGLETON_ID))
				.thenReturn(Optional.of(new PlatformAdminBootstrapLock(PlatformAdminBootstrapLock.SINGLETON_ID)));
		return newBootstrapService();
	}

	private PlatformAdminBootstrapService newBootstrapService() {
		return new PlatformAdminBootstrapService(
				bootstrapLockRepository,
				platformAdminRepository,
				userRepository,
				FIXED_CLOCK);
	}

	private static User user(String email, UserStatus status) {
		LocalDateTime now = LocalDateTime.of(2026, 7, 15, 0, 0);
		return new User(email, "hashed", "bootstrap", null, status, now, now);
	}
}
