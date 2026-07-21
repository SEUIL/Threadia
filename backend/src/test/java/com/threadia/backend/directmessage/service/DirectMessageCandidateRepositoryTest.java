package com.threadia.backend.directmessage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.threadia.backend.auth.domain.User;
import com.threadia.backend.auth.domain.UserStatus;
import com.threadia.backend.auth.repository.UserRepository;
import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.domain.CompanyStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;
import com.threadia.backend.company.repository.CompanyRepository;
import com.threadia.backend.directmessage.domain.DirectMessageRoom;
import com.threadia.backend.directmessage.repository.DirectMessageRoomRepository;
import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:dm-candidates;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
@Transactional
class DirectMessageCandidateRepositoryTest {
	@Autowired UserRepository users;
	@Autowired CompanyRepository companies;
	@Autowired CompanyMemberRepository members;
	@Autowired DirectMessageRoomRepository rooms;
	@Autowired DirectMessageService directMessageService;
	@MockitoBean AuthContextService authContextService;
	@MockitoBean PlatformAdminBootstrap platformAdminBootstrap;

	@Test void filtersAndOrdersCandidatesAndIncludesExistingRoomPeer() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 20, 12, 0);
		User caller = users.save(user("caller@example.com", "caller", UserStatus.ACTIVE, now));
		Company company = companies.save(company("primary", caller, CompanyStatus.ACTIVE, now));
		members.save(membership(company, caller, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, now));
		User sameNicknameFirst = users.save(user("first@example.com", "alpha", UserStatus.ACTIVE, now));
		User sameNicknameSecond = users.save(user("second@example.com", "alpha", UserStatus.ACTIVE, now));
		User existingRoomPeer = users.save(user("peer@example.com", "beta", UserStatus.ACTIVE, now));
		for (User candidate : List.of(sameNicknameFirst, sameNicknameSecond, existingRoomPeer)) {
			members.save(membership(company, candidate, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, now));
		}
		rooms.save(new DirectMessageRoom(company, caller, existingRoomPeer, now));

		User left = users.save(user("left@example.com", "excluded-left", UserStatus.ACTIVE, now));
		members.save(membership(company, left, CompanyMemberRole.MEMBER, CompanyMemberStatus.LEFT, now));
		User suspended = users.save(user("suspended@example.com", "excluded-suspended", UserStatus.SUSPENDED, now));
		members.save(membership(company, suspended, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, now));
		User otherCompanyUser = users.save(user("other@example.com", "excluded-company", UserStatus.ACTIVE, now));
		Company otherCompany = companies.save(company("other", otherCompanyUser, CompanyStatus.ACTIVE, now));
		members.save(membership(otherCompany, otherCompanyUser, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE, now));

		List<Long> candidateIds = members.findDirectMessageCandidates(company.getId(), caller.getId()).stream()
				.map(member -> member.getUser().getId()).toList();

		assertThat(candidateIds).containsExactly(sameNicknameFirst.getId(), sameNicknameSecond.getId(), existingRoomPeer.getId());
	}

	@Test void supportsCompanyAdminCallerAndEmptyCandidateList() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 20, 12, 0);
		User admin = users.save(user("admin@example.com", "admin", UserStatus.ACTIVE, now));
		Company company = companies.save(company("admin-only", admin, CompanyStatus.ACTIVE, now));
		members.save(membership(company, admin, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE, now));

		assertThat(members.findDirectMessageCandidates(company.getId(), admin.getId())).isEmpty();
	}

	@Test void candidatePathAllowsActiveCompanyAndActiveCallerMembership() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 20, 12, 0);
		User caller = users.save(user("active-caller@example.com", "active-caller", UserStatus.ACTIVE, now));
		Company company = companies.save(company("active-company", caller, CompanyStatus.ACTIVE, now));
		members.save(membership(company, caller, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, now));
		authenticate("active-token", caller);

		assertThat(directMessageService.listCandidates("active-token", company.getId()).candidates()).isEmpty();
	}

	@Test void candidatePathRejectsMissingCompany() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 20, 12, 0);
		User caller = users.save(user("missing-company@example.com", "missing-company", UserStatus.ACTIVE, now));
		authenticate("missing-company-token", caller);

		assertThatThrownBy(() -> directMessageService.listCandidates("missing-company-token", Long.MAX_VALUE))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test void candidatePathRejectsInactiveCompany() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 20, 12, 0);
		User caller = users.save(user("inactive-company@example.com", "inactive-company", UserStatus.ACTIVE, now));
		Company company = companies.save(company("inactive-company", caller, CompanyStatus.INACTIVE, now));
		members.save(membership(company, caller, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE, now));
		authenticate("inactive-company-token", caller);

		assertThatThrownBy(() -> directMessageService.listCandidates("inactive-company-token", company.getId()))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test void candidatePathRejectsMissingCallerMembership() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 20, 12, 0);
		User caller = users.save(user("missing-membership@example.com", "missing-membership", UserStatus.ACTIVE, now));
		Company company = companies.save(company("missing-membership", caller, CompanyStatus.ACTIVE, now));
		authenticate("missing-membership-token", caller);

		assertThatThrownBy(() -> directMessageService.listCandidates("missing-membership-token", company.getId()))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test void candidatePathRejectsLeftCallerMembership() {
		LocalDateTime now = LocalDateTime.of(2026, 7, 20, 12, 0);
		User caller = users.save(user("left-membership@example.com", "left-membership", UserStatus.ACTIVE, now));
		Company company = companies.save(company("left-membership", caller, CompanyStatus.ACTIVE, now));
		members.save(membership(company, caller, CompanyMemberRole.MEMBER, CompanyMemberStatus.LEFT, now));
		authenticate("left-membership-token", caller);

		assertThatThrownBy(() -> directMessageService.listCandidates("left-membership-token", company.getId()))
				.isInstanceOf(ForbiddenException.class);
	}

	private void authenticate(String token, User user) {
		AuthContext auth = new AuthContext(user.getId(), user.getEmail());
		when(authContextService.fromAccessToken(token)).thenReturn(auth);
		when(authContextService.requireActiveUser(auth)).thenReturn(user);
	}

	private static User user(String email, String nickname, UserStatus status, LocalDateTime now) {
		return new User(email, "hash", nickname, null, status, now, now);
	}
	private static Company company(String name, User creator, CompanyStatus status, LocalDateTime now) {
		return new Company(name, null, null, null, null, status, creator, now, now);
	}
	private static CompanyMember membership(Company company, User user, CompanyMemberRole role, CompanyMemberStatus status, LocalDateTime now) {
		return new CompanyMember(company, user, null, role, status, now, now, now);
	}
}
