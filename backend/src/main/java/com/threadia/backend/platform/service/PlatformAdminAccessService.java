package com.threadia.backend.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.domain.PlatformAdminStatus;
import com.threadia.backend.platform.repository.PlatformAdminRepository;

@Service
public class PlatformAdminAccessService {

	private final AuthContextService authContextService;
	private final PlatformAdminRepository platformAdminRepository;

	public PlatformAdminAccessService(
			AuthContextService authContextService,
			PlatformAdminRepository platformAdminRepository) {
		this.authContextService = authContextService;
		this.platformAdminRepository = platformAdminRepository;
	}

	@Transactional(readOnly = true)
	public PlatformAdminAuthority requireAdmin(String accessToken) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		User actor = authContextService.requireActiveUser(authContext);
		PlatformAdmin platformAdmin = platformAdminRepository.findByUser_Id(actor.getId())
				.filter(candidate -> candidate.getStatus() == PlatformAdminStatus.ACTIVE)
				.orElseThrow(() -> new ForbiddenException("플랫폼 관리자 권한이 필요합니다."));
		return new PlatformAdminAuthority(actor, platformAdmin);
	}

	@Transactional
	public PlatformAdminAuthority requireSuperAdminFresh(String accessToken) {
		AuthContext authContext = authContextService.fromAccessToken(accessToken);
		User actor = authContextService.requireActiveUser(authContext);
		PlatformAdmin platformAdmin = platformAdminRepository
				.findByUser_IdAndStatus(actor.getId(), PlatformAdminStatus.ACTIVE)
				.orElseThrow(() -> new ForbiddenException("SUPER_ADMIN 권한이 필요합니다."));
		if (platformAdmin.getRole() != PlatformAdminRole.SUPER_ADMIN) {
			throw new ForbiddenException("SUPER_ADMIN 권한이 필요합니다.");
		}
		return new PlatformAdminAuthority(actor, platformAdmin);
	}
}
