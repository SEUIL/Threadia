package com.threadia.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

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
import com.threadia.backend.company.domain.Company;
import com.threadia.backend.company.domain.CompanyMember;
import com.threadia.backend.company.domain.CompanyMemberRole;
import com.threadia.backend.company.domain.CompanyMemberStatus;
import com.threadia.backend.company.repository.CompanyMemberRepository;

@ExtendWith(MockitoExtension.class)
class UserCompanyMembershipServiceTest {

	@Mock
	private AuthContextService authContextService;

	@Mock
	private CompanyMemberRepository companyMemberRepository;

	private UserCompanyMembershipService userCompanyMembershipService;

	@BeforeEach
	void setUp() {
		userCompanyMembershipService = new UserCompanyMembershipService(authContextService, companyMemberRepository);
	}

	@Test
	void 현재_사용자_id와_ACTIVE_상태로_repository를_조회하고_필드를_정확히_매핑한다() {
		AuthContext authContext = new AuthContext(1L, "user@example.com");
		CompanyMember membership = companyMember(
				101L,
				company(10L, "Threadia"),
				user(1L),
				CompanyMemberRole.COMPANY_ADMIN,
				CompanyMemberStatus.ACTIVE);
		when(authContextService.fromAccessToken("valid-token")).thenReturn(authContext);
		when(companyMemberRepository.findByUserIdAndStatusWithCompanyOrderByCompanyIdAscCompanyMemberIdAsc(
				1L,
				CompanyMemberStatus.ACTIVE))
				.thenReturn(List.of(membership));

		UserCompanyMembershipsData data = userCompanyMembershipService.list("valid-token");

		InOrder inOrder = inOrder(authContextService, companyMemberRepository);
		inOrder.verify(authContextService).fromAccessToken("valid-token");
		inOrder.verify(companyMemberRepository).findByUserIdAndStatusWithCompanyOrderByCompanyIdAscCompanyMemberIdAsc(
				1L,
				CompanyMemberStatus.ACTIVE);
		assertThat(data.companyMemberships()).hasSize(1);
		UserCompanyMembershipResponse response = data.companyMemberships().get(0);
		assertThat(response.companyMemberId()).isEqualTo(101L);
		assertThat(response.companyId()).isEqualTo(10L);
		assertThat(response.companyName()).isEqualTo("Threadia");
		assertThat(response.role()).isEqualTo("COMPANY_ADMIN");
		assertThat(response.status()).isEqualTo("ACTIVE");
	}

	@Test
	void repository_결과가_없으면_빈_목록을_반환한다() {
		AuthContext authContext = new AuthContext(1L, "user@example.com");
		when(authContextService.fromAccessToken("valid-token")).thenReturn(authContext);
		when(companyMemberRepository.findByUserIdAndStatusWithCompanyOrderByCompanyIdAscCompanyMemberIdAsc(
				1L,
				CompanyMemberStatus.ACTIVE))
				.thenReturn(List.of());

		UserCompanyMembershipsData data = userCompanyMembershipService.list("valid-token");

		verify(authContextService).fromAccessToken("valid-token");
		verify(companyMemberRepository).findByUserIdAndStatusWithCompanyOrderByCompanyIdAscCompanyMemberIdAsc(
				1L,
				CompanyMemberStatus.ACTIVE);
		assertThat(data.companyMemberships()).isEmpty();
	}

	private static User user(Long id) {
		LocalDateTime now = LocalDateTime.parse("2026-07-08T00:00:00");
		User user = new User("user" + id + "@example.com", "hashed", "user" + id, null, UserStatus.ACTIVE, now, now);
		setId(user, id);
		return user;
	}

	private static Company company(Long id, String name) {
		LocalDateTime now = LocalDateTime.parse("2026-07-08T00:00:00");
		Company company = new Company(name, "대표", "workspace", "IT", null, "ACTIVE", user(99L), now, now);
		setId(company, id);
		return company;
	}

	private static CompanyMember companyMember(
			Long id,
			Company company,
			User user,
			CompanyMemberRole role,
			CompanyMemberStatus status) {
		LocalDateTime now = LocalDateTime.parse("2026-07-08T00:00:00");
		CompanyMember companyMember = new CompanyMember(company, user, null, role, status, now, now, now);
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
