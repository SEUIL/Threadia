import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "message-attachment-smoke" } });
const USER = { id: 1, email: "me@test.dev", nickname: "나", avatarUrl: null };
const AVATAR_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

async function mockApi(page: Page, viewer: { companyRole: "COMPANY_ADMIN" | "MEMBER" | null; projectRole: "PROJECT_ADMIN" | "MEMBER" | null }) {
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const origin = request.headers().origin ?? "http://127.0.0.1:3001";
    const headers = {
      "access-control-allow-origin": origin,
      "access-control-allow-methods": "GET,POST,OPTIONS",
      "access-control-allow-headers": "authorization,content-type",
      "access-control-allow-credentials": "true",
    };
    const json = async (data: unknown) => route.fulfill({ status: 200, contentType: "application/json", headers, body: JSON.stringify(apiResponse(data)) });

    if (request.method() === "OPTIONS") {
      await route.fulfill({ status: 204, headers });
      return;
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      await json({ accessToken: "test-token", user: USER });
      return;
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/users/me/company-memberships") {
      await json({ companyMemberships: [] });
      return;
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/messages") {
      await json({ messages: [{ id: 31, projectId: 10, authorUserId: 2, content: "다른 사용자의 메시지", threadId: null, createdAt: "2026-07-20T10:00:00" }], nextCursor: null });
      return;
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/project-members") {
      await json({ projectMembers: [{ id: 2, projectId: 10, role: "MEMBER", companyRole: "MEMBER", status: "ACTIVE", joinedAt: null, createdAt: "", updatedAt: "", user: { id: 2, email: "other@test.dev", nickname: "다른 사용자", avatarUrl: null } }] });
      return;
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10") {
      await json({ project: { id: 10, companyId: 1, name: "프로젝트", projectKey: "THR", status: "ACTIVE" }, viewer: { ...viewer, canManageProject: viewer.companyRole === "COMPANY_ADMIN" || viewer.projectRole === "PROJECT_ADMIN" } });
      return;
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/projects/10/messages/mark-read") {
      await json({ projectId: 10, lastReadMessageId: 31, unreadCount: 0 });
      return;
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/issues") {
      await json({ issues: [] });
      return;
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/attachments") {
      await json(url.searchParams.get("targetType") === "USER_AVATAR" && url.searchParams.get("targetId") === "2" ? [{ id: 91, targetType: "USER_AVATAR", targetId: 2, filename: "shared-avatar.png", contentType: "image/png", contentLength: 70, status: "READY", completedAt: "2026-07-20T10:00:00", createdAt: "2026-07-20T10:00:00" }] : []);
      return;
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/attachments/91/download") {
      await json({ downloadUrl: AVATAR_IMAGE });
      return;
    }
    await route.abort();
  });
}

test("PROJECT_ADMIN은 다른 작성자의 message attachment를 관리할 수 있다", async ({ page }) => {
  await mockApi(page, { companyRole: "MEMBER", projectRole: "PROJECT_ADMIN" });
  await page.goto("/1/10/messages");
  await expect(page.getByText("다른 사용자의 메시지")).toBeVisible();
  await expect(page.getByRole("button", { name: "파일 추가" })).toBeVisible();
});

test("COMPANY_ADMIN은 다른 작성자의 message attachment를 관리할 수 있다", async ({ page }) => {
  await mockApi(page, { companyRole: "COMPANY_ADMIN", projectRole: "MEMBER" });
  await page.goto("/1/10/messages");
  await expect(page.getByRole("button", { name: "파일 추가" })).toBeVisible();
});

test("일반 멤버는 다른 작성자의 message attachment mutation control을 보지 못한다", async ({ page }) => {
  await mockApi(page, { companyRole: "MEMBER", projectRole: "MEMBER" });
  await page.goto("/1/10/messages");
  await expect(page.getByRole("button", { name: "파일 추가" })).toHaveCount(0);
});

test("읽기 전용 사용자가 보는 첨부 없는 메시지에는 '첨부 파일' 헤더가 아예 안 뜬다", async ({ page }) => {
  // 이 메시지(id 31)는 MESSAGE 첨부가 없고(mock이 빈 배열 반환), 일반 멤버는
  // 추가 권한도 없다 — 이럴 때 "첨부 파일" 섹션을 통째로 숨겨 목록을 깔끔하게
  // 유지한다(첨부가 없는 모든 메시지마다 헤더가 깔리던 클러터 제거).
  await mockApi(page, { companyRole: "MEMBER", projectRole: "MEMBER" });
  await page.goto("/1/10/messages");
  await expect(page.getByText("다른 사용자의 메시지")).toBeVisible();
  await expect(page.getByText("첨부 파일")).toHaveCount(0);
});

test("shared project member의 attachment avatar가 message author와 member list initials보다 우선한다", async ({ page }) => {
  await mockApi(page, { companyRole: "MEMBER", projectRole: "MEMBER" });
  await page.goto("/1/10/messages");

  await expect(page.getByText("다른 사용자의 메시지")).toBeVisible();
  await expect(page.locator(`img[src="${AVATAR_IMAGE}"]`)).toHaveCount(2);
});
