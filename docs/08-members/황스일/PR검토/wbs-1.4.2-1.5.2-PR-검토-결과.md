> **작성자**: 황스일
> **작성일**: 2026-07-05
> **최종 수정**: 2026-07-05
> **버전**: v1.0
> **상태**: 검토 중
> **태그**: pr-review, auth, frontend, qa, wbs

# WBS 1.4.2 / 1.5.2 PR 검토 결과

## 검토 범위

- WBS 1.4.2: 로그인/회원가입 화면 API 연동.
- WBS 1.5.2: Sprint 1 Day 3 QA 버그 수정 / 버퍼.
- GitHub open PR 목록은 비어 있었다. 따라서 GitHub review action은 추가로 남기지 않고, 이미 merge된 PR에 대한 사후 검토로 정리했다.
- WBS 1.4.2는 PR 본문 검색 기준으로 PR #34가 직접 매핑되지만, WBS 작업명 기준 실제 로그인/회원가입 API 연동은 PR #25 안에 포함되어 있었다. 그래서 1.4.2 검토에는 PR #25 auth 관련 변경과 PR #34 password reset frontend 연동을 함께 포함했다.

## 종합 판단

| WBS | PR | GitHub 상태 | 검토 판단 | 후속 필요 |
|---|---|---|---|---|
| 1.4.2 | PR #25 `feat: 전 화면(S04-S14) 골격 구현 및 인증 백엔드 연동` | MERGED | 보완 필요 | 공통 401 interceptor가 public auth API 실패를 가로채는 문제 수정 필요 |
| 1.4.2 / 1.4.1 연장 | PR #34 `feat(auth): 비밀번호 찾기/재설정 화면 실제 백엔드 연동` | MERGED | 보완 필요 | 만료/무효 reset token 처리 claim이 실제 동작과 충돌 가능 |
| 1.5.2 | PR #35 `docs(qa): Sprint 1 Day 3 QA 버그 수정 및 버퍼 검증 결과 문서 추가` | MERGED / APPROVED | 내용 근거는 대체로 재현됨 | WBS 완료 조건, 문서 형식, frontend build 실패 후속 정리 필요 |

## 1.4.2 검토

### PR #25

확인한 주요 파일:

- `frontend/app/(auth)/login/page.tsx`
- `frontend/app/(auth)/signup/page.tsx`
- `frontend/contexts/AuthContext.tsx`
- `frontend/app/(app)/layout.tsx`
- `frontend/components/hub/HubOverlay.tsx`
- `frontend/lib/axios.ts`

긍정 확인:

- 로그인/회원가입 화면이 `react-hook-form`, `zod`, `authService.login()`, `authService.signup()`으로 실제 API 호출을 수행한다.
- `AuthContext`가 refresh cookie 기반 세션 복원을 담당하고, `(app)` route group은 인증 상태에 따라 `/login`으로 보호된다.
- 변경된 auth 관련 파일의 LSP diagnostics는 확인되지 않았다.

리뷰 발견사항:

| 심각도 | 위치 | 내용 | 판단 |
|---|---|---|---|
| High | `frontend/lib/axios.ts` | 공통 response interceptor가 모든 401을 refresh 시도 대상으로 처리한다. 로그인 실패처럼 public auth API의 401도 interceptor가 먼저 가로챈다. | invalid credential 등 page-level error 처리가 안정적으로 동작하지 않는다. public auth endpoint는 interceptor opt-out이 필요하다. |
| Medium | `frontend/app/(auth)/login/page.tsx` | 로그인 실패 시 `catch`에서 `err.response.data.error.message`를 표시하려 하지만, 401이 interceptor에서 refresh/login redirect로 처리될 수 있다. | 사용자에게 backend error message가 아니라 generic error 또는 redirect가 발생할 수 있다. |
| Low | 전체 frontend | `npm ci` 결과 moderate vulnerability 2건이 보고되었다. | PR 직접 변경에서 유입됐는지는 별도 확인 필요하지만 follow-up audit 대상으로 기록한다. |

### PR #34

확인한 주요 파일:

- `frontend/app/(auth)/forgot-password/page.tsx`
- `frontend/app/(auth)/reset-password/page.tsx`
- `frontend/services/auth.service.ts`
- `frontend/lib/axios.ts`

긍정 확인:

- forgot-password 화면은 이메일 존재 여부를 노출하지 않는 backend 정책에 맞춰 성공 응답 시 항상 안내 화면으로 전환한다.
- reset-password 화면은 `token` query parameter를 읽고, 새 비밀번호와 확인값 일치 검사를 추가했다.
- PR #34 변경 파일의 LSP diagnostics는 확인되지 않았다.

리뷰 발견사항:

| 심각도 | 위치 | 내용 | 판단 |
|---|---|---|---|
| High | `frontend/app/(auth)/reset-password/page.tsx`, `frontend/lib/axios.ts` | PR 본문은 만료/무효 token 401을 화면에서 처리한다고 설명하지만, 공통 axios interceptor가 401을 먼저 refresh/login redirect 대상으로 처리한다. refresh cookie가 없는 사용자는 reset token 실패 시 page-level `catch`가 의도대로 실행되지 않는다. | PR #34의 핵심 실패 시나리오 claim과 충돌한다. `passwordResetConfirm` 또는 public auth endpoint에 `skipAuthRefresh` 계열 opt-out이 필요하다. refresh 실패 후에는 원래 error를 reject해야 한다. |
| Medium | `frontend/app/(auth)/reset-password/page.tsx` | reset token을 query parameter에서 읽고 성공 후 `done` 화면으로만 전환한다. | one-time token이라도 보안 민감 값이므로 성공 후 token 없는 URL로 `replace`하는 처리가 안전하다. |
| Medium | PR 검증 기록 | PR 본문에 실제 SMTP credential 부재로 email 발송부터 링크 클릭까지 E2E를 검증하지 못했다고 적혀 있다. | merge 전 reviewer 재검증이 필요했던 항목이다. 사후 기준으로도 실제 email 수신 E2E evidence가 별도로 필요하다. |
| Medium | 전체 frontend | PR #25, #34 head 모두 `npm run build`가 `backlog/page.tsx:50:25` TypeScript 오류로 실패한다. | 오류 위치는 password reset 변경 파일은 아니지만, PR merge gate 관점에서는 production build 차단 상태다. |

