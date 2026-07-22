/*
 * S09/S14 DM — production 빌드에서만 결정적으로 재현되는 mount race 회귀.
 * PR #173 4차 재리뷰(pktewon) blocker 5 반영, React Query 마이그레이션
 * (이슈 #217) 이후 갱신.
 *
 * dev 서버(Strict Mode)는 mount effect를 두 번 실행해 "몇 번째 GET 요청을
 * 붙잡을지"가 실행마다 달라진다 — 이 파일은 production 빌드(Strict Mode
 * 없음, mount당 effect 정확히 1번)로만 돈다(playwright.production.config.ts).
 *
 * ⚠️ React Query 도입 이후 이 파일의 시나리오가 달라졌다: 이전엔
 * dm/layout.tsx(rail)와 dm/[roomId]/page.tsx(본문)가 각자 GET .../rooms를
 * 불러서 같은 요청이 두 번 나갔는데, 이제 둘 다 같은 useDmRoomsQuery(같은
 * queryKey)를 써서 React Query가 dedup한다 — mount당 GET 요청이 정확히
 * 한 번만 나간다. "옛 응답 vs 새 응답" 두 개를 인위적으로 겹치게 만들던
 * call-index 기반 시나리오는 이제 성립하지 않는다(그 자체가 이 마이그레이션의
 * 목적이었다 — 애초에 겹칠 두 번째 요청이 없다). 대신 아래 두 테스트는
 * "요청이 느려도(hold) 최종 상태가 항상 올바르다"는, React Query 시대에
 * 맞는 형태로 다시 짰다.
 */
import { expect, test } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "dm-mount-race" } });

const corsHeaders = (origin: string) => ({
  "access-control-allow-origin": origin,
  "access-control-allow-methods": "GET,POST,PATCH,DELETE,OPTIONS",
  "access-control-allow-headers": "authorization,content-type",
  "access-control-allow-credentials": "true",
});

const room = (over: { id: number; otherUserId: number; nickname: string; unreadCount: number }) => ({
  id: over.id,
  companyId: 1,
  otherUser: { id: over.otherUserId, email: `${over.nickname}@test.dev`, nickname: over.nickname, avatarUrl: null },
  unreadCount: over.unreadCount,
  createdAt: "2026-07-01T00:00:00Z",
});

/** 특정 요청을 external release() 전까지 붙잡아두는 게이트. */
function createHold() {
  let release: () => void = () => {};
  const gate = new Promise<void>((resolve) => {
    release = resolve;
  });
  return { gate, release };
}

test("초기 room 목록 조회가 느려도(hold), 풀린 뒤 mark-read가 정상 반영된 최종 상태로 렌더된다", async ({
  page,
}) => {
  const rooms = [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 3 })];
  const messages = [
    { id: 1, roomId: 301, sender: { id: 2, email: "b@test.dev", nickname: "김민정", avatarUrl: null }, content: "안녕하세요", createdAt: "2026-07-19T00:00:00Z" },
  ];
  const hold = createHold();

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const headers = corsHeaders(request.headers().origin ?? "http://127.0.0.1:3002");
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: "application/json", headers, body: JSON.stringify(body) });

    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers });
    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      return json(apiResponse({ accessToken: "test-token", user: { id: 1, email: "a@test.dev", nickname: "나", avatarUrl: null } }));
    }
    if (url.pathname === "/api/v1/users/me/company-memberships") {
      return json(apiResponse({ companyMemberships: [{ companyId: 1, companyName: "Threadia", role: "MEMBER" }] }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      // React Query dedup 덕분에 mount당 이 GET은 정확히 한 번만 나간다 —
      // 그 유일한 호출을 붙잡는다. dm/[roomId]/page.tsx는 이게 풀리기 전까지
      // room 검증을 못 끝내고, messages/markRead는 그 검증 뒤에만 나가도록
      // enabled로 걸려 있어서(순차 의존) 자동으로 뒤따라 대기한다.
      await hold.gate;
      return json(apiResponse({ rooms }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/direct-messages/rooms/301/messages") {
      return json(apiResponse({ messages }));
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/direct-messages/rooms/301/mark-read") {
      return json(apiResponse({ roomId: 301, lastReadDirectMessageId: 1, unreadCount: 1, readAt: "2026-07-20T00:00:00Z" }));
    }
    await route.continue();
  });

  try {
    await page.goto("/1/dm/301");
    // room 목록이 아직 안 풀렸으니 본문은 로딩 상태여야 하고, mark-read도
    // 아직 나갈 수 없다(순차 의존 확인).
    await expect(page.getByText("대화를 불러오는 중...")).toBeVisible();
  } finally {
    hold.release();
  }

  // 풀린 뒤: room 목록 → messages → mark-read 순서로 이어지고, mark-read가
  // 돌려준 unreadCount=1이 배지 숫자에 정확히 반영돼야 한다 — 초기 목록의
  // unreadCount=3이 그대로 남아있거나 무조건 0으로 되돌아가면 안 된다(둘 다
  // "배지가 떠 있다"는 조건만으로는 구분이 안 돼서 정확한 숫자를 확인한다).
  await expect(page.getByRole("heading", { name: "김민정" })).toBeVisible();
  const railBadge = page.locator('a[title="김민정"] span.bg-destructive');
  await expect(railBadge).toHaveText("1");
});

