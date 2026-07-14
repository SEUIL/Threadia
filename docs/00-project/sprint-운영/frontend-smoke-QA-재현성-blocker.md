> **작성자**: Threadia 팀
> **작성일**: 2026-07-13
> **최종 수정**: 2026-07-13
> **버전**: v1.2
> **상태**: 검토 중
> **태그**: frontend, playwright, smoke, qa, reproducibility

# Frontend smoke QA 재현성 blocker

## 1. 목적

이 문서는 local frontend browser/smoke QA를 재현할 때 확인된 실행 blocker와, blocker 상태에서도 수행한 안전한 검증 범위를 기록한다. AWS, 외부 시스템, secret, 실행 중인 다른 프로세스는 변경하지 않는다.

## 2. 실행 결과

| 항목 | 명령 또는 절차 | 결과 | 근거 |
|---|---|---|---|
| Frontend lint | `npm run lint` | PASS | exit code 0 |
| Frontend build | `npm run build` | PASS | Next.js build와 TypeScript 검사 완료 |
| 기존 local auth route | `curl -L`로 `/`, `/hub` 확인 | PASS (HTTP 200, 최종 URL `/login`) | 기존 port 3000 dev server 응답 |
| 기존 local auth form route | `curl`로 `/login`, `/signup`, `/forgot-password` 확인 | PASS (각 HTTP 200) | 기존 port 3000 dev server 응답 |
| local backend precondition | `/actuator/health`, invalid reset-confirm 요청 확인 | PASS (각 HTTP 200, HTTP 401) | port 8080 응답 |
| Playwright smoke suite | isolated worktree에서 `PLAYWRIGHT_BROWSERS_PATH=/tmp/opencode/playwright-browsers npm run test:smoke` | PASS (3 tests, 26.9s) | anonymous redirect, auth form, invalid reset-token 흐름 통과 |

## 3. Blocker

### BROWSER-QA-01: Next dev lock으로 전용 Playwright server를 기동할 수 없음 (격리 실행으로 해소)

`frontend/playwright.config.ts`는 `127.0.0.1:3001`에서 `next dev`를 새로 실행하고 `reuseExistingServer: false`를 사용한다. 실행 시 기존 `next dev` 프로세스가 `frontend/.next/dev` lock을 점유하고 있어, Playwright webServer가 다음 오류로 종료된다.

```text
Another next dev server is already running.
Local: http://localhost:3000
PID: 2631666
Dir: /home/ubuntu/dev/Threadia/frontend
Run kill 2631666 to stop it.
Error: Process from config.webServer was not able to start. Exit code: 1
```

이 기록은 해당 PID를 종료하지 않는다. 실행 중인 프로세스의 소유와 용도를 확인할 수 없기 때문이다. `/tmp/opencode/threadia-browser-qa` detached worktree에서는 독립 `.next` cache를 사용해 이 lock을 우회했고, 동일 smoke suite가 통과했다.

### BROWSER-QA-02: managed Chromium 배포본이 local cache에 없음 (격리 cache 설치로 해소)

초기 no-install QA 단계에서 local Playwright browser cache는 비어 있었다. Playwright browser automation은 `/opt/google/chrome/chrome`에서 Chrome 배포본을 찾았으나 존재하지 않아 초기화하지 못했다. `npx playwright install --dry-run chromium`은 필요한 Chromium과 headless shell의 download URL 및 cache 설치 경로만 출력했고, 브라우저 파일이 준비되어 있지 않음을 확인했다. 당시 설치 금지 조건에 따라 download 또는 install은 수행하지 않았다.

이후 별도 승인된 isolated QA 단계에서만 version-matched Chromium, headless shell, FFmpeg를 `/tmp/opencode/playwright-browsers`에 설치했다. 이 cache를 사용해 isolated smoke suite를 실행했고 세 DOM assertion 흐름은 모두 통과했다. suite 실행 중 Next.js가 `scroll-behavior: smooth` 관련 warning을 출력했다. 기본 smoke reporter는 browser console event를 별도로 수집하지 않으므로 browser console error 유무는 이 실행에서 판정하지 않았다.

## 4. 최소 안전 remediation

1. 기존 worktree에 다른 `next dev`가 실행 중이면 clean local checkout 또는 독립 `.next` cache에서 `npm run test:smoke`를 실행한다. 실행 중인 port 3000 dev server는 QA 작업자가 종료하지 않는다.
2. project의 Playwright version(`@playwright/test` 1.58.2)에 맞는 Chromium browser artifact를 준비한다.
3. smoke config의 base URL과 CORS origin은 `127.0.0.1:3001`이다. 다른 port 3002에서 password-reset browser 흐름을 수동 관찰한 결과 login redirect 대기가 timeout되었다. 이 관찰은 smoke config와 다른 origin에서의 결과이므로 suite 판정에는 포함하지 않고, browser console/network 증빙이 필요하면 config와 같은 port 3001에서 event listener를 둔 harness를 실행한다.

## 5. 범위 및 결론

`frontend/tests/smoke/anonymous-auth.spec.ts`의 세 test는 isolated worktree와 `/tmp/opencode` Playwright cache에서 모두 통과했다. 기존 worktree에서는 BROWSER-QA-01과 BROWSER-QA-02 때문에 browser smoke를 재현할 수 없지만, project code 또는 AWS를 변경하지 않는 격리 실행 경로는 재현 가능하다.
