/*
 * next.config.ts가 output:"standalone"이라 `next build`가 만드는
 * .next/standalone/server.js는 정적 자산(.next/static, public)을 따로 갖고
 * 있지 않다(Next.js 공식 가이드에 명시된 동작) — 배포 시 이 스크립트처럼
 * 직접 복사해줘야 한다. playwright.production.config.ts의 webServer.command가
 * 빌드 직후 이 스크립트를 호출한다.
 *
 * ⚠️ fs.cpSync(recursive)를 처음 썼는데, 이 저장소 경로(OneDrive + 한글 폴더명
 * "바탕 화면")의 Windows 로컬 개발 환경에서 Node가 재귀 복사 도중 매번
 * STATUS_STACK_BUFFER_OVERRUN으로 죽었다(exit code 3221226505, 원인 미상 —
 * Node fs 재귀 복사와 OneDrive 파일 시스템 필터 드라이버의 상호작용으로 추정).
 * Windows에서는 바로 이 시나리오(대량 파일 복사, 긴 한글 경로)를 위해 만들어진
 * robocopy를 쓰고, CI(ubuntu-latest, 이 문제가 없음)에서는 표준 `cp -r`을 쓴다.
 */
/* eslint-disable @typescript-eslint/no-require-imports -- 앱 TS 번들이 아니라
   `node`로 직접 실행하는 순수 CJS 스크립트라 require를 쓴다. */
const { spawnSync } = require("child_process");
const path = require("path");

const root = path.join(__dirname, "..");
const isWindows = process.platform === "win32";

function copyDir(src, dest) {
  if (isWindows) {
    // robocopy 종료 코드는 비트마스크다 — 0~7이면 성공(파일 복사/스킵 등), 8
    // 이상만 진짜 실패다. 일반적인 "0이 아니면 실패" 관례를 그대로 쓰면 안 된다.
    const result = spawnSync("robocopy", [src, dest, "/E", "/NFL", "/NDL", "/NJH", "/NJS"], {
      cwd: root,
      stdio: "inherit",
    });
    if ((result.status ?? 0) >= 8) {
      console.error(`robocopy failed (${src} -> ${dest}): exit code ${result.status}`);
      process.exit(result.status);
    }
    return;
  }
  const result = spawnSync("cp", ["-r", src, dest], { cwd: root, stdio: "inherit" });
  if (result.status !== 0) {
    console.error(`cp failed (${src} -> ${dest}): exit code ${result.status}`);
    process.exit(result.status ?? 1);
  }
}

copyDir(path.join(".next", "static"), path.join(".next", "standalone", ".next", "static"));
copyDir("public", path.join(".next", "standalone", "public"));
