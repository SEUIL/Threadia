/*
 * S09 1:1 DM smoke — WBS 4.7.3.2 (PR #173 리뷰 반영, pktewon).
 *
 * board.spec.ts와 같은 방식으로 /api/v1/** 를 route mock해서 백엔드 없이 돈다.
 * 리뷰에서 지적된 것들을 회귀 없이 커버한다:
 *  1. room 진입 → mark-read → 레일 unread 배지가 즉시 사라진다.
 *  2. 메시지 전송이 목록에 반영된다.
 *  3. "+" 로 새 대화를 시작하면 새 room으로 이동한다.
 *  4. markRoomRead 이후 지연 도착한 loadRooms 응답이 배지를 되돌리지 않는다(재리뷰 반영).
 *  5. mark-read가 실패하면 배지가 그대로 남는다(재리뷰 반영).
 *
 * 3차 재리뷰(pktewon) 반영 — 프론트에서 고칠 수 있는 항목만 커버한다(이슈 #208
 * 로 분리했던 항목은 별도 — 일반 MEMBER의 DM 상대 후보 조회, 정지된 사용자 필터링):
 *  6. mark-read 응답의 실제 unreadCount(0이 아닐 수 있음)가 배지에 그대로 반영된다.
 *  7. 현재 회사의 room 목록에 없는 roomId로 접근하면 에러를 보여주고 메시지를
 *     노출하지 않는다(다른 회사 room 뒤섞임 방지).
 *  8. S14 구성원 관리의 DM 버튼은 user.id를 room.id로 오인하지 않고
 *     get-or-create로 실제 room을 찾아 이동한다.
 *  9. room 목록 재조회가 실패해도 기존 레일(아이콘)은 유지된다.
 *
 * 이슈 #208 후속(PR #213, PR #173 이어서): openPicker()가 이제
 * `GET .../direct-messages/candidates`를 쓴다 — 이 테스트 파일의 mockApi()는
 * 이미 `role: "MEMBER"`로 로그인시키므로, "+ 버튼으로 새 대화를 시작하면" 테스트가
 * 일반 MEMBER도 후보 조회가 되는 것까지 같이 검증한다(COMPANY_ADMIN 전용
 * company-members endpoint를 더 이상 쓰지 않음).
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "dm-smoke" } });

const room = (over: { id: number; otherUserId: number; nickname: string; unreadCount: number }) => ({
  id: over.id,
  companyId: 1,
  otherUser: { id: over.otherUserId, email: `${over.nickname}@test.dev`, nickname: over.nickname, avatarUrl: null },
  unreadCount: over.unreadCount,
  createdAt: "2026-07-01T00:00:00Z",
});

async function mockApi(
  page: Page,
  options: {
    rooms: ReturnType<typeof room>[];
    messages?: Record<number, unknown[]>;
    candidates?: { id: number; userId: number; nickname: string; email: string }[];
    markReadShouldFail?: boolean;
    markReadUnreadCount?: number;
    // true가 되면 그 시점 이후의 GET rooms 호출부터 실패시킨다. mount 시점의
    // Strict Mode 이중 호출과 뒤섞이지 않도록, "몇 번째 호출인지" 대신 "새 room
    // 생성(POST) 이후"라는 이벤트를 기준으로 실패를 트리거한다.
    roomsFailAfterCreate?: boolean;
  }
) {
  const rooms = [...options.rooms];
  const messages: Record<number, unknown[]> = { ...(options.messages ?? {}) };
  const markReadCalls: number[] = [];
  const sentMessages: { roomId: number; content: string }[] = [];
  let roomsShouldFail = false;

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const requestOrigin = request.headers().origin ?? "http://127.0.0.1:3001";
    const corsHeaders = {
      "access-control-allow-origin": requestOrigin,
      "access-control-allow-methods": "GET,POST,PATCH,DELETE,OPTIONS",
      "access-control-allow-headers": "authorization,content-type",
      "access-control-allow-credentials": "true",
    };
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: "application/json", headers: corsHeaders, body: JSON.stringify(body) });

    if (request.method() === "OPTIONS") {
      await route.fulfill({ status: 204, headers: corsHeaders });
      return;
    }

    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      await json(
        apiResponse({
          accessToken: "test-token",
          user: { id: 1, email: "a@test.dev", nickname: "나", avatarUrl: null },
        })
      );
      return;
    }

    if (url.pathname === "/api/v1/users/me/company-memberships") {
      await json(apiResponse({ companyMemberships: [{ companyId: 1, companyName: "Threadia", role: "MEMBER" }] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      if (roomsShouldFail) {
        await json({ error: { code: "INTERNAL_ERROR", message: "목록을 불러오지 못했습니다." } }, 500);
        return;
      }
      await json(apiResponse({ rooms }));
      return;
    }

    if (request.method() === "POST" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      const body = request.postDataJSON() as { otherUserId: number };
      const candidate = (options.candidates ?? []).find((c) => c.userId === body.otherUserId);
      const newRoom = room({
        id: 999,
        otherUserId: body.otherUserId,
        nickname: candidate?.nickname ?? "새 상대",
        unreadCount: 0,
      });
      rooms.push(newRoom);
      messages[999] = [];
      if (options.roomsFailAfterCreate) roomsShouldFail = true;
      await json(apiResponse({ room: newRoom }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/candidates") {
      await json(
        apiResponse({
          candidates: (options.candidates ?? []).map((c) => ({
            id: c.userId,
            nickname: c.nickname,
            avatarUrl: null,
          })),
        })
      );
      return;
    }

    const messagesMatch = url.pathname.match(/^\/api\/v1\/direct-messages\/rooms\/(\d+)\/messages$/);
    if (messagesMatch) {
      const roomId = Number(messagesMatch[1]);
      if (request.method() === "GET") {
        await json(apiResponse({ messages: messages[roomId] ?? [] }));
        return;
      }
      if (request.method() === "POST") {
        const body = request.postDataJSON() as { content: string };
        sentMessages.push({ roomId, content: body.content });
        const newMessage = {
          id: 9000 + (messages[roomId]?.length ?? 0),
          roomId,
          sender: { id: 1, email: "a@test.dev", nickname: "나", avatarUrl: null },
          content: body.content,
          createdAt: "2026-07-20T00:00:00Z",
        };
        messages[roomId] = [...(messages[roomId] ?? []), newMessage];
        await json(apiResponse({ message: newMessage }));
        return;
      }
    }

    const markReadMatch = url.pathname.match(/^\/api\/v1\/direct-messages\/rooms\/(\d+)\/mark-read$/);
    if (markReadMatch && request.method() === "POST") {
      const roomId = Number(markReadMatch[1]);
      markReadCalls.push(roomId);
      if (options.markReadShouldFail) {
        await json({ error: { code: "INTERNAL_ERROR", message: "읽음 처리에 실패했습니다." } }, 500);
        return;
      }
      const unreadCount = options.markReadUnreadCount ?? 0;
      const target = rooms.find((r) => r.id === roomId);
      if (target) target.unreadCount = unreadCount;
      await json(apiResponse({ roomId, lastReadDirectMessageId: 9999, unreadCount, readAt: "2026-07-20T00:00:00Z" }));
      return;
    }

    await route.continue();
  });

  return { markReadCalls, sentMessages };
}

test("room에 입장하면 mark-read가 호출되고 레일 unread 배지가 사라진다", async ({ page }) => {
  await mockApi(page, {
    rooms: [
      room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 3 }),
      room({ id: 302, otherUserId: 3, nickname: "이현우", unreadCount: 0 }),
    ],
    messages: {
      301: [
        { id: 1, roomId: 301, sender: { id: 2, email: "b@test.dev", nickname: "김민정", avatarUrl: null }, content: "안녕하세요", createdAt: "2026-07-19T00:00:00Z" },
      ],
    },
  });

  await page.goto("/1/dm/301");

  await expect(page.getByRole("heading", { name: "김민정" })).toBeVisible();
  await expect(page.getByText("안녕하세요")).toBeVisible();

  // mark-read 성공 후 레일의 unread dot(김민정 아바타 옆 배지)이 사라져야 한다.
  const railLink = page.locator('a[title="김민정"]');
  await expect(railLink.locator("span.bg-destructive")).toHaveCount(0);
});

test("메시지를 전송하면 목록에 즉시 반영된다", async ({ page }) => {
  await mockApi(page, {
    rooms: [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 0 })],
    messages: { 301: [] },
  });

  await page.goto("/1/dm/301");
  await expect(page.getByText("아직 메시지가 없습니다.")).toBeVisible();

  await page.getByLabel("메시지 입력").fill("테스트 메시지입니다");
  await page.getByRole("button", { name: "전송" }).click();

  await expect(page.getByText("테스트 메시지입니다")).toBeVisible();
  await expect(page.getByLabel("메시지 입력")).toHaveValue("");
});

test("+ 버튼으로 새 대화를 시작하면 새 room으로 이동한다", async ({ page }) => {
  await mockApi(page, {
    rooms: [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 0 })],
    messages: { 301: [] },
    candidates: [{ id: 50, userId: 4, nickname: "박서연", email: "c@test.dev" }],
  });

  await page.goto("/1/dm/301");

  await page.getByTitle("새 대화 시작").click();
  await page.getByRole("button", { name: "박서연" }).click();

  await expect(page).toHaveURL(/\/1\/dm\/999$/);
  await expect(page.getByRole("heading", { name: "박서연" })).toBeVisible();
});

test("mark-read가 실패하면 unread 배지가 그대로 남는다", async ({ page }) => {
  await mockApi(page, {
    rooms: [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 3 })],
    messages: {
      301: [
        { id: 1, roomId: 301, sender: { id: 2, email: "b@test.dev", nickname: "김민정", avatarUrl: null }, content: "안녕하세요", createdAt: "2026-07-19T00:00:00Z" },
      ],
    },
    markReadShouldFail: true,
  });

  await page.goto("/1/dm/301");
  await expect(page.getByRole("heading", { name: "김민정" })).toBeVisible();

  // mark-read가 실패했으므로 markRoomRead()가 호출되지 않고 배지가 그대로 남아야 한다.
  const railLink = page.locator('a[title="김민정"]');
  await expect(railLink.locator("span.bg-destructive")).toBeVisible();
});

/*
 * markRoomRead 이후 지연 도착한 loadRooms 응답 시나리오 — 2차 재리뷰(pktewon) 반영.
 *
 * 1차 fix(generation 불일치 시 응답 전체 폐기)는 dev의 React Strict Mode가
 * mount effect를 두 번 실행해 loadRooms()가 두 번 호출되는 상황에서만 우연히
 * 통과했다 — 먼저 끝난 "여벌" 호출이 폐기된 응답의 빈자리를 채워줬을 뿐이다.
 * production처럼 초기 loadRooms() 호출이 단 하나뿐이고 그게 지연되면, 응답을
 * 통째로 버리는 순간 rail 자체가 텅 비어버리는 회귀가 있었다(room 링크 자체가
 * 사라짐 — 배지만 사라진 게 아니라). 지금 fix는 응답을 폐기하지 않고 room 단위로
 * merge하므로 이 문제가 구조적으로 없다.
 *
 * ⚠️ 정직한 한계: `HOLD_CALL_INDEX`를 1로 잡아도 이 dev 서버 환경에서는 Strict
 * Mode가 항상 두 번째 호출을 "여벌"로 만들어주기 때문에, 실측 결과 1차 fix
 * (응답 전체 폐기)로 되돌려도 이 테스트는 여전히 통과했다 — 즉 이 두 테스트가
 * "production에 단 하나의 호출만 있는 상황"을 강제로 재현하지는 못한다(이 dev
 * 환경에서 그걸 결정론적으로 만들 방법을 못 찾았다). 대신 이 두 테스트는 "어느
 * 순번의 초기 호출이 지연되든 merge 로직이 해당 room의 읽음 상태를 지키면서
 * 목록 데이터 자체는 항상 반영한다"는 걸 확인한다 — merge 방식은 설계상 응답을
 * 절대 통째로 버리지 않으므로(개별 room의 unreadCount만 보정) 호출이 몇 개든,
 * 어떤 순서로 도착하든 같은 보장이 성립한다.
 */
