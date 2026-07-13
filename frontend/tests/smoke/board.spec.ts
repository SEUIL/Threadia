import { expect, test } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "board-smoke" } });

test("loads issues, creates a title-only issue, and persists an allowed status change", async ({ page }) => {
  const issues = [
    { id: 101, issueKey: "THR-101", title: "첫 번째 이슈", status: "BACKLOG" },
  ];
  let createBody: unknown;
  let statusBody: unknown;

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
        body: JSON.stringify(apiResponse({ viewer: { companyRole: null, projectRole: "MEMBER", canManageProject: false } })),
      });
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/issues") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        headers: corsHeaders,
        body: JSON.stringify(apiResponse({ issues })),
      });
      return;
    }

    if (request.method() === "POST" && url.pathname === "/api/v1/projects/10/issues") {
      createBody = request.postDataJSON();
      const issue = { id: 102, issueKey: "THR-102", title: "새 이슈", status: "BACKLOG" };
      issues.push(issue);
      await route.fulfill({
        status: 201,
        contentType: "application/json",
        headers: corsHeaders,
        body: JSON.stringify(apiResponse({ issue })),
      });
      return;
    }

    if (request.method() === "PATCH" && url.pathname === "/api/v1/issues/101/status") {
      statusBody = request.postDataJSON();
      issues[0] = { ...issues[0], status: "TODO" };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        headers: corsHeaders,
        body: JSON.stringify(apiResponse({ issue: issues[0] })),
      });
      return;
    }

    await route.abort();
  });

  await page.goto("/1/10/board");
  await expect(page.getByRole("heading", { name: "칸반 보드" })).toBeVisible();
  await expect(page.getByText("첫 번째 이슈")).toBeVisible();

  await page.getByLabel("새 이슈 제목").fill("새 이슈");
  await page.getByRole("button", { name: "이슈 추가" }).click();
  await expect.poll(() => createBody).toEqual({ title: "새 이슈" });
  await expect(page.getByText("THR-102").locator("xpath=..").getByText("새 이슈", { exact: true })).toBeVisible();

  await page.getByText("첫 번째 이슈").click();
  const statusSelect = page.getByLabel("상태 변경");
  await expect(statusSelect.locator("option[value='DONE']")).toHaveCount(0);
  await statusSelect.selectOption("TODO");
  await expect.poll(() => statusBody).toEqual({ status: "TODO" });
  await expect(
    page.getByRole("heading", { name: "할일" }).locator("xpath=../..").getByText("첫 번째 이슈")
  ).toBeVisible();
});
