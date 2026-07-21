package com.threadia.backend.directmessage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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

@SpringBootTest(properties = "threadia.jwt.secret=01234567890123456789012345678901")
@Transactional
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_SCHEMA_VALIDATION", matches = "true")
class DirectMessageCandidateMysqlIntegrationTests {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 21, 12, 0);

	@Autowired UserRepository users;
	@Autowired CompanyRepository companies;
	@Autowired CompanyMemberRepository members;
	@Autowired DirectMessageRoomRepository rooms;
	@Autowired DirectMessageService directMessageService;
	@MockitoBean AuthContextService authContextService;
	@MockitoBean PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void filtersOrdersCandidatesAndIncludesExistingRoomPeerOnMysql() {
		String suffix = suffix();
		User caller = users.save(user("caller-" + suffix, "aardvark-caller", UserStatus.ACTIVE));
		Company company = companies.save(company("candidate-" + suffix, caller, CompanyStatus.ACTIVE));
		members.save(membership(company, caller, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE));

		User sameNicknameFirst = users.save(user("first-" + suffix, "alpha", UserStatus.ACTIVE));
		User sameNicknameSecond = users.save(user("second-" + suffix, "alpha", UserStatus.ACTIVE));
		User adminCandidate = users.save(user("admin-" + suffix, "bravo-admin", UserStatus.ACTIVE));
		User existingRoomPeer = users.save(user("peer-" + suffix, "charlie-peer", UserStatus.ACTIVE));
		members.save(membership(company, sameNicknameFirst, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE));
		members.save(membership(company, sameNicknameSecond, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE));
		members.save(membership(company, adminCandidate, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE));
		members.save(membership(company, existingRoomPeer, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE));
		rooms.save(new DirectMessageRoom(company, caller, existingRoomPeer, NOW));

		User left = users.save(user("left-" + suffix, "excluded-left", UserStatus.ACTIVE));
		members.save(membership(company, left, CompanyMemberRole.MEMBER, CompanyMemberStatus.LEFT));
		User suspended = users.save(user("suspended-" + suffix, "excluded-suspended", UserStatus.SUSPENDED));
		members.save(membership(company, suspended, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE));
		User otherCompanyUser = users.save(user("other-" + suffix, "excluded-company", UserStatus.ACTIVE));
		Company otherCompany = companies.save(company("other-" + suffix, otherCompanyUser, CompanyStatus.ACTIVE));
		members.save(membership(otherCompany, otherCompanyUser, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE));
		authenticate("member-token", caller);

		List<Long> candidateIds = directMessageService.listCandidates("member-token", company.getId()).candidates().stream()
				.map(candidate -> candidate.id())
				.toList();

		assertThat(candidateIds).containsExactly(
				sameNicknameFirst.getId(),
				sameNicknameSecond.getId(),
				adminCandidate.getId(),
				existingRoomPeer.getId());
		assertThat(candidateIds).doesNotContain(caller.getId(), left.getId(), suspended.getId(), otherCompanyUser.getId());
	}

	@Test
	void activeCompanyAdminCallerReceivesEmptyCandidateListOnMysql() {
		String suffix = suffix();
		User admin = users.save(user("empty-admin-" + suffix, "empty-admin", UserStatus.ACTIVE));
		Company company = companies.save(company("empty-" + suffix, admin, CompanyStatus.ACTIVE));
		members.save(membership(company, admin, CompanyMemberRole.COMPANY_ADMIN, CompanyMemberStatus.ACTIVE));
		authenticate("admin-token", admin);

		assertThat(directMessageService.listCandidates("admin-token", company.getId()).candidates()).isEmpty();
	}

	@Test
	void missingCompanyIsForbiddenOnMysql() {
		User caller = users.save(user("missing-" + suffix(), "missing", UserStatus.ACTIVE));
		authenticate("missing-token", caller);

		assertThatThrownBy(() -> directMessageService.listCandidates("missing-token", Long.MAX_VALUE))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void inactiveCompanyIsForbiddenOnMysql() {
		String suffix = suffix();
		User caller = users.save(user("inactive-" + suffix, "inactive", UserStatus.ACTIVE));
		Company company = companies.save(company("inactive-" + suffix, caller, CompanyStatus.INACTIVE));
		members.save(membership(company, caller, CompanyMemberRole.MEMBER, CompanyMemberStatus.ACTIVE));
		authenticate("inactive-token", caller);

		assertThatThrownBy(() -> directMessageService.listCandidates("inactive-token", company.getId()))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void missingCallerMembershipIsForbiddenOnMysql() {
		String suffix = suffix();
		User caller = users.save(user("non-member-" + suffix, "non-member", UserStatus.ACTIVE));
		Company company = companies.save(company("non-member-" + suffix, caller, CompanyStatus.ACTIVE));
		authenticate("non-member-token", caller);

		assertThatThrownBy(() -> directMessageService.listCandidates("non-member-token", company.getId()))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void leftCallerMembershipIsForbiddenOnMysql() {
		String suffix = suffix();
		User caller = users.save(user("left-caller-" + suffix, "left-caller", UserStatus.ACTIVE));
		Company company = companies.save(company("left-caller-" + suffix, caller, CompanyStatus.ACTIVE));
		members.save(membership(company, caller, CompanyMemberRole.MEMBER, CompanyMemberStatus.LEFT));
		authenticate("left-token", caller);

		assertThatThrownBy(() -> directMessageService.listCandidates("left-token", company.getId()))
				.isInstanceOf(ForbiddenException.class);
	}

	private void authenticate(String token, User user) {
		AuthContext auth = new AuthContext(user.getId(), user.getEmail());
		when(authContextService.fromAccessToken(token)).thenReturn(auth);
		when(authContextService.requireActiveUser(auth)).thenReturn(user);
	}

	private static User user(String emailPrefix, String nickname, UserStatus status) {
		return new User(emailPrefix + "@example.com", "hash", nickname, null, status, NOW, NOW);
	}

	private static Company company(String name, User creator, CompanyStatus status) {
		return new Company(name, null, null, null, null, status, creator, NOW, NOW);
	}

	private static CompanyMember membership(
			Company company,
			User user,
			CompanyMemberRole role,
			CompanyMemberStatus status) {
		return new CompanyMember(company, user, null, role, status, NOW, NOW, NOW);
	}

	private static String suffix() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}
}
