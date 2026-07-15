> **작성자**: 여도현
> **작성일**: 2026-07-03
> **최종 수정**: 2026-07-07
> **버전**: v1.1
> **상태**: 검토 중
> **태그**: auth, oauth, security, jwt, refresh-token

# Google OAuth 로그인 보안 현황과 남은 갭

이 문서는 Google OAuth와 이메일 로그인 구현에서 이미 반영된 보안 통제와 production-ready 전까지 남은 보안 gap을 구분해 정리한다. 최초 기준은 `feature/auth-common`이었고, 2026-07-07에는 최신 `dev` 상태를 반영했다. 구현 완료와 정책 확정, 미구현 항목을 섞어 쓰지 않는 것이 목적이다.

## 1. 판단 기준

| 기준 | 근거 |
|---|---|
| API 계약 | `docs/04-api/API-명세서.md` |
| Backend 구조 | `docs/05-architecture/백엔드-구조.md` |
| Frontend token 흐름 | `docs/05-architecture/프론트엔드-구조.md` |
| 구현 정리 | `docs/08-members/member-여도현/기술-연구/google-oauth-auth-common-재구현-정리.md` |
| 코드 기준 | `backend/src/main/java/com/threadia/backend` |

## 2. 코드로 확인된 완료 항목

| 항목 | 상태 | 근거 |
|---|---|---|
| Google 식별자 | 완료 | Google `sub`를 `oauth_accounts.provider_user_id`로 저장 |
| Google email 검증 | 완료 | `email_verified = true`인 계정만 허용 |
| email normalization | 완료 | Google/email auth 모두 `trim + lowercase` 기준 사용 |
| blind auto-link 금지 | 완료 | 같은 email의 local user가 있어도 OAuth account 없으면 자동 연결하지 않음 |
| ACTIVE-only | 완료 | login, OAuth callback, `/auth/me`에서 `users.status = ACTIVE`만 성공 |
| refresh token raw 저장 금지 | 완료 | raw token은 cookie로만 전달, DB에는 hash 저장 |
| refresh cookie 속성 | 완료 | `HttpOnly`, `SameSite=Lax`, `Path=/`, 14일 |
| prod cookie Secure | 완료 | `prod` profile에서 `THREADIA_COOKIE_SECURE=true`가 아니면 startup 실패 |
| access token URL 노출 방지 | 완료 | OAuth callback 후 access token을 query/string에 싣지 않음 |
| logout revoke + clear cookie | 완료 | `POST /api/v1/auth/logout`에서 refresh token revoke 후 `Max-Age=0` clear cookie 응답 |
| revoke된 refresh token 재사용 차단 | 완료 | 이미 revoke된 refresh token으로 재로그아웃하면 401 처리하고 `revoked_at` 보존 |
| frontend CORS | 완료 | `/api/**`에 credential 포함 CORS 설정과 회귀 테스트 추가 |
| OAuth failure log sanitize | 완료 | OAuth callback/failure handler가 provider exception message를 log에 직접 남기지 않음 |
| `/api/v1/auth/refresh` | 완료 | refresh cookie 기반 access token 복구와 refresh token rotation 구현 |
| password reset 연동 | 완료 | reset confirm 성공 시 기존 refresh token revoke |
| 실제 OAuth smoke | 완료 | 실제 Google OAuth backend callback, DB row, refresh cookie 흐름 확인 기록 존재 |
| secret 비노출 | 완료 | `.env`를 사용하고 문서/코드에는 실제 secret 값 미기록 |

## 3. 정책은 확정됐지만 구현이 남은 항목

| 항목 | 현재 상태 | 왜 남은 gap인가 |
|---|---|---|
| refresh token reuse detection | 미구현 | 탈취된 refresh token 재사용 탐지/전체 revoke 정책이 없음 |
| status 변경 시 revoke | 부분 구현 | password reset 성공 시 revoke는 구현됨. `SUSPENDED`, `DELETED` 상태 변경 API 연동은 후속 |
| logout 만료 access token 정책 | 미정 | 현재 logout은 valid access token과 refresh cookie를 모두 요구하므로 access token 만료 후 cookie clear UX 정책 결정 필요 |

## 4. 보안상 아쉬운 점

### 4.1 OAuth callback 후 access token handoff 미확정

현재 OAuth 성공 handler는 refresh cookie를 설정하고 `/hub`로 redirect한다. access token은 URL query/string에 노출하지 않는다. frontend는 `/hub` 도착 후 `/api/v1/auth/refresh`를 호출해 access token을 복구할 수 있다.

현재 유지할 방향:

