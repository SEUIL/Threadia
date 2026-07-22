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

test("redirects to login after the backend rejects an invalid reset token", async ({ page }) => {
  await page.route("**/api/v1/auth/password-reset/confirm", async (route) => {
    if (route.request().method() === "OPTIONS") {
      await route.fulfill({
        status: 204,
        headers: {
          "access-control-allow-origin": "http://127.0.0.1:3001",
          "access-control-allow-methods": "POST",
          "access-control-allow-headers": "content-type",
          "access-control-allow-credentials": "true",
        },
      });
      return;
    }

    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({ error: { message: "유효하지 않거나 만료된 재설정 링크입니다" } }),
      headers: {
        "access-control-allow-origin": "http://127.0.0.1:3001",
        "access-control-allow-credentials": "true",
      },
    });
  });

  await page.route("**/api/v1/auth/refresh", async (route) => {
    if (route.request().method() === "OPTIONS") {
      await route.fulfill({
        status: 204,
        headers: {
          "access-control-allow-origin": "http://127.0.0.1:3001",
          "access-control-allow-methods": "POST",
          "access-control-allow-headers": "content-type",
          "access-control-allow-credentials": "true",
        },
      });
      return;
    }

    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({ error: { message: "세션이 만료되었습니다" } }),
      headers: {
        "access-control-allow-origin": "http://127.0.0.1:3001",
        "access-control-allow-credentials": "true",
      },
    });
  });

  await page.goto("/reset-password?token=bad");
  await expect(page.getByRole("heading", { name: "새 비밀번호 설정" })).toBeVisible();

  await page.getByRole("textbox", { name: "새 비밀번호", exact: true }).fill("password123");
  await page.getByRole("textbox", { name: "새 비밀번호 확인", exact: true }).fill("password123");
  await page.getByRole("button", { name: "비밀번호 변경하기" }).click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("heading", { name: "로그인" })).toBeVisible();
});
