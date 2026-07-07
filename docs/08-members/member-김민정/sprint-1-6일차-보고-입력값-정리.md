> **작성자**: 김민정 (WBS 1.5.3 Owner)
> **작성일**: 2026-07-04
> **최종 수정**: 2026-07-07
> **버전**: v1.1
> **상태**: 작성 중
> **태그**: sprint-1, report, auth, wbs-1.5.3

# Sprint 1 6일차 보고 입력값 정리

> 기준 문서: `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md`, `docs/00-project/sprint-운영/sprint-1-backlog.md`, `docs/00-project/sprint-운영/sprint-1-상세계획.md`
> 이 문서는 WBS 1.5.3(6일차 보고 입력값 정리) 산출물이다. 완료 조건인 "Sprint 1 목표/Backlog/DoD/검증/blocker 요약"을 정리한다.

---

## 1. Sprint 1 목표 요약

Sprint 1 Backend/Frontend 범위는 **Auth only**였다. 이메일 회원가입/로그인/로그아웃/`me`, Google OAuth, 비밀번호 재설정을 Spring Boot 3.x + MySQL 8.4 기반으로 동작시키는 것이 핵심 목표였고, 회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림/프로필수정/invitation은 명시적으로 Sprint 1 범위 제외였다.

**달성 여부**: 목표한 Auth 기능(signup/login/logout/refresh/me/Google OAuth/password-reset) 전부 구현·merge 완료했고, 아래 §4 검증 결과대로 실동작까지 확인했다.

---

## 2. Backlog (이월 항목)

Sprint 1 기간 중 발견했으나 Sprint 1 범위에서 즉시 처리하지 않고 이월하기로 한 항목이다.

| 항목 | 발견 경위 | 이월 사유 |
|---|---|---|
| `backlog/page.tsx`의 `MOCK_ROLE` TypeScript 타입 비교 오류 (production build 차단) | WBS 1.5.2(김민경) QA 중 발견 | Frontend 담당자가 동일 패턴(다른 화면의 mock role 비교)을 함께 정리하는 게 안전하다고 판단, 원 파일 owner에게 이월 |
| `docs/05-architecture`/`docs/04-api`의 OAuth accessToken 전달 방식 canonical 문서 미동기화 | PR #33 리뷰 코멘트(김태준)로 발견 | 담당자 미정으로 team 채널에 확인 요청 → **PR #37로 이미 처리 완료됨** (`docs/auth-canonical-handoff`) |
| signup nickname XSS 미이스케이프 발견 | WBS 1.5 이상값 테스트 중 발견 | 백엔드는 sanitize 안 하지만, 프론트가 `dangerouslySetInnerHTML` 없이 JSX 텍스트로만 렌더링해 실제 위험 없음을 확인 → 후속 조치 불필요로 종결 |
| E2E/이상값 테스트 중 DB에 남은 테스트 계정 일부 | 반복 테스트 과정에서 누적 | 일부 정리했으나 잔여분 있음, 치명적이지 않아 후속 정리로 이월 |
| Invitation | Sprint 1 목표 확정 시 Auth 이외 범위(회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림/프로필수정/invitation)로 제외 확정 | Sprint 1에서는 제외했지만, invitation은 Sprint 1.5 backlog로 이어지는 후속 범위다. Sprint 1.5에서 invitation 관련 seed 회사/accept 시 membership 생성/notification 분리 등을 처리할 예정이다 |

---

## 3. DoD 반영 여부 (`sprint-1-백엔드-dod.md` 9장 기준)

