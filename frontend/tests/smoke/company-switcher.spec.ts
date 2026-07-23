import { expect, test, type Page } from "@playwright/test";

test.use({ launchOptions: { args: ["--enable-webgl", "--use-gl=swiftshader"] } });

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "company-switcher-smoke" } });

const USER = { id: 1, email: "user@test.dev", nickname: "나", avatarUrl: null };
const HUB_COMPANY_SESSION_KEY = "threadia:hubCompanyId";

const MEMBERSHIPS = [
  { companyMemberId: 101, companyId: 1, companyName: "Alpha Corp", role: "MEMBER", status: "ACTIVE" },
  { companyMemberId: 202, companyId: 2, companyName: "Beta Corp", role: "COMPANY_ADMIN", status: "ACTIVE" },
];
const SINGLE_MEMBERSHIP = [MEMBERSHIPS[0]];

const COMPANY_INVITATION = {
  id: 77,
  scopeType: "COMPANY",
  companyId: 2,
  companyName: "Beta Corp",
  projectId: null,
  projectName: null,
  inviterUserId: 9,
  inviterNickname: "초대한 사람",
  inviteeUserId: USER.id,
  inviteeEmail: USER.email,
  inviteeNickname: USER.nickname,
  role: "MEMBER",
  status: "PENDING",
  expiresAt: "2099-01-01T00:00:00Z",
};

type ApiRecorder = {
  heartbeats: number[];
  candidates: number[];
  projectLists: number[];
};

function corsHeaders(origin: string) {
  return {
    "access-control-allow-origin": origin,
    "access-control-allow-methods": "GET,POST,PATCH,DELETE,OPTIONS",
    "access-control-allow-headers": "authorization,content-type",
    "access-control-allow-credentials": "true",
  };
}

async function mockApi(page: Page, options: { invitations?: unknown[]; memberships?: unknown[] } = {}): Promise<ApiRecorder> {
  const recorder: ApiRecorder = { heartbeats: [], candidates: [], projectLists: [] };

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const headers = corsHeaders(request.headers().origin ?? "http://127.0.0.1:3001");
    const json = (body: unknown, status = 200) =>
      route.fulfill({ status, contentType: "application/json", headers, body: JSON.stringify(body) });

    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers });

    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      return json(apiResponse({ accessToken: "test-token", user: USER }));
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/users/me/company-memberships") {
      return json(apiResponse({ companyMemberships: options.memberships ?? MEMBERSHIPS }));
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/users/me/invitations") {
      return json(apiResponse({ invitations: options.invitations ?? [] }));
    }

    if (request.method() === "POST" && url.pathname === "/api/v1/invitations/77/accept") {
      return json(apiResponse({ invitationId: 77, status: "ACCEPTED", membershipCreated: true }));
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/direct-messages/unread-count") {
      return json(apiResponse({ unreadCount: 0 }));
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/notifications/unread-count") {
      return json(apiResponse({ unreadCount: 0 }));
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/platform/actor") {
      return json({ error: { code: "FORBIDDEN", message: "플랫폼 관리자가 아닙니다." } }, 403);
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/attachments") {
      return json(apiResponse([]));
    }

    const heartbeatMatch = url.pathname.match(/^\/api\/v1\/companies\/(\d+)\/hub-presence\/heartbeat$/);
    if (request.method() === "POST" && heartbeatMatch) {
      const companyId = Number(heartbeatMatch[1]);
      recorder.heartbeats.push(companyId);
      return json(apiResponse({ onlineUserIds: [USER.id] }));
    }

    const candidatesMatch = url.pathname.match(/^\/api\/v1\/companies\/(\d+)\/direct-messages\/candidates$/);
    if (request.method() === "GET" && candidatesMatch) {
      const companyId = Number(candidatesMatch[1]);
      recorder.candidates.push(companyId);
      return json(apiResponse({ candidates: [{ id: companyId + 10, nickname: `동료 ${companyId}`, avatarUrl: null }] }));
    }

    const projectsMatch = url.pathname.match(/^\/api\/v1\/companies\/(\d+)\/projects$/);
    if (request.method() === "GET" && projectsMatch) {
      const companyId = Number(projectsMatch[1]);
      recorder.projectLists.push(companyId);
      return json(apiResponse({ projects: [] }));
    }

    return json({ error: { code: "UNMOCKED", message: `${request.method()} ${url.pathname}` } }, 404);
  });

  return recorder;
}

async function waitForHubCompany(recorder: ApiRecorder, companyId: number) {
  await expect.poll(() => recorder.heartbeats.includes(companyId)).toBe(true);
  await expect.poll(() => recorder.candidates.includes(companyId)).toBe(true);
}

async function openHubFlyout(page: Page) {
  await page.getByTitle("허브").first().hover();
}

async function presetHubCompany(page: Page, companyId: string) {
  await page.addInitScript(
    ({ key, value }) => {
      window.sessionStorage.setItem(key, value);
    },
    { key: HUB_COMPANY_SESSION_KEY, value: companyId }
  );
}

test("/hub는 query 없이 sessionStorage의 Beta Corp를 선택하고 company 1 presence/candidates를 호출하지 않는다", async ({ page }) => {
  const recorder = await mockApi(page);
  await presetHubCompany(page, "2");

  await page.goto("/hub");
  await waitForHubCompany(recorder, 2);
  await openHubFlyout(page);

  await expect(page).toHaveURL(/\/hub$/);
  expect(new URL(page.url()).search).toBe("");
  await expect(page.getByText("Beta Corp")).toBeVisible();
  await expect(page.locator('a[href="/2/projects"]').first()).toBeVisible();
  await expect(page.locator('a[href="/2/settings"]').first()).toBeVisible();
  expect(recorder.heartbeats).not.toContain(1);
  expect(recorder.candidates).not.toContain(1);
});

