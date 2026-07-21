/*
 * 이슈 상세 "연결 문서" smoke — WBS 감사로 찾은 갭 수정.
 *
 * 화면정의서 §S05 "주요 데이터: 연결 문서" / "주요 액션: 관련 문서 연결/조회"가
 * 미구현이었다. 백엔드(`GET /issues/{issueId}/documents`)와 프론트 서비스
 * 메서드(`documentService.documentsOfIssue`)는 이미 있었는데 `IssueDetailModal`
 * 이 호출하는 곳이 없었다.
 *
 * board.spec.ts와 같은 방식으로 /api/v1/** 를 route mock해서 백엔드 없이 돈다.
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "issue-docs-smoke" } });

async function mockApi(page: Page) {
  const issues = [
    { id: 101, issueKey: "THR-101", title: "첫 번째 이슈", status: "BACKLOG", reporterUserId: 1, trashedAt: null },
  ];
  const documents = [{ id: 201, projectId: 10, title: "설계 문서", type: "DOC", trashedAt: null }];
  let links: { id: number; projectId: number; documentId: number; issueId: number }[] = [];
  let nextLinkId = 500;

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
      await json(apiResponse({ accessToken: "test-token", user: { id: 1, email: "a@test.dev", nickname: "A 사용자", avatarUrl: null } }));
      return;
    }

    if (url.pathname === "/api/v1/users/me/company-memberships") {
      await json(apiResponse({ companyMemberships: [] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10") {
      await json(apiResponse({ viewer: { companyRole: null, projectRole: "PROJECT_ADMIN", canManageProject: true } }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/issues") {
      await json(apiResponse({ issues }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/project-members") {
      await json(apiResponse({ projectMembers: [] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/issues/101/documents") {
      const linkedIds = new Set(links.filter((l) => l.issueId === 101).map((l) => l.documentId));
      await json(apiResponse({ documents: documents.filter((d) => linkedIds.has(d.id)) }));
      return;
    }

    if (url.pathname === "/api/v1/projects/10/documents" && request.method() === "GET") {
      await json(apiResponse({ documents }));
      return;
    }

    if (url.pathname === "/api/v1/projects/10/document-issue-links") {
      if (request.method() === "GET") {
        await json(apiResponse({ links }));
        return;
      }
      if (request.method() === "POST") {
        const body = request.postDataJSON() as { documentId: number; issueId: number };
        const link = { id: nextLinkId++, projectId: 10, documentId: body.documentId, issueId: body.issueId };
        links.push(link);
        await json(apiResponse({ link }), 201);
        return;
      }
    }

    const unlinkMatch = url.pathname.match(/^\/api\/v1\/document-issue-links\/(\d+)$/);
    if (unlinkMatch && request.method() === "DELETE") {
      const linkId = Number(unlinkMatch[1]);
      links = links.filter((l) => l.id !== linkId);
      await json(apiResponse({ deleted: true }));
      return;
    }

    await route.abort();
  });
}

test("이슈 상세에서 문서를 연결하고 해제할 수 있다", async ({ page }) => {
  await mockApi(page);
  await page.goto("/1/10/board");

  await page.getByText("첫 번째 이슈").click();
  await page.getByRole("button", { name: "상세 열기", exact: true }).click();

  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(page.getByText("연결된 문서가 없습니다.")).toBeVisible();

  await page.getByLabel("연결할 문서").selectOption({ label: "설계 문서" });
  await page.getByRole("button", { name: "연결" }).click();

  await expect(page.getByRole("link", { name: "설계 문서" })).toBeVisible();
  await expect(page.getByText("연결된 문서가 없습니다.")).toHaveCount(0);

  await page.getByRole("button", { name: "연결 해제" }).click();
  await expect(page.getByText("연결된 문서가 없습니다.")).toBeVisible();
});
