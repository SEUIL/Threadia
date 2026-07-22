/*
 * S09 프로젝트 채팅 "메시지 → 이슈 생성" smoke — WBS 4.5.1.2.
 *
 * Backend(WBS 4.5.1.1, PR #214)는 `POST /projects/{projectId}/issues/from-message`가
 * P/C(PROJECT_ADMIN/COMPANY_ADMIN) 전용이다(일반 멤버는 403). 이 테스트는:
 *  1. P/C에게는 미변환 메시지에 "이슈로 만들기" 버튼이 보이고, 제출하면 이슈가
 *     생성되며 메시지 옆에 이슈 키 링크로 바뀐다.
 *  2. 일반 멤버(A)에게는 애초에 그 버튼이 보이지 않는다(서버가 403을 주기 전에
 *     프론트에서 액션 자체를 숨긴다).
 *  3. 이미 변환된 메시지(sourceMessageId로 대조)는 버튼 대신 이슈 키 링크만 보인다.
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "message-to-issue-smoke" } });
const USER = { id: 1, email: "me@test.dev", nickname: "나", avatarUrl: null };

const issue = (over: {
  id: number;
  issueKey: string;
  title: string;
  sourceType: "MANUAL" | "MESSAGE";
  sourceMessageId: number | null;
}) => ({
  id: over.id,
  projectId: 10,
  issueKey: over.issueKey,
  title: over.title,
  status: "BACKLOG",
  description: null,
  priority: "MEDIUM",
  assigneeUserId: null,
  reporterUserId: 1,
  sprintId: null,
  dueDate: null,
  sourceType: over.sourceType,
  sourceMessageId: over.sourceMessageId,
  trashedAt: null,
  restoredAt: null,
});

/** GET .../issues 응답을 붙잡아두는 게이트 — 재리뷰(dhy257) flicker 재현용. */
function createHold() {
  let release: () => void = () => {};
  const gate = new Promise<void>((resolve) => {
    release = resolve;
  });
  return { gate, release };
}

async function mockApi(
  page: Page,
  options: {
    viewer: { companyRole: "COMPANY_ADMIN" | "MEMBER" | null; projectRole: "PROJECT_ADMIN" | "MEMBER" | null };
    issues?: ReturnType<typeof issue>[];
    /** PR #232 리뷰(dhy257) 재현용 — GET .../issues만 500으로 실패시킨다. */
    issuesFail?: boolean;
    /** PR #232 재리뷰(dhy257) 재현용 — GET .../issues 응답을 hold.release()까지 붙잡는다. */
    issuesHold?: ReturnType<typeof createHold>;
  }
) {
  const createdIssues: unknown[] = [];
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
    const json = (data: unknown, status = 200) =>
      route.fulfill({ status, contentType: "application/json", headers, body: JSON.stringify(apiResponse(data)) });

    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers });
    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      return json({ accessToken: "test-token", user: USER });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/users/me/company-memberships") {
      return json({ companyMemberships: [] });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/messages") {
      return json({
        messages: [
          { id: 31, projectId: 10, authorUserId: 2, content: "로그인 버튼이 모바일에서 안 눌립니다", threadId: null, createdAt: "2026-07-20T10:00:00" },
        ],
        nextCursor: null,
      });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/project-members") {
      return json({
        projectMembers: [
          { id: 2, projectId: 10, role: "MEMBER", companyRole: "MEMBER", status: "ACTIVE", joinedAt: null, createdAt: "", updatedAt: "", user: { id: 2, email: "other@test.dev", nickname: "다른 사용자", avatarUrl: null } },
        ],
      });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10") {
      return json({
        project: { id: 10, companyId: 1, name: "프로젝트", projectKey: "THR", status: "ACTIVE" },
        viewer: {
          ...options.viewer,
          canManageProject: options.viewer.companyRole === "COMPANY_ADMIN" || options.viewer.projectRole === "PROJECT_ADMIN",
        },
      });
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/projects/10/messages/mark-read") {
      return json({ projectId: 10, lastReadMessageId: 31, unreadCount: 0 });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/issues") {
      if (options.issuesHold) await options.issuesHold.gate;
      if (options.issuesFail) {
        return route.fulfill({
          status: 500,
          contentType: "application/json",
          headers,
          body: JSON.stringify({ error: { code: "INTERNAL_ERROR", message: "issues 조회 실패" }, meta: { requestId: "message-to-issue-smoke" } }),
        });
      }
      return json({ issues: options.issues ?? [] });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/attachments") {
      return json([]);
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/projects/10/issues/from-message") {
      const body = request.postDataJSON() as { messageId: number; title: string };
      createdIssues.push(body);
      return json({ issue: issue({ id: 99, issueKey: "THR-99", title: body.title, sourceType: "MESSAGE", sourceMessageId: body.messageId }) }, 201);
    }
    await route.abort();
  });
  return createdIssues;
}