Oracle 검토 결과도 위 401 interceptor 문제를 false positive가 아니라 High / Blocking 성격의 실제 결함으로 판단했다.

## 1.5.2 검토

### PR #35

확인한 파일:

- `docs/08-members/member-김민경/Sprint1_Day3_QA_버그수정_버퍼_검증결과.md`

긍정 확인:

- 문서에 적힌 backend test 통과, frontend lint warning 2건, frontend build 실패 내용은 PR #35 head 기준으로 재현되었다.
- PR #35는 코드 수정이 아니라 QA/버퍼 검증 결과 문서화 PR이며, 문서 본문도 현재 시점에 코드 수정 없이 검증 결과만 기록했다고 명시한다.
- PR에는 여도현 reviewer의 approval이 남아 있다.

리뷰 발견사항:

| 심각도 | 위치 | 내용 | 판단 |
|---|---|---|---|
| Medium | `docs/08-members/member-김민경/Sprint1_Day3_QA_버그수정_버퍼_검증결과.md` | 새 문서인데 `docs/00-project/문서-작성-규칙.md`의 상단 메타정보 블록이 없다. | 후속 문서 정정 필요. |
| Low | 파일명 | `Sprint1_Day3_QA_버그수정_버퍼_검증결과.md`는 underscore와 혼합 표기를 사용한다. | 문서 규칙의 kebab-case 기준과 맞지 않는다. 후속 rename 또는 다음 문서부터 규칙 준수가 필요하다. |
| Low | 문서 13행 | 문서는 `진행중 50%`로 기록되어 있으나 WBS 현재값은 1.5.2 `진행중 90%`로 확인된다. | WBS 갱신 후 상태값이 달라진 것으로 보이며, 현재 상태 기준 보고서라면 동기화가 필요하다. |
| High | WBS 완료 판단 | 1.5.2는 `버그 수정 / 버퍼`이고 완료 조건은 `통합 테스트 이슈 수정`, 검증 방법은 `수정 대상별 재검증`이다. PR #35는 build 실패를 문서화했을 뿐 실제 frontend build 실패를 수정하지 않는다. | PR #35 자체는 QA evidence로는 유효하지만, 1.5.2 완료 처리 근거로 쓰기에는 부족하다. build 실패 수정 PR과 재검증 evidence가 별도로 필요하다. |

## 재검증 결과

| 대상 | 명령 | 결과 |
|---|---|---|
| GitHub PR 목록 | `gh pr list --state open --limit 100` | open PR 없음 |
| PR #25 auth files | LSP diagnostics | no diagnostics |
| PR #34 changed files | LSP diagnostics | no diagnostics |
| PR #25 frontend | `npm ci` | 성공, moderate vulnerability 2건 |
| PR #25 frontend | `npm run lint` | error 0, warning 2건 |
| PR #25 frontend | `npm run build` | TypeScript 실패: `backlog/page.tsx:50:25` |
| PR #34 GitHub checks | `gh pr checks 34 --repo SEUIL/Threadia` | `Vercel` fail, `Vercel Preview Comments` pass |
| PR #34 frontend | `npm ci` | 성공, moderate vulnerability 2건 |
| PR #34 frontend | `npm run lint` | error 0, warning 2건 |
| PR #34 frontend | `npm run build` | TypeScript 실패: `backlog/page.tsx:50:25` |
| PR #35 GitHub checks | `gh pr checks 35 --repo SEUIL/Threadia` | `Vercel` fail, `Vercel Preview Comments` pass |
| PR #35 backend | `./gradlew test` | BUILD SUCCESSFUL |
| PR #35 frontend | `npm ci` | 성공, moderate vulnerability 2건 |
| PR #35 frontend | `npm run lint` | error 0, warning 2건 |
| PR #35 frontend | `npm run build` | TypeScript 실패: `backlog/page.tsx:50:25` |
| PR #34/#35 diff | `git diff --check` | 출력 없음 |
| PR #35 문서 | LSP diagnostics | no diagnostics |

## 후속 조치 제안

1. `apiClient` response interceptor에 public auth endpoint opt-out을 추가한다.
2. refresh 실패 시 원래 401 error를 `Promise.reject(error)`로 반환해 page-level `catch`가 처리할 수 있게 한다.
3. `/api/v1/auth/login`, `/api/v1/auth/password-reset/confirm` 실패 케이스를 브라우저에서 재검증한다.
4. `frontend/app/(app)/[companyId]/[projectId]/backlog/page.tsx`의 `MOCK_ROLE` 타입 narrowing 오류를 수정하고 `npm run build`를 재실행한다.
5. frontend lint warning 2건을 정리한다.
6. PR #35 문서에 상단 메타정보를 추가하고, 현재 WBS 상태값과 파일명 규칙을 맞춘다.
