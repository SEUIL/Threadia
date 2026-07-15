/*
 * 프로필 수정 모달 smoke — WBS 4.2.2, PR #102 리뷰 지적 사항의 회귀 테스트.
 *
 * board.spec.ts와 같은 방식으로 /api/v1/** 를 전부 route mock해서 백엔드 없이 돈다.
 * 레일(RailShell)이 붙는 회사 route로 진입해야 아바타 trigger가 있으므로 board를 쓴다.
 */
import { expect, test, type Page } from "@playwright/test";

const apiResponse = (data: unknown) => ({ data, meta: { requestId: "profile-smoke" } });

const USER = { id: 1, email: "a@test.dev", nickname: "A 사용자", avatarUrl: null as string | null };

// 1x1 투명 PNG. data: URI라 네트워크 없이 즉시 load 되어 성공 케이스가 안정적이다.
const VALID_IMAGE =
  "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
// 연결이 거부되는 주소 — onError를 확실히 태운다.
const BROKEN_IMAGE = "http://127.0.0.1:9/nope.png";

/** PATCH /users/me/profile의 request body를 기록한다. handler를 넘기면 응답을 갈아끼운다. */
async function mockApi(
  page: Page,
  onPatch?: (route: Parameters<Parameters<Page["route"]>[1]>[0], headers: Record<string, string>) => Promise<void>,
) {
  const patchBodies: unknown[] = [];
  const user = { ...USER };

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

    if (request.method() === "OPTIONS") {
      await route.fulfill({ status: 204, headers: corsHeaders });
      return;
    }

    if (request.method() === "POST" && url.pathname === "/api/v1/auth/refresh") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        headers: corsHeaders,
        body: JSON.stringify(apiResponse({ accessToken: "test-token", user })),
      });
      return;
    }

    if (request.method() === "PATCH" && url.pathname === "/api/v1/users/me/profile") {
      patchBodies.push(request.postDataJSON());
      if (onPatch) {
        await onPatch(route, corsHeaders);
        return;
      }
      Object.assign(user, request.postDataJSON());
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        headers: corsHeaders,
        body: JSON.stringify(apiResponse({ user })),
      });
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/users/me/company-memberships") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        headers: corsHeaders,
        body: JSON.stringify(apiResponse({ companyMemberships: [] })),
      });
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        headers: corsHeaders,
        body: JSON.stringify(apiResponse({ viewer: { companyRole: null, projectRole: "MEMBER", canManageProject: false } })),
      });
      return;
    }

    if (request.method() === "GET" && url.pathname === "/api/v1/projects/10/issues") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        headers: corsHeaders,
        body: JSON.stringify(apiResponse({ issues: [] })),
      });
      return;
    }

    await route.abort();
  });

  return { patchBodies };
}

const profileTrigger = (page: Page) => page.getByRole("button", { name: /프로필 수정/ }).first();

async function openModal(page: Page) {
  await page.goto("/1/10/board");
  const trigger = profileTrigger(page);
  await expect(trigger).toBeVisible();
  await trigger.click();
  await expect(page.getByRole("dialog")).toBeVisible();
  return trigger;
}

test("모달을 열면 focus가 안으로 들어오고, Tab이 갇히고, 닫으면 trigger로 돌아온다", async ({ page }) => {
  await mockApi(page);
  const trigger = await openModal(page);

  // 열림 직후 focus가 모달 안 첫 입력으로 이동한다.
  await expect(page.locator("#profile-nickname")).toBeFocused();

  // Tab을 충분히 눌러도 배경으로 새지 않는다(모달 내부 순환).
  for (let i = 0; i < 8; i += 1) {
    await page.keyboard.press("Tab");
    expect(await page.evaluate(() => !!document.activeElement?.closest("[role=dialog]"))).toBe(true);
  }

  // 닫으면 열었던 trigger로 focus가 복원된다.
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(trigger).toBeFocused();
});

test("focus가 배경에 있어도 Tab이 모달 안으로 끌어온다", async ({ page }) => {
  await mockApi(page);
  await openModal(page);

  // 배경 요소로 focus를 강제로 옮긴 상황을 만든다.
  await page.evaluate(() => {
    const outside = [...document.querySelectorAll<HTMLElement>("button")].find(
      (b) => !b.closest("[role=dialog]"),
    );
    outside?.focus();
  });
  expect(await page.evaluate(() => !!document.activeElement?.closest("[role=dialog]"))).toBe(false);

  await page.keyboard.press("Tab");
  expect(await page.evaluate(() => !!document.activeElement?.closest("[role=dialog]"))).toBe(true);
});

test("깨진 avatar URL 뒤에 유효한 URL을 넣으면 이미지가 다시 렌더된다", async ({ page }) => {
  await mockApi(page);
  await openModal(page);

  const dialog = page.getByRole("dialog");
  const avatarField = page.locator("#profile-avatar-url");

  await avatarField.fill(BROKEN_IMAGE);
  // 로드 실패 → 이니셜 폴백.
  await expect(dialog.locator("img")).toHaveCount(0);

  await avatarField.fill(VALID_IMAGE);
  // 실패 상태가 이전 URL에만 묶여 있으므로 새 URL은 정상 렌더된다.
  await expect(dialog.locator("img")).toHaveCount(1);
});

test("바뀐 필드만 PATCH body에 담기고, 변경이 없으면 요청하지 않는다", async ({ page }) => {
  const { patchBodies } = await mockApi(page);
  await openModal(page);

  // 닉네임만 변경 → nickname만 전송(avatarUrl 생략 = 서버에서 기존 값 보존).
  await page.locator("#profile-nickname").fill("바뀐 이름");
  await page.getByRole("button", { name: "저장" }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect.poll(() => patchBodies).toEqual([{ nickname: "바뀐 이름" }]);

  // 아무것도 바꾸지 않고 저장 → 빈 body는 서버가 400이므로 아예 호출하지 않는다.
  await profileTrigger(page).click();
  await page.getByRole("button", { name: "저장" }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  expect(patchBodies).toHaveLength(1);
});

test("error envelope이 아닌 5xx 응답에서도 기본 오류 메시지를 보여준다", async ({ page }) => {
  await mockApi(page, async (route, corsHeaders) => {
    // proxy/gateway가 끼면 JSON이 아니라 HTML이 올 수 있다.
    await route.fulfill({
      status: 502,
      contentType: "text/html",
      headers: corsHeaders,
      body: "<html><body>502 Bad Gateway</body></html>",
    });
  });
  await openModal(page);

  await page.locator("#profile-nickname").fill("오류 유발");
  await page.getByRole("button", { name: "저장" }).click();

  await expect(page.getByText("프로필 수정에 실패했습니다. 잠시 후 다시 시도해주세요.")).toBeVisible();
  await expect(page.getByRole("dialog")).toBeVisible();
});