test("mark-read가 실패하면 낙관적으로 0으로 바꿨던 배지가 원래 unreadCount로 되돌아간다", async ({
  page,
}) => {
  // 이슈 #217 코멘트(dhy257)의 optimistic update/rollback 지적 반영 —
  // onMutate에서 배지를 먼저 0으로 반영하고, 서버가 실패를 돌려주면
  // onError가 onMutate 시점 스냅샷(unreadCount=5)으로 되돌려야 한다.
  const rooms = [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 5 })];
  const messages = [
    { id: 1, roomId: 301, sender: { id: 2, email: "b@test.dev", nickname: "김민정", avatarUrl: null }, content: "안녕하세요", createdAt: "2026-07-19T00:00:00Z" },
  ];
  const hold = createHold();

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const headers = corsHeaders(request.headers().origin ?? "http://127.0.0.1:3002");
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: "application/json", headers, body: JSON.stringify(body) });

    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers });
    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      return json(apiResponse({ accessToken: "test-token", user: { id: 1, email: "a@test.dev", nickname: "나", avatarUrl: null } }));
    }
    if (url.pathname === "/api/v1/users/me/company-memberships") {
      return json(apiResponse({ companyMemberships: [{ companyId: 1, companyName: "Threadia", role: "MEMBER" }] }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      return json(apiResponse({ rooms }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/direct-messages/rooms/301/messages") {
      return json(apiResponse({ messages }));
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/direct-messages/rooms/301/mark-read") {
      // 서버 응답을 잡아둬서 "낙관적으로 0이 된 직후, 아직 실패가 안 온" 순간을 관측한다.
      await hold.gate;
      return route.fulfill({
        status: 500,
        contentType: "application/json",
        headers,
        body: JSON.stringify({ error: { code: "INTERNAL_ERROR", message: "mark-read failed" } }),
      });
    }
    await route.continue();
  });

  const railBadge = page.locator('a[title="김민정"] span.bg-destructive');

  await page.goto("/1/dm/301");
  await expect(page.getByRole("heading", { name: "김민정" })).toBeVisible();
  // mark-read가 아직 (hold로) 응답을 못 받은 상태 — onMutate의 낙관적 patch로
  // 배지가 이미 사라져 있어야 한다(서버 왕복을 기다리지 않는 게 이번 보강의 목적).
  await expect(railBadge).toHaveCount(0);

  hold.release();

  // 실패 응답이 도착하면 onError가 되돌린 스냅샷값(5)이 다시 떠야 한다.
  await expect(railBadge).toHaveText("5");
});

