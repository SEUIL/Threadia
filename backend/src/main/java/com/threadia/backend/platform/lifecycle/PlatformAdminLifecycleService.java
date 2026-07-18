package com.threadia.backend.platform.lifecycle;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.common.exception.UserNotFoundException;
import com.threadia.backend.platform.audit.AdminAction;
import com.threadia.backend.platform.audit.AdminActionAuditService;
import com.threadia.backend.platform.audit.AuditReason;
import com.threadia.backend.platform.audit.AuditTargetType;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminBootstrapLock;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.domain.PlatformAdminStatus;
import com.threadia.backend.platform.repository.PlatformAdminBootstrapLockRepository;
import com.threadia.backend.platform.repository.PlatformAdminRepository;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;

@Service
public class PlatformAdminLifecycleService {

	private final PlatformAdminAccessService accessService;
	private final PlatformAdminBootstrapLockRepository lockRepository;
	private final PlatformAdminRepository adminRepository;
	private final UserRepository userRepository;
	private final AdminActionAuditService auditService;
	private final Clock clock;

	@Autowired
	public PlatformAdminLifecycleService(
			PlatformAdminAccessService accessService,
			PlatformAdminBootstrapLockRepository lockRepository,
			PlatformAdminRepository adminRepository,
			UserRepository userRepository,
			AdminActionAuditService auditService) {
		this(accessService, lockRepository, adminRepository, userRepository, auditService, Clock.systemUTC());
	}

	PlatformAdminLifecycleService(
			PlatformAdminAccessService accessService,
			PlatformAdminBootstrapLockRepository lockRepository,
			PlatformAdminRepository adminRepository,
			UserRepository userRepository,
			AdminActionAuditService auditService,
			Clock clock) {
		this.accessService = accessService;
		this.lockRepository = lockRepository;
		this.adminRepository = adminRepository;
		this.userRepository = userRepository;
		this.auditService = auditService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public PlatformAdminsData list(String accessToken) {
		accessService.requireAdmin(accessToken);
		return new PlatformAdminsData(adminRepository.findAllByOrderByCreatedAtAscIdAsc().stream().map(PlatformAdminResponse::from).toList());
	}

	@Transactional(readOnly = true)
	public PlatformActorData actor(String accessToken) {
		return new PlatformActorData(PlatformAdminResponse.from(accessService.requireAdmin(accessToken).platformAdmin()));
	}

	@Transactional
	public PlatformAdminResponse grant(String accessToken, GrantPlatformAdminRequest request) {
		PlatformAdminAuthority authority = lockAndRequireFreshSuperAdmin(accessToken);
		String reason = AuditReason.requireValid(request.reason());
		User user = userRepository.findById(request.userId()).orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new ForbiddenException("ACTIVE 사용자에게만 플랫폼 관리자 권한을 부여할 수 있습니다.");
		}
		if (adminRepository.findByUser_Id(user.getId()).isPresent()) {
			throw new PlatformAdminConflictException("DUPLICATE_PLATFORM_ADMIN_HISTORY", "이미 플랫폼 관리자 이력이 있는 사용자입니다.");
		}
		PlatformAdmin created = adminRepository.save(new PlatformAdmin(user, request.role(), LocalDateTime.now(clock)));
		audit(authority, created, AdminAction.PLATFORM_ADMIN_GRANTED, reason, null, snapshot(created));
		return PlatformAdminResponse.from(created);
	}

	@Transactional
	public PlatformAdminResponse revoke(String accessToken, Long platformAdminId, PlatformAdminLifecycleRequest request) {
		PlatformAdminAuthority authority = lockAndRequireFreshSuperAdmin(accessToken);
		String reason = AuditReason.requireValid(request.reason());
		PlatformAdmin target = activeTarget(platformAdminId);
		ensureNotLastActiveSuperAdmin(target, null);
		Map<String, Object> before = snapshot(target);
		target.revoke(LocalDateTime.now(clock));
		audit(authority, target, AdminAction.PLATFORM_ADMIN_REVOKED, reason, before, snapshot(target));
		return PlatformAdminResponse.from(target);
	}

