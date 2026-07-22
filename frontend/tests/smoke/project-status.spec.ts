/*
 * S12 프로젝트 설정 — 상태(status) 변경 smoke. 이슈 #226.
 *
 * 배포 사이트에서 프로젝트 상태가 "기획중"에서 "진행중"으로 안 바뀌는 버그였다 —
 * 백엔드 `PATCH /projects/{projectId}`는 status 포함 full-replace를 지원하는데
 * settings/page.tsx 수정 폼에 status 편집 컨트롤 자체가 없었다(단순 구현 누락).
 * "상태" select를 추가하고 PATCH body에 선택값이 실리는지, 저장 후 화면에 반영
 * 되는지 검증한다.
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "project-status-smoke" } });

async function mockApi(page: Page) {
  let projectStatus = "PLANNING";
  const patchBodies: unknown[] = [];

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const origin = request.headers().origin ?? "http://127.0.0.1:3001";
    const headers = {
      "access-control-allow-origin": origin,
      "access-control-allow-methods": "GET,POST,PATCH,DELETE,OPTIONS",
      "access-control-allow-headers": "authorization,content-type",
      "access-control-allow-credentials": "true",
    };
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: "application/json", headers, body: JSON.stringify(body) });

    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers });
    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      return json(apiResponse({ accessToken: "test-token", user: { id: 1, email: "a@test.dev", nickname: "관리자", avatarUrl: null } }));
    }
    if (url.pathname === "/api/v1/users/me/company-memberships") {
      return json(apiResponse({ companyMemberships: [] }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10") {
      return json(
        apiResponse({
          project: {
            id: 10,
            companyId: 1,
            name: "Seed Project",
            projectKey: "SEED",
            description: null,
            status: projectStatus,
            startDate: null,
            endDate: null,
            createdAt: "2026-07-01T00:00:00Z",
            updatedAt: "2026-07-01T00:00:00Z",
          },
          viewer: { companyRole: "COMPANY_ADMIN", projectRole: null, canManageProject: true },
        })
      );
    }
    if (request.method() === "PATCH" && url.pathname === "/api/v1/projects/10") {
      const body = request.postDataJSON();
      patchBodies.push(body);
      projectStatus = (body as { status: string }).status;
      return json(
        apiResponse({
          project: {
            id: 10,
            companyId: 1,
            name: (body as { name: string }).name,
            projectKey: "SEED",
            description: (body as { description: string | null }).description,
            status: projectStatus,
            startDate: null,
            endDate: null,
            createdAt: "2026-07-01T00:00:00Z",
            updatedAt: "2026-07-01T00:00:01Z",
          },
        })
      );
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/project-members") {
      return json(apiResponse({ projectMembers: [] }));
    }
    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/invitations") {
      return json(apiResponse({ invitations: [] }));
    }
    await route.abort();
  });

  return patchBodies;
}

test("상태 select로 프로젝트 상태를 진행중으로 바꾸면 PATCH에 실리고 배지에 반영된다", async ({ page }) => {
  const patchBodies = await mockApi(page);
  await page.goto("/1/10/settings");

  await expect(page.getByText("기획중")).toBeVisible();

  await page.getByRole("button", { name: "수정", exact: true }).click();
  const statusSelect = page.getByLabel("프로젝트 상태");
  await expect(statusSelect).toHaveValue("PLANNING");
  await statusSelect.selectOption("ACTIVE");
  await page.getByRole("button", { name: "저장" }).click();

  await expect(page.getByText("진행중")).toBeVisible();
  await expect.poll(() => patchBodies.length).toBe(1);
  expect(patchBodies[0]).toMatchObject({ status: "ACTIVE" });
});

test("상태 select에 ARCHIVED 옵션은 없다 — 아카이빙은 별도 버튼 경로다", async ({ page }) => {
  await mockApi(page);
  await page.goto("/1/10/settings");

  await page.getByRole("button", { name: "수정", exact: true }).click();
  const options = await page.getByLabel("프로젝트 상태").locator("option").allTextContents();
  expect(options).toEqual(["기획중", "진행중", "일시중지", "완료"]);
});
