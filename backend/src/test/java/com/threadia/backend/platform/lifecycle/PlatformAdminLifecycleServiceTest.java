package com.threadia.backend.platform.lifecycle;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.platform.audit.AdminActionAuditService;
import com.threadia.backend.platform.audit.AdminActionAudit;
import com.threadia.backend.platform.audit.AdminActionAuditRepository;
import com.threadia.backend.platform.audit.AdminAction;
import com.threadia.backend.platform.audit.AuditTargetType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminBootstrapLock;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.domain.PlatformAdminStatus;
import com.threadia.backend.platform.repository.PlatformAdminBootstrapLockRepository;
import com.threadia.backend.platform.repository.PlatformAdminRepository;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;

import jakarta.persistence.OneToOne;

@ExtendWith(MockitoExtension.class)
class PlatformAdminLifecycleServiceTest {
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-18T00:00:00Z"), ZoneOffset.UTC);
	@Mock private PlatformAdminAccessService accessService;
	@Mock private PlatformAdminBootstrapLockRepository lockRepository;
	@Mock private PlatformAdminRepository adminRepository;
	@Mock private UserRepository userRepository;
	@Mock private AdminActionAuditService auditService;

	@Test
	void grantLocksBeforeFreshSuperAdminReauthorizationAndAudits() throws Exception {
		User actor = user(1L, "super@example.com");
		PlatformAdmin superAdmin = admin(actor, 10L, PlatformAdminRole.SUPER_ADMIN);
		User target = user(2L, "target@example.com");
		when(lockRepository.findByIdForUpdate(PlatformAdminBootstrapLock.SINGLETON_ID)).thenReturn(Optional.of(new PlatformAdminBootstrapLock(1)));
		when(accessService.requireSuperAdminFresh("token")).thenReturn(new PlatformAdminAuthority(actor, superAdmin));
		when(userRepository.findById(2L)).thenReturn(Optional.of(target));
		when(adminRepository.findByUser_Id(2L)).thenReturn(Optional.empty());
		when(adminRepository.save(any())).thenAnswer(invocation -> { PlatformAdmin created = invocation.getArgument(0); setId(created, 11L); return created; });

		service().grant("token", new GrantPlatformAdminRequest(2L, PlatformAdminRole.PLATFORM_ADMIN, "운영 권한 부여"));

		InOrder order = inOrder(lockRepository, accessService);
		order.verify(lockRepository).findByIdForUpdate(1);
		order.verify(accessService).requireSuperAdminFresh("token");
		org.mockito.Mockito.verify(auditService).recordPlatformMutation(any(), any(), any(),
				org.mockito.ArgumentMatchers.eq(com.threadia.backend.platform.audit.AdminAction.PLATFORM_ADMIN_GRANTED), any(), any(), any(), any(), any());
	}

	@Test
	void preventsRevokingLastActiveSuperAdmin() throws Exception {
		User actor = user(1L, "super@example.com");
		PlatformAdmin superAdmin = admin(actor, 10L, PlatformAdminRole.SUPER_ADMIN);
		when(lockRepository.findByIdForUpdate(1)).thenReturn(Optional.of(new PlatformAdminBootstrapLock(1)));
		when(accessService.requireSuperAdminFresh("token")).thenReturn(new PlatformAdminAuthority(actor, superAdmin));
		when(adminRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(superAdmin));
		when(adminRepository.countByStatusAndRole(PlatformAdminStatus.ACTIVE, PlatformAdminRole.SUPER_ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> service().revoke("token", 10L, new PlatformAdminLifecycleRequest("권한 정리")))
				.isInstanceOf(PlatformAdminConflictException.class)
				.extracting("code").isEqualTo("LAST_ACTIVE_SUPER_ADMIN");
	}

	@Test
	void mapsUniquePlatformAdminUserAsOneToOne() throws Exception {
		assertNotNull(PlatformAdmin.class.getDeclaredField("user").getAnnotation(OneToOne.class));
	}

	@Test
	void selfDemotionAuditsAuthorizationTimeSuperAdminRole() throws Exception {
		User actor = user(1L, "super@example.com");
		PlatformAdmin superAdmin = admin(actor, 10L, PlatformAdminRole.SUPER_ADMIN);
		AdminActionAuditRepository auditRepository = org.mockito.Mockito.mock(AdminActionAuditRepository.class);
		AdminActionAuditService realAuditService = new AdminActionAuditService(auditRepository, new ObjectMapper());
		when(lockRepository.findByIdForUpdate(1)).thenReturn(Optional.of(new PlatformAdminBootstrapLock(1)));
		when(accessService.requireSuperAdminFresh("token")).thenReturn(new PlatformAdminAuthority(actor, superAdmin));
		when(adminRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(superAdmin));
		when(adminRepository.countByStatusAndRole(PlatformAdminStatus.ACTIVE, PlatformAdminRole.SUPER_ADMIN)).thenReturn(2L);

		new PlatformAdminLifecycleService(accessService, lockRepository, adminRepository, userRepository, realAuditService, CLOCK)
				.changeRole("token", 10L, new ChangePlatformAdminRoleRequest(PlatformAdminRole.PLATFORM_ADMIN, "권한 이관"));

		ArgumentCaptor<AdminActionAudit> captor = ArgumentCaptor.forClass(AdminActionAudit.class);
		verify(auditRepository).save(captor.capture());
		assertThat(captor.getValue().getAuthorityRole()).isEqualTo(PlatformAdminRole.SUPER_ADMIN);
		assertThat(captor.getValue().getAfterState()).contains("PLATFORM_ADMIN");
	}

	private PlatformAdminLifecycleService service() { return new PlatformAdminLifecycleService(accessService, lockRepository, adminRepository, userRepository, auditService, CLOCK); }
	private static User user(Long id, String email) throws Exception { User user = new User(email, "hash", "name", null, UserStatus.ACTIVE, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK)); setId(user, id); return user; }
	private static PlatformAdmin admin(User user, Long id, PlatformAdminRole role) throws Exception { PlatformAdmin admin = new PlatformAdmin(user, role, LocalDateTime.now(CLOCK)); setId(admin, id); return admin; }
	private static void setId(Object target, Long id) throws Exception { Field field = target.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(target, id); }
}
