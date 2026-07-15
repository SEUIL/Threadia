/*
 * S06 백로그/스프린트 smoke — WBS 4.4.2.
 *
 * board.spec.ts와 같은 방식으로 /api/v1/** 를 route mock해서 백엔드 없이 돈다.
 * 서버 계약을 흉내내는 부분(종료 시 이슈 정리 등)은 backend SprintService 기준이다.
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "backlog-smoke" } });

type MockIssue = {
  id: number;
  issueKey: string;
  title: string;
  status: string;
  sprintId: number | null;
};

type MockSprint = { id: number; name: string; status: string; summary: string | null };

const issue = (over: Partial<MockIssue> & { id: number; issueKey: string }): MockIssue => ({
  title: "이슈",
  status: "BACKLOG",
  sprintId: null,
  ...over,
});

/** viewer.canManageProject로 P/C 여부를 흉내낸다. */
async function mockApi(page: Page, options: { canManage: boolean; sprints: MockSprint[]; issues: MockIssue[] }) {
  const sprints = options.sprints;
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
      await json(
        apiResponse({
          accessToken: "test-token",
          user: { id: 1, email: "a@test.dev", nickname: "A 사용자", avatarUrl: null },
        })
      );
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

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/sprints") {
      await json(apiResponse({ sprints }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/issues") {
      await json(apiResponse({ issues }));
      return;
    }

    if (request.method() === "POST" && url.pathname === "/api/v1/projects/10/sprints") {
      const body = request.postDataJSON();
      calls.push(`create:${JSON.stringify(body)}`);
      sprints.push({ id: 900, name: body.name, status: "PLANNED", summary: null });
      await json(apiResponse({ sprint: sprints[sprints.length - 1] }), 201);
      return;
    }

    const startMatch = url.pathname.match(/^\/api\/v1\/sprints\/(\d+)\/start$/);
    if (request.method() === "POST" && startMatch) {
      const target = sprints.find((s) => s.id === Number(startMatch[1]));
      if (target) target.status = "ACTIVE";
      calls.push(`start:${startMatch[1]}`);
      await json(apiResponse({ sprint: target }));
      return;
    }

    const completeMatch = url.pathname.match(/^\/api\/v1\/sprints\/(\d+)\/complete$/);
    if (request.method() === "POST" && completeMatch) {
      const sprintId = Number(completeMatch[1]);
      const target = sprints.find((s) => s.id === sprintId);
      if (target) target.status = "COMPLETED";
      // 서버 계약: DONE 포함 모든 배정 이슈의 sprint를 clear하고,
      // DONE도 BACKLOG도 아닌 이슈만 BACKLOG로 되돌린다.
      for (const item of issues) {
        if (item.sprintId !== sprintId) continue;
        item.sprintId = null;
        if (item.status !== "DONE" && item.status !== "BACKLOG") item.status = "BACKLOG";
      }
      calls.push(`complete:${sprintId}`);
      await json(apiResponse({ sprint: target }));
      return;
    }

    const assignMatch = url.pathname.match(/^\/api\/v1\/sprints\/(\d+)\/issues$/);
    if (request.method() === "POST" && assignMatch) {
      const sprintId = Number(assignMatch[1]);
      const body = request.postDataJSON();
      calls.push(`assign:${sprintId}:${JSON.stringify(body)}`);
      for (const id of body.issueIds) {
        const target = issues.find((i) => i.id === id);
        if (target) target.sprintId = sprintId;
      }
      await json(apiResponse({ sprint: sprints.find((s) => s.id === sprintId) }));
      return;
    }

    await route.abort();
  });

  return { calls, issues, sprints };
}

