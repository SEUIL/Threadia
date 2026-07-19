package com.threadia.backend.platform.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;
import com.threadia.backend.common.exception.ForbiddenException;

@ExtendWith(MockitoExtension.class)
class AuditReadServiceTest {
	private static final String SECRET = "01234567890123456789012345678901";
	@Mock private PlatformAdminAccessService accessService;
	@Mock private AdminActionAuditRepository auditRepository;
	private AuditReadService service;
	private User actor;

	@BeforeEach
	void setUp() throws Exception {
		actor = user(1L, "admin@example.com");
		PlatformAdmin admin = new PlatformAdmin(actor, PlatformAdminRole.SUPER_ADMIN, LocalDateTime.of(2026, 7, 18, 0, 0));
		setId(admin, 2L);
		lenient().when(accessService.requireAdmin("token")).thenReturn(new PlatformAdminAuthority(actor, admin));
		service = new AuditReadService(accessService, auditRepository, new AuditCursorCodec(SECRET), new AuditSnapshotSanitizer(), new ObjectMapper());
	}

	@Test
	void returnsRedactedScalarSnapshotsAndUsesSizePlusOne() throws Exception {
		AdminActionAudit audit = audit(9L, "{\"title\":\"private title\",\"body\":\"private body\",\"status\":\"OPEN\",\"nested\":{\"x\":1}}", LocalDateTime.of(2026, 7, 18, 1, 0));
		when(auditRepository.findAuditPage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(audit));

		AuditPageData data = service.list("token", null, null, null, null, null, null, null, null, 20);

		assertThat(data.audits()).hasSize(1);
		assertThat(data.audits().get(0).afterState()).containsEntry("status", "OPEN").doesNotContainKeys("title", "body", "nested");
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(auditRepository).findAuditPage(any(), any(), any(), any(), any(), any(), any(), any(), any(), pageable.capture());
		assertThat(pageable.getValue().getPageSize()).isEqualTo(21);
	}

	@Test
	void returnsEmptyPageAndRejectsInvalidFilters() {
		when(auditRepository.findAuditPage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
		assertThat(service.list("token", null, null, null, null, null, null, null, null, null).audits()).isEmpty();
		assertThatThrownBy(() -> service.list("token", 0L, null, null, null, null, null, null, null, 20)).isInstanceOf(AuditValidationException.class);
		assertThatThrownBy(() -> service.list("token", null, "INVALID", null, null, null, null, null, null, 20)).isInstanceOf(AuditValidationException.class);
		assertThatThrownBy(() -> service.list("token", null, null, null, 1L, null, null, null, null, 20)).isInstanceOf(AuditValidationException.class);
		assertThatThrownBy(() -> service.list("token", null, null, null, null, null, "2026-07-18T00:00:00+09:00", null, null, 20)).isInstanceOf(AuditValidationException.class);
	}

	@Test
	void emitsNextCursorWhenSizePlusOneRowsExist() throws Exception {
		AdminActionAudit first = audit(10L, "{\"status\":\"OPEN\"}", LocalDateTime.of(2026, 7, 18, 2, 0));
		AdminActionAudit second = audit(9L, "{\"status\":\"IN_PROGRESS\"}", LocalDateTime.of(2026, 7, 18, 1, 0));
		when(auditRepository.findAuditPage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(first, second));

		AuditPageData data = service.list("token", null, null, null, null, null, null, null, null, 1);

		assertThat(data.audits()).extracting(AuditResponse::id).containsExactly(10L);
		assertThat(data.hasNext()).isTrue();
		assertThat(data.nextCursor()).isNotBlank();
	}

	@Test
	void rejectsNonPlatformActorBeforeQueryingAudits() {
		when(accessService.requireAdmin("regular-token")).thenThrow(new ForbiddenException("플랫폼 관리자 권한이 필요합니다."));

		assertThatThrownBy(() -> service.list("regular-token", null, null, null, null, null, null, null, null, 20))
				.isInstanceOf(ForbiddenException.class);
		verifyNoInteractions(auditRepository);
	}

	private AdminActionAudit audit(Long id, String afterState, LocalDateTime createdAt) throws Exception {
		AdminActionAudit audit = new AdminActionAudit(null, actor, null, AuditAuthoritySource.USER, null,
				AdminAction.SUPPORT_CASE_CREATED, AuditTargetType.SUPPORT_CASE, 7L, "case reason", null, afterState, createdAt);
		setId(audit, id);
		return audit;
	}

	private static User user(Long id, String email) throws Exception { User user = new User(email, "hash", "admin", null, UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now()); setId(user, id); return user; }
	private static void setId(Object target, Long id) throws Exception { Field field = target.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(target, id); }
}