test("PROJECT_ADMIN은 메시지를 이슈로 만들 수 있고, 생성 후 이슈 키 링크로 바뀐다", async ({ page }) => {
  await mockApi(page, { viewer: { companyRole: "MEMBER", projectRole: "PROJECT_ADMIN" } });
  await page.goto("/1/10/messages");

  await expect(page.getByText("로그인 버튼이 모바일에서 안 눌립니다")).toBeVisible();
  const openButton = page.getByRole("button", { name: "이슈로 만들기" });
  await expect(openButton).toBeVisible();
  await openButton.click();

  await expect(page.getByRole("dialog", { name: "메시지로 이슈 만들기" })).toBeVisible();
  const titleInput = page.getByLabel("제목");
  await expect(titleInput).toHaveValue("로그인 버튼이 모바일에서 안 눌립니다");

  await page.getByRole("button", { name: "이슈 생성" }).click();

  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.getByRole("link", { name: "→ THR-99" })).toBeVisible();
  await expect(page.getByRole("button", { name: "이슈로 만들기" })).toHaveCount(0);
});

test("일반 멤버에게는 '이슈로 만들기' 액션이 보이지 않는다", async ({ page }) => {
  await mockApi(page, { viewer: { companyRole: "MEMBER", projectRole: "MEMBER" } });
  await page.goto("/1/10/messages");

  await expect(page.getByText("로그인 버튼이 모바일에서 안 눌립니다")).toBeVisible();
  await expect(page.getByRole("button", { name: "이슈로 만들기" })).toHaveCount(0);
});

test("이미 이슈로 변환된 메시지는 버튼 대신 이슈 키 링크만 보인다", async ({ page }) => {
  await mockApi(page, {
    viewer: { companyRole: "COMPANY_ADMIN", projectRole: "MEMBER" },
    issues: [issue({ id: 50, issueKey: "THR-50", title: "기존 이슈", sourceType: "MESSAGE", sourceMessageId: 31 })],
  });
  await page.goto("/1/10/messages");

  await expect(page.getByRole("link", { name: "→ THR-50" })).toBeVisible();
  await expect(page.getByRole("button", { name: "이슈로 만들기" })).toHaveCount(0);
});

test("이슈 목록 조회가 실패해도 채팅 본기능(메시지 읽기)은 그대로 유지된다", async ({ page }) => {
  // PR #232 리뷰(dhy257) 재현: GET .../issues만 500으로 막고 S09를 로드하면
  // 채팅 전체가 error 화면으로 빠지면 안 된다 — "이슈로 만들기" 액션만 숨긴다.
  await mockApi(page, { viewer: { companyRole: "COMPANY_ADMIN", projectRole: "MEMBER" }, issuesFail: true });
  await page.goto("/1/10/messages");

  await expect(page.getByText("채팅을 불러오지 못했습니다.")).toHaveCount(0);
  await expect(page.getByText("로그인 버튼이 모바일에서 안 눌립니다")).toBeVisible();
  await expect(page.getByRole("button", { name: "이슈로 만들기" })).toHaveCount(0);
  await expect(page.getByRole("link", { name: /^→ / })).toHaveCount(0);
});

test("이슈 목록 조회가 느린 동안엔 이미 변환된 메시지에도 '이슈로 만들기' 버튼이 잠깐 뜨지 않는다", async ({ page }) => {
  // PR #232 재리뷰(dhy257, optional 제안) 재현: issuesState를 boolean(실패 여부)
  // 이 아니라 loading/ready/error 3단계로 둬야, 응답이 아직 안 온 "issues=[]"
  // 상태를 "정말 빈 목록"으로 오인해서 이미 변환된 메시지에 액션이 깜빡이는
  // 걸 막을 수 있다.
  const hold = createHold();
  await mockApi(page, {
    viewer: { companyRole: "COMPANY_ADMIN", projectRole: "MEMBER" },
    issues: [issue({ id: 50, issueKey: "THR-50", title: "기존 이슈", sourceType: "MESSAGE", sourceMessageId: 31 })],
    issuesHold: hold,
  });

  try {
    await page.goto("/1/10/messages");
    // 메시지는 이미 보이지만(messages/members/project detail은 안 붙잡혔다),
    // issues 응답이 아직 안 왔으니 액션도 링크도 뜨면 안 된다.
    await expect(page.getByText("로그인 버튼이 모바일에서 안 눌립니다")).toBeVisible();
    await expect(page.getByRole("button", { name: "이슈로 만들기" })).toHaveCount(0);
    await expect(page.getByRole("link", { name: "→ THR-50" })).toHaveCount(0);
  } finally {
    hold.release();
  }

  await expect(page.getByRole("link", { name: "→ THR-50" })).toBeVisible();
});
