package com.threadia.backend.platform.lifecycle;

import com.threadia.backend.platform.domain.PlatformAdminRole;

import jakarta.validation.constraints.NotNull;

public record ChangePlatformAdminRoleRequest(@NotNull PlatformAdminRole role, String reason) {
}