async function runStaleRoomsRaceScenario(page: Page, holdCallIndex: number) {
  const rooms = [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 3 })];
  const messages: Record<number, unknown[]> = {
    301: [{ id: 1, roomId: 301, sender: { id: 2, email: "b@test.dev", nickname: "김민정", avatarUrl: null }, content: "안녕하세요", createdAt: "2026-07-19T00:00:00Z" }],
  };

  let roomsCallIndex = 0;
  let staleCallHeld = false;
  let releaseStaleRooms: () => void = () => {};
  const staleGate = new Promise<void>((resolve) => {
    releaseStaleRooms = resolve;
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const requestOrigin = request.headers().origin ?? "http://127.0.0.1:3001";
    const corsHeaders = {
      "access-control-allow-origin": requestOrigin,
      "access-control-allow-methods": "GET,POST,PATCH,DELETE,OPTIONS",
      "access-control-allow-headers": "authorization,content-type",
      "access-control-allow-credentials": "true",
    };
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: "application/json", headers: corsHeaders, body: JSON.stringify(body) });

    if (request.method() === "OPTIONS") {
      await route.fulfill({ status: 204, headers: corsHeaders });
      return;
    }

    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      await json(apiResponse({ accessToken: "test-token", user: { id: 1, email: "a@test.dev", nickname: "나", avatarUrl: null } }));
      return;
    }

    if (url.pathname === "/api/v1/users/me/company-memberships") {
      await json(apiResponse({ companyMemberships: [{ companyId: 1, companyName: "Threadia", role: "MEMBER" }] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      roomsCallIndex += 1;
      // 응답 시점이 아니라 요청 도착 시점의 스냅샷을 캡처한다 — "옛 데이터가
      // 늦게 도착"하는 상황을 재현하려면 mark-read로 뮤테이션되기 전 상태를
      // 붙잡아둬야 한다.
      const snapshot = rooms.map((r) => ({ ...r }));
      if (roomsCallIndex === holdCallIndex) {
        staleCallHeld = true;
        await staleGate;
      }
      await json(apiResponse({ rooms: snapshot }));
      return;
    }

    const messagesMatch = url.pathname.match(/^\/api\/v1\/direct-messages\/rooms\/(\d+)\/messages$/);
    if (messagesMatch && request.method() === "GET") {
      const roomId = Number(messagesMatch[1]);
      await json(apiResponse({ messages: messages[roomId] ?? [] }));
      return;
    }

    const markReadMatch = url.pathname.match(/^\/api\/v1\/direct-messages\/rooms\/(\d+)\/mark-read$/);
    if (markReadMatch && request.method() === "POST") {
      const roomId = Number(markReadMatch[1]);
      const target = rooms.find((r) => r.id === roomId);
      if (target) target.unreadCount = 0;
      await json(apiResponse({ roomId, lastReadDirectMessageId: 9999, unreadCount: 0, readAt: "2026-07-20T00:00:00Z" }));
      return;
    }

    await route.continue();
  });

  await page.goto("/1/dm/301");
  await expect(page.getByRole("heading", { name: "김민정" })).toBeVisible();
  await expect.poll(() => staleCallHeld).toBe(true);

  const railLink = page.locator('a[title="김민정"]');
  const railBadge = railLink.locator("span.bg-destructive");

  // 지연된 응답이 아직 안 왔어도(단, holdCallIndex가 유일한 초기 호출인 경우엔
  // 이 시점엔 rail이 비어 있을 수 있다 — 그건 정상이다). 응답을 풀어준 뒤가
  // 진짜 검증 지점이다.
  releaseStaleRooms();
  await page.waitForTimeout(300);

  // room 링크 자체가 사라지면 안 되고(1차 fix의 회귀 지점), unread 배지만
  // 제거된 상태여야 한다.
  await expect(railLink).toHaveCount(1);
  await expect(railBadge).toHaveCount(0);
}

