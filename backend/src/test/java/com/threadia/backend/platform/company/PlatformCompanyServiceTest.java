package com.threadia.backend.platform.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.exception.CompanyNotFoundException;
import com.threadia.backend.company.exception.CompanyStatusConflictException;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.platform.audit.AdminAction;
import com.threadia.backend.platform.audit.AdminActionAuditService;
import com.threadia.backend.platform.audit.AuditTargetType;
import com.threadia.backend.platform.domain.PlatformAdmin;
import com.threadia.backend.platform.domain.PlatformAdminRole;
import com.threadia.backend.platform.service.PlatformAdminAccessService;
import com.threadia.backend.platform.service.PlatformAdminAuthority;
import com.threadia.backend.supportcase.exception.SupportCaseValidationException;

@ExtendWith(MockitoExtension.class)
class PlatformCompanyServiceTest {
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC);
	@Mock private PlatformAdminAccessService accessService;
	@Mock private CompanyRepository companyRepository;
	@Mock private AdminActionAuditService auditService;
	private PlatformCompanyService service;
	private User actor;
	private PlatformAdmin admin;
	private Company company;

	@BeforeEach
	void setUp() {
		service = new PlatformCompanyService(accessService, companyRepository, auditService, CLOCK);
		actor = user(1L, "admin@example.com");
		admin = new PlatformAdmin(actor, PlatformAdminRole.SUPER_ADMIN, LocalDateTime.now(CLOCK));
		setId(admin, 2L);
		company = new Company("Threadia", "대표", null, "IT", null, CompanyStatus.ACTIVE, actor, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK));
		setId(company, 10L);
	}

	@Test
	void platformAdminChangesStatusAndRecordsCompanyAuditWithoutSupportCase() {
		when(accessService.requireAdmin("platform-token")).thenReturn(new PlatformAdminAuthority(actor, admin));
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));

		var data = service.updateStatus("platform-token", 10L, new CompanyStatusUpdateRequest(CompanyStatus.INACTIVE, "  policy violation  "));

		assertThat(data.company().status()).isEqualTo("INACTIVE");
		verify(auditService).recordPlatformMutation(eq(null), eq(actor), eq(admin), eq(AdminAction.COMPANY_STATUS_CHANGED), eq(AuditTargetType.COMPANY), eq(10L), eq("policy violation"), eq(java.util.Map.of("status", "ACTIVE")), eq(java.util.Map.of("status", "INACTIVE")));
	}

	@Test
	void rejectsNonPlatformActorBeforeCompanyLookup() {
		when(accessService.requireAdmin("company-admin-token")).thenThrow(new ForbiddenException("플랫폼 관리자 권한이 필요합니다."));
		assertThatThrownBy(() -> service.updateStatus("company-admin-token", 10L, new CompanyStatusUpdateRequest(CompanyStatus.INACTIVE, "policy violation"))).isInstanceOf(ForbiddenException.class);
		verifyNoInteractions(companyRepository, auditService);
	}

	@Test
	void rejectsInvalidReasonMissingCompanyAndSameStatus() {
		when(accessService.requireAdmin("platform-token")).thenReturn(new PlatformAdminAuthority(actor, admin));
		assertThatThrownBy(() -> service.updateStatus("platform-token", 10L, new CompanyStatusUpdateRequest(CompanyStatus.INACTIVE, " x "))).isInstanceOf(SupportCaseValidationException.class);
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.updateStatus("platform-token", 10L, new CompanyStatusUpdateRequest(CompanyStatus.INACTIVE, "policy violation"))).isInstanceOf(CompanyNotFoundException.class);
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));
		assertThatThrownBy(() -> service.updateStatus("platform-token", 10L, new CompanyStatusUpdateRequest(CompanyStatus.ACTIVE, "policy violation"))).isInstanceOf(CompanyStatusConflictException.class);
	}

	private static User user(Long id, String email) { User user = new User(email, "hash", "admin", null, UserStatus.ACTIVE, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK)); setId(user, id); return user; }
	private static void setId(Object entity, Long id) { try { Field field = entity.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(entity, id); } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); } }
}