test("스프린트와 백로그를 실 API 응답으로 나눠 보여준다", async ({ page }) => {
  await mockApi(page, {
    canManage: true,
    sprints: [{ id: 300, name: "Sprint 1", status: "ACTIVE", summary: null }],
    issues: [
      issue({ id: 1, issueKey: "THR-1", title: "배정된 이슈", status: "TODO", sprintId: 300 }),
      issue({ id: 2, issueKey: "THR-2", title: "백로그 이슈" }),
    ],
  });

  await page.goto("/1/10/backlog");

  await expect(page.getByRole("heading", { name: "Sprint 1" })).toBeVisible();
  await expect(page.getByText("배정된 이슈")).toBeVisible();
  await expect(page.getByRole("heading", { name: "백로그 (1)" })).toBeVisible();
});

test("이슈를 스프린트에 배정하면 issueIds 배열로 보낸다", async ({ page }) => {
  const mock = await mockApi(page, {
    canManage: true,
    sprints: [{ id: 300, name: "Sprint 1", status: "PLANNED", summary: null }],
    issues: [issue({ id: 2, issueKey: "THR-2", title: "백로그 이슈" })],
  });

  await page.goto("/1/10/backlog");
  await page.getByLabel("THR-2 스프린트 배정").selectOption("300");

  await expect.poll(() => mock.calls).toContain('assign:300:{"issueIds":[2]}');
  // 재조회 결과가 반영돼 백로그에서 빠진다.
  await expect(page.getByRole("heading", { name: "백로그 (0)" })).toBeVisible();
});

test("스프린트를 종료하면 서버 정리 결과를 재조회로 반영하고 요약을 보여준다", async ({ page }) => {
  await mockApi(page, {
    canManage: true,
    sprints: [{ id: 300, name: "Sprint 1", status: "ACTIVE", summary: null }],
    issues: [
      issue({ id: 1, issueKey: "THR-1", title: "미완료 이슈", status: "IN_PROGRESS", sprintId: 300 }),
      issue({ id: 3, issueKey: "THR-3", title: "완료된 이슈", status: "DONE", sprintId: 300 }),
    ],
  });

  await page.goto("/1/10/backlog");
  await page.getByRole("button", { name: "종료" }).click();

  await expect(page.getByText("종료됨")).toBeVisible();
  // DONE 이슈까지 sprint가 clear되므로 종료된 스프린트에는 이슈 목록을 그리지 않는다.
  await expect(page.getByText("배정됐던 이슈는 백로그로 돌아갔습니다.")).toBeVisible();
  // 미완료 이슈는 BACKLOG로 돌아오고, DONE 이슈도 sprint가 없어져 백로그 목록에 들어온다.
  await expect(page.getByRole("heading", { name: "백로그 (2)" })).toBeVisible();
});

test("이미 진행중인 스프린트가 있으면 다른 스프린트를 시작할 수 없다", async ({ page }) => {
  await mockApi(page, {
    canManage: true,
    sprints: [
      { id: 300, name: "진행중 스프린트", status: "ACTIVE", summary: null },
      { id: 301, name: "예정 스프린트", status: "PLANNED", summary: null },
    ],
    issues: [],
  });

  await page.goto("/1/10/backlog");

  // 프로젝트당 ACTIVE는 하나뿐이라 서버가 409로 막는다 — UI에서도 시작 버튼을 내지 않는다.
  await expect(page.getByRole("button", { name: "시작" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "종료" })).toHaveCount(1);
});

test("관리 권한이 없으면 스프린트 관리 UI가 보이지 않는다", async ({ page }) => {
  await mockApi(page, {
    canManage: false,
    sprints: [{ id: 300, name: "Sprint 1", status: "PLANNED", summary: null }],
    issues: [issue({ id: 2, issueKey: "THR-2", title: "백로그 이슈" })],
  });

  await page.goto("/1/10/backlog");

  await expect(page.getByRole("heading", { name: "Sprint 1" })).toBeVisible();
  await expect(page.getByRole("button", { name: "+ 새 스프린트" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "시작" })).toHaveCount(0);
  await expect(page.getByLabel("THR-2 스프린트 배정")).toHaveCount(0);
});