| DoD 항목 | 완료 조건 | 상태 |
|---|---|---|
| 범위 | Sprint 1 Auth API와 인증 DB만 반영 | 충족 |
| 실행 | `./gradlew bootRun` 기동 | 충족 (여러 차례 재기동 확인) |
| 테스트 | 관련 단위/통합 테스트 통과 | 충족 (`./gradlew test` 전체 통과, `auth-e2e-preflight.sh` 통과) |
| API 일치 | API 명세서 v2.3 Auth request/response/error 형식과 일치 | 충족 |
| DB 일치 | Auth 4개 table(`users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens`) 기준 유지 | 충족 |
| 보안 | secret/password/raw reset token/개인정보 미노출 | 충족 (raw token은 로그/문서에 남기지 않음, e2e script도 토큰 원문 미출력) |
| 시간 | UTC 저장, ISO-8601 `Z` 응답 | 충족 (KST 환경 timezone 버그 발견·수정으로 재확인, PR #28) |
| 후속 조치 | 미확정 항목 owner/시점 기록 | 충족 (§2 Backlog 표로 정리) |
| 로그인 | access 30분/refresh 14일/hash 저장/email 정규화/ACTIVE 확인 | 확정 (DoD 문서 8장 기준과 실측 일치, 대소문자 이메일 로그인 curl 검증) |
| 로그아웃 | refresh token invalidation, cookie 만료 | 확정 (재사용 시 401 확인, revoke-후-재사용 버그 발견·수정) |
| `/auth/me` | minimal user context, token 유무/만료 응답 확인 | 확정 |
| Google OAuth | 앱 등록/callback/mapping, blind auto-link 금지 | 확정 (여도현 실제 Google 계정 기반 backend callback smoke 통과) |
| 비밀번호 재설정 | request/confirm, provider 비의존, refresh token revoke | 확정 (curl 전체 라이프사이클 + MailHog 기반 자동화 검증 모두 통과) |

---

## 4. 검증 결과 요약

- **Backend**: `./gradlew test` 전체 통과 (signup/login/refresh/logout/me/password-reset 전 범위 unit/controller test)
- **API 레벨 이상값 테스트**: SQL injection, XSS, null/빈값, 잘못된 타입, 이모지, 필드 주입, 긴 문자열, 경계값, 깨진 JSON, 잘못된 Content-Type 등 signup 11건 + login/refresh/logout/me 28건, 총 39건 curl로 직접 실행·확인 (상세: `인증-api-이상값-테스트-결과.md`)
- **Password reset 전체 라이프사이클**: request(이메일 존재/미존재 동일 응답) → confirm → token 재사용 차단 → 구/신규 비밀번호 로그인 → 기존 refresh token revoke까지 curl로 직접 검증
- **자동화 E2E**: 여도현의 `auth-e2e-preflight.sh`(31개 검증 항목) + MailHog 연동 실제 이메일 기반 검증 + 실제 Google 계정 기반 OAuth backend callback smoke test + frontend forgot/reset password Playwright E2E 모두 통과
- **문서화**: `인증-api-전체-요청응답-예시.md`(signup/login/refresh/logout/me/OAuth/password-reset 전체 요청·응답 예시). PR #33 review 기록(`gh api repos/SEUIL/Threadia/pulls/33/reviews`)에는 review가 없어 Reviewer 김태준 승인 완료로 표기하지 않는다. 실제 승인 근거가 확인되면 링크를 추가하고, 그 전까지는 merge 완료/후속 리뷰 필요 상태로 본다

---

## 5. Blocker (미해결 이슈)

| Blocker | 영향 | 상태 |
|---|---|---|
| `backlog/page.tsx` production build 차단 TS 에러 (`MOCK_ROLE` 타입 비교) | `npm run build` 실패 | **미해결** — 방금 재확인함(2026-07-04). Frontend 담당자 처리 필요 |
| WBS 1.5.2(김민경) 완료 근거 부족 | 1.5.2 완료 조건(통합 테스트 이슈 수정) 충족 여부 | PR #41(황스일 사후 검토, `wbs-1.4.2-1.5.2-PR-검토-결과.md`)에서 High로 판정: PR #35는 여도현 approval은 있으나 frontend build 실패를 문서화만 하고 실제로 수정하지 않아 1.5.2 완료 처리 근거로 부족. 별도 build 수정 PR과 재검증 필요 |
| 공통 axios interceptor의 401 처리 | 로그인 실패/비밀번호 재설정 token 실패 시 page-level error 처리 | PR #41에서 High/Blocking으로 판정(Oracle 검토 동일 결론): `frontend/lib/axios.ts`의 response interceptor가 public auth endpoint의 401까지 refresh 대상으로 가로채 로그인 실패·reset token 실패 화면에 backend error message가 정상 노출되지 않을 수 있음. **미해결** |
| WBS 1.5.1(여도현) 최종 팀 reviewer 확인 | 1.5 자체 완료 조건 충족 여부 | 진척률상 99%이나 정식 리뷰 승인 기록은 별도 확인 필요 |
| DB 잔여 테스트 계정 | 운영 데이터 오염 아님(로컬 test DB 한정), 정리 여지 있음 | 낮은 우선순위, 이월 |
| GitHub PR의 `Vercel` check 실패(X) | PR 화면에 check 실패로 표시됨 | **코드 실패 아님** — 원인은 "Git author minjeong333 must have access to the project on Vercel to create deployments"(Vercel team 미가입). `dev` branch ruleset(`protection_dev`)에 required status check로 걸려 있지 않아 merge를 막지 않음. Vercel team 초대는 보류하고 진행, 후속으로 필요 시 초대 요청 |

**Sprint 1 Auth backend 범위 자체의 치명적 blocker는 없다.** 다만 frontend 쪽 401 interceptor 결함과 1.5.2 완료 근거는 Sprint 2 착수 전에 반드시 정리해야 한다. `Vercel` check 실패는 권한 문제일 뿐 코드 결함이 아니며, merge 요건에도 포함되지 않는다.
