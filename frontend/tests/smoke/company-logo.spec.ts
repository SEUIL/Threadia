/*
 * 회사 로고 URL 저장 smoke — WBS 감사 중 발견한 갭 수정.
 *
 * 백엔드 `POST /api/v1/companies/{companyId}/logo`(API-명세서 §5, "로고 URL 저장")가
 * 있는데 프론트가 아예 호출하지 않고 있었다(생성 시 항상 null 고정, 수정 폼에도
 * 필드 없음). S13 회사 관리 화면에 이름/설명 저장 폼과 분리된 로고 URL 입력·저장을
 * 붙였다.
 *
 * board.spec.ts와 같은 방식으로 /api/v1/** 를 route mock해서 백엔드 없이 돈다.
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "company-logo-smoke" } });

const company = (logoUrl: string | null) => ({
  id: 1,
  name: "Threadia",
  representativeName: null,
  description: null,
  industryCategory: null,
  logoUrl,
  status: "ACTIVE",
  createdAt: "2026-07-01T00:00:00Z",
  updatedAt: "2026-07-01T00:00:00Z",
});

async function mockApi(page: Page, options: { logoShouldFail?: boolean } = {}) {
  let currentCompany = company(null);
  const logoUpdateCalls: string[] = [];

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

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1") {
      await json(apiResponse({ company: currentCompany }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/stats") {
      await json(
        apiResponse({
          companyId: 1,
          memberCount: 1,
          departmentCount: 0,
          projectCount: 0,
          projectStatusCounts: {},
        })
      );
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/companies/1/departments") {
      await json(apiResponse({ departments: [] }));
      return;
    }

    if (request.method() === "POST" && url.pathname === "/api/v1/companies/1/logo") {
      const body = request.postDataJSON() as { logoUrl: string };
      logoUpdateCalls.push(body.logoUrl);
      if (options.logoShouldFail) {
        await json({ error: { code: "VALIDATION_ERROR", message: "올바른 URL 형식이 아닙니다." } }, 400);
        return;
      }
      currentCompany = company(body.logoUrl);
      await json(apiResponse({ company: currentCompany }));
      return;
    }

    await route.abort();
  });

  return { logoUpdateCalls };
}

test("로고 URL을 저장하면 성공하고 미리보기가 반영된다", async ({ page }) => {
  const { logoUpdateCalls } = await mockApi(page);
  await page.goto("/1/settings");

  const input = page.getByLabel("회사 로고 URL");
  await expect(input).toBeVisible();

  await input.fill("https://example.com/logo.png");
  await page.getByRole("button", { name: "로고 저장" }).click();

  await expect(page.getByAltText("회사 로고")).toHaveAttribute("src", "https://example.com/logo.png");
  expect(logoUpdateCalls).toEqual(["https://example.com/logo.png"]);
});

test("로고 저장이 실패하면 에러가 보이고 입력값은 남아있다", async ({ page }) => {
  await mockApi(page, { logoShouldFail: true });
  await page.goto("/1/settings");

  const input = page.getByLabel("회사 로고 URL");
  await input.fill("not-a-valid-url");
  await page.getByRole("button", { name: "로고 저장" }).click();

  await expect(page.getByText("올바른 URL 형식이 아닙니다.")).toBeVisible();
  await expect(input).toHaveValue("not-a-valid-url");
});
