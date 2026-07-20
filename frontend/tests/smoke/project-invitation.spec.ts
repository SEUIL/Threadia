/*
 * S12 프로젝트 초대 smoke — WBS 4.3.2.
 *
 * board.spec.ts와 같은 방식으로 /api/v1/** 를 route mock해서 백엔드 없이 돈다.
 * 서버 계약(후보가 아니면 전부 404 등)은 backend ProjectInvitationService 기준이다.
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "invite-smoke" } });

const errorResponse = (code: string, message: string) => ({
  error: { code, message },
  meta: { requestId: "invite-smoke" },
});

type MockProjectMember = {
  id: number;
  email: string;
  nickname: string;
  role: string;
  companyRole: string | null;
};

type MockInvitation = {
  id: number;
  status: string;
  inviteeEmail: string;
  inviteeNickname: string;
  role: string;
};

const projectMember = (over: Partial<MockProjectMember> & { id: number }): MockProjectMember => ({
  email: `member${over.id}@example.com`,
  nickname: `멤버${over.id}`,
  role: "MEMBER",
  companyRole: null,
  ...over,
});

const projectMemberResponse = (member: MockProjectMember) => ({
  id: member.id,
  projectId: 10,
  role: member.role,
  companyRole: member.companyRole,
  status: "ACTIVE",
  joinedAt: "2026-07-01T00:00:00Z",
  createdAt: "2026-07-01T00:00:00Z",
  updatedAt: "2026-07-01T00:00:00Z",
  user: { id: member.id, email: member.email, nickname: member.nickname, avatarUrl: null },
});

const invitation = (over: Partial<MockInvitation> & { id: number }): MockInvitation => ({
  status: "PENDING",
  inviteeEmail: "member@example.com",
  inviteeNickname: "팀원",
  role: "MEMBER",
  ...over,
});

const toResponse = (item: MockInvitation) => ({
  id: item.id,
  scopeType: "PROJECT",
  companyId: 1,
  companyName: "Threadia",
  projectId: 10,
  inviterUserId: 1,
  inviterNickname: "admin",
  inviteeUserId: 2,
  inviteeEmail: item.inviteeEmail,
  inviteeNickname: item.inviteeNickname,
  role: item.role,
  status: item.status,
  expiresAt: "2026-07-22T10:15:30Z",
});

async function mockApi(
  page: Page,
  options: {
    canManage: boolean;
    invitations?: MockInvitation[];
    members?: MockProjectMember[];
    /** invitable-members 응답을 갈아끼운다. 기본은 후보 1명 반환. */
    lookup?: { status: number; body: unknown };
  }
) {
  const invitations = options.invitations ?? [];
  const members = options.members ?? [
    projectMember({ id: 201, email: "admin@example.com", nickname: "관리자", companyRole: "COMPANY_ADMIN" }),
    projectMember({ id: 202, email: "member@example.com", nickname: "일반 멤버", companyRole: "MEMBER" }),
  ];
  const calls: string[] = [];

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

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10") {
      await json(
        apiResponse({
          project: {
            id: 10,
            companyId: 1,
            name: "P",
            projectKey: "THR",
            description: null,
            status: "ACTIVE",
            startDate: null,
            endDate: null,
            createdAt: "2026-07-01T00:00:00Z",
            updatedAt: "2026-07-01T00:00:00Z",
          },
          viewer: {
            companyRole: null,
            projectRole: options.canManage ? "PROJECT_ADMIN" : "MEMBER",
            canManageProject: options.canManage,
          },
        })
      );
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/project-members") {
      await json(apiResponse({ projectMembers: members.map(projectMemberResponse) }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/invitable-members") {
      calls.push(`lookup:${url.searchParams.get("email")}`);
      const stub = options.lookup;
      if (stub) {
        await json(stub.body, stub.status);
        return;
      }
      await json(apiResponse({ user: { id: 2, email: "member@example.com", nickname: "팀원", avatarUrl: null } }));
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/invitations") {
      await json(apiResponse({ invitations: invitations.map(toResponse) }));
      return;
    }

    if (request.method() === "POST" && url.pathname === "/api/v1/projects/10/invitations") {
      const body = request.postDataJSON();
      calls.push(`create:${JSON.stringify(body)}`);
      invitations.push(invitation({ id: 601, role: body.role }));
      await json(apiResponse({ invitation: toResponse(invitations[invitations.length - 1]) }), 201);
      return;
    }

    const cancelMatch = url.pathname.match(/^\/api\/v1\/invitations\/(\d+)\/cancel$/);
    if (request.method() === "POST" && cancelMatch) {
      const id = Number(cancelMatch[1]);
      calls.push(`cancel:${id}`);
      const target = invitations.find((i) => i.id === id);
      if (target) target.status = "CANCELED";
      await json(apiResponse({ invitationId: id, status: "CANCELED", membershipCreated: false }));
      return;
    }

    await route.abort();
  });

  return { calls, invitations };
}

