package com.threadia.backend.platform.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.platform.repository.PlatformAdminRepository;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;

@ExtendWith(MockitoExtension.class)
class PlatformAdminAccessServiceTest {

	@Mock
	private AuthContextService authContextService;

	@Mock
	private PlatformAdminRepository platformAdminRepository;

	@ParameterizedTest(name = "{0} is denied without platform_admins authority")
	@MethodSource("nonPlatformActors")
	void deniesRegularCompanyAndProjectAdmins(String actorLabel, Long userId) {
		String token = actorLabel + "-token";
		AuthContext context = new AuthContext(userId, actorLabel + "@example.com");
		User actor = user(userId, context.email());
		when(authContextService.fromAccessToken(token)).thenReturn(context);
		when(authContextService.requireActiveUser(context)).thenReturn(actor);
		when(platformAdminRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
		PlatformAdminAccessService service = new PlatformAdminAccessService(
				authContextService,
				platformAdminRepository);

		assertThatThrownBy(() -> service.requireAdmin(token))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("플랫폼 관리자 권한이 필요합니다.");
	}

	@ParameterizedTest
	@MethodSource("platformRoles")
	void allowsBothPlatformRoles(PlatformAdminRole role) {
		AuthContext context = new AuthContext(10L, "platform@example.com");
		User actor = user(10L, context.email());
		PlatformAdmin platformAdmin = new PlatformAdmin(actor, role, LocalDateTime.of(2026, 7, 15, 0, 0));
		when(authContextService.fromAccessToken("platform-token")).thenReturn(context);
		when(authContextService.requireActiveUser(context)).thenReturn(actor);
		when(platformAdminRepository.findByUser_Id(10L)).thenReturn(Optional.of(platformAdmin));
		PlatformAdminAccessService service = new PlatformAdminAccessService(
				authContextService,
				platformAdminRepository);

		assertThat(service.requireAdmin("platform-token").platformAdmin().getRole()).isEqualTo(role);
	}

	private static Stream<Arguments> nonPlatformActors() {
		return Stream.of(
				Arguments.of("regular", 1L),
				Arguments.of("company-admin", 2L),
				Arguments.of("project-admin", 3L));
	}

	private static Stream<Arguments> platformRoles() {
		return Stream.of(
				Arguments.of(PlatformAdminRole.PLATFORM_ADMIN),
				Arguments.of(PlatformAdminRole.SUPER_ADMIN));
	}

	private static User user(Long id, String email) {
		LocalDateTime now = LocalDateTime.of(2026, 7, 15, 0, 0);
		User user = new User(email, "hashed", "actor", null, UserStatus.ACTIVE, now, now);
		setId(user, id);
		return user;
	}

	private static void setId(Object entity, Long id) {
		try {
			Field field = entity.getClass().getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