test("새 room 생성 후 목록 재조회(invalidate)가 느려도(hold), 풀린 뒤 새 room이 rail에 반영된다", async ({
  page,
}) => {
  const rooms = [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 0 })];
  const candidates = [{ id: 4, nickname: "박서연" }];
  let holdNextRoomsFetch = false;
  const hold = createHold();

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const headers = corsHeaders(request.headers().origin ?? "http://127.0.0.1:3002");
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: "application/json", headers, body: JSON.stringify(body) });

    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers });
    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      return json(apiResponse({ accessToken: "test-token", user: { id: 1, email: "a@test.dev", nickname: "나", avatarUrl: null } }));
    }
    if (url.pathname === "/api/v1/users/me/company-memberships") {
      return json(apiResponse({ companyMemberships: [{ companyId: 1, companyName: "Threadia", role: "MEMBER" }] }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/candidates") {
      return json(apiResponse({ candidates: candidates.map((c) => ({ id: c.id, nickname: c.nickname, avatarUrl: null })) }));
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      const body = request.postDataJSON() as { otherUserId: number };
      const candidate = candidates.find((c) => c.id === body.otherUserId);
      const newRoom = room({ id: 999, otherUserId: body.otherUserId, nickname: candidate?.nickname ?? "새 상대", unreadCount: 0 });
      rooms.push(newRoom);
      // 이 POST가 성공하는 순간부터 useStartDmMutation의 onSuccess가 rooms
      // 쿼리를 invalidate한다 — 그 뒤에 나가는 GET(딱 하나)을 붙잡는다.
      holdNextRoomsFetch = true;
      return json(apiResponse({ room: newRoom }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      if (holdNextRoomsFetch) {
        holdNextRoomsFetch = false;
        await hold.gate;
      }
      return json(apiResponse({ rooms: rooms.map((r) => ({ ...r })) }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/direct-messages/rooms/301/messages") {
      return json(apiResponse({ messages: [] }));
    }
    await route.continue();
  });

  try {
    await page.goto("/1/dm/301");
    await expect(page.getByRole("heading", { name: "김민정" })).toBeVisible();

    await page.getByTitle("새 대화 시작").click();
    await page.getByRole("button", { name: "박서연" }).click();

    // invalidate로 나간 재조회가 아직 안 풀렸으니, 박서연은 아직 rail에 없다.
    await expect(page.locator('a[title="박서연"]')).toHaveCount(0);
  } finally {
    hold.release();
  }

  // 풀린 뒤엔 기존 방(김민정)과 새 방(박서연) 둘 다 rail에 남아 있어야 한다.
  await expect(page.locator('a[title="김민정"]')).toHaveCount(1);
  await expect(page.locator('a[title="박서연"]')).toHaveCount(1);
});

/*
 * 4차 재리뷰(pktewon) 반영 — blocker 4(DmLayout): 새 DM 생성(get-or-create)
 * 응답을 기다리는 동안 사용자가 기존 room 링크를 눌러 이미 다른 곳으로
 * 이동했다면, 늦게 도착한 응답이 그 이동을 덮어쓰고 새로 만든 room으로
 * 강제 이동시키면 안 된다. React Query 마이그레이션 이후에도 이 부분은
 * useStartDmMutation을 감싼 startDm()이 pathname ref로 직접 확인한다(React
 * Query는 캐시 최신성은 챙겨주지만 router.push는 캐시 밖의 부수효과라서
 * 여전히 직접 확인해야 한다 — dm/layout.tsx 주석 참고).
 */
test("새 DM 생성 응답을 기다리는 동안 다른 room으로 이미 이동했다면 응답이 그 이동을 덮지 않는다", async ({ page }) => {
  const candidates = [{ id: 4, nickname: "박서연" }];
  const hold = createHold();

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const headers = corsHeaders(request.headers().origin ?? "http://127.0.0.1:3002");
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: "application/json", headers, body: JSON.stringify(body) });

    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers });
    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      return json(apiResponse({ accessToken: "test-token", user: { id: 1, email: "a@test.dev", nickname: "나", avatarUrl: null } }));
    }
    if (url.pathname === "/api/v1/users/me/company-memberships") {
      return json(apiResponse({ companyMemberships: [{ companyId: 1, companyName: "Threadia", role: "MEMBER" }] }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/candidates") {
      return json(apiResponse({ candidates: candidates.map((c) => ({ id: c.id, nickname: c.nickname, avatarUrl: null })) }));
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      await hold.gate;
      return json(apiResponse({ room: room({ id: 999, otherUserId: 4, nickname: "박서연", unreadCount: 0 }) }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      return json(
        apiResponse({
          rooms: [
            room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 0 }),
            room({ id: 302, otherUserId: 3, nickname: "이현우", unreadCount: 0 }),
          ],
        })
      );
    }
    const messagesMatch = url.pathname.match(/^\/api\/v1\/direct-messages\/rooms\/(\d+)\/messages$/);
    if (messagesMatch && request.method() === "GET") return json(apiResponse({ messages: [] }));
    await route.continue();
  });

  try {
    await page.goto("/1/dm/301");
    await page.getByTitle("새 대화 시작").click();
    await page.getByRole("button", { name: "박서연" }).click(); // get-or-create 요청 시작(held)

    // 응답을 기다리는 동안 기존 room(이현우)으로 먼저 이동한다.
    await page.locator('a[title="이현우"]').click();
    await expect(page).toHaveURL(/\/1\/dm\/302$/);

    // toHaveURL은 조건이 이미 참이면 그 즉시 통과한다 — release 직후 곧바로
    // 검증하면 startDm의 뒤이은 router.push(비정상 케이스)가 아직 실행되기
    // 전이라 통과해버릴 수 있다. startDm이 get-or-create 다음에 부르는
    // invalidate 재조회 GET 응답까지 기다려서, router.push가 실행될 시점을
    // 이미 지나친 뒤에 최종 검증하게 만든다.
    const startDmLoadRoomsResponse = page.waitForResponse(
      (res) => res.request().method() === "GET" && res.url().includes("/direct-messages/rooms") && res.status() === 200
    );
    hold.release();
    await startDmLoadRoomsResponse;
  } finally {
    hold.release();
  }

  // 늦게 도착한 get-or-create 응답이 방금 이동한 302를 999로 덮어쓰면 안 된다.
  await expect(page).toHaveURL(/\/1\/dm\/302$/);
});
