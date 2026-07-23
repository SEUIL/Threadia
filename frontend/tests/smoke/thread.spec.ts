import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "thread-smoke" } });
const USER = { id: 1, email: "me@test.dev", nickname: "나", avatarUrl: null };

async function mockApi(page: Page) {
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
    const json = (data: unknown) =>
      route.fulfill({ status: 200, contentType: "application/json", headers, body: JSON.stringify(apiResponse(data)) });

    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers });
    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      return json({ accessToken: "test-token", user: USER });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/users/me/company-memberships") {
      return json({ companyMemberships: [] });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/threads/100") {
      return json({
        thread: {
          id: 100,
          projectId: 10,
          targetType: "MESSAGE",
          targetId: 20,
          topic: "작성자 표시",
          createdByUserId: USER.id,
          createdAt: "2026-07-20T10:00:00Z",
          updatedAt: "2026-07-20T10:00:00Z",
        },
      });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/threads/100/comments") {
      return json({
        comments: [
          {
            id: 101,
            threadId: 100,
            authorUserId: 2,
            author: { id: 2, nickname: "  지민  ", avatarUrl: null },
            content: "nickname이 있는 댓글",
            createdAt: "2026-07-20T10:00:00Z",
          },
          {
            id: 102,
            threadId: 100,
            authorUserId: 3,
            author: { id: 3, nickname: "   ", avatarUrl: null },
            content: "공백 nickname 댓글",
            createdAt: "2026-07-20T10:01:00Z",
          },
          {
            id: 103,
            threadId: 100,
            authorUserId: 4,
            content: "기존 응답 댓글",
            createdAt: "2026-07-20T10:02:00Z",
          },
        ],
      });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10") {
      return json({
        project: { id: 10, companyId: 1, name: "프로젝트", projectKey: "THR", status: "ACTIVE" },
        viewer: { companyRole: null, projectRole: "MEMBER", canManageProject: false },
      });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/messages/20") {
      return json({ message: { id: 20, projectId: 10, authorUserId: 2, content: "연결된 메시지", threadId: 100, createdAt: "2026-07-20T10:00:00Z" } });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/attachments") {
      return json([]);
    }
    await route.abort();
  });
}

test("댓글 작성자는 nickname을 우선 표시하고 없거나 공백이면 ID로 fallback한다", async ({ page }) => {
  await mockApi(page);

  await page.goto("/1/10/threads/100");

  await expect(page.getByText("nickname이 있는 댓글")).toBeVisible();
  await expect(page.getByText("지민", { exact: true })).toBeVisible();
  await expect(page.getByText("사용자 #3", { exact: true })).toBeVisible();
  await expect(page.getByText("사용자 #4", { exact: true })).toBeVisible();
});
