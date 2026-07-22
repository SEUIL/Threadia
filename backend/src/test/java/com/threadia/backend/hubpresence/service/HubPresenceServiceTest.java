package com.threadia.backend.hubpresence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.common.exception.ForbiddenException;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.hubpresence.dto.HubPresenceOnlineData;

@ExtendWith(MockitoExtension.class)
class HubPresenceServiceTest {

	@Mock private AuthContextService auth;
	@Mock private CompanyMembershipService companyMembership;

	@Test
	void heartbeatRequiresActiveMembershipAndIncludesCaller() {
		AuthContext context = new AuthContext(1L, "member@example.com");
		when(auth.fromAccessToken("token")).thenReturn(context);

		HubPresenceOnlineData data = service(Instant.parse("2026-07-21T00:00:00Z")).heartbeat("token", 10L);

		assertThat(data.onlineUserIds()).containsExactly(1L);
		verify(companyMembership).requireActiveMember(10L, context);
	}

	@Test
	void heartbeatRejectsNonMember() {
		AuthContext context = new AuthContext(1L, "member@example.com");
		when(auth.fromAccessToken("token")).thenReturn(context);
		doThrow(new ForbiddenException("회사 접근 권한이 없습니다.")).when(companyMembership).requireActiveMember(10L, context);

		assertThatThrownBy(() -> service(Instant.parse("2026-07-21T00:00:00Z")).heartbeat("token", 10L))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void heartbeatDropsUsersOutsideOnlineWindow() {
		AuthContext user1 = new AuthContext(1L, "one@example.com");
		AuthContext user2 = new AuthContext(2L, "two@example.com");
		when(auth.fromAccessToken("token-1")).thenReturn(user1);
		when(auth.fromAccessToken("token-2")).thenReturn(user2);
		MutableClock clock = new MutableClock(Instant.parse("2026-07-21T00:00:00Z"));
		HubPresenceService service = new HubPresenceService(auth, companyMembership, clock);

		service.heartbeat("token-1", 10L);
		// user2가 21초 뒤(ONLINE_WINDOW=20초 초과)에 heartbeat를 보내면 그 시점에
		// user1은 이미 window 밖이라 목록에서 빠지고 user2만 남는다.
		clock.set(Instant.parse("2026-07-21T00:00:21Z"));
		HubPresenceOnlineData data = service.heartbeat("token-2", 10L);

		assertThat(data.onlineUserIds()).containsExactly(2L);
	}

	@Test
	void heartbeatScopesOnlineUsersPerCompany() {
		AuthContext company10User = new AuthContext(1L, "one@example.com");
		AuthContext company20User = new AuthContext(2L, "two@example.com");
		when(auth.fromAccessToken("token-1")).thenReturn(company10User);
		when(auth.fromAccessToken("token-2")).thenReturn(company20User);
		HubPresenceService service = new HubPresenceService(auth, companyMembership, new MutableClock(Instant.parse("2026-07-21T00:00:00Z")));

		service.heartbeat("token-1", 10L);
		HubPresenceOnlineData company20Data = service.heartbeat("token-2", 20L);

		assertThat(company20Data.onlineUserIds()).containsExactly(2L);
	}

	private HubPresenceService service(Instant now) {
		return new HubPresenceService(auth, companyMembership, Clock.fixed(now, ZoneOffset.UTC));
	}

	// java.time.Clock은 불변이라, 같은 서비스 인스턴스(=같은 내부 store)를 유지한 채
	// 시간만 흐르게 하려고 테스트 전용으로 만든 가변 Clock.
	private static final class MutableClock extends Clock {
		private Instant instant;

		MutableClock(Instant instant) {
			this.instant = instant;
		}

		void set(Instant instant) {
			this.instant = instant;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
