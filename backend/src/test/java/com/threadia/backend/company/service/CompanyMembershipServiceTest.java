package com.threadia.backend.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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

@ExtendWith(MockitoExtension.class)
class CompanyMembershipServiceTest {

	@Mock
	private CompanyMemberRepository companyMemberRepository;

	private CompanyMembershipService companyMembershipService;

	@BeforeEach
	void setUp() {
		companyMembershipService = new CompanyMembershipService(companyMemberRepository);
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
}