	@Transactional
	public PlatformAdminResponse reactivate(String accessToken, Long platformAdminId, PlatformAdminLifecycleRequest request) {
		PlatformAdminAuthority authority = lockAndRequireFreshSuperAdmin(accessToken);
		String reason = AuditReason.requireValid(request.reason());
		PlatformAdmin target = adminRepository.findByIdForUpdate(platformAdminId)
				.orElseThrow(() -> new PlatformAdminNotFoundException(platformAdminId));
		if (target.getStatus() == PlatformAdminStatus.ACTIVE) {
			throw new PlatformAdminConflictException("PLATFORM_ADMIN_ALREADY_ACTIVE", "이미 ACTIVE 플랫폼 관리자입니다.");
		}
		if (target.getUser().getStatus() != UserStatus.ACTIVE) {
			throw new ForbiddenException("ACTIVE 사용자만 플랫폼 관리자로 재활성화할 수 있습니다.");
		}
		Map<String, Object> before = snapshot(target);
		target.reactivate(LocalDateTime.now(clock));
		audit(authority, target, AdminAction.PLATFORM_ADMIN_REACTIVATED, reason, before, snapshot(target));
		return PlatformAdminResponse.from(target);
	}

	@Transactional
	public PlatformAdminResponse changeRole(String accessToken, Long platformAdminId, ChangePlatformAdminRoleRequest request) {
		PlatformAdminAuthority authority = lockAndRequireFreshSuperAdmin(accessToken);
		PlatformAdminRole authorizationRole = authority.platformAdmin().getRole();
		String reason = AuditReason.requireValid(request.reason());
		PlatformAdmin target = activeTarget(platformAdminId);
		if (target.getRole() == request.role()) {
			throw new PlatformAdminConflictException("PLATFORM_ADMIN_ROLE_NO_CHANGE", "플랫폼 관리자 역할이 이미 요청한 역할입니다.");
		}
		ensureNotLastActiveSuperAdmin(target, request.role());
		Map<String, Object> before = snapshot(target);
		target.changeRole(request.role(), LocalDateTime.now(clock));
		audit(authority, authorizationRole, target, AdminAction.PLATFORM_ADMIN_ROLE_CHANGED, reason, before, snapshot(target));
		return PlatformAdminResponse.from(target);
	}

	private PlatformAdminAuthority lockAndRequireFreshSuperAdmin(String accessToken) {
		lockRepository.findByIdForUpdate(PlatformAdminBootstrapLock.SINGLETON_ID)
				.orElseThrow(() -> new IllegalStateException("platform admin singleton lock row가 없습니다."));
		return accessService.requireSuperAdminFresh(accessToken);
	}

	private PlatformAdmin activeTarget(Long platformAdminId) {
		PlatformAdmin target = adminRepository.findByIdForUpdate(platformAdminId)
				.orElseThrow(() -> new PlatformAdminNotFoundException(platformAdminId));
		if (target.getStatus() != PlatformAdminStatus.ACTIVE) {
			throw new PlatformAdminConflictException("PLATFORM_ADMIN_REVOKED", "REVOKED 플랫폼 관리자는 변경할 수 없습니다.");
		}
		return target;
	}

	private void ensureNotLastActiveSuperAdmin(PlatformAdmin target, PlatformAdminRole requestedRole) {
		if (target.getRole() == PlatformAdminRole.SUPER_ADMIN
				&& requestedRole != PlatformAdminRole.SUPER_ADMIN
				&& adminRepository.countByStatusAndRole(PlatformAdminStatus.ACTIVE, PlatformAdminRole.SUPER_ADMIN) == 1) {
			throw new PlatformAdminConflictException("LAST_ACTIVE_SUPER_ADMIN", "마지막 ACTIVE SUPER_ADMIN은 revoke 또는 demote할 수 없습니다.");
		}
	}

	private void audit(PlatformAdminAuthority authority, PlatformAdmin target, AdminAction action, String reason, Map<String, Object> before, Map<String, Object> after) {
		auditService.recordPlatformMutation(null, authority.actor(), authority.platformAdmin(), action,
				AuditTargetType.PLATFORM_ADMIN, target.getId(), reason, before, after);
	}

	private void audit(PlatformAdminAuthority authority, PlatformAdminRole authorizationRole, PlatformAdmin target, AdminAction action, String reason, Map<String, Object> before, Map<String, Object> after) {
		auditService.recordPlatformMutation(null, authority.actor(), authority.platformAdmin(), authorizationRole, action,
				AuditTargetType.PLATFORM_ADMIN, target.getId(), reason, before, after);
	}

	private static Map<String, Object> snapshot(PlatformAdmin admin) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("id", admin.getId());
		snapshot.put("userId", admin.getUser().getId());
		snapshot.put("role", admin.getRole().name());
		snapshot.put("status", admin.getStatus().name());
		snapshot.put("createdAt", admin.getCreatedAt().toString());
		snapshot.put("revokedAt", admin.getRevokedAt() == null ? null : admin.getRevokedAt().toString());
		snapshot.put("reactivatedAt", admin.getReactivatedAt() == null ? null : admin.getReactivatedAt().toString());
		snapshot.put("updatedAt", admin.getUpdatedAt().toString());
		return snapshot;
	}
}
