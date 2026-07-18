package com.threadia.backend.platform.lifecycle;

import java.time.ZoneOffset;

import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.domain.PlatformAdminStatus;

public record PlatformAdminResponse(
		Long id,
		Long userId,
		String email,
		String nickname,
		PlatformAdminRole role,
		PlatformAdminStatus status,
		String createdAt,
		String revokedAt,
		String reactivatedAt,
		String updatedAt) {

	static PlatformAdminResponse from(PlatformAdmin admin) {
		return new PlatformAdminResponse(
				admin.getId(), admin.getUser().getId(), admin.getUser().getEmail(), admin.getUser().getNickname(),
				admin.getRole(), admin.getStatus(), admin.getCreatedAt().atOffset(ZoneOffset.UTC).toString(),
				toUtc(admin.getRevokedAt()), toUtc(admin.getReactivatedAt()), admin.getUpdatedAt().atOffset(ZoneOffset.UTC).toString());
	}

	private static String toUtc(java.time.LocalDateTime value) {
		return value == null ? null : value.atOffset(ZoneOffset.UTC).toString();
	}
}
