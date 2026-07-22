/*
 * S11 알림 smoke — WBS 4.8.2 + WBS 4.5.2.2(문서 수정 알림) 검증.
 *
 * notifications/page.tsx는 그동안 스모크 테스트가 하나도 없었다. WBS 4.5.2.2
 * 착수 전 백엔드(WBS 4.5.2.1, PR #229)와 실제 계약을 맞춰본 결과 이 화면은
 * 이미 DOCUMENT_UPDATED/DOCUMENT target을 정상 처리하고 있었지만(seed 데이터로
 * curl + 브라우저 수동 검증 완료), 회귀를 막을 자동화된 테스트가 없었다.
 *
 * PR #229 확인 계약: DOCUMENT_UPDATED의 body는 항상 null(고정 title만 온다).
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "notification-smoke" } });

async function mockApi(page: Page) {
  const readCalls: number[] = [];
  let readAllCalled = false;

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const origin = request.headers().origin ?? "http://127.0.0.1:3001";
    const headers = {
      "access-control-allow-origin": origin,
      "access-control-allow-methods": "GET,POST,OPTIONS",
      "access-control-allow-headers": "authorization,content-type",
      "access-control-allow-credentials": "true",
    };
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: "application/json", headers, body: JSON.stringify(body) });

    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers });
    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      return json(apiResponse({ accessToken: "test-token", user: { id: 2, email: "member@test.dev", nickname: "멤버", avatarUrl: null } }));
    }
    if (url.pathname === "/api/v1/users/me/company-memberships") {
      return json(apiResponse({ companyMemberships: [{ companyId: 1, companyName: "Threadia", role: "MEMBER" }] }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/notifications") {
      // page.tsx는 raw shape(ApiResponse로 안 감쌈)을 기대한다 — notification.service.ts 참고.
      return json({
        data: [
          {
            notificationRecipientId: 501,
            notificationEventId: 401,
            type: "DOCUMENT_UPDATED",
            title: "문서가 업데이트되었습니다.",
            body: null,
            readAt: null,
            target: { type: "DOCUMENT", id: 5 },
            createdAt: "2026-07-22T05:43:35Z",
          },
        ],
        page: { cursor: null, hasNext: false, size: 20 },
        meta: { requestId: "notification-smoke" },
      });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/documents/5") {
      return json(apiResponse({ document: { id: 5, projectId: 10, authorUserId: 1, title: "문서", type: "DOC", content: "", trashedAt: null, restoredAt: null, createdAt: "", updatedAt: "" } }));
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/notifications/501/read") {
      readCalls.push(501);
      return json(apiResponse({ notificationRecipientId: 501, readAt: "2026-07-22T06:00:00Z" }));
    }
    if (request.method() === "POST" && url.pathname === "/api/v1/notifications/read-all") {
      readAllCalled = true;
      return json(apiResponse({ updatedCount: 1 }));
    }
    await route.abort();
  });

  return { readCalls, isReadAllCalled: () => readAllCalled };
}

test("DOCUMENT_UPDATED 알림은 body 없이 렌더되고, 클릭하면 읽음 처리 + 문서 화면으로 이동한다", async ({ page }) => {
  const { readCalls } = await mockApi(page);
  await page.goto("/1/notifications");

  await expect(page.getByText("문서 수정")).toBeVisible();
  await expect(page.getByText("문서가 업데이트되었습니다.")).toBeVisible();

  await page.getByText("문서가 업데이트되었습니다.").click();

  await expect(page).toHaveURL("/1/10/docs/5");
  await expect.poll(() => readCalls).toEqual([501]);
});

test("'모두 읽음' 버튼을 누르면 read-all이 호출되고 목록을 다시 불러온다", async ({ page }) => {
  const { isReadAllCalled } = await mockApi(page);
  await page.goto("/1/notifications");

  await expect(page.getByText("읽지 않은 알림 1개")).toBeVisible();
  await page.getByRole("button", { name: "모두 읽음" }).click();

  await expect.poll(() => isReadAllCalled()).toBe(true);
});
