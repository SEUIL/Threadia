package com.threadia.backend.platform.service;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.platform.domain.PlatformAdmin;

public record PlatformAdminAuthority(User actor, PlatformAdmin platformAdmin) {
}
