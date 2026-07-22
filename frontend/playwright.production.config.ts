import { defineConfig } from "@playwright/test";

/*
 * production 빌드 전용 Playwright 설정 — playwright.config.ts(dev 서버, Strict
 * Mode 이중 mount)와 분리한다.
 *
 * tests/smoke-production/의 테스트들은 "컴포넌트가 정확히 한 번만 mount되는"
 * 상황을 전제로 요청 순서를 특정한다(예: DmRoomPage의 자체 조회가 DmLayout의
 * rail 조회보다 항상 먼저 시작된다 — React가 자식 effect를 부모보다 먼저
 * 실행하는 규칙은 결정적이다). dev 서버는 React Strict Mode가 mount effect를
 * 두 번 실행해 이 순서를 예측할 수 없게 만들어서(PR #173 4차 재리뷰, pktewon:
 * blocker 5) 같은 테스트를 production 빌드로 돌린다.
 */
const baseUrl = "http://127.0.0.1:3002";

export default defineConfig({
  testDir: "./tests/smoke-production",
  // production 서버 하나를 여러 테스트가 공유한다 — 서버 자체의 mount 순서
  // 보장과는 무관하지만, 라우트 mock끼리 섞이지 않도록 직렬로 돈다.
  fullyParallel: false,
  workers: 1,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  reporter: "list",
  use: {
    baseURL: baseUrl,
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
  webServer: {
    // next.config.ts가 output:"standalone"이라 `next start`(=npm run start)는
    // 못 쓴다("next start" does not work with "output: standalone" 경고 그대로
    // 뜨고 서버가 정상 기동하지 않는다) — standalone 산출물(.next/standalone/
    // server.js)을 직접 실행해야 한다.
    //
    // ⚠️ 빌드(`npm run build`)는 이 webServer.command 안에서 실행하지 않는다 —
    // Playwright가 spawn한 프로세스 밑에서 다시 npm(.cmd)을 spawn해 next build를
    // 돌리면 Windows에서 결정적으로 크래시했다(exit code 3221226505 =
    // STATUS_STACK_BUFFER_OVERRUN, 셸 체이닝으로 묶어도 wrapper 스크립트로
    // 감싸도 동일 — 같은 빌드를 커맨드라인에서 직접 실행하면 재현되지 않는 걸
    // 보면 "Playwright가 띄운 프로세스의 자식으로 next build가 도는 상황" 자체가
    // 원인으로 보인다). 그래서 빌드+정적 자산 복사는 `npm run test:smoke:production`
    // (package.json)에서 playwright보다 먼저, 별도 단계로 실행하고, 여기서는
    // 이미 빌드된 standalone 서버를 기동만 한다.
    command: "node .next/standalone/server.js",
    env: { PORT: "3002", HOSTNAME: "127.0.0.1" },
    url: `${baseUrl}/login`,
    reuseExistingServer: !process.env.CI,
    timeout: 180_000,
  },
});