test("markRoomRead 이후 지연 도착한 두 번째 초기 loadRooms 응답이 room 링크를 유지한 채 배지만 제거한다", async ({ page }) => {
  await runStaleRoomsRaceScenario(page, 2);
});

test("markRoomRead 이후 지연 도착한 첫 번째 초기 loadRooms 응답이 room 링크를 유지한 채 배지만 제거한다", async ({ page }) => {
  await runStaleRoomsRaceScenario(page, 1);
});

/*
 * 3차 재리뷰(pktewon) 반영 — blocker #6: markRoomRead가 unreadCount를 항상
 * 0으로 강제했었다. mark-read 응답이 실제로 돌려주는 unreadCount(방 입장과
 * 동시에 새 메시지가 온 경우 등)를 무시하지 않고 배지에 그대로 반영해야 한다.
 */
test("mark-read 응답의 실제 unreadCount가 0이 아니면 레일 배지가 그대로 남는다", async ({ page }) => {
  await mockApi(page, {
    rooms: [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 3 })],
    messages: {
      301: [
        { id: 1, roomId: 301, sender: { id: 2, email: "b@test.dev", nickname: "김민정", avatarUrl: null }, content: "안녕하세요", createdAt: "2026-07-19T00:00:00Z" },
      ],
    },
    markReadUnreadCount: 1,
  });

  await page.goto("/1/dm/301");
  await expect(page.getByRole("heading", { name: "김민정" })).toBeVisible();

  // markRead 자체는 성공했지만 서버가 unreadCount: 1을 돌려줬으므로(동시 도착한
  // 새 메시지 등) 배지를 0으로 강제해선 안 된다.
  const railLink = page.locator('a[title="김민정"]');
  await expect(railLink.locator("span.bg-destructive")).toBeVisible();
});

