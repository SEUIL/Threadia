package com.threadia.backend.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.dto.CompanyCreateData;
import com.threadia.backend.company.dto.CompanyCreateRequest;
import com.threadia.backend.company.dto.CompanyData;
import com.threadia.backend.company.dto.CompanyLogoRequest;
import com.threadia.backend.company.dto.CompanyStatsData;
import com.threadia.backend.company.dto.CompanyUpdateRequest;
import com.threadia.backend.company.exception.CompanyNotFoundException;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.repository.CompanyStatsRepository;
import com.threadia.backend.config.jwt.JwtAuthenticationException;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-07T00:00:00Z"), ZoneOffset.UTC);

	@Mock
	private AuthContextService authContextService;

	@Mock
	private CompanyAccessService companyAccessService;

	@Mock
	private CompanyRepository companyRepository;

	@Mock
	private CompanyMemberRepository companyMemberRepository;

	@Mock
	private CompanyStatsRepository companyStatsRepository;

	private CompanyService companyService;

	@BeforeEach
	void setUp() {
		companyService = new CompanyService(
				authContextService,
				companyAccessService,
				companyRepository,
				companyMemberRepository,
				companyStatsRepository,
				FIXED_CLOCK);
	}

	@Test
	void createCreatesCompanyAndCompanyAdminMembershipForActiveUser() {
		AuthContext authContext = new AuthContext(1L, "admin@example.com");
		User admin = user(1L);
		when(authContextService.fromAccessToken("valid-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
			Company company = invocation.getArgument(0);
			setId(company, 10L);
			return company;
		});
		when(companyMemberRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
			CompanyMember companyMember = invocation.getArgument(0);
			setId(companyMember, 100L);
			return companyMember;
		});

		CompanyCreateData data = companyService.create(
				"valid-token",
				new CompanyCreateRequest("Threadia Inc.", "Admin", "workspace", "IT", null));

		assertThat(data.company().id()).isEqualTo(10L);
		assertThat(data.companyMember().role()).isEqualTo("COMPANY_ADMIN");
		assertThat(data.companyMember().status()).isEqualTo("ACTIVE");
		ArgumentCaptor<CompanyMember> memberCaptor = ArgumentCaptor.forClass(CompanyMember.class);
		verify(companyMemberRepository).save(memberCaptor.capture());
		assertThat(memberCaptor.getValue().getUser()).isSameAs(admin);
		assertThat(memberCaptor.getValue().getRole()).isEqualTo(CompanyMemberRole.COMPANY_ADMIN);
		assertThat(memberCaptor.getValue().getStatus()).isEqualTo(CompanyMemberStatus.ACTIVE);
	}

	@Test
	void getRequiresActiveMemberBeforeReturningCompany() {
		AuthContext authContext = new AuthContext(1L, "member@example.com");
		User member = user(1L);
		Company company = company(10L, user(1L));
		when(authContextService.fromAccessToken("valid-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(member);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));

		CompanyData data = companyService.get("valid-token", 10L);

		InOrder inOrder = inOrder(authContextService, companyRepository, companyAccessService);
		inOrder.verify(authContextService).fromAccessToken("valid-token");
		inOrder.verify(authContextService).requireActiveUser(authContext);
		inOrder.verify(companyRepository).findById(10L);
		inOrder.verify(companyAccessService).requireActiveMember(10L, authContext);
		assertThat(data.company().id()).isEqualTo(10L);
	}

	@Test
	void getThrowsNotFoundAfterAuthenticationWhenCompanyDoesNotExist() {
		AuthContext authContext = new AuthContext(1L, "member@example.com");
		User member = user(1L);
		when(authContextService.fromAccessToken("valid-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(member);
		when(companyRepository.findById(9999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> companyService.get("valid-token", 9999L))
				.isInstanceOf(CompanyNotFoundException.class);

		InOrder inOrder = inOrder(authContextService, companyRepository);
		inOrder.verify(authContextService).fromAccessToken("valid-token");
		inOrder.verify(authContextService).requireActiveUser(authContext);
		inOrder.verify(companyRepository).findById(9999L);
		verify(companyAccessService, never())
				.requireActiveMember(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void getDoesNotCheckCompanyExistenceWhenTokenIsInvalid() {
		when(authContextService.fromAccessToken("invalid-token"))
				.thenThrow(new JwtAuthenticationException("invalid token"));

		assertThatThrownBy(() -> companyService.get("invalid-token", 9999L))
				.isInstanceOf(JwtAuthenticationException.class);

		verify(companyRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
		verify(companyAccessService, never())
				.requireActiveMember(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void updateRequiresCompanyAdminAndDoesNotUpdateLogoUrl() {
		AuthContext authContext = new AuthContext(1L, "admin@example.com");
		User admin = user(1L);
		Company company = company(10L, user(1L));
		company.setLogoUrl("https://example.com/original-logo.png");
		when(authContextService.fromAccessToken("valid-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));

		CompanyData data = companyService.update(
				"valid-token",
				10L,
				new CompanyUpdateRequest("Threadia Corp.", "Admin", "updated", "IT", "https://example.com/logo.png"));

		InOrder inOrder = inOrder(authContextService, companyRepository, companyAccessService);
		inOrder.verify(authContextService).fromAccessToken("valid-token");
		inOrder.verify(authContextService).requireActiveUser(authContext);
		inOrder.verify(companyRepository).findByIdForUpdate(10L);
		inOrder.verify(companyAccessService).requireCompanyAdmin(10L, authContext);
		assertThat(data.company().name()).isEqualTo("Threadia Corp.");
		assertThat(data.company().logoUrl()).isEqualTo("https://example.com/original-logo.png");
	}

	@Test
	void updateLogoRequiresCompanyAdminAndUpdatesLogoUrlOnly() {
		AuthContext authContext = new AuthContext(1L, "admin@example.com");
		User admin = user(1L);
		Company company = company(10L, user(1L));
		when(authContextService.fromAccessToken("valid-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));

		CompanyData data = companyService.updateLogo(
				"valid-token",
				10L,
				new CompanyLogoRequest("https://example.com/logo.png"));

		InOrder inOrder = inOrder(authContextService, companyRepository, companyAccessService);
		inOrder.verify(authContextService).fromAccessToken("valid-token");
		inOrder.verify(authContextService).requireActiveUser(authContext);
		inOrder.verify(companyRepository).findByIdForUpdate(10L);
		inOrder.verify(companyAccessService).requireCompanyAdmin(10L, authContext);
		assertThat(data.company().name()).isEqualTo("Threadia Inc.");
		assertThat(data.company().logoUrl()).isEqualTo("https://example.com/logo.png");
	}

	@Test
	void statsRequiresCompanyAdminAndReturnsAggregateCounts() {
		AuthContext authContext = new AuthContext(1L, "admin@example.com");
		User admin = user(1L);
		when(authContextService.fromAccessToken("valid-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company(10L, user(1L))));
		when(companyMemberRepository.countByCompany_IdAndStatus(10L, CompanyMemberStatus.ACTIVE)).thenReturn(3L);
		when(companyStatsRepository.countDepartments(10L)).thenReturn(2L);
		when(companyStatsRepository.countProjects(10L)).thenReturn(4L);
		when(companyStatsRepository.countProjectsByStatus(10L)).thenReturn(Map.of("ACTIVE", 2L, "PLANNING", 2L));

		CompanyStatsData data = companyService.stats("valid-token", 10L);

		InOrder inOrder = inOrder(authContextService, companyRepository, companyAccessService);
		inOrder.verify(authContextService).fromAccessToken("valid-token");
		inOrder.verify(authContextService).requireActiveUser(authContext);
		inOrder.verify(companyRepository).findById(10L);
		inOrder.verify(companyAccessService).requireCompanyAdmin(10L, authContext);
		assertThat(data.memberCount()).isEqualTo(3L);
		assertThat(data.departmentCount()).isEqualTo(2L);
		assertThat(data.projectCount()).isEqualTo(4L);
		assertThat(data.projectStatusCounts()).containsEntry("ACTIVE", 2L);
	}

	private static User user(Long id) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		User user = new User("admin@example.com", "hashed", "admin", null, UserStatus.ACTIVE, now, now);
		setId(user, id);
		return user;
	}

	private static Company company(Long id, User createdBy) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		Company company = new Company("Threadia Inc.", "Admin", "workspace", "IT", null, "ACTIVE", createdBy, now, now);
		setId(company, id);
		return company;
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
