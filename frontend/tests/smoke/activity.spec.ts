/*
 * 프로젝트 활동 피드 smoke — WBS 4.9.1.2.
 *
 * 이슈 #217 React Query 마이그레이션으로 activity/page.tsx의 `loadSeqRef`
 * 손짠 stale-response 가드(PR #205 리뷰, xaphxe)가 useInfiniteQuery로 바뀌었다
 * — 이전엔 이 화면에 스모크 테스트가 없었다. 초기 로드와 "더 보기" 페이지네이션이
 * 정상 동작하는지만 회귀 없이 커버한다(project 전환 시 stale 응답이 안 섞이는
 * 구조적 보장은 queryKey에 projectId를 넣는 것으로 확보된다 — search.spec.ts와
 * 같은 원리, 이 파일에서 별도로 재현하지 않는다).
 */
import { expect, test } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "activity-smoke" } });

const event = (over: { id: number; eventType: string; issueKey: string; title: string }) => ({
  id: over.id,
  eventType: over.eventType,
  targetType: "ISSUE",
  targetId: over.id,
  actorUserId: 1,
  metadata: { issueKey: over.issueKey, title: over.title, assigneeUserId: null, fromStatus: null, toStatus: null },
  createdAt: "2026-07-20T00:00:00Z",
});

test("활동 피드를 불러오고, 더 보기로 다음 페이지를 이어붙인다", async ({ page }) => {
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
          user: { id: 1, email: "a@test.dev", nickname: "나", avatarUrl: null },
        })
      );
      return;
    }

    if (url.pathname === "/api/v1/users/me/company-memberships") {
      await json(apiResponse({ companyMemberships: [{ companyId: 1, companyName: "Threadia", role: "MEMBER" }] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/activity-events") {
      const requestedPage = Number(url.searchParams.get("page") ?? "0");
      if (requestedPage === 0) {
        await json(
          apiResponse({
            events: [event({ id: 1, eventType: "ISSUE_CREATED", issueKey: "TH-1", title: "첫 이슈" })],
            page: 0,
            size: 20,
            total: 2,
          })
        );
        return;
      }
      await json(
        apiResponse({
          events: [event({ id: 2, eventType: "ISSUE_STATUS_CHANGED", issueKey: "TH-2", title: "두번째 이슈" })],
          page: requestedPage,
          size: 20,
          total: 2,
        })
      );
      return;
    }

    await route.continue();
  });

  await page.goto("/1/10/activity");

  await expect(page.getByText('이슈 TH-1 "첫 이슈"를 생성했습니다.')).toBeVisible();
  await expect(page.getByRole("button", { name: "더 보기" })).toBeVisible();

  await page.getByRole("button", { name: "더 보기" }).click();

  await expect(page.getByText("이슈 TH-2 상태를")).toBeVisible();
  // 두 페이지를 합쳐 total(2)에 도달했으니 "더 보기" 버튼이 사라져야 한다.
  await expect(page.getByRole("button", { name: "더 보기" })).toHaveCount(0);
});
