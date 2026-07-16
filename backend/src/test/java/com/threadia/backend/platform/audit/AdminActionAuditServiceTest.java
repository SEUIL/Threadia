package com.threadia.backend.platform.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.supportcase.domain.SupportCase;
import com.threadia.backend.supportcase.domain.SupportCaseType;

@ExtendWith(MockitoExtension.class)
class AdminActionAuditServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-15T06:00:00Z"), ZoneOffset.UTC);

	@Mock
	private AdminActionAuditRepository auditRepository;

	@Test
	void persistsActorAuthorityActionTargetReasonStatesTimestampAndCaseConnection() {
		User actor = user(1L, "admin@example.com");
		PlatformAdmin admin = new PlatformAdmin(actor, PlatformAdminRole.SUPER_ADMIN, LocalDateTime.now(FIXED_CLOCK));
		setId(admin, 2L);
		SupportCase supportCase = supportCase(10L, user(3L, "reporter@example.com"));
		when(auditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		AdminActionAuditService service = new AdminActionAuditService(
				auditRepository,
				new ObjectMapper().findAndRegisterModules(),
				FIXED_CLOCK);

		service.recordPlatformMutation(
				supportCase,
				actor,
				admin,
				AdminAction.STATUS_CHANGED,
				AuditTargetType.SUPPORT_CASE,
				10L,
				"Resolve reviewed case",
				Map.of("status", "IN_PROGRESS"),
				Map.of("status", "RESOLVED"));

		ArgumentCaptor<AdminActionAudit> captor = ArgumentCaptor.forClass(AdminActionAudit.class);
		verify(auditRepository).save(captor.capture());
		AdminActionAudit audit = captor.getValue();
		assertThat(audit.getSupportCase()).isSameAs(supportCase);
		assertThat(audit.getActor()).isSameAs(actor);
		assertThat(audit.getActorPlatformAdmin()).isSameAs(admin);
		assertThat(audit.getAuthoritySource()).isEqualTo(AuditAuthoritySource.PLATFORM_ADMIN);
		assertThat(audit.getAuthorityRole()).isEqualTo(PlatformAdminRole.SUPER_ADMIN);
		assertThat(audit.getAction()).isEqualTo(AdminAction.STATUS_CHANGED);
		assertThat(audit.getTargetType()).isEqualTo(AuditTargetType.SUPPORT_CASE);
		assertThat(audit.getTargetId()).isEqualTo(10L);
		assertThat(audit.getReason()).isEqualTo("Resolve reviewed case");
		assertThat(audit.getBeforeState()).contains("IN_PROGRESS");
		assertThat(audit.getAfterState()).contains("RESOLVED");
		assertThat(audit.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 15, 6, 0));
	}

	@Test
	void persistsCompanyAuditWithoutSupportCase() {
		User actor = user(1L, "admin@example.com");
		PlatformAdmin admin = new PlatformAdmin(actor, PlatformAdminRole.SUPER_ADMIN, LocalDateTime.now(FIXED_CLOCK));
		setId(admin, 2L);
		when(auditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		AdminActionAuditService service = new AdminActionAuditService(auditRepository, new ObjectMapper().findAndRegisterModules(), FIXED_CLOCK);

		service.recordPlatformMutation(null, actor, admin, AdminAction.COMPANY_STATUS_CHANGED, AuditTargetType.COMPANY, 7L,
				"Company policy review", Map.of("status", "ACTIVE"), Map.of("status", "INACTIVE"));

		ArgumentCaptor<AdminActionAudit> captor = ArgumentCaptor.forClass(AdminActionAudit.class);
		verify(auditRepository).save(captor.capture());
		AdminActionAudit audit = captor.getValue();
		assertThat(audit.getSupportCase()).isNull();
		assertThat(audit.getTargetType()).isEqualTo(AuditTargetType.COMPANY);
		assertThat(audit.getTargetId()).isEqualTo(7L);
	}

	private static User user(Long id, String email) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		User user = new User(email, "hashed", "user", null, UserStatus.ACTIVE, now, now);
		setId(user, id);
		return user;
	}

	private static SupportCase supportCase(Long id, User reporter) {
		SupportCase supportCase = new SupportCase(
				reporter,
				SupportCaseType.INQUIRY,
				null,
				null,
				"Help",
				"Need assistance",
				LocalDateTime.now(FIXED_CLOCK));
		setId(supportCase, id);
		return supportCase;
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
