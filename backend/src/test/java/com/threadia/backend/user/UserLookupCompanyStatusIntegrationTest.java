package com.threadia.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.config.jwt.AuthenticatedUser;
import com.threadia.backend.config.jwt.JwtTokenService;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:user-lookup-company-status;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop", "threadia.jwt.secret=01234567890123456789012345678901"})
@Transactional
class UserLookupCompanyStatusIntegrationTest {
	@Autowired private UserLookupService service;
	@Autowired private UserRepository users;
	@Autowired private CompanyRepository companies;
	@Autowired private CompanyMemberRepository members;
	@MockitoBean private JwtTokenService jwtTokenService;
	@MockitoBean private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void lookupCountsOnlyCompanyAdminsInActiveCompanies() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 16, 3, 0);
		User target = users.save(user("target@example.com", now));
		User activeAdmin = admin("active-admin@example.com", CompanyStatus.ACTIVE, now);
		User inactiveAdmin = admin("inactive-admin@example.com", CompanyStatus.INACTIVE, now);
		when(jwtTokenService.parseAccessToken("active-token")).thenReturn(new AuthenticatedUser(activeAdmin.getId(), activeAdmin.getEmail()));
		when(jwtTokenService.parseAccessToken("inactive-token")).thenReturn(new AuthenticatedUser(inactiveAdmin.getId(), inactiveAdmin.getEmail()));

		assertThat(service.lookupByEmail("active-token", target.getEmail()).user().id()).isEqualTo(target.getId());
		assertThatThrownBy(() -> service.lookupByEmail("inactive-token", target.getEmail())).isInstanceOf(ForbiddenException.class);
	}

	private User admin(String email, CompanyStatus status, LocalDateTime now) { User user = users.save(user(email, now)); Company company = companies.save(new Company(email + " Co", "대표", null, "IT", null, status, user, now, now)); members.save(new CompanyMember(company, user, null, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE, now, now, now)); return user; }
	private static User user(String email, LocalDateTime now) { return new User(email, "hash", email, null, UserStatus.ACTIVE, now, now); }
}
