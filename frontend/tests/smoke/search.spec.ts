/*
 * S-없음(프로젝트 탭) 통합 검색 smoke — WBS 4.9.4.2 (PR #191 리뷰 반영, xaphxe).
 *
 * board.spec.ts와 같은 방식으로 /api/v1/** 를 route mock해서 백엔드 없이 돈다.
 * 리뷰에서 지적된 loadMore() stale response 방어를 회귀 없이 커버한다:
 * "더 보기" 응답이 늦게 도착하기 전에 검색어를 바꾸면, 늦게 온 이전 검색어의
 * pagination 응답이 새 검색 결과 뒤에 섞여 붙으면 안 된다.
 */
import { expect, test } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "search-smoke" } });

test("loadMore 응답이 늦게 도착해도 검색어를 바꾸면 이전 결과가 섞이지 않는다", async ({ page }) => {
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

    if (request.method() === "GET" && url.pathname === "/api/v1/search") {
      const q = url.searchParams.get("q");
      const cursor = url.searchParams.get("cursor");

      if (q === "alpha" && !cursor) {
        await json(
          apiResponse({
            results: [{ id: 1, type: "DOCUMENT", title: "alpha 첫 페이지", snippet: "", sortAt: "2026-07-20T00:00:00Z" }],
            nextCursor: "cursor-alpha-1",
          })
        );
        return;
      }

      if (q === "alpha" && cursor === "cursor-alpha-1") {
        // 의도적으로 늦게 응답한다 — 그사이 사용자가 검색어를 "beta"로 바꾼다.
        await new Promise((resolve) => setTimeout(resolve, 600));
        await json(
          apiResponse({
            results: [{ id: 2, type: "DOCUMENT", title: "alpha 두번째 페이지(늦게 도착)", snippet: "", sortAt: "2026-07-20T00:00:00Z" }],
            nextCursor: null,
          })
        );
        return;
      }

      if (q === "beta" && !cursor) {
        await json(
          apiResponse({
            results: [{ id: 3, type: "THREAD", title: "beta 결과", snippet: "", sortAt: "2026-07-20T00:00:00Z" }],
            nextCursor: null,
          })
        );
        return;
      }

      await json(apiResponse({ results: [], nextCursor: null }));
      return;
    }

    await route.continue();
  });

  await page.goto("/1/10/search");

  await page.getByPlaceholder("검색어를 입력하세요").fill("alpha");
  await expect(page.getByText("alpha 첫 페이지")).toBeVisible();

  // "더 보기" 클릭 — 응답은 600ms 뒤에나 온다.
  await page.getByRole("button", { name: "더 보기" }).click();

  // 응답이 오기 전에 검색어를 바꾼다.
  await page.getByPlaceholder("검색어를 입력하세요").fill("beta");
  await expect(page.getByText("beta 결과")).toBeVisible();

  // alpha의 늦은 loadMore 응답이 도착할 시간(600ms)까지 기다린 뒤에도
  // beta 결과만 남아 있어야 한다 — alpha 두번째 페이지가 섞여 붙으면 안 된다.
  await page.waitForTimeout(800);
  await expect(page.getByText("alpha 두번째 페이지(늦게 도착)")).toHaveCount(0);
  await expect(page.getByText("beta 결과")).toBeVisible();
  await expect(page.locator("li")).toHaveCount(1);
});
