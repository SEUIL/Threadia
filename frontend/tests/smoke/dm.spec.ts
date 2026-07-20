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
  }
) {
  const rooms = [...options.rooms];
  const messages: Record<number, unknown[]> = { ...(options.messages ?? {}) };
  const markReadCalls: number[] = [];
  const sentMessages: { roomId: number; content: string }[] = [];

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
      await json(apiResponse({ room: newRoom }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/company-members") {
      await json(
        apiResponse({
          companyMembers: (options.candidates ?? []).map((c) => ({
            id: c.id,
            role: "MEMBER",
            status: "ACTIVE",
            departmentId: null,
            user: { id: c.userId, email: c.email, nickname: c.nickname, avatarUrl: null },
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
      const target = rooms.find((r) => r.id === roomId);
      if (target) target.unreadCount = 0;
      await json(apiResponse({ roomId, lastReadDirectMessageId: 9999, unreadCount: 0, readAt: "2026-07-20T00:00:00Z" }));
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

test("markRoomRead 이후 지연 도착한 loadRooms 응답이 unread 배지를 되돌리지 않는다", async ({ page }) => {
  /*
   * 재현 순서(재리뷰 지적, pktewon): dev 서버는 React Strict Mode라 DmLayout의
   * mount effect가 두 번 실행되어 loadRooms()가 초기 마운트에서 두 번 호출된다.
   *  1. 두 loadRooms() 호출 중 하나(두 번째 GET rooms 요청)를 응답 직전에 붙잡아둔다.
   *  2. 그사이 room 301(unread 3) 페이지의 자체 조회 + mark-read가 먼저 끝나
   *     markRoomRead(301)로 레일 배지가 지워진다.
   *  3. 붙잡아뒀던 응답(옛 unreadCount 3 스냅샷)을 그제서야 흘려보낸다.
   *  4. generation이 이미 지나갔으므로 이 응답은 버려져야 하고, 배지는 계속
   *     지워진 상태로 남아야 한다(고치기 전에는 3으로 되돌아갔다 — 실측 확인).
   */
  const rooms = [room({ id: 301, otherUserId: 2, nickname: "김민정", unreadCount: 3 })];
  const messages: Record<number, unknown[]> = {
    301: [{ id: 1, roomId: 301, sender: { id: 2, email: "b@test.dev", nickname: "김민정", avatarUrl: null }, content: "안녕하세요", createdAt: "2026-07-19T00:00:00Z" }],
  };

  // 실측(HOLD_IDX 1~4로 스캔) 결과 dev의 Strict Mode 이중 mount에서 DmLayout의
  // loadRooms()가 두 번째로 도착하는 GET rooms 요청이 이 race를 재현한다.
  const HOLD_CALL_INDEX = 2;
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
      if (roomsCallIndex === HOLD_CALL_INDEX) {
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

  // mark-read가 끝나 배지가 지워질 시간을 충분히 준다(held 요청과 무관하게 진행된다).
  const railBadge = page.locator('a[title="김민정"] span.bg-destructive');
  await expect(railBadge).toHaveCount(0);
  await expect.poll(() => staleCallHeld).toBe(true);

  // 붙잡아뒀던 stale 응답(unreadCount 3)을 풀어준다 — 배지가 되돌아가면 안 된다.
  releaseStaleRooms();
  await page.waitForTimeout(300);
  await expect(railBadge).toHaveCount(0);
});