1. OAuth callback은 계속 access token을 URL에 싣지 않는다.
2. frontend는 `/hub` 진입 직후 `POST /api/v1/auth/refresh`를 호출한다.
3. backend는 HttpOnly refresh cookie를 검증하고 새 access token을 JSON body로 반환한다.
4. refresh 성공 시 refresh token을 rotate한다.
5. 남은 과제는 reuse detection과 CSRF/Origin 방어다.

### 4.2 CSRF 전략 부재

현재 `SecurityConfig`는 OAuth login을 위해 `csrf.disable()`과 `permitAll`을 사용한다. CORS 설정은 추가됐지만 CORS는 browser origin 허용 정책이지 CSRF 방어 자체가 아니다. refresh/logout처럼 cookie 기반 state-changing endpoint에는 CSRF 또는 Origin 검증 정책이 필요하다.

후속 설계 선택지:

| 선택지 | 설명 |
|---|---|
| SameSite + Origin 검증 | local/dev 단순 구현. cross-site POST 방어를 보강 |
| CSRF token | refresh/logout 같은 cookie-auth endpoint에 token 요구 |
| strict endpoint auth | SecurityFilterChain에서 endpoint별 allow/auth 정책 분리 |

### 4.3 `permitAll` 범위가 넓음

현재 SecurityFilterChain은 모든 request를 filter 레벨에서 허용하고, `/auth/me`는 controller/service에서 Bearer token을 직접 검증한다. Sprint 1 초기 구현에서는 단순하지만 API가 늘어나면 실수로 보호되지 않은 endpoint가 생길 수 있다.

후속 기준:

- public endpoint: signup, login, OAuth start/callback, password reset request/confirm
- protected endpoint: me, logout, refresh, user/profile, project/issue/document API
- SecurityFilterChain 또는 method-level security로 protected endpoint를 명시한다.

### 4.4 rate limiting / abuse 방어 없음

이메일 login, OAuth callback, password reset request는 abuse 대상이 될 수 있다. 현재 backend source에는 rate limiting 구현이 없다.

후속 기준:

| 대상 | 권장 제한 |
|---|---|
| `POST /api/v1/auth/login` | IP + email 기준 실패 횟수 제한 |
| `GET /oauth2/authorization/google` | IP 기준 과도한 시작 요청 제한 |
| OAuth callback | provider error 반복과 state mismatch 반복 감시 |
| password reset request | email enumeration 방지와 요청 횟수 제한 |

## 5. 우선순위

| 우선순위 | 항목 | 이유 |
|---|---|---|
| P0 | CSRF/Origin 방어 | cookie 기반 refresh/logout endpoint 보호 정책 필요 |
| P0 | refresh token reuse detection | 탈취된 refresh token 재사용 탐지와 전체 revoke 정책 필요 |
| P1 | endpoint별 security policy 축소 | API 증가 전 `permitAll` 위험 축소 |
| P1 | status/password reset 시 refresh revoke | 계정 상태 변경 후 기존 세션 차단 |
| P1 | rate limiting | brute force, abuse 방어 |

## 6. 후속 작업 체크리스트

- [x] `POST /api/v1/auth/refresh` 구현
- [x] refresh token 만료/revoke 검증
- [x] refresh 성공 시 token rotation
- [ ] refresh token reuse detection 기준 결정
- [x] `POST /api/v1/auth/logout` 구현
- [x] logout 시 refresh token revoke + cookie clear
- [ ] logout 시 expired access token 허용 여부 결정
- [ ] `SUSPENDED`, `DELETED` 상태 변경 시 기존 refresh token 전체 revoke
- [x] password reset 성공 시 기존 refresh token revoke
- [ ] cookie 기반 endpoint CSRF/Origin 방어 설계
- [ ] SecurityFilterChain endpoint별 정책 분리
- [ ] login/OAuth/password reset rate limiting 설계
- [x] OAuth failure log sanitize
- [x] 실제 browser OAuth flow와 DB 저장 결과 검증 기록 남기기

## 7. 결론

현재 구현은 Sprint 1 Google OAuth 최소 보안 기준과 OAuth 후 access token 복구 경로를 충족한다. 특히 Google `sub`, `email_verified=true`, blind auto-link 금지, ACTIVE-only, refresh token hash 저장, HttpOnly cookie, prod Secure guardrail, refresh rotation, logout revoke/clear cookie, password reset revoke는 코드와 테스트로 확인된다.

다만 production-ready 인증으로 보기에는 아직 부족하다. 핵심 후속 과제는 CSRF/Origin 방어, refresh token reuse detection, endpoint별 security policy, rate limiting이다.
