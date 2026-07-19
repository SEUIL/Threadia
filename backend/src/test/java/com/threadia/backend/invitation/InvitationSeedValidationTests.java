package com.threadia.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.invitation.domain.Invitation;
import com.threadia.backend.invitation.domain.InvitationScopeType;
import com.threadia.backend.invitation.domain.InvitationStatus;
import com.threadia.backend.invitation.repository.InvitationRepository;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_SCHEMA_VALIDATION", matches = "true")
class InvitationSeedValidationTests {

	@MockitoBean
	private PlatformAdminBootstrap platformAdminBootstrap;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private CompanyMemberRepository companyMemberRepository;

	@Autowired
	private InvitationRepository invitationRepository;

	@Test
	void persistsSeedCompanyAdminAndPendingCompanyInvitation() {
		LocalDateTime now = LocalDateTime.now(Clock.system(ZoneOffset.UTC));
		String suffix = UUID.randomUUID().toString();
		User admin = userRepository.save(new User(
				"seed-admin-" + suffix + "@example.com",
				"hashed",
				"Seed Admin",
				null,
				UserStatus.ACTIVE,
				now,
				now));
		User invitee = userRepository.save(new User(
				"seed-invitee-" + suffix + "@example.com",
				"hashed",
				"Seed Invitee",
				null,
				UserStatus.ACTIVE,
				now,
				now));
		Company company = companyRepository.save(new Company("Threadia Seed " + suffix,
				"여도현",
				"Sprint 1.5 invitation seed verification",
				"IT",
				null, com.threadia.backend.company.domain.CompanyStatus.ACTIVE, admin,
				now,
				now));
		CompanyMember adminMembership = companyMemberRepository.save(new CompanyMember(
				company,
				admin,
				null,
				CompanyMemberRole.COMPANY_ADMIN,
				CompanyMemberStatus.ACTIVE,
				now,
				now,
				now));
		Invitation invitation = invitationRepository.save(new Invitation(
				company,
				null,
				admin,
				invitee,
				InvitationScopeType.COMPANY,
				CompanyMemberRole.MEMBER.name(),
				InvitationStatus.PENDING,
				now.plusDays(7),
				now,
				now));

		assertThat(company.getId()).isNotNull();
		assertThat(company.getCreatedBy()).isSameAs(admin);
		assertThat(adminMembership.getId()).isNotNull();
		assertThat(adminMembership.getCompany()).isSameAs(company);
		assertThat(adminMembership.getUser()).isSameAs(admin);
		assertThat(adminMembership.getRole()).isEqualTo(CompanyMemberRole.COMPANY_ADMIN);
		assertThat(adminMembership.getStatus()).isEqualTo(CompanyMemberStatus.ACTIVE);
		assertThat(invitation.getId()).isNotNull();
		assertThat(invitation.getCompany()).isSameAs(company);
		assertThat(invitation.getInviter()).isSameAs(admin);
		assertThat(invitation.getInvitee()).isSameAs(invitee);
		assertThat(invitation.getScopeType()).isEqualTo(InvitationScopeType.COMPANY);
		assertThat(invitation.getProjectId()).isNull();
		assertThat(invitation.getRole()).isEqualTo(CompanyMemberRole.MEMBER.name());
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
		assertThat(invitation.getExpiresAt()).isAfter(invitation.getCreatedAt());
	}
}
