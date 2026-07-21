/*
 * "플랫폼 관리자" nav 진입점 smoke — WBS 감사 중 발견한 갭 수정.
 *
 * S15 플랫폼 관리자 콘솔(WBS 4.10.5, PR #198)이 구현·머지는 됐지만 RailShell/
 * HubOverlay 어디에도 `/platform` 진입점이 없어서, 실제 플랫폼 관리자도 URL을
 * 직접 입력해야만 들어갈 수 있었다. useIsPlatformAdmin()이 GET /api/v1/platform/actor
 * 결과로 nav 노출 여부를 정하므로, 그 성공/실패(403) 두 경로를 각각 확인한다.
 *
 * board.spec.ts/profile.spec.ts와 같은 방식으로 /api/v1/** 를 route mock해서
 * 백엔드 없이 돈다. RailShell이 붙는 회사 route(board)로 진입해서 확인한다.
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "platform-nav-smoke" } });

async function mockApi(page: Page, options: { isPlatformAdmin: boolean }) {
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
      await json(apiResponse({ companyMemberships: [{ companyId: 1, companyName: "Threadia", role: "MEMBER" }] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/platform/actor") {
      if (options.isPlatformAdmin) {
        await json(
          apiResponse({
            platformAdmin: { id: 1, nickname: "나", role: "SUPER_ADMIN", status: "ACTIVE" },
          })
        );
      } else {
        await json({ error: { code: "FORBIDDEN", message: "플랫폼 관리자가 아닙니다." } }, 403);
      }
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10") {
      await json(apiResponse({ project: { id: 10, companyId: 1, name: "테스트 프로젝트", projectKey: "TP", status: "ACTIVE" } }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/viewer") {
      await json(apiResponse({ viewer: { companyRole: null, projectRole: "MEMBER", canManageProject: false } }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/issues") {
      await json(apiResponse({ issues: [] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/platform/support-cases") {
      await json(apiResponse({ supportCases: [] }));
      return;
    }

    await route.abort();
  });
}

const platformNavLink = (page: Page) => page.locator('a[title="플랫폼 관리자"]');

test("platform admin이면 레일에 '플랫폼 관리자' 진입점이 보이고 클릭하면 콘솔로 이동한다", async ({ page }) => {
  await mockApi(page, { isPlatformAdmin: true });
  await page.goto("/1/10/board");

  await expect(platformNavLink(page)).toBeVisible();
  await platformNavLink(page).click();

  await expect(page).toHaveURL(/\/platform$/);
  await expect(page.getByText("플랫폼 관리자 콘솔")).toBeVisible();
});

test("platform admin이 아니면 레일에 '플랫폼 관리자' 진입점이 안 보인다", async ({ page }) => {
  await mockApi(page, { isPlatformAdmin: false });
  await page.goto("/1/10/board");

  await expect(page.getByRole("button", { name: /프로필 수정/ }).first()).toBeVisible();
  await expect(platformNavLink(page)).toHaveCount(0);
});
