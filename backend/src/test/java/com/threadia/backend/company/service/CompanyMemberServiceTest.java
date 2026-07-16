package com.threadia.backend.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.dto.CompanyMemberData;
import com.threadia.backend.company.dto.CompanyMemberUpdateRequest;
import com.threadia.backend.company.dto.CompanyMembersData;
import com.threadia.backend.company.exception.CompanyMemberConflictException;
import com.threadia.backend.company.exception.CompanyMemberNotFoundException;
import com.threadia.backend.company.exception.CompanyNotFoundException;
import com.threadia.backend.company.exception.DepartmentNotFoundException;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.repository.DepartmentRepository;

@ExtendWith(MockitoExtension.class)
class CompanyMemberServiceTest {

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
	private DepartmentRepository departmentRepository;

	private CompanyMemberService companyMemberService;

	@BeforeEach
	void setUp() {
		companyMemberService = new CompanyMemberService(
				authContextService,
				companyAccessService,
				companyRepository,
				companyMemberRepository,
				departmentRepository,
				FIXED_CLOCK);
	}

	@Test
	void listRequiresCompanyAdminAndReturnsMembersOrderedByRepository() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		CompanyMember adminMember = companyMember(100L, company, admin, null, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE);
		CompanyMember member = companyMember(101L, company, user(2L, "member@example.com"), 1L, CompanyMemberRole.MEMBER, CompanyMemberStatus.SUSPENDED);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
		when(companyMemberRepository.findByCompanyIdWithFiltersOrderByIdAsc(10L, null, null, null)).thenReturn(List.of(adminMember, member));

		CompanyMembersData data = companyMemberService.list("admin-token", 10L, null, null, null);

