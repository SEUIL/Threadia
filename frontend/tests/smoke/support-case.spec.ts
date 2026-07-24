/*
 * 문의하기(/support) smoke — WBS 감사로 찾은 갭 수정.
 *
 * `POST/GET /api/v1/support-cases`(인증 ACTIVE 사용자 누구나 호출 가능, API-명세서
 * §10-A)는 완전히 구현·문서화돼 있는데 일반 사용자용 화면이 아예 없었다.
 *
 * board.spec.ts와 같은 방식으로 /api/v1/** 를 route mock해서 백엔드 없이 돈다.
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "support-case-smoke" } });

async function mockApi(page: Page) {
  const cases: {
    id: number;
    reporterUserId: number;
    type: string;
    status: string;
    targetType: null;
    targetId: null;
    title: string;
    body: string;
    createdAt: string;
    updatedAt: string;
  }[] = [];
  let nextId = 900;

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
      await json(apiResponse({ companyMemberships: [] }));
      return;
    }

    if (url.pathname === "/api/v1/support-cases") {
      if (request.method() === "GET") {
        await json(apiResponse({ supportCases: cases }));
        return;
      }
      if (request.method() === "POST") {
        const body = request.postDataJSON() as { type: string; title: string; body: string; reason: string };
        const created = {
          id: nextId++,
          reporterUserId: 1,
          type: body.type,
          status: "OPEN",
          targetType: null,
          targetId: null,
          title: body.title,
          body: body.body,
          createdAt: "2026-07-21T00:00:00Z",
          updatedAt: "2026-07-21T00:00:00Z",
        };
        cases.unshift(created);
        await json(apiResponse({ supportCase: created, publicReplies: [] }), 201);
        return;
      }
    }

    const detailMatch = url.pathname.match(/^\/api\/v1\/support-cases\/(\d+)$/);
    if (detailMatch && request.method() === "GET") {
      const c = cases.find((item) => item.id === Number(detailMatch[1]));
      if (!c) {
        await json({ error: { code: "NOT_FOUND", message: "찾을 수 없습니다." } }, 404);
        return;
      }
      await json(apiResponse({ supportCase: c, publicReplies: [] }));
      return;
    }

    await route.abort();
  });
}

test("문의를 등록하면 목록에 즉시 반영되고 상세로 들어가 확인할 수 있다", async ({ page }) => {
  await mockApi(page);
  await page.goto("/support");

  await expect(page.getByRole("link", { name: "← 허브로" })).toHaveAttribute("href", "/hub");
  await expect(page.getByText("등록한 문의가 없습니다.")).toBeVisible();

  await page.getByLabel("문의 제목").fill("배지 관련 문의");
  await page.getByLabel("문의 내용").fill("배지가 안 사라져요.");
  await page.getByLabel("문의 사유").fill("접수 테스트 사유");
  await page.getByRole("button", { name: "문의 등록" }).click();

  await expect(page.getByText("내 문의 (1)")).toBeVisible();
  await expect(page.getByText("배지 관련 문의")).toBeVisible();
  await expect(page.getByText("접수")).toBeVisible();

  await page.getByText("배지 관련 문의").click();
  await expect(page.getByRole("link", { name: "← 허브로" })).toHaveAttribute("href", "/hub");
  await expect(page.getByRole("heading", { name: "배지 관련 문의" })).toBeVisible();
  await expect(page.getByText("배지가 안 사라져요.")).toBeVisible();
  await expect(page.getByText("아직 답변이 등록되지 않았습니다.")).toBeVisible();
});

test("사유가 3자 미만이면 등록하지 않고 에러를 보여준다", async ({ page }) => {
  await mockApi(page);
  await page.goto("/support");

  await page.getByLabel("문의 제목").fill("제목");
  await page.getByLabel("문의 내용").fill("내용");
  await page.getByLabel("문의 사유").fill("짧");
  await page.getByRole("button", { name: "문의 등록" }).click();

  await expect(page.getByText("문의 사유는 3~500자로 입력해 주세요.")).toBeVisible();
  await expect(page.getByText("등록한 문의가 없습니다.")).toBeVisible();
});
