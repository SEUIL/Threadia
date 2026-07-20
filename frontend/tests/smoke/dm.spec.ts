/*
 * S09 1:1 DM smoke — WBS 4.7.3.2 (PR #173 리뷰 반영, pktewon).
 *
 * board.spec.ts와 같은 방식으로 /api/v1/** 를 route mock해서 백엔드 없이 돈다.
 * 리뷰에서 지적된 세 가지를 회귀 없이 커버한다:
 *  1. room 진입 → mark-read → 레일 unread 배지가 즉시 사라진다.
 *  2. 메시지 전송이 목록에 반영된다.
 *  3. "+" 로 새 대화를 시작하면 새 room으로 이동한다.
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
