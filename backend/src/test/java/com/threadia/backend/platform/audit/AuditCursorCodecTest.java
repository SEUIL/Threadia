package com.threadia.backend.platform.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;

class AuditCursorCodecTest {
	private static final String SECRET = "01234567890123456789012345678901";

	@Test
	void bindsCursorToRequesterAndNormalizedFiltersWithoutSnapshotPayload() throws Exception {
		AuditCursorCodec codec = new AuditCursorCodec(SECRET);
		AuditFilters filters = new AuditFilters(2L, AdminAction.STATUS_CHANGED, AuditTargetType.SUPPORT_CASE, 7L, 8L,
				LocalDateTime.of(2026, 7, 18, 0, 0), LocalDateTime.of(2026, 7, 19, 0, 0));
		AdminActionAudit audit = audit(9L, LocalDateTime.of(2026, 7, 18, 1, 0));

		String cursor = codec.encode(1L, filters, audit);

		assertThat(cursor).doesNotContain("reason", "body", "snapshot", "admin@example.com");
		assertThat(codec.decode(cursor, 1L, filters).id()).isEqualTo(9L);
		assertThatThrownBy(() -> codec.decode(cursor, 3L, filters)).isInstanceOf(AuditValidationException.class);
		assertThatThrownBy(() -> codec.decode(cursor, 1L, new AuditFilters(2L, AdminAction.STATUS_CHANGED,
				AuditTargetType.SUPPORT_CASE, 99L, 8L, filters.createdFrom(), filters.createdTo()))).isInstanceOf(AuditValidationException.class);
		assertThatThrownBy(() -> codec.decode(cursor + "x", 1L, filters)).isInstanceOf(AuditValidationException.class);
	}

	private static AdminActionAudit audit(Long id, LocalDateTime createdAt) throws Exception {
		User actor = new User("admin@example.com", "hash", "admin", null, UserStatus.ACTIVE, createdAt, createdAt);
		setId(actor, 1L);
		AdminActionAudit audit = new AdminActionAudit(null, actor, null, AuditAuthoritySource.USER, null,
				AdminAction.STATUS_CHANGED, AuditTargetType.SUPPORT_CASE, 7L, "reason", null, "{}", createdAt);
		setId(audit, id);
		return audit;
	}

	private static void setId(Object target, Long id) throws Exception { Field field = target.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(target, id); }
}
