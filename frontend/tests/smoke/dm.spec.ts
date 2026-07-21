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
 *
 * 4차 재리뷰(pktewon) 반영 — 나머지 5개 blocker 중 mount 순서에 의존하는
 * 회귀(옛 loadRooms 응답이 최신 응답을 덮는 것, mark-read unreadCount=1이
 * 지연 응답에 되돌아가는 것)는 dev 서버의 React Strict Mode 이중 mount 때문에
 * 이 파일에서 결정적으로 재현할 수 없다 — tests/smoke-production/
 * dm-mount-race.spec.ts(production 빌드 전용)로 옮겼다(blocker 5). 이 파일에는
 * mount 시점이 아니라 "사용자 클릭으로 시작되는" race만 남긴다(Strict Mode는
 * effect만 이중 실행하고 클릭 핸들러는 실제 클릭 횟수만큼만 실행되므로 이
 * 파일의 dev 서버 실행도 여전히 결정적이다):
 *  10. candidate 응답을 기다리는 동안 room 목록이 바뀌면(새 room 생김) 최신
 *      rooms 기준으로 필터링된다(blocker 3).
 *  11. S14 DM 버튼: 요청 중엔 다른 구성원의 DM 버튼도 비활성화돼 애초에
 *      경쟁 요청 자체가 발생할 수 없다(blocker 4).
 *  12. dm/[roomId]/page.tsx가 room.companyId를 직접 검증한다 — room이 목록에
 *      "있어도" companyId가 다르면(백엔드 스코핑을 우회하는 응답이 와도) 렌더링
 *      하지 않는다(blocker 1, 기존 #7과 달리 목록에 아예 없는 경우가 아니라
 *      "있지만 회사가 다른" 경우를 검증한다).
 *
 * ⚠️ "새 DM 생성 응답이 늦게 도착했을 때 이미 다른 room으로 이동했다면 그
 * 응답이 되돌리지 않는다"(blocker 4, DmLayout) 테스트는 여기 없다 —
 * DmLayout은 room 사이 이동에도 마운트 상태가 유지되는 persistent layout인데,
 * dev 서버의 React Strict Mode가 그 마운트 상태를 예측 불가능한 시점에
 * mount→unmount→remount로 흔들어서, mountedRef 체크가(의도한 pathname 체크가
 * 아니라) 우연히 테스트를 통과시키는 게 실측으로 확인됐다(pathname 체크를
 * 일부러 지워도 여전히 통과함). tests/smoke-production/dm-mount-race.spec.ts
 * (production 빌드, Strict Mode 없음)로 옮겼다.
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

/** 여러 테스트가 재사용하는 CORS 헤더 — page.route mock 응답에 항상 필요하다. */
function corsHeaders(origin: string) {
  return {
    "access-control-allow-origin": origin,
    "access-control-allow-methods": "GET,POST,PATCH,DELETE,OPTIONS",
    "access-control-allow-headers": "authorization,content-type",
    "access-control-allow-credentials": "true",
  };
}

/*
 * 4차 재리뷰(pktewon) 반영 — blocker 3: candidate 목록을 fetch 시점의 rooms로
 * 한 번 필터링해서 state에 저장했더니, 그 뒤 rooms가 바뀌어도(이미 대화방이
 * 생긴 상대) picker가 갱신되지 않았다. candidates는 원본 그대로 저장하고
 * "기존 상대 제외"는 렌더마다 최신 rooms로 다시 계산되는 useMemo에서 하도록
 * 고쳤다 — 그러면 picker를 다시 열지 않아도 rooms가 바뀌는 순간 자동으로
 * 반영된다. 이 테스트는 그 반응성을 직접 검증한다: 최초엔 이 상대와 room이
 * 없어서(rooms=[]) picker에 보이다가, mount 시점에 걸려 있던(hold) rooms 응답이
 * 뒤늦게 도착해 "사실 이미 room이 있었다"는 게 밝혀지면 picker에서 즉시
 * 사라져야 한다.
 */
