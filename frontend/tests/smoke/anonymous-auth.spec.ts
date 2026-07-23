import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.route("**/api/v1/auth/refresh", async (route) => {
    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({ error: { message: "세션이 만료되었습니다" } }),
    });
  });
});

test("redirects anonymous entry routes to login", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("heading", { name: "로그인" })).toBeVisible();

  await page.goto("/hub");
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("heading", { name: "로그인" })).toBeVisible();
});

test("renders anonymous auth forms", async ({ page }) => {
  await page.goto("/login");
  await expect(page).toHaveTitle("로그인 | Threadia");
  await expect(page.getByRole("heading", { name: "로그인" })).toBeVisible();
  await expect(page.getByLabel("이메일")).toBeVisible();
  await expect(page.getByRole("textbox", { name: "비밀번호", exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "회원가입" })).toBeVisible();

  await page.goto("/signup");
  await expect(page).toHaveTitle("회원가입 | Threadia");
  await expect(page.getByRole("heading", { name: "회원가입" })).toBeVisible();
  await expect(page.getByLabel("이메일")).toBeVisible();
  await expect(page.getByLabel("닉네임")).toBeVisible();
  await expect(page.getByRole("textbox", { name: "비밀번호 확인", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "계정 만들기" })).toBeVisible();

  await page.goto("/forgot-password");
  await expect(page).toHaveTitle("비밀번호 찾기 | Threadia");
  await expect(page.getByRole("heading", { name: "비밀번호 찾기" })).toBeVisible();
  await expect(page.getByLabel("이메일")).toBeVisible();
  await expect(page.getByRole("button", { name: "재설정 링크 받기" })).toBeVisible();
});

test("shows invalid login feedback without refreshing or attempting token refresh", async ({ page }) => {
  let loginRequests = 0;
  let refreshRequests = 0;

  await page.route("**/api/v1/auth/refresh", async (route) => {
    refreshRequests += 1;
    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({ error: { message: "세션이 만료되었습니다" } }),
    });
  });
  await page.route("**/api/v1/auth/login", async (route) => {
    loginRequests += 1;
    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({ error: { message: "이메일 또는 비밀번호가 올바르지 않습니다." } }),
    });
  });

  await page.goto("/login");
  await expect(page.getByRole("heading", { name: "로그인" })).toBeVisible();
  await expect.poll(() => refreshRequests).toBe(1);
  const refreshRequestsBeforeLogin = refreshRequests;

  await page.getByLabel("이메일").fill("wrong@example.com");
  await page.getByRole("textbox", { name: "비밀번호", exact: true }).fill("wrong-password");
  await page.getByRole("button", { name: "로그인" }).click();

  await expect(page.getByText("이메일 또는 비밀번호가 올바르지 않습니다.", { exact: true })).toBeVisible();
  await expect(page.getByLabel("이메일")).toHaveValue("wrong@example.com");
  await expect(page).toHaveURL(/\/login$/);
  expect(loginRequests).toBe(1);
  expect(refreshRequests).toBe(refreshRequestsBeforeLogin);
});

test("clicking login rapidly while a request is in flight only sends one request", async ({ page }) => {
  // 이슈 #236 완료조건 — "로그인 연속 클릭이 중복 요청을 만들지 않는다."
  // Button의 loading={isSubmitting}이 이미 막고 있지만(react-hook-form이
  // 제출 시작과 동시에 동기적으로 isSubmitting을 true로 바꿔 버튼이 disabled됨),
  // 이 흐름 자체를 검증하는 테스트가 없었다. 응답을 hold해서 "요청이 진행
  // 중인 동안" 연타해도 실제로는 한 번만 나가는지 확인한다.
  let loginRequests = 0;
  let releaseLogin: () => void = () => {};
  const loginGate = new Promise<void>((resolve) => {
    releaseLogin = resolve;
  });

  await page.route("**/api/v1/auth/login", async (route) => {
    loginRequests += 1;
    await loginGate;
    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({ error: { message: "이메일 또는 비밀번호가 올바르지 않습니다." } }),
    });
  });

  await page.goto("/login");
  await page.getByLabel("이메일").fill("wrong@example.com");
  await page.getByRole("textbox", { name: "비밀번호", exact: true }).fill("wrong-password");

  // Button은 loading 중 텍스트를 "처리 중…"으로 바꾸므로(components/ui/Button.tsx),
  // name: "로그인"으로 찾는 role locator는 제출 시작 후 더 이상 매치되지 않는다.
  // type="submit"으로 찾아 제출 전후 동일한 요소를 가리키게 한다.
  const loginButton = page.locator('button[type="submit"]');
  await loginButton.click();
  await expect(loginButton).toBeDisabled();
  // 버튼이 비활성화된 뒤에도 연타해본다 — disabled 버튼 클릭은 이벤트가 아예
  // 안 붙지만, 그 사실 자체를 실측으로 확인하는 것이 이 테스트의 목적이다.
  await loginButton.click({ force: true });
  await loginButton.click({ force: true });

  releaseLogin();
  await expect(page.getByText("이메일 또는 비밀번호가 올바르지 않습니다.", { exact: true })).toBeVisible();
  expect(loginRequests).toBe(1);
});

