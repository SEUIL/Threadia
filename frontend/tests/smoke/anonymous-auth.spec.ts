import { expect, test } from "@playwright/test";

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
  await expect(page.getByRole("heading", { name: "로그인" })).toBeVisible();
  await expect(page.getByLabel("이메일")).toBeVisible();
  await expect(page.getByRole("textbox", { name: "비밀번호", exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "회원가입" })).toBeVisible();

  await page.goto("/signup");
  await expect(page.getByRole("heading", { name: "회원가입" })).toBeVisible();
  await expect(page.getByLabel("이메일")).toBeVisible();
  await expect(page.getByLabel("닉네임")).toBeVisible();
  await expect(page.getByRole("button", { name: "계정 만들기" })).toBeVisible();

  await page.goto("/forgot-password");
  await expect(page.getByRole("heading", { name: "비밀번호 찾기" })).toBeVisible();
  await expect(page.getByLabel("이메일")).toBeVisible();
  await expect(page.getByRole("button", { name: "재설정 링크 받기" })).toBeVisible();
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
