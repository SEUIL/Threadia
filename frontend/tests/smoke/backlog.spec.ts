import { expect, test } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "backlog-smoke" } });

test("moves a sprint issue to the backlog after the unassignment request succeeds", async ({ page }) => {
  let unassignmentRequest: { method: string; pathname: string; body: unknown } | undefined;

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

    if (request.method() === "OPTIONS") {
      await route.fulfill({ status: 204, headers: corsHeaders });
      return;
    }

    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        headers: corsHeaders,
        body: JSON.stringify(apiResponse({ accessToken: "test-token", user: { id: 1, email: "a@test.dev", nickname: "A 사용자", avatarUrl: null } })),
      });
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/users/me/company-memberships") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        headers: corsHeaders,
        body: JSON.stringify(apiResponse({ companyMemberships: [] })),
      });
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        headers: corsHeaders,
        body: JSON.stringify(apiResponse({ viewer: { companyRole: null, projectRole: "PROJECT_ADMIN", canManageProject: true } })),
      });
      return;
    }

    if (request.method() === "PATCH" && url.pathname === "/api/v1/issues/i1") {
      unassignmentRequest = {
        method: request.method(),
        pathname: url.pathname,
        body: request.postDataJSON(),
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        headers: corsHeaders,
        body: JSON.stringify(apiResponse({ issue: { id: "i1", issueKey: "THR-101", title: "로그인 화면 반응형 깨짐", status: "IN_REVIEW" } })),
      });
      return;
    }

    await route.abort();
  });

  await page.goto("/1/10/backlog");
  const sprint = page.locator("section").filter({
    has: page.getByRole("heading", { name: "Sprint 2 — Company/Project" }),
  });
  const backlog = page.locator("section").filter({ has: page.getByRole("heading", { name: /백로그/ }) });

  await expect(sprint.getByText("로그인 화면 반응형 깨짐")).toBeVisible();
  await sprint.getByRole("button", { name: "백로그로" }).first().click();
  await expect.poll(() => unassignmentRequest).toEqual({
    method: "PATCH",
    pathname: "/api/v1/issues/i1",
    body: { sprintId: null },
  });
  await expect(sprint.getByText("로그인 화면 반응형 깨짐")).toHaveCount(0);
  await expect(backlog.getByText("로그인 화면 반응형 깨짐")).toBeVisible();
});