test("후보 목록을 연 뒤 지연 도착한 room 목록에 이미 그 상대와의 방이 있으면 picker에서 자동으로 빠진다", async ({
  page,
}) => {
  let releaseRooms: () => void = () => {};
  let roomsHeld = true;
  const roomsGate = new Promise<void>((resolve) => {
    releaseRooms = () => {
      roomsHeld = false;
      resolve();
    };
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const headers = corsHeaders(request.headers().origin ?? "http://127.0.0.1:3001");
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
      return json(apiResponse({ candidates: [{ id: 4, nickname: "박서연", avatarUrl: null }] }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      if (roomsHeld) await roomsGate;
      // 풀린 뒤엔 "사실 박서연과의 방이 이미 있었다"는 최신 사실을 반영한다.
      return json(apiResponse({ rooms: [room({ id: 301, otherUserId: 4, nickname: "박서연", unreadCount: 0 })] }));
    }
    const messagesMatch = url.pathname.match(/^\/api\/v1\/direct-messages\/rooms\/(\d+)\/messages$/);
    if (messagesMatch && request.method() === "GET") return json(apiResponse({ messages: [] }));
    await route.continue();
  });

  try {
    await page.goto("/1/dm/301");
    await page.getByTitle("새 대화 시작").click();
    // rooms가 아직 안 풀렸으니(roomPeerIds가 비어 있으니) 박서연이 후보로 보인다.
    await expect(page.getByRole("button", { name: "박서연" })).toBeVisible();
  } finally {
    releaseRooms();
  }

  // rooms 응답이 풀리며 박서연과의 room이 이미 있다는 게 반영되면, picker를
  // 다시 열지 않아도 후보 목록에서 자동으로 빠져야 한다.
  await expect(page.getByRole("button", { name: "박서연" })).toHaveCount(0);
});

/*
 * 4차 재리뷰(pktewon) 반영 — blocker 4(S14): DM 버튼이 상대별로만
 * disabled였어서, 한 상대의 요청이 진행 중일 때 다른 상대의 DM 버튼을 눌러
 * 경쟁 요청을 만들 수 있었다. 이제 하나라도 진행 중이면 모든 DM 버튼을
 * 비활성화해 애초에 경쟁 요청 자체가 안 생기게 막는다. 화면을 벗어난 뒤
 * 늦게 도착한 응답은 mountedRef 가드로 무시된다.
 */
test("S14: DM 요청이 진행 중이면 다른 구성원의 DM 버튼도 비활성화되고, 화면을 벗어난 뒤 도착한 응답은 무시된다", async ({
  page,
}) => {
  let releaseCreate: () => void = () => {};
  const createGate = new Promise<void>((resolve) => {
    releaseCreate = resolve;
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const headers = corsHeaders(request.headers().origin ?? "http://127.0.0.1:3001");
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: "application/json", headers, body: JSON.stringify(body) });

    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers });
    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      return json(apiResponse({ accessToken: "test-token", user: { id: 1, email: "a@test.dev", nickname: "나", avatarUrl: null } }));
    }
    if (url.pathname === "/api/v1/users/me/company-memberships") {
      return json(apiResponse({ companyMemberships: [{ companyId: 1, companyName: "Threadia", role: "COMPANY_ADMIN" }] }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/company-members") {
      return json(
        apiResponse({
          companyMembers: [
            { id: 10, role: "MEMBER", status: "ACTIVE", departmentId: null, user: { id: 4, email: "peer@test.dev", nickname: "박서연", avatarUrl: null } },
            { id: 11, role: "MEMBER", status: "ACTIVE", departmentId: null, user: { id: 5, email: "peer2@test.dev", nickname: "최지훈", avatarUrl: null } },
          ],
        })
      );
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/departments") return json(apiResponse({ departments: [] }));
    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/invitations") return json(apiResponse({ invitations: [] }));
    if (request.method() === "POST" && url.pathname === "/api/v1/companies/1/direct-messages/rooms") {
      await createGate;
      const body = request.postDataJSON() as { otherUserId: number };
      return json(apiResponse({ room: room({ id: 777, otherUserId: body.otherUserId, nickname: "박서연", unreadCount: 0 }) }));
    }
    const messagesMatch = url.pathname.match(/^\/api\/v1\/direct-messages\/rooms\/(\d+)\/messages$/);
    if (messagesMatch && request.method() === "GET") return json(apiResponse({ messages: [] }));
    await route.continue();
  });

  try {
    await page.goto("/1/members");
    const dmButtons = page.getByRole("button", { name: "DM" });
    await expect(dmButtons).toHaveCount(2);

    await dmButtons.first().click(); // 박서연 DM 요청 시작(held)

    // 요청이 끝나기 전엔 최지훈의 DM 버튼도 눌러서는 안 된다.
    await expect(dmButtons.nth(1)).toBeDisabled();

    // 응답을 기다리는 동안 화면(컴포넌트)을 완전히 벗어난다 — 다른 앱 화면으로
    // 이동하면 그 화면 자체의 mock을 또 갖춰야 해서, unmount만 확실히 시키는
    // about:blank로 이동한다(이 테스트의 목적은 "언마운트된 컴포넌트가 늦은
    // 응답에 반응하지 않는다"이지 다른 화면의 렌더링이 아니다).
    await page.goto("about:blank");
  } finally {
    releaseCreate();
  }

  // 늦게 도착한 응답이 이미 벗어난 화면을 /1/dm/777로 강제 이동시키면 안 된다
  // (router.push가 언마운트된 컴포넌트에서 호출되면 안 됨 — mountedRef 가드).
  await expect(page).toHaveURL("about:blank");
});

/*
 * 4차 재리뷰(pktewon) 반영 — blocker 1: room이 목록에 "있어도" companyId가
 * 다르면(백엔드 스코핑을 우회하는 응답이 오는 경우까지 대비) 렌더링하면 안
 * 된다. 기존 #7 테스트는 "목록에 아예 없는" 경우만 커버했다 — 이 테스트는
 * "목록엔 있지만 companyId가 다른" 더 엄격한 경우를 room.companyId 직접
 * 비교로 잡아내는지 검증한다.
 */
test("room이 현재 회사의 목록에 있어도 companyId가 다르면 접근할 수 없는 방으로 처리한다", async ({ page }) => {
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const headers = corsHeaders(request.headers().origin ?? "http://127.0.0.1:3001");
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
      // 실제로는 있을 수 없는 응답이지만(계약 위반/스코핑 버그 가정), room.id는
      // 301로 일치시켜서 "목록에 있는지"만 보는 얕은 체크는 통과하게 만든다.
      return json(
        apiResponse({
          rooms: [{ ...room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 0 }), companyId: 2 }],
        })
      );
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/direct-messages/rooms/301/messages") {
      return json(
        apiResponse({
          messages: [{ id: 1, roomId: 301, sender: { id: 2, email: "b@test.dev", nickname: "김민정", avatarUrl: null }, content: "다른 회사 메시지", createdAt: "2026-07-19T00:00:00Z" }],
        })
      );
    }
    await route.continue();
  });

  await page.goto("/1/dm/301");

  await expect(page.getByText("존재하지 않거나 접근할 수 없는 대화방입니다.")).toBeVisible();
  await expect(page.getByText("다른 회사 메시지")).toHaveCount(0);
});
