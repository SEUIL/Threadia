/*
 * S05 이슈 상세/수정/휴지통 smoke — WBS 4.4.2 (리뷰 대응, ISSUE-02/03/04).
 *
 * board.spec.ts와 같은 방식으로 /api/v1/** 를 route mock해서 백엔드 없이 돈다.
 * 권한 규칙은 backend IssueService 기준이다.
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "issue-detail-smoke" } });

const ME = { id: 1, email: "a@test.dev", nickname: "나", avatarUrl: null };

type MockIssue = {
  id: number;
  issueKey: string;
  title: string;
  status: string;
  description: string | null;
  priority: string;
  assigneeUserId: number | null;
  reporterUserId: number | null;
  sprintId: number | null;
  dueDate: string | null;
  trashedAt: string | null;
};

const issue = (over: Partial<MockIssue> & { id: number; issueKey: string }): MockIssue => ({
  title: "이슈",
  status: "BACKLOG",
  description: null,
  priority: "MEDIUM",
  assigneeUserId: null,
  reporterUserId: 99,
  sprintId: null,
  dueDate: null,
  trashedAt: null,
  ...over,
});

const toResponse = (item: MockIssue) => ({ ...item, sourceType: "MANUAL", restoredAt: null });

async function mockApi(page: Page, options: { canManage: boolean; issues: MockIssue[] }) {
  const issues = options.issues;
  const calls: string[] = [];

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const requestOrigin = request.headers().origin ?? "http://127.0.0.1:3001";
    const corsHeaders = {
      "access-control-allow-origin": requestOrigin,
      "access-control-allow-methods": "GET,POST,PATCH,OPTIONS",
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
      await json(apiResponse({ accessToken: "test-token", user: ME }));
      return;
    }

    if (url.pathname === "/api/v1/users/me/company-memberships") {
      await json(apiResponse({ companyMemberships: [] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10") {
      await json(
        apiResponse({
          project: { id: 10, companyId: 1, name: "P", projectKey: "THR", status: "ACTIVE" },
          viewer: {
            companyRole: null,
            projectRole: options.canManage ? "PROJECT_ADMIN" : "MEMBER",
            canManageProject: options.canManage,
          },
        })
      );
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/project-members") {
      await json(
        apiResponse({
          projectMembers: [
            { id: 1, projectId: 10, role: "MEMBER", status: "ACTIVE", joinedAt: null, createdAt: "", updatedAt: "", user: { id: 2, email: "b@test.dev", nickname: "담당자B", avatarUrl: null } },
          ],
        })
      );
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/issues") {
      await json(apiResponse({ issues: issues.filter((i) => i.trashedAt === null).map(toResponse) }));
      return;
    }

    const patchMatch = url.pathname.match(/^\/api\/v1\/issues\/(\d+)$/);
    if (request.method() === "PATCH" && patchMatch) {
      const body = request.postDataJSON();
      calls.push(`PATCH:${JSON.stringify(body)}`);
      const target = issues.find((i) => i.id === Number(patchMatch[1]))!;
      Object.assign(target, body);
      await json(apiResponse({ issue: toResponse(target) }));
      return;
    }

    const trashMatch = url.pathname.match(/^\/api\/v1\/issues\/(\d+)\/trash$/);
    if (request.method() === "POST" && trashMatch) {
      const target = issues.find((i) => i.id === Number(trashMatch[1]))!;
      target.trashedAt = "2026-07-16T00:00:00Z";
      calls.push(`trash:${target.id}`);
      await json(apiResponse({ issue: toResponse(target) }));
      return;
    }

    await route.abort();
  });

  return { calls, issues };
}

/**
 * 카드를 눌러 확장한 뒤 "상세 열기"로 모달을 연다.
 *
 * name에 exact를 준 이유: 카드 바깥 div가 dnd-kit의 attributes로 role="button"을
 * 갖는 탓에 접근성 이름이 자식 텍스트를 전부 합친 문자열("THR-1 … 상세 열기 …")이 된다.
 * getByRole의 name은 기본이 부분 일치라 exact 없이는 카드와 버튼이 둘 다 잡힌다.
 */
async function openDetail(page: Page, title: string) {
  await page.getByText(title, { exact: true }).click();
  await page.getByRole("button", { name: "상세 열기", exact: true }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
}

test("바뀐 필드만 PATCH로 보낸다", async ({ page }) => {
  const mock = await mockApi(page, {
    canManage: true,
    issues: [issue({ id: 1, issueKey: "THR-1", title: "원래 제목", priority: "MEDIUM" })],
  });

  await page.goto("/1/10/board");
  await openDetail(page, "원래 제목");

  // 보드의 "새 이슈 제목" 입력과 부분 일치하므로 exact로 집는다.
  await page.getByLabel("제목", { exact: true }).fill("바뀐 제목");
  await page.getByRole("button", { name: "저장" }).click();

  // 서버가 보낸 key로만 판정하고 허용 안 된 필드는 400이라, 안 바꾼 필드는 담으면 안 된다.
  await expect.poll(() => mock.calls).toContain('PATCH:{"title":"바뀐 제목"}');
});

test("바꾼 게 없으면 요청하지 않고 닫는다", async ({ page }) => {
  const mock = await mockApi(page, {
    canManage: true,
    issues: [issue({ id: 1, issueKey: "THR-1", title: "원래 제목" })],
  });

  await page.goto("/1/10/board");
  await openDetail(page, "원래 제목");
  await page.getByRole("button", { name: "저장" }).click();

  await expect(page.getByRole("dialog")).toHaveCount(0);
  expect(mock.calls).toHaveLength(0);
});

test("A는 우선순위·담당자·마감일을 수정할 수 없다", async ({ page }) => {
  await mockApi(page, {
    canManage: false,
    issues: [issue({ id: 1, issueKey: "THR-1", title: "원래 제목" })],
  });

  await page.goto("/1/10/board");
  await openDetail(page, "원래 제목");

  // 서버가 A에게 이 필드들을 403으로 막으므로 화면에서도 읽기 전용이다.
  await expect(page.getByLabel("제목", { exact: true })).toBeEnabled();
  await expect(page.getByLabel("우선순위")).toBeDisabled();
  await expect(page.getByLabel("담당자")).toBeDisabled();
  await expect(page.getByLabel("마감일")).toBeDisabled();
});

test("등록자가 아니고 관리자도 아니면 휴지통 버튼이 없다", async ({ page }) => {
  await mockApi(page, {
    canManage: false,
    // reporterUserId가 내가 아님(99)
    issues: [issue({ id: 1, issueKey: "THR-1", title: "남의 이슈", reporterUserId: 99 })],
  });

  await page.goto("/1/10/board");
  await openDetail(page, "남의 이슈");

  await expect(page.getByRole("button", { name: "휴지통으로" })).toHaveCount(0);
});

test("관리자가 아니어도 내가 등록한 이슈는 휴지통에 넣을 수 있다", async ({ page }) => {
  const mock = await mockApi(page, {
    canManage: false,
    // 서버는 P/C 또는 등록자에게 허용한다 — reporterUserId로 판정한다.
    issues: [issue({ id: 1, issueKey: "THR-1", title: "내 이슈", reporterUserId: ME.id })],
  });

  await page.goto("/1/10/board");
  await openDetail(page, "내 이슈");
  await page.getByRole("button", { name: "휴지통으로" }).click();

  await expect.poll(() => mock.calls).toContain("trash:1");
  // 기본 목록은 includeTrashed=false라 휴지통 이슈는 보드에서 사라진다.
  await expect(page.getByText("내 이슈", { exact: true })).toHaveCount(0);
});
