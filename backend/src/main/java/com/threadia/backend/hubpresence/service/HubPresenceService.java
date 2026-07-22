package com.threadia.backend.hubpresence.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.threadia.backend.common.auth.AuthContext;
import com.threadia.backend.common.auth.AuthContextService;
import com.threadia.backend.company.service.CompanyMembershipService;
import com.threadia.backend.hubpresence.dto.HubPresenceOnlineData;

/**
 * S03 허브 온라인 상태 — "온라인 = 허브 페이지 접속 여부"(정철진 확인,
 * 2026-07-21)로 정의한다. 프론트가 /hub에 떠 있는 동안 주기적으로
 * heartbeat를 보내고, 이 서비스는 최근 ONLINE_WINDOW 안에 heartbeat가 온
 * userId만 "온라인"으로 본다.
 *
 * DB 대신 메모리(ConcurrentHashMap)에 둔다 — 온라인 상태는 그 자체로
 * 휘발성 데이터라(서버 재시작하면 어차피 다 다시 접속해서 새로 heartbeat를
 * 보낸다) 영속화할 이유가 없고, hub_chat_messages처럼 스키마 마이그레이션을
 * 추가할 필요도 없다. 다중 인스턴스로 수평 확장하면 인스턴스마다 상태가
 * 갈라지는 한계가 있지만, 지금 규모(Phase 1, 단일 인스턴스)에서는 충분하다.
 */
@Service
public class HubPresenceService {

	private static final Duration ONLINE_WINDOW = Duration.ofSeconds(20);

	private final AuthContextService authContextService;
	private final CompanyMembershipService companyMembershipService;
	private final Clock clock;
	private final Map<Long, Map<Long, Instant>> lastSeenByCompany = new ConcurrentHashMap<>();

	@Autowired
	public HubPresenceService(AuthContextService authContextService, CompanyMembershipService companyMembershipService) {
		this(authContextService, companyMembershipService, Clock.systemUTC());
	}

	HubPresenceService(AuthContextService authContextService, CompanyMembershipService companyMembershipService, Clock clock) {
		this.authContextService = authContextService;
		this.companyMembershipService = companyMembershipService;
		this.clock = clock;
	}

	public HubPresenceOnlineData heartbeat(String accessToken, Long companyId) {
		AuthContext auth = activeAuth(accessToken);
		companyMembershipService.requireActiveMember(companyId, auth);

		Instant now = clock.instant();
		Map<Long, Instant> lastSeenByUser = lastSeenByCompany.computeIfAbsent(companyId, id -> new ConcurrentHashMap<>());
		lastSeenByUser.put(auth.userId(), now);
		lastSeenByUser.entrySet().removeIf(entry -> Duration.between(entry.getValue(), now).compareTo(ONLINE_WINDOW) > 0);

		return new HubPresenceOnlineData(List.copyOf(lastSeenByUser.keySet()));
	}

	private AuthContext activeAuth(String accessToken) {
		AuthContext auth = authContextService.fromAccessToken(accessToken);
		authContextService.requireActiveUser(auth);
		return auth;
	}
}
