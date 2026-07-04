package com.threadia.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceTest {

	@Mock
	private UserRepository userRepository;

	private UserLookupService userLookupService;

	@BeforeEach
	void setUp() {
		userLookupService = new UserLookupService(userRepository);
	}

	@Test
	void lookupByEmailNormalizesEmailAndReturnsActiveUser() {
		User user = userWithStatus(UserStatus.ACTIVE);
		when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

		UserLookupData data = userLookupService.lookupByEmail("  MEMBER@example.com  ");

		assertThat(data.user().id()).isEqualTo(2L);
		assertThat(data.user().email()).isEqualTo("member@example.com");
		assertThat(data.user().nickname()).isEqualTo("member");
		verify(userRepository).findByEmail("member@example.com");
	}

	@Test
	void lookupByEmailThrowsWhenUserDoesNotExist() {
		when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userLookupService.lookupByEmail("ghost@example.com"))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void lookupByEmailThrowsWhenUserIsNotActive() {
		User user = userWithStatus(UserStatus.SUSPENDED);
		when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> userLookupService.lookupByEmail("member@example.com"))
				.isInstanceOf(UserNotFoundException.class);
	}

	private static User userWithStatus(UserStatus status) {
		LocalDateTime now = LocalDateTime.now();
		User user = new User("member@example.com", "hashed", "member", null, status, now, now);
		setId(user, 2L);
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
}
