/*
 * S14 구성원 제거 smoke — WBS 감사로 찾은 갭 수정.
 *
 * 백엔드 `DELETE /companies/{companyId}/company-members/{companyMemberId}`
 * (PR #181)는 있었는데 S14 화면에 액션이 없어서 못 쓰고 있었다.
 *
 * board.spec.ts와 같은 방식으로 /api/v1/** 를 route mock해서 백엔드 없이 돈다.
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "member-remove-smoke" } });

async function mockApi(page: Page) {
  let members = [
    {
      id: 11,
      companyId: 1,
      departmentId: null,
      role: "MEMBER",
      status: "ACTIVE",
      joinedAt: "2026-07-01T00:00:00Z",
      createdAt: "2026-07-01T00:00:00Z",
      updatedAt: "2026-07-01T00:00:00Z",
      user: { id: 2, email: "b@test.dev", nickname: "김민정", avatarUrl: null },
    },
  ];
  const removeCalls: number[] = [];

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
      await json(apiResponse({ companyMemberships: [{ companyId: 1, companyName: "Threadia", role: "COMPANY_ADMIN" }] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/company-members") {
      await json(apiResponse({ companyMembers: members }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/departments") {
      await json(apiResponse({ departments: [] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/invitations") {
      await json(apiResponse({ invitations: [] }));
      return;
    }

    const removeMatch = url.pathname.match(/^\/api\/v1\/companies\/1\/company-members\/(\d+)$/);
    if (removeMatch && request.method() === "DELETE") {
      const companyMemberId = Number(removeMatch[1]);
      removeCalls.push(companyMemberId);
      members = members.filter((m) => m.id !== companyMemberId);
      await json(apiResponse({ removed: true, removedProjectMemberCount: 0 }));
      return;
    }

    await route.abort();
  });

  return { removeCalls };
}

test("제거 버튼은 2단계 확인 후에만 실제로 구성원을 제거한다", async ({ page }) => {
  const { removeCalls } = await mockApi(page);
  await page.goto("/1/members");

  await expect(page.getByText("김민정")).toBeVisible();

  await page.getByRole("button", { name: "제거" }).click();
  await expect(page.getByText("정말 제거?")).toBeVisible();
  // 1단계 클릭만으로는 아직 호출되지 않는다.
  expect(removeCalls).toEqual([]);
  await expect(page.getByText("김민정")).toBeVisible();

  await page.getByRole("button", { name: "확인" }).click();

  await expect(page.getByText("김민정")).toHaveCount(0);
  expect(removeCalls).toEqual([11]);
});

test("제거 실패하면 에러가 보이고 목록에 그대로 남는다", async ({ page }) => {
  await mockApi(page);
  await page.route("**/api/v1/companies/1/company-members/11", async (route) => {
    if (route.request().method() !== "DELETE") return route.fallback();
    await route.fulfill({
      status: 409,
      contentType: "application/json",
      body: JSON.stringify({ error: { code: "CONFLICT", message: "마지막 관리자는 제거할 수 없습니다." } }),
    });
  });
  await page.goto("/1/members");

  await page.getByRole("button", { name: "제거" }).click();
  await page.getByRole("button", { name: "확인" }).click();

  await expect(page.getByText("마지막 관리자는 제거할 수 없습니다.")).toBeVisible();
  await expect(page.getByText("김민정")).toBeVisible();
});