/*
 * 3차 재리뷰(pktewon) 반영 — blocker #5: room 목록(companyId로 스코프된) API를
 * roomId만으로 호출하는 messages/mark-read가 companyId를 검증하지 않아, 다른
 * 회사 room이거나 존재하지 않는 room이어도 그대로 렌더링됐다. 지금은 room 목록에
 * 없는 roomId면 에러로 처리하고 메시지를 노출하지 않아야 한다.
 */
test("현재 회사의 room 목록에 없는 roomId로 접근하면 에러를 보여주고 메시지를 노출하지 않는다", async ({ page }) => {
  await mockApi(page, {
    rooms: [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 0 })],
    messages: {
      999: [
        { id: 1, roomId: 999, sender: { id: 5, email: "x@test.dev", nickname: "다른회사사람", avatarUrl: null }, content: "이건 다른 회사 메시지", createdAt: "2026-07-19T00:00:00Z" },
      ],
    },
  });

  await page.goto("/1/dm/999");

  await expect(page.getByText("존재하지 않거나 접근할 수 없는 대화방입니다.")).toBeVisible();
  await expect(page.getByText("이건 다른 회사 메시지")).toHaveCount(0);
});

/*
 * 3차 재리뷰(pktewon) 반영 — blocker #7: loadRooms()가 실패하면 무조건 rail을
 * 비웠다(setRooms([])). 이미 정상 로드된 rail이 있는데 재조회(예: "+"로 새 대화
 * 시작 후 목록 새로고침)가 실패했다고 기존 아이콘까지 사라지면 안 된다.
 */
