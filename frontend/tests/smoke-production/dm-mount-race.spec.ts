/*
 * S09/S14 DM — production 빌드에서만 결정적으로 재현되는 mount race 회귀.
 * PR #173 4차 재리뷰(pktewon) blocker 5 반영.
 *
 * dev 서버(Strict Mode)는 mount effect를 두 번 실행해 "몇 번째 GET 요청을
 * 붙잡을지"가 실행마다 달라진다 — 이전 버전의 회귀 테스트(dm.spec.ts에 있던
 * runStaleRoomsRaceScenario)는 이 불확실성을 `HOLD_CALL_INDEX` 파라미터로
 * 우회했는데, 그 자체가 production에서는 안 맞아서 타임아웃으로 실패했다.
 *
 * 이 파일은 production 빌드(Strict Mode 없음, mount당 effect 정확히 1번)로만
 * 돈다(playwright.production.config.ts). production에서는 다음이 항상
 * 결정적이다 — React가 같은 커밋에서 자식 effect를 부모보다 먼저 실행하는
 * 규칙(공식 문서화된 동작, 타이밍 우연이 아님) 때문에:
 *   1번째 GET .../direct-messages/rooms = DmRoomPage(children, 자식)의
 *     자체 조회 — 메시지 렌더/mark-read를 위해 필요하니 절대 붙잡으면 안 된다.
 *   2번째 GET .../direct-messages/rooms = DmLayout(부모)의 rail 조회 — 이게
 *     이 파일이 실제로 붙잡는 호출이다.
 * 이후 사용자 액션(예: startDm)으로 발생하는 추가 호출은 세 번째 이후로
 * 순서가 고정된다.
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

/** GET .../rooms의 N번째 호출을 external release() 전까지 붙잡아두는 게이트. */
function createHold() {
  let release: () => void = () => {};
  const gate = new Promise<void>((resolve) => {
    release = resolve;
  });
  return { gate, release };
}

test("2번째(rail) rooms 응답이 mark-read의 unreadCount=1보다 늦게 도착해도 배지가 0으로 되돌아가지 않는다", async ({
  page,
}) => {
  const rooms = [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 3 })];
  const staleSnapshot = rooms.map((r) => ({ ...r })); // mark-read로 뮤테이션되기 전 상태를 미리 붙잡아둔다
  const messages = [
    { id: 1, roomId: 301, sender: { id: 2, email: "b@test.dev", nickname: "김민정", avatarUrl: null }, content: "안녕하세요", createdAt: "2026-07-19T00:00:00Z" },
  ];

  let roomsCallCount = 0;
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
      roomsCallCount += 1;
      if (roomsCallCount === 2) {
        await hold.gate;
        return json(apiResponse({ rooms: staleSnapshot }));
      }
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
    // waitForResponse는 리스너를 등록한 "이후" 발생하는 응답만 잡는다 — goto
    // 뒤에 걸면 production처럼 마운트~mark-read가 아주 빨리 끝나는 환경에서는
    // 응답이 이미 지나가버린 뒤라 영원히 대기하게 된다. navigate 전에 먼저
    // Promise를 만들어두고 나중에 await한다.
    const markReadResponse = page.waitForResponse((res) => res.url().includes("/mark-read") && res.status() === 200);
    await page.goto("/1/dm/301");
    await expect(page.getByRole("heading", { name: "김민정" })).toBeVisible();
    // mark-read가 실제로 끝난 뒤(unreadCount=1 응답 수신 후)에 지연 응답을
    // 풀어야 "그 이후에 도착한 옛 응답" 시나리오가 된다 — waitForResponse는
    // 고정 sleep이 아니라 실제 네트워크 이벤트를 기다리는 web-first 대기다.
    await markReadResponse;
  } finally {
    hold.release();
  }

  // 지연 응답(unreadCount=3, mark-read 이전 스냅샷)이 도착해도 mark-read가
  // 확정한 unreadCount=1(0이 아님)이 유지돼야 한다 — 예전 버그는 이 경우
  // 배지를 무조건 0으로 되돌려서 배지가 사라졌다.
  const railBadge = page.locator('a[title="김민정"] span.bg-destructive');
  await expect(railBadge).toBeVisible();
});

