/*
 * S07 그래프 뷰 — 문서 카드가 ROWS_PER_SUBCOLUMN(6)개를 넘어 서브열이 2개
 * 이상 생기는 경우를 검증한다.
 *
 * 버그: graphLayout.ts의 이슈 열 시작 x가 고정값(380)이었어서, 문서가 6개를
 * 넘어 두 번째 서브열(x=250~440)이 생기면 이슈 열(x=380~)과 겹쳤다(QA
 * 스크린샷, 2026-07-23). layoutGraphColumns()로 바꿔 이슈 열 시작 x를 문서
 * 열이 실제로 차지하는 폭 기준으로 다시 계산했다 — 이 테스트는 문서 8개(서브열
 * 2개 필요)로 그 겹침이 실제 렌더 결과에서 재발하지 않는지 화면 좌표로 확인한다.
 */
import { expect, test } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "graph-smoke" } });

test("문서가 6개를 넘어 서브열이 2개가 되어도 이슈 카드와 겹치지 않는다", async ({ page }) => {
  const docs = Array.from({ length: 8 }, (_, i) => ({
    id: i + 1,
    type: "DOCUMENT" as const,
    title: `문서 ${i + 1}`,
    issueKey: null,
    status: null,
  }));
  const issues = Array.from({ length: 3 }, (_, i) => ({
    id: 100 + i,
    type: "ISSUE" as const,
    title: `이슈 ${i + 1}`,
    issueKey: `THR-${100 + i}`,
    status: "BACKLOG",
  }));

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const origin = request.headers().origin ?? "http://127.0.0.1:3001";
    const headers = {
      "access-control-allow-origin": origin,
      "access-control-allow-methods": "GET,OPTIONS",
      "access-control-allow-headers": "authorization,content-type",
      "access-control-allow-credentials": "true",
    };
    const json = (data: unknown, status = 200) =>
      route.fulfill({ status, contentType: "application/json", headers, body: JSON.stringify(apiResponse(data)) });

    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers });
    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      return json({ accessToken: "test-token", user: { id: 1, email: "a@test.dev", nickname: "A", avatarUrl: null } });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/users/me/company-memberships") {
      return json({ companyMemberships: [] });
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/documents/graph") {
      return json({ nodes: [...docs, ...issues], edges: [] });
    }
    return route.abort();
  });

  await page.goto("/1/10/graph");
  await expect(page.getByRole("heading", { name: "그래프" })).toBeVisible();

  // 문서 8개 전부와 이슈 1개(대표로 1개면 충분 — 나머지도 같은 열이라 x가 같다)가
  // 그려질 때까지 기다린 뒤 화면 좌표를 잰다.
  await expect(page.getByText("문서 8")).toBeVisible();
  await expect(page.getByText("THR-100")).toBeVisible();

  const docBoxes = await Promise.all(
    docs.map((d) => page.getByText(`문서 ${d.id}`).boundingBox())
  );
  const issueBoxes = await Promise.all(
    issues.map((i) => page.getByText(i.issueKey!).boundingBox())
  );

  const docRight = Math.max(...docBoxes.map((b) => b!.x + b!.width));
  const issueLeft = Math.min(...issueBoxes.map((b) => b!.x));

  expect(docRight).toBeLessThan(issueLeft);
});