		InOrder inOrder = inOrder(authContextService, companyRepository, companyAccessService, companyMemberRepository);
		inOrder.verify(authContextService).fromAccessToken("admin-token");
		inOrder.verify(authContextService).requireActiveUser(authContext);
		inOrder.verify(companyRepository).findById(10L);
		inOrder.verify(companyAccessService).requireCompanyAdmin(10L, authContext);
		inOrder.verify(companyMemberRepository).findByCompanyIdWithFiltersOrderByIdAsc(10L, null, null, null);
		assertThat(data.companyMembers()).extracting("id").containsExactly(100L, 101L);
		assertThat(data.companyMembers().get(1).user().email()).isEqualTo("member@example.com");
	}

	@Test
	void listThrowsNotFoundBeforePermissionWhenCompanyDoesNotExist() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findById(9999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> companyMemberService.list("admin-token", 9999L, null, null, null))
				.isInstanceOf(CompanyNotFoundException.class);
		verify(companyAccessService, never()).requireCompanyAdmin(9999L, authContext);
		verifyNoInteractions(companyMemberRepository);
	}

	@Test
	void listPassesFiltersAndNormalizedQToRepository() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
		when(companyMemberRepository.findByCompanyIdWithFiltersOrderByIdAsc(
				10L,
				CompanyMemberStatus.ACTIVE,
				5L,
				"admin@example"))
				.thenReturn(List.of());

		companyMemberService.list("admin-token", 10L, CompanyMemberStatus.ACTIVE, 5L, "  Admin@Example  ");

		verify(companyMemberRepository).findByCompanyIdWithFiltersOrderByIdAsc(
				10L,
				CompanyMemberStatus.ACTIVE,
				5L,
				"admin@example");
	}

	@Test
	void listTreatsBlankQAsNoFilter() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
		when(companyMemberRepository.findByCompanyIdWithFiltersOrderByIdAsc(
				10L,
				CompanyMemberStatus.SUSPENDED,
				null,
				null))
				.thenReturn(List.of());

		companyMemberService.list("admin-token", 10L, CompanyMemberStatus.SUSPENDED, null, "   ");

		verify(companyMemberRepository).findByCompanyIdWithFiltersOrderByIdAsc(
				10L,
				CompanyMemberStatus.SUSPENDED,
				null,
				null);
	}

	@Test
	void listReturnsNullJoinedAtWhenCompanyMemberJoinedAtIsNull() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		CompanyMember member = companyMember(101L, company, user(2L, "member@example.com"), null, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE);
		member.setJoinedAt(null);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
		when(companyMemberRepository.findByCompanyIdWithFiltersOrderByIdAsc(10L, null, null, null)).thenReturn(List.of(member));

		CompanyMembersData data = companyMemberService.list("admin-token", 10L, null, null, null);

		assertThat(data.companyMembers().get(0).joinedAt()).isNull();
	}

	@Test
	void updateRequiresCompanyAdminValidatesDepartmentAndUpdatesRoleStatusDepartmentAndUpdatedAt() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		CompanyMember member = companyMember(101L, company, user(2L, "member@example.com"), null, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyMemberRepository.findByIdAndCompanyIdForUpdate(101L, 10L)).thenReturn(Optional.of(member));
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));
		when(departmentRepository.existsByIdAndCompany_Id(5L, 10L)).thenReturn(true);

		CompanyMemberData data = companyMemberService.update(
				"admin-token",
				10L,
				101L,
				new CompanyMemberUpdateRequest(CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE, 5L));

		verify(companyAccessService).requireCompanyAdmin(10L, authContext);
		assertThat(member.getRole()).isEqualTo(CompanyMemberRole.COMPANY_ADMIN);
		assertThat(member.getStatus()).isEqualTo(CompanyMemberStatus.ACTIVE);
		assertThat(member.getDepartmentId()).isEqualTo(5L);
		assertThat(member.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 7, 0, 0));
		assertThat(data.companyMember().departmentId()).isEqualTo(5L);
	}

	@Test
	void updateClearsDepartmentWhenDepartmentIdIsNull() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		CompanyMember member = companyMember(101L, company, user(2L, "member@example.com"), 5L, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyMemberRepository.findByIdAndCompanyIdForUpdate(101L, 10L)).thenReturn(Optional.of(member));
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));

		companyMemberService.update(
				"admin-token",
				10L,
				101L,
				new CompanyMemberUpdateRequest(CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, null));

		assertThat(member.getDepartmentId()).isNull();
		verifyNoInteractions(departmentRepository);
	}

	@Test
	void updateThrowsForbiddenBeforeTargetMemberLookupWhenRequesterIsNotCompanyAdmin() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		when(authContextService.fromAccessToken("member-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));
		doThrow(new ForbiddenException("회사 관리자 권한이 필요합니다."))
				.when(companyAccessService).requireCompanyAdmin(10L, authContext);

		assertThatThrownBy(() -> companyMemberService.update(
				"member-token",
				10L,
				101L,
				new CompanyMemberUpdateRequest(CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, null)))
				.isInstanceOf(ForbiddenException.class);
		verify(companyRepository).findByIdForUpdate(10L);
		verifyNoInteractions(companyMemberRepository, departmentRepository);
	}

	@Test
	void updateThrowsNotFoundWhenCompanyMemberDoesNotExistInRequestedCompany() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));
		when(companyMemberRepository.findByIdAndCompanyIdForUpdate(999L, 10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> companyMemberService.update(
				"admin-token",
				10L,
				999L,
				new CompanyMemberUpdateRequest(CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, null)))
				.isInstanceOf(CompanyMemberNotFoundException.class);
	}

	@Test
	void updateThrowsNotFoundBeforePermissionWhenCompanyDoesNotExist() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyRepository.findByIdForUpdate(9999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> companyMemberService.update(
				"admin-token",
				9999L,
				101L,
				new CompanyMemberUpdateRequest(CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, null)))
				.isInstanceOf(CompanyNotFoundException.class);
		verify(companyAccessService, never()).requireCompanyAdmin(9999L, authContext);
		verifyNoInteractions(companyMemberRepository, departmentRepository);
	}

	@Test
	void updateThrowsNotFoundWhenDepartmentDoesNotExist() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		CompanyMember member = companyMember(101L, company, user(2L, "member@example.com"), null, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyMemberRepository.findByIdAndCompanyIdForUpdate(101L, 10L)).thenReturn(Optional.of(member));
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));
		when(departmentRepository.existsByIdAndCompany_Id(99L, 10L)).thenReturn(false);

		assertThatThrownBy(() -> companyMemberService.update(
				"admin-token",
				10L,
				101L,
				new CompanyMemberUpdateRequest(CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, 99L)))
				.isInstanceOf(DepartmentNotFoundException.class);
	}

	@Test
	void updateThrowsNotFoundWhenDepartmentBelongsToAnotherCompany() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		CompanyMember member = companyMember(101L, company, user(2L, "member@example.com"), null, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyMemberRepository.findByIdAndCompanyIdForUpdate(101L, 10L)).thenReturn(Optional.of(member));
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));
		when(departmentRepository.existsByIdAndCompany_Id(7L, 10L)).thenReturn(false);

		assertThatThrownBy(() -> companyMemberService.update(
				"admin-token",
				10L,
				101L,
				new CompanyMemberUpdateRequest(CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, 7L)))
				.isInstanceOf(DepartmentNotFoundException.class);
	}

	@Test
	void updateThrowsConflictWhenReactivatingLeftMember() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		CompanyMember leftMember = companyMember(101L, company, user(2L, "member@example.com"), null, CompanyMemberRole.MEMBER, CompanyMemberStatus.LEFT);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyMemberRepository.findByIdAndCompanyIdForUpdate(101L, 10L)).thenReturn(Optional.of(leftMember));
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));

		assertThatThrownBy(() -> companyMemberService.update(
				"admin-token",
				10L,
				101L,
				new CompanyMemberUpdateRequest(CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, null)))
				.isInstanceOf(CompanyMemberConflictException.class)
				.hasMessage("탈퇴한 회사 구성원은 직접 재활성화할 수 없습니다.");
		assertThat(leftMember.getStatus()).isEqualTo(CompanyMemberStatus.LEFT);
	}

	@Test
	void updateThrowsConflictWhenDemotingLastActiveCompanyAdmin() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		CompanyMember companyAdmin = companyMember(100L, company, admin, null, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyMemberRepository.findByIdAndCompanyIdForUpdate(100L, 10L)).thenReturn(Optional.of(companyAdmin));
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));
		when(companyMemberRepository.countByCompany_IdAndRoleAndStatus(10L, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE))
				.thenReturn(1L);

		assertThatThrownBy(() -> companyMemberService.update(
				"admin-token",
				10L,
				100L,
				new CompanyMemberUpdateRequest(CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, null)))
				.isInstanceOf(CompanyMemberConflictException.class);
	}

	@Test
	void updateAllowsAdminChangeWhenAnotherActiveCompanyAdminExists() {
		AuthContext authContext = authContext();
		User admin = user(1L, "admin@example.com");
		Company company = company(10L, admin);
		CompanyMember companyAdmin = companyMember(100L, company, admin, null, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("admin-token")).thenReturn(authContext);
		when(authContextService.requireActiveUser(authContext)).thenReturn(admin);
		when(companyMemberRepository.findByIdAndCompanyIdForUpdate(100L, 10L)).thenReturn(Optional.of(companyAdmin));
		when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));
		when(companyMemberRepository.countByCompany_IdAndRoleAndStatus(10L, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE))
				.thenReturn(2L);

		CompanyMemberData data = companyMemberService.update(
				"admin-token",
				10L,
				100L,
				new CompanyMemberUpdateRequest(CompanyMemberRole.MEMBER, CompanyMemberStatus.SUSPENDED, null));

		assertThat(companyAdmin.getRole()).isEqualTo(CompanyMemberRole.MEMBER);
		assertThat(companyAdmin.getStatus()).isEqualTo(CompanyMemberStatus.SUSPENDED);
		assertThat(data.companyMember().status()).isEqualTo("SUSPENDED");
	}

	private static AuthContext authContext() {
		return new AuthContext(1L, "admin@example.com");
	}

	private static User user(Long id, String email) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		User user = new User(email, "hashed", "user" + id, null, UserStatus.ACTIVE, now, now);
		setId(user, id);
		return user;
	}

	private static Company company(Long id, User createdBy) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
		Company company = new Company("Threadia Inc.", "Admin", "workspace", "IT", null, com.threadia.backend.company.domain.CompanyStatus.ACTIVE, createdBy, now, now);
		setId(company, id);
		return company;
	}

	private static CompanyMember companyMember(
			Long id,
			Company company,
			User user,
			Long departmentId,
			CompanyMemberRole role,
			CompanyMemberStatus status) {
		LocalDateTime now = LocalDateTime.now(FIXED_CLOCK).minusDays(1);
		CompanyMember companyMember = new CompanyMember(company, user, departmentId, role, status, now, now, now);
		setId(companyMember, id);
		return companyMember;
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