test("prevents signup when password confirmation does not match", async ({ page }) => {
  let signupRequests = 0;
  await page.route("**/api/v1/auth/signup", async (route) => {
    signupRequests += 1;
    await route.abort();
  });

  await page.goto("/signup");
  await page.getByLabel("이메일").fill("qa@example.com");
  await page.getByLabel("닉네임").fill("QA 사용자");
  await page.getByRole("textbox", { name: "비밀번호", exact: true }).fill("password123");
  await page.getByRole("textbox", { name: "비밀번호 확인", exact: true }).fill("password124");
  await page.getByRole("button", { name: "계정 만들기" }).click();

  await expect(page.getByText("비밀번호가 일치하지 않습니다")).toBeVisible();
  expect(signupRequests).toBe(0);
});

test("renders a branded Korean not-found page with a recovery link", async ({ page }) => {
  const response = await page.goto("/qa-definitely-not-found");
  expect(response?.status()).toBe(404);
  await expect(page.getByRole("heading", { name: "페이지를 찾을 수 없습니다" })).toBeVisible();
  await expect(page.getByRole("link", { name: "로그인으로 돌아가기" })).toHaveAttribute("href", "/login");
});

test("shows an invalid reset token error inline without redirecting or attempting token refresh", async ({ page }) => {
  // 리뷰 관찰사항(PR #238, 이슈 #236) 반영 — password-reset/confirm의 401도
  // 로그인과 같은 근본 원인(비즈니스 로직 실패를 세션 만료로 오분류)이었다.
  // 예전엔 이 401이 refresh 시도 → refresh도 401 → /login 강제 리다이렉트로
  // 이어져서, 화면이 준비한 "유효하지 않거나 만료된 재설정 링크입니다" 메시지를
  // 사용자가 볼 수 없었다. 이제는 리다이렉트/refresh 시도 없이 인라인으로 보여야 한다.
  let confirmRequests = 0;
  let refreshRequests = 0;

  await page.route("**/api/v1/auth/refresh", async (route) => {
    refreshRequests += 1;
    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({ error: { message: "세션이 만료되었습니다" } }),
    });
  });
  await page.route("**/api/v1/auth/password-reset/confirm", async (route) => {
    confirmRequests += 1;
    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({ error: { message: "유효하지 않거나 만료된 재설정 링크입니다" } }),
    });
  });

  await page.goto("/reset-password?token=bad");
  await expect(page).toHaveTitle("비밀번호 재설정 | Threadia");
  await expect(page.getByRole("heading", { name: "새 비밀번호 설정" })).toBeVisible();
  await expect.poll(() => refreshRequests).toBe(1);
  const refreshRequestsBeforeSubmit = refreshRequests;

  await page.getByRole("textbox", { name: "새 비밀번호", exact: true }).fill("password123");
  await page.getByRole("textbox", { name: "새 비밀번호 확인", exact: true }).fill("password123");
  await page.getByRole("button", { name: "비밀번호 변경하기" }).click();

  await expect(page.getByText("유효하지 않거나 만료된 재설정 링크입니다", { exact: true })).toBeVisible();
  await expect(page).toHaveURL(/\/reset-password/);
  expect(confirmRequests).toBe(1);
  expect(refreshRequests).toBe(refreshRequestsBeforeSubmit);
});