test("오래된(2번째) rooms 응답이 새로 만든 room을 포함한 최신(3번째) 응답을 덮지 않는다", async ({ page }) => {
  const rooms = [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 0 })];
  const staleSnapshot = rooms.map((r) => ({ ...r })); // room 999가 생기기 전 스냅샷
  const candidates = [{ id: 50, userId: 4, nickname: "박서연", email: "c@test.dev" }];

  let roomsCallCount = 0;
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
      return json(apiResponse({ candidates: candidates.map((c) => ({ id: c.userId, nickname: c.nickname, avatarUrl: null })) }));
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      const body = request.postDataJSON() as { otherUserId: number };
      const candidate = candidates.find((c) => c.userId === body.otherUserId);
      const newRoom = room({ id: 999, otherUserId: body.otherUserId, nickname: candidate?.nickname ?? "새 상대", unreadCount: 0 });
      rooms.push(newRoom);
      return json(apiResponse({ room: newRoom }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      roomsCallCount += 1;
      if (roomsCallCount === 2) {
        await hold.gate;
        return json(apiResponse({ rooms: staleSnapshot }));
      }
      // 3번째 이후(=startDm이 room 생성 후 부르는 loadRooms) 호출 시점엔
      // rooms 배열에 이미 999가 들어있는 최신 상태다.
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
    // 2번째(rail) 호출이 held 상태로 들어올 때까지 기다린다 — 이 시점 이후에
    // startDm을 실행해야 "옛 응답이 붙잡혀 있는 동안 새 room이 생겼다"가 된다.
    await expect.poll(() => roomsCallCount).toBeGreaterThanOrEqual(2);

    await page.getByTitle("새 대화 시작").click();
    await page.getByRole("button", { name: "박서연" }).click();
    // startDm의 loadRooms()(3번째 호출, [301,999])가 반영될 때까지 기다린다.
    await expect(page.locator('a[title="박서연"]')).toHaveCount(1);
  } finally {
    hold.release();
  }

  // 지연됐던 2번째 응답([301]만 있는 옛 스냅샷)이 뒤늦게 풀려도, 이미 반영된
  // 3번째 응답([301,999])을 덮어써서 301만 남기면 안 된다 — 둘 다 남아있어야
  // 한다.
  await expect(page.locator('a[title="김민정"]')).toHaveCount(1);
  await expect(page.locator('a[title="박서연"]')).toHaveCount(1);
});

/*
 * 4차 재리뷰(pktewon) 반영 — blocker 4(DmLayout): 새 DM 생성(get-or-create)
 * 응답을 기다리는 동안 사용자가 기존 room 링크를 눌러 이미 다른 곳으로
 * 이동했다면, 늦게 도착한 응답이 그 이동을 덮어쓰고 새로 만든 room으로
 * 강제 이동시키면 안 된다. mountedRef만으로는 "언마운트 안 됐지만 다른
 * room으로 이동한" 이 경우를 못 잡아서, 요청 시작 시점의 pathname을 저장해두고
 * 응답 시점에 비교하도록 고쳤다.
 *
 * dm.spec.ts(dev 서버)에 처음 이 테스트를 만들었을 때 dev 서버가 시간에 따라
 * 들쭉날쭉해서 production으로 옮겼다. 디버그 로그로 실측해보니, 이 앱은 room
 * 사이 이동(/dm/301 → /dm/302)에도 DmLayout이 그때그때 재마운트돼(dev/production
 * 둘 다 동일) mountedRef 체크만으로 이미 이 시나리오가 걸러진다 — pathname
 * 체크(blocker 4가 요청한 방식)는 지금은 중복 방어이지만, 라우팅 동작이 바뀌어
 * DmLayout이 진짜 persistent해지는 경우까지 대비해 소스에 그대로 남겨뒀다
 * (dm/layout.tsx 주석 참고). 이 테스트는 "늦게 도착한 응답이 이미 이동한 화면을
 * 덮지 않는다"는 최종 동작 자체를 검증한다 — 그 동작을 지금 실제로 지켜주는 게
 * mountedRef든 pathname이든, 회귀 감지라는 목적은 동일하게 달성된다.
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
    // loadRooms() GET 응답까지 기다려서, router.push가 실행될 시점을 이미
    // 지나친 뒤에 최종 검증하게 만든다.
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