test("invalid stored hub companyId는 Alpha Corp로 fallback하고 company 999 API를 호출하지 않는다", async ({ page }) => {
  const recorder = await mockApi(page);
  await presetHubCompany(page, "999");

  await page.goto("/hub");
  await waitForHubCompany(recorder, 1);
  await openHubFlyout(page);

  await expect(page.getByText("Alpha Corp")).toBeVisible();
  await expect(page).toHaveURL(/\/hub$/);
  await expect.poll(() => page.evaluate((key) => window.sessionStorage.getItem(key), HUB_COMPANY_SESSION_KEY)).toBe("1");
  expect(recorder.heartbeats).not.toContain(999);
  expect(recorder.candidates).not.toContain(999);
});

test("두 hub tab은 sessionStorage companyId별 heartbeat target을 따로 유지한다", async ({ page, context }) => {
  const secondPage = await context.newPage();
  const firstRecorder = await mockApi(page);
  const secondRecorder = await mockApi(secondPage);
  await presetHubCompany(page, "1");
  await presetHubCompany(secondPage, "2");

  await Promise.all([page.goto("/hub"), secondPage.goto("/hub")]);
  await waitForHubCompany(firstRecorder, 1);
  await waitForHubCompany(secondRecorder, 2);

  await expect(page).toHaveURL(/\/hub$/);
  await expect(secondPage).toHaveURL(/\/hub$/);
  expect(firstRecorder.heartbeats.every((companyId) => companyId === 1)).toBe(true);
  expect(secondRecorder.heartbeats.every((companyId) => companyId === 2)).toBe(true);

  await secondPage.close();
});

test("RailShell 회사 switcher는 현재 tab hub company를 저장하고 projects로 이동하며 hub link는 /hub다", async ({ page }) => {
  const recorder = await mockApi(page);

  await page.goto("/1/projects");
  await expect.poll(() => recorder.projectLists.includes(1)).toBe(true);
  await expect(page.locator('a[title="허브"]')).toHaveAttribute("href", "/hub");

  await page.locator('a[title="허브"]').hover();
  await expect(page.getByRole("button", { name: "회사 교체" })).toBeVisible();
  await page.getByRole("button", { name: "회사 교체" }).click();
  await page.getByRole("menuitem", { name: /Beta Corp/ }).click();

  await expect(page).toHaveURL(/\/2\/projects$/);
  await expect.poll(() => page.evaluate((key) => window.sessionStorage.getItem(key), HUB_COMPANY_SESSION_KEY)).toBe("2");
  await expect.poll(() => recorder.projectLists.includes(2)).toBe(true);
  await expect(page.locator('a[title="허브"]')).toHaveAttribute("href", "/hub");

  await page.locator('a[title="허브"]').click();
  await expect(page).toHaveURL(/\/hub$/);
  expect(new URL(page.url()).search).toBe("");
  await waitForHubCompany(recorder, 2);
});

test("단일 회사 소속이면 회사명만 보이고 교체 버튼은 숨긴다", async ({ page }) => {
  const recorder = await mockApi(page, { memberships: SINGLE_MEMBERSHIP });

  await page.goto("/1/projects");
  await expect.poll(() => recorder.projectLists.includes(1)).toBe(true);
  await page.locator('a[title="허브"]').hover();

  await expect(page.getByText("Alpha Corp")).toBeVisible();
  await expect(page.getByRole("button", { name: "회사 교체" })).toHaveCount(0);
});

test("company-scoped 본문 허브 링크도 현재 회사 hub company를 저장한다", async ({ page }) => {
  const recorder = await mockApi(page);

  await page.goto("/2/projects");
  await expect.poll(() => recorder.projectLists.includes(2)).toBe(true);
  await expect(page.getByText("← 허브로")).toBeVisible();
  await page.getByText("← 허브로").click();

  await expect(page).toHaveURL(/\/hub$/);
  expect(new URL(page.url()).search).toBe("");
  await expect.poll(() => page.evaluate((key) => window.sessionStorage.getItem(key), HUB_COMPANY_SESSION_KEY)).toBe("2");
  await waitForHubCompany(recorder, 2);
});

test("회사 초대 수락 후 새 회사 허브 CTA는 이전 hub session을 새 회사로 바꾼다", async ({ page }) => {
  const recorder = await mockApi(page, { invitations: [COMPANY_INVITATION] });
  await page.emulateMedia({ reducedMotion: "reduce" });
  await presetHubCompany(page, "1");

  await page.goto("/invitations");
  await page.getByRole("button", { name: /Beta Corp 초대 봉투 열기/ }).click();
  await expect(page.getByRole("button", { name: "수락" })).toBeVisible();
  await page.getByRole("button", { name: "수락" }).click();
  await expect(page.getByText("Beta Corp 소속이 반영됐습니다.")).toBeVisible();
  await page.getByText("새 회사 허브로 이동").click();

  await expect(page).toHaveURL(/\/hub$/);
  expect(new URL(page.url()).search).toBe("");
  await expect.poll(() => page.evaluate((key) => window.sessionStorage.getItem(key), HUB_COMPANY_SESSION_KEY)).toBe("2");
  await waitForHubCompany(recorder, 2);
});