test("room 목록 재조회가 실패해도 기존 레일 아이콘은 사라지지 않는다", async ({ page }) => {
  await mockApi(page, {
    rooms: [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 0 })],
    messages: { 301: [] },
    candidates: [{ id: 50, userId: 4, nickname: "박서연", email: "c@test.dev" }],
    roomsFailAfterCreate: true,
  });

  await page.goto("/1/dm/301");
  const existingRailLink = page.locator('a[title="김민정"]');
  await expect(existingRailLink).toHaveCount(1);

  await page.getByTitle("새 대화 시작").click();
  await page.getByRole("button", { name: "박서연" }).click();

  // 재조회가 실패해도 기존 room 아이콘은 그대로 남아 있어야 한다.
  await expect(existingRailLink).toHaveCount(1);
});

/*
 * 3차 재리뷰(pktewon) 반영 — blocker #3: S14 구성원 관리의 DM 버튼이
 * `/${companyId}/dm/${member.user.id}`로 바로 링크해서 user.id를 room.id로
 * 오인했다(우연히 같은 숫자대일 때만 동작). 이제 get-or-create API로 실제
 * room을 찾은 뒤 그 room.id로 이동해야 한다.
 */
test("구성원 관리 화면의 DM 버튼은 get-or-create로 실제 room을 찾아 이동한다", async ({ page }) => {
  const corsHeaders = {
    "access-control-allow-origin": "http://127.0.0.1:3001",
    "access-control-allow-methods": "GET,POST,PATCH,DELETE,OPTIONS",
    "access-control-allow-headers": "authorization,content-type",
    "access-control-allow-credentials": "true",
  };
  const json = (route: Parameters<Parameters<Page["route"]>[1]>[0], body: unknown, status = 200) =>
    route.fulfill({
      status,
      contentType: "application/json",
      headers: { ...corsHeaders, "access-control-allow-origin": route.request().headers().origin ?? corsHeaders["access-control-allow-origin"] },
      body: JSON.stringify(body),
    });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());

    if (request.method() === "OPTIONS") {
      await route.fulfill({ status: 204, headers: corsHeaders });
      return;
    }

    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      await json(route, apiResponse({ accessToken: "test-token", user: { id: 1, email: "a@test.dev", nickname: "나", avatarUrl: null } }));
      return;
    }

    if (url.pathname === "/api/v1/users/me/company-memberships") {
      await json(route, apiResponse({ companyMemberships: [{ companyId: 1, companyName: "Threadia", role: "COMPANY_ADMIN" }] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/company-members") {
      await json(
        route,
        apiResponse({
          companyMembers: [
            {
              id: 10,
              role: "MEMBER",
              status: "ACTIVE",
              departmentId: null,
              user: { id: 4, email: "peer@test.dev", nickname: "박서연", avatarUrl: null },
            },
          ],
        })
      );
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/departments") {
      await json(route, apiResponse({ departments: [] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/invitations") {
      await json(route, apiResponse({ invitations: [] }));
      return;
    }

    if (request.method() === "POST" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      const body = request.postDataJSON() as { otherUserId: number };
      await json(
        route,
        apiResponse({
          room: room({ id: 777, otherUserId: body.otherUserId, nickname: "박서연", unreadCount: 0 }),
        })
      );
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      await json(route, apiResponse({ rooms: [] }));
      return;
    }

    const messagesMatch = url.pathname.match(/^\/api\/v1\/direct-messages\/rooms\/(\d+)\/messages$/);
    if (messagesMatch && request.method() === "GET") {
      await json(route, apiResponse({ messages: [] }));
      return;
    }

    await route.continue();
  });

  await page.goto("/1/members");
  await expect(page.getByRole("button", { name: "DM" })).toBeVisible();

  await page.getByRole("button", { name: "DM" }).click();

  await expect(page).toHaveURL(/\/1\/dm\/777$/);
});
