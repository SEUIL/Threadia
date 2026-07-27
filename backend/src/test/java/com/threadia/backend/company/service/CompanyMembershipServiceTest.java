package com.threadia.backend.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyStatus;

@ExtendWith(MockitoExtension.class)
class CompanyMembershipServiceTest {

	@Mock
	private CompanyMemberRepository companyMemberRepository;
	@Mock private CompanyRepository companyRepository;

	private CompanyMembershipService companyMembershipService;

	@BeforeEach
	void setUp() {
		companyMembershipService = new CompanyMembershipService(companyMemberRepository, companyRepository);
		Company company = org.mockito.Mockito.mock(Company.class);
		lenient().when(company.getStatus()).thenReturn(CompanyStatus.ACTIVE);
		lenient().when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
	}

	@Test
	void findMembershipDelegatesToRepository() {
		CompanyMember companyMember = org.mockito.Mockito.mock(CompanyMember.class);
		when(companyMemberRepository.findByCompany_IdAndUser_Id(10L, 1L)).thenReturn(Optional.of(companyMember));

		Optional<CompanyMember> result = companyMembershipService.findMembership(10L, new AuthContext(1L, "user@example.com"));

		assertThat(result).contains(companyMember);
	}

	@Test
	void isActiveMemberChecksActiveStatus() {
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(10L, 1L, CompanyMemberStatus.ACTIVE))
				.thenReturn(true);

		assertThat(companyMembershipService.isActiveMember(10L, new AuthContext(1L, "user@example.com"))).isTrue();
	}

	@Test
	void sharesActiveCompanyUsesSingleIntersectionQueryWithActiveMembershipAndCompanyStatus() {
		when(companyMemberRepository.existsSharedCompanyWithMembershipAndCompanyStatus(
				1L, 2L, CompanyMemberStatus.ACTIVE, CompanyStatus.ACTIVE)).thenReturn(true);

		assertThat(companyMembershipService.sharesActiveCompany(1L, 2L)).isTrue();

		verify(companyMemberRepository).existsSharedCompanyWithMembershipAndCompanyStatus(
				1L, 2L, CompanyMemberStatus.ACTIVE, CompanyStatus.ACTIVE);
	}

	@Test
	void hasActiveRoleChecksRequestedRoleWithActiveStatus() {
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(
				10L,
				1L,
				CompanyMemberRole.COMPANY_ADMIN,
				CompanyMemberStatus.ACTIVE)).thenReturn(true);

		assertThat(companyMembershipService.hasActiveRole(
				10L,
				new AuthContext(1L, "user@example.com"),
				CompanyMemberRole.COMPANY_ADMIN)).isTrue();
	}

	@Test
	void requireActiveMemberThrowsWhenUserIsNotActiveCompanyMember() {
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndStatus(10L, 1L, CompanyMemberStatus.ACTIVE))
				.thenReturn(false);

		assertThatThrownBy(() -> companyMembershipService.requireActiveMember(
				10L,
				new AuthContext(1L, "user@example.com")))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void requireCompanyAdminThrowsWhenUserIsNotActiveCompanyAdmin() {
		when(companyMemberRepository.existsByCompany_IdAndUser_IdAndRoleAndStatus(
				10L,
				1L,
				CompanyMemberRole.COMPANY_ADMIN,
				CompanyMemberStatus.ACTIVE)).thenReturn(false);

		assertThatThrownBy(() -> companyMembershipService.requireCompanyAdmin(
				10L,
				new AuthContext(1L, "user@example.com")))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void inactiveCompanyRejectsMembershipBeforeMembershipRepositoryLookup() {
		Company company = org.mockito.Mockito.mock(Company.class);
		when(company.getStatus()).thenReturn(CompanyStatus.INACTIVE);
		when(companyRepository.findById(10L)).thenReturn(Optional.of(company));

		assertThatThrownBy(() -> companyMembershipService.requireActiveMember(10L, new AuthContext(1L, "user@example.com")))
				.isInstanceOf(ForbiddenException.class);
		org.mockito.Mockito.verifyNoInteractions(companyMemberRepository);
	}
}