test("회사 관리자인 프로젝트 멤버에만 뱃지를 표시한다", async ({ page }) => {
  await mockApi(page, { canManage: true });

  await page.goto("/1/10/settings");

  await expect(page.getByText("회사 관리자")).toHaveCount(1);
  await expect(page.locator("li").filter({ hasText: "admin@example.com" }).getByText("회사 관리자")).toBeVisible();
  await expect(page.locator("li").filter({ hasText: "member@example.com" }).getByText("회사 관리자")).toHaveCount(0);
});

test("이메일 조회 후 확인을 거쳐 초대를 생성한다", async ({ page }) => {
  const mock = await mockApi(page, { canManage: true });

  await page.goto("/1/10/settings");
  await page.getByLabel("초대할 이메일").fill("member@example.com");
  await page.getByLabel("초대 역할").selectOption("PROJECT_ADMIN");
  await page.getByRole("button", { name: "조회" }).click();

  // 후보 확인 단계를 거친다 — 조회만으로 초대가 생성되면 안 된다.
  await expect(page.getByText("초대할까요?")).toBeVisible();
  expect(mock.calls.filter((c) => c.startsWith("create:"))).toHaveLength(0);

  await page.getByRole("button", { name: "초대하기" }).click();
  await expect.poll(() => mock.calls).toContain('create:{"inviteeUserId":2,"role":"PROJECT_ADMIN"}');
  await expect(page.getByRole("heading", { name: "보낸 초대 (1)" })).toBeVisible();
});

test("후보가 아닌 이메일은 하나의 안내로 합쳐 보여준다", async ({ page }) => {
  // 서버는 미가입·비활성·다른 회사를 구분 없이 404로 준다(enumeration 방지 계약).
  await mockApi(page, {
    canManage: true,
    lookup: { status: 404, body: errorResponse("NOT_FOUND", "사용자를 찾을 수 없습니다.") },
  });

  await page.goto("/1/10/settings");
  await page.getByLabel("초대할 이메일").fill("ghost@example.com");
  await page.getByRole("button", { name: "조회" }).click();

  await expect(page.getByText("이 회사의 구성원 중에서 해당 이메일을 찾을 수 없습니다.")).toBeVisible();
  await expect(page.getByText("초대할까요?")).toHaveCount(0);
});

test("PENDING 초대만 취소할 수 있고 취소하면 상태가 바뀐다", async ({ page }) => {
  const mock = await mockApi(page, {
    canManage: true,
    invitations: [
      invitation({ id: 601, inviteeEmail: "pending@example.com" }),
      invitation({ id: 602, inviteeEmail: "accepted@example.com", status: "ACCEPTED" }),
    ],
  });

  await page.goto("/1/10/settings");
  await expect(page.getByRole("heading", { name: "보낸 초대 (2)" })).toBeVisible();

  // ACCEPTED에는 취소 버튼이 없다(서버도 409).
  await expect(page.getByRole("button", { name: "취소", exact: true })).toHaveCount(1);

  await page.getByRole("button", { name: "취소", exact: true }).click();
  await expect.poll(() => mock.calls).toContain("cancel:601");
  await expect(page.getByText("취소됨")).toBeVisible();
});

test("관리 권한이 없으면 초대 섹션이 보이지 않는다", async ({ page }) => {
  await mockApi(page, { canManage: false });

  await page.goto("/1/10/settings");
  await expect(page.getByRole("heading", { name: "프로젝트 멤버" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "멤버 초대" })).toHaveCount(0);
  await expect(page.getByLabel("초대할 이메일")).toHaveCount(0);
});
