// 레이아웃 구조 실험(/layouts) 문서용 스크린샷 캡처 스크립트.
// 일회성 문서 작업 도구 — 필요할 때 `node scripts/capture-screenshots.mjs` 로 재실행한다.
import { chromium } from "playwright";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const outDir = path.resolve(
  __dirname,
  "../../../docs/08-members/member-정철진/기술-연구/레이아웃-구조-실험-이미지"
);
const baseUrl = "http://localhost:3001/layouts";

async function clickByText(page, text) {
  await page.getByRole("button", { name: text, exact: false }).first().click();
}

async function run() {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto(baseUrl, { waitUntil: "networkidle" });

  // A. 커맨드 레일 — 리스트 기본 화면
  await clickByText(page, "A. 커맨드 레일");
  await page.waitForTimeout(300);
  await page.screenshot({ path: path.join(outDir, "a-command-rail-list.png") });

  // A. 커맨드 레일 — 레일 hover 플라이아웃
  await page.hover('[title="허브"]');
  await page.waitForTimeout(400);
  await page.screenshot({ path: path.join(outDir, "a-command-rail-flyout.png") });

  // A. 커맨드 레일 — 이슈 클릭 후 오버레이 디테일
  await page.mouse.move(700, 500); // 레일에서 마우스 치우기
  await page.waitForTimeout(300);
  await clickByText(page, "로그인 리다이렉트 버그");
  await page.waitForTimeout(400);
  await page.screenshot({ path: path.join(outDir, "a-command-rail-detail.png") });

  // C. 그래프 캔버스 — 기본 화면
  await clickByText(page, "C. 그래프 캔버스");
  await page.waitForTimeout(300);
  await page.screenshot({ path: path.join(outDir, "c-graph-canvas.png") });

  // D. 레일 + 그래프 — 기본(필터 전체 on)
  await clickByText(page, "D. 레일 + 그래프");
  await page.waitForTimeout(300);
  await page.screenshot({ path: path.join(outDir, "d-rail-graph-default.png") });

  // D. 레일 + 그래프 — 문서 필터 off + 검색 "허브" 적용
  await clickByText(page, "문서");
  const search = page.getByPlaceholder("제목으로 검색");
  await search.fill("허브");
  await page.waitForTimeout(300);
  await page.screenshot({ path: path.join(outDir, "d-rail-graph-filtered.png") });

  // 필터/검색 원복 + 노드 클릭으로 상세 카드 확인
  await clickByText(page, "문서");
  await search.fill("");
  await page.waitForTimeout(200);
  await clickByText(page, "로그인 리다이렉트 버그");
  await page.waitForTimeout(400);
  await page.screenshot({ path: path.join(outDir, "d-rail-graph-node-detail.png") });

  await browser.close();
  console.log("Saved screenshots to", outDir);
}

run();
