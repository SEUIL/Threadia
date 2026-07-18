/*
 * S14 수신(/invitations) smoke — WBS 4.3.2 PROJECT scope 대응.
 *
 * 이 목록에는 COMPANY와 PROJECT 초대가 섞여 내려온다. 화면 표기가 scope별로
 * 갈리는지 고정한다. route mock이라 백엔드 없이 돈다.
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "received-smoke" } });

const companyInvitation = {
  id: 501,
  scopeType: "COMPANY",
  companyId: 1,
  companyName: "Threadia",
  projectId: null,
  projectName: null,
  inviterUserId: 9,
  inviterNickname: "초대자",
  inviteeUserId: 2,
  inviteeEmail: "member@example.com",
  inviteeNickname: "팀원",
  role: "MEMBER",
  status: "PENDING",
  expiresAt: "2126-07-22T10:15:30Z",
};

const projectInvitation = {
  ...companyInvitation,
  id: 601,
  scopeType: "PROJECT",
  projectId: 20,
  projectName: "Threadia 웹",
  role: "PROJECT_ADMIN",
};

async function mockApi(page: Page, invitations: unknown[]) {
  const calls: string[] = [];

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const requestOrigin = request.headers().origin ?? "http://127.0.0.1:3001";
    const corsHeaders = {
      "access-control-allow-origin": requestOrigin,
      "access-control-allow-methods": "GET,POST,PATCH,OPTIONS",
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
          user: { id: 2, email: "member@example.com", nickname: "팀원", avatarUrl: null },
        })
      );
      return;
    }

    if (url.pathname === "/api/v1/users/me/company-memberships") {
      await json(apiResponse({ companyMemberships: [] }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/users/me/invitations") {
      await json(apiResponse({ invitations }));
      return;
    }

    const acceptMatch = url.pathname.match(/^\/api\/v1\/invitations\/(\d+)\/accept$/);
    if (request.method() === "POST" && acceptMatch) {
      const id = Number(acceptMatch[1]);
      calls.push(`accept:${id}`);
      await json(apiResponse({ invitationId: id, status: "ACCEPTED", membershipCreated: true }));
      return;
    }

    await route.abort();
  });

  return { calls };
}

/** 봉투는 클릭해야 초대장이 나온다. */
async function openEnvelope(page: Page, name: RegExp) {
  const envelope = page.getByRole("button", { name });
  await expect(envelope).toBeVisible();
  await envelope.click();
}

test("프로젝트 초대는 회사명과 프로젝트명을 함께 보여준다", async ({ page }) => {
  await mockApi(page, [projectInvitation]);
  await page.goto("/invitations");

  // 회사명만 보여주면 회사에 프로젝트가 여러 개일 때 어디로 온 초대인지 알 수 없다.
  await openEnvelope(page, /Threadia · Threadia 웹 초대 봉투 열기/);
  await expect(page.getByText("Threadia · Threadia 웹")).toBeVisible();
  await expect(page.getByText("초대자 님이 프로젝트 관리자(으)로 초대했습니다")).toBeVisible();
});

test("회사 초대는 회사명과 회사 role로 보여준다", async ({ page }) => {
  await mockApi(page, [companyInvitation]);
  await page.goto("/invitations");

  await openEnvelope(page, /Threadia 초대 봉투 열기/);
  await expect(page.getByText("초대자 님이 회사 구성원(으)로 초대했습니다")).toBeVisible();
});

test("수락 후 안내가 scope에 따라 다르다", async ({ page }) => {
  await mockApi(page, [projectInvitation]);
  await page.goto("/invitations");

  await openEnvelope(page, /Threadia · Threadia 웹 초대 봉투 열기/);
  await page.getByRole("button", { name: "수락" }).click();

  // 프로젝트 초대 수락자는 이미 그 회사 소속이라 재로그인 안내가 필요 없다.
  await expect(page.getByText("Threadia 웹 프로젝트에 참여했습니다.")).toBeVisible();
  await expect(page.getByText("재로그인")).toHaveCount(0);
});

test("회사 초대 수락은 재로그인 없이 소속 반영 안내를 보여준다", async ({ page }) => {
  // PR #171: refreshCompanyMemberships()로 세션이 즉시 갱신되므로 더 이상
  // 재로그인 안내를 기대하면 안 된다(구버전 기대값 갱신).
  await mockApi(page, [companyInvitation]);
  await page.goto("/invitations");

  await openEnvelope(page, /Threadia 초대 봉투 열기/);
  await page.getByRole("button", { name: "수락" }).click();

  await expect(page.getByText("Threadia 소속이 반영됐습니다.")).toBeVisible();
  await expect(page.getByText("재로그인")).toHaveCount(0);
});
