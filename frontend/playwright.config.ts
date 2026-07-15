import { defineConfig } from "@playwright/test";

const defaultBaseUrl = "http://127.0.0.1:3001";
const suppliedBaseUrl = process.env.PLAYWRIGHT_BASE_URL;

export default defineConfig({
  testDir: "./tests/smoke",
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  reporter: "list",
  use: {
    baseURL: suppliedBaseUrl ?? defaultBaseUrl,
    browserName: "chromium",
    headless: true,
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  projects: [
    {
      name: "chromium",
    },
  ],
  webServer: suppliedBaseUrl
    ? undefined
    : {
        command: "npm run dev -- --hostname 127.0.0.1 --port 3001",
        url: `${defaultBaseUrl}/login`,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
});
