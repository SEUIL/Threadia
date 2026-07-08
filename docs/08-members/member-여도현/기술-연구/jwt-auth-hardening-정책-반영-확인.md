> **작성자**: 여도현
> **작성일**: 2026-07-02
> **최종 수정**: 2026-07-07
> **버전**: v1.1
> **상태**: 검토 중
> **태그**: jwt, auth, security, hardening, sprint-1

# JWT/Auth hardening 정책 반영 확인

이 문서는 Sprint 1 WBS `1.2.3 JWT/Auth hardening 정책 반영 확인`의 결정 기준, 구현 반영 상태, 후속 hardening 기준을 정리한다. 최초 작성 시점에는 구현 전 정책 기준 산출물이었고, 2026-07-07 기준으로 Sprint 1 Auth 구현과 password reset 검증 결과를 반영해 최신화했다.

## 1. 결론

Sprint 1 Auth 구현은 아래 정책을 기준으로 진행한다.

| 정책 영역 | 결정 기준 | 상태 |
|---|---|---|
| access token 만료 | 30분 | 문서 기준 확정 |
| refresh token 만료 | 14일 | 문서 기준 확정 |
| refresh token 저장 | raw token 저장 금지, hash 저장 | 문서 기준 확정 |
| refresh token rotation | refresh 성공 시 새 refresh token으로 rotate | 문서 기준 확정 |
| refresh token invalidation | logout 실패/성공 정책과 무관하게 logout 대상 refresh token invalidation | 문서 기준 확정 |
| refresh token security revoke | password reset 성공, `SUSPENDED`/`DELETED` 상태 변경 시 해당 user의 기존 refresh token 전부 revoke | 문서 기준 확정 |
| token 저장 권장안 | access token은 memory, refresh token은 `HttpOnly`, `SameSite=Lax` cookie 권장 | 문서 기준 확정 |
| HTTPS cookie | 운영 HTTPS 환경에서는 `Secure` 사용, local 예외 문서화 | 문서 기준 확정 |
| email normalization | signup, login, password reset, OAuth, invitation lookup 모두 `trim + lowercase` | 문서 기준 확정 |
| OAuth account linking | 이메일만 같다는 이유의 blind auto-link 금지 | 문서 기준 확정 |
| 계정 상태 인증 | login, OAuth callback, token refresh, `/auth/me`는 `users.status = ACTIVE`만 성공 | 문서 기준 확정 |
| 시간 기준 | 서버 생성/만료 시각은 UTC 저장, API 응답은 ISO-8601 UTC `Z` | 문서 기준 확정 |
| secret 비노출 | secret, password, raw reset token, raw refresh token, 개인정보는 코드/문서/log/PR에 노출 금지 | 문서 기준 확정 |

## 2. WBS 1.2.3 작업 범위

WBS `1.2.3`은 Day 3 API 구현 전에 Auth 정책 기준이 코드와 문서에서 흔들리지 않도록 확인하는 작업이다.

| 항목 | 내용 |
|---|---|
| WBS ID | `1.2.3` |
| 작업명 | JWT/Auth hardening 정책 반영 확인 |
| 목적 | Day 3 Auth API 구현 전 JWT/Auth 정책 기준 확정 |
| 주요 확인 대상 | access 30분, refresh 14일, rotation/logout invalidation, email 정규화, OAuth blind auto-link 금지, non-ACTIVE 인증 차단 |
| 후속 영향 | `1.3.1` 이메일 로그인 API, `1.3.2` Google OAuth API, `1.4.1` password reset API 구현 기준 |

근거:

- `docs/00-project/sprint-운영/sprint-1-역할분담.md:43`
- `docs/00-project/sprint-운영/sprint-1-상세계획.md:155`
- `docs/00-project/sprint-운영/sprint-1-상세계획.md:197`

## 3. Canonical 결정 기준

### 3.1 JWT access/refresh 정책

| 정책 | 결정 기준 | 근거 |
|---|---|---|
| access token lifetime | 30분 | `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:111` |
| refresh token lifetime | 14일 | `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:111` |
| refresh token 저장 | raw token이 아니라 hash 저장 | `docs/04-api/API-명세서.md:190` |
| refresh token rotation | refresh 성공 시 rotate | `docs/04-api/API-명세서.md:191`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:112` |
| refresh token invalidation | 실패 또는 logout 시 해당 refresh token invalidation | `docs/04-api/API-명세서.md:192`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:113` |
| refresh token security revoke | password reset 성공, `SUSPENDED`/`DELETED` 상태 변경 시 해당 user의 기존 refresh token 전부 revoke | `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:114` |

### 3.2 Cookie/security/CORS 정책

| 정책 | 결정 기준 | 근거 |
|---|---|---|
| access token 저장 | browser memory 권장 | `docs/04-api/API-명세서.md:194`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:115` |
| refresh token 저장 | `HttpOnly`, `SameSite=Lax` cookie 권장 | `docs/04-api/API-명세서.md:194`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:115` |
| HTTPS 운영 환경 | cookie `Secure` 사용 | `docs/04-api/API-명세서.md:194`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:115` |
| local 예외 | local development 예외는 별도 명시 | `docs/04-api/API-명세서.md:194`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:115` |

### 3.3 Email normalization 정책

| 적용 지점 | 결정 기준 | 근거 |
|---|---|---|
| signup | 저장·조회·비교 전 `trim + lowercase` | `docs/04-api/API-명세서.md:59`, `docs/04-api/API-명세서.md:60` |
| login | 저장·조회·비교 전 `trim + lowercase` | `docs/04-api/API-명세서.md:59`, `docs/04-api/API-명세서.md:60` |
| password reset | 저장·조회·비교 전 `trim + lowercase` | `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:117` |
| OAuth account linking | 저장·조회·비교 전 `trim + lowercase` | `docs/04-api/API-명세서.md:154`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:117` |
| invitation lookup | 저장·조회·비교 전 `trim + lowercase` | `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:117` |

### 3.4 OAuth account linking 정책

| 정책 | 결정 기준 | 근거 |
|---|---|---|
| blind auto-link | 이메일만 같다는 이유로 provider 계정을 기존 local 계정에 자동 연결하지 않는다 | `docs/04-api/API-명세서.md:153`, `docs/03-design/ERD.md:160` |
| 명시적 연결 | 이미 로그인한 사용자가 명시적으로 연결하는 경우는 별도 연결 흐름으로 허용 가능 | `docs/04-api/API-명세서.md:153`, `docs/03-design/ERD.md:160` |
| 신뢰 가능한 provider email | provider 이메일 검증을 신뢰할 수 있는 경우에만 같은 `users` 계정 연결 가능 | `docs/04-api/API-명세서.md:153`, `docs/03-design/ERD.md:160` |
| provider 식별자 | Google `sub`를 `oauth_accounts.provider_user_id`에 저장 | `docs/03-design/ERD.md:154`, `docs/08-members/member-여도현/기술-연구/google-oauth-login-api-구현-조사.md:21` |

### 3.5 계정 상태 인증 정책

| API/흐름 | 결정 기준 | 근거 |
|---|---|---|
| login | `users.status = ACTIVE`만 성공 | `docs/04-api/API-명세서.md:32`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:102` |
| OAuth callback | `users.status = ACTIVE`만 성공 | `docs/04-api/API-명세서.md:32`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:102` |
| token refresh | `users.status = ACTIVE`만 성공 | `docs/04-api/API-명세서.md:193`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:102` |
| `/auth/me` | `users.status = ACTIVE`만 성공 | `docs/04-api/API-명세서.md:32`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:102` |
| 상태 변경 | `SUSPENDED`/`DELETED` 변경 시 기존 refresh token revoke | `docs/04-api/API-명세서.md:33`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:114` |

### 3.6 Secret/token 비노출 정책

| 항목 | 결정 기준 | 근거 |
|---|---|---|
| OAuth client secret | 코드, 문서, PR, issue, log에 기록하지 않는다 | `docs/08-members/황스일/OAuth/2026-07-01-Google-OAuth-앱-등록-작업기록.md:75`, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:238` |
| raw refresh token | 서버에는 hash만 저장하고 원문은 저장하지 않는다 | `docs/04-api/API-명세서.md:190` |
| raw reset token | `password_reset_tokens.token_hash`에 hash만 저장한다 | `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:94` |
| 개인정보 | 코드/문서/log에 노출하지 않는다 | `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:238` |

## 4. 현재 반영 상태

2026-07-07 기준 backend는 Sprint 1 Auth 구현과 password reset 흐름까지 반영된 상태다. 이 문서는 최초 정책 기준 산출물이었지만, 현재는 구현 반영 여부를 함께 추적한다.

| 확인 대상 | 문서 반영 상태 | 코드 반영 상태 | 판단 |
|---|---|---|---|
| access 30분 / refresh 14일 | 반영 | 구현됨 | `JwtTokenService`, refresh token 설정 기준 유지 |
| refresh hash 저장 | 반영 | 구현됨 | raw refresh token은 cookie로만 전달하고 DB에는 hash 저장 |
| refresh rotation | 반영 | 구현됨 | `/api/v1/auth/refresh` 성공 시 기존 token rotate/revoke 후 새 token 발급 |
| logout invalidation | 반영 | 구현됨 | access token과 refresh cookie 소유자 일치 확인 후 revoke |
| password reset 성공 시 revoke | 반영 | 구현됨 | confirm 성공 시 기존 refresh token revoke |
| `SUSPENDED`/`DELETED` 상태 차단 | 반영 | 부분 구현 | login/OAuth/refresh/me 차단 기준은 구현, 상태 변경 API는 후속 |
| access memory / refresh HttpOnly SameSite=Lax cookie | 반영 | 구현됨 | backend는 access body, refresh cookie 기준 제공 |
| email `trim + lowercase` | 반영 | 구현됨 | signup/login/password reset/OAuth/user lookup 기준 적용 |
| OAuth blind auto-link 금지 | 반영 | 구현됨 | provider account 기준 매핑, email-only 자동 연결 금지 |
| ACTIVE-only 인증 | 반영 | 구현됨 | login/OAuth/refresh/me에서 ACTIVE 기준 확인 |
| UTC 시간 기준 | 반영 | 구현됨 | token 만료/생성 시각은 UTC 기준 |
| secret/token 비노출 | 반영 | 구현됨 | `.env`와 환경변수 주입 기준, raw token 문서/로그 미노출 |

코드 상태 근거:

- `backend/src/main/java/com/threadia/backend/auth/controller/AuthController.java`에 signup/login/logout/refresh/password reset endpoint가 있다.
- `backend/src/main/java/com/threadia/backend/auth/service/GoogleOAuthLoginService.java`가 Google OAuth user mapping과 refresh cookie 발급을 처리한다.
- `backend/src/main/java/com/threadia/backend/auth/service/PasswordResetService.java`가 reset token 검증과 refresh token revoke를 처리한다.
- `backend/scripts/auth-e2e-preflight.sh` 기준 backend Auth core flow, password reset positive confirm, OAuth smoke 검증 기록이 있다.

## 5. 후속 구현 완료 기준

### 5.1 이메일 로그인/로그아웃/refresh/me 구현 완료 기준

| 기능 | 완료 기준 |
|---|---|
| login | email 정규화, credential 검증, `ACTIVE` 확인, access/refresh token pair 발급 |
| logout | 대상 refresh token invalidation 처리 |
| refresh | refresh token hash 조회, 만료/revoke 확인, `ACTIVE` 확인, refresh rotation, 새 access token 발급 |
| `/auth/me` | token 없음/만료/정상/non-ACTIVE 케이스를 구분하고 minimal user context 반환 |

### 5.2 Google OAuth 구현 완료 기준

| 기능 | 완료 기준 |
|---|---|
| OAuth 시작 | `GET /oauth2/authorization/google`로 Google OAuth flow 시작 |
| OAuth callback | `GET /api/v1/auth/oauth2/callback/google` callback 처리 |
| provider 식별 | Google `sub`를 `oauth_accounts.provider_user_id`로 저장 |
| account mapping | `oauth_accounts` 기준으로 user 매핑 또는 생성 |
| blind auto-link 방지 | email만 같은 local user에 자동 연결하지 않음 |
| ACTIVE-only | OAuth callback 후 Threadia 인증 성공은 `ACTIVE` user만 허용 |
| token 발급 | 이메일 로그인과 동일한 JWT access/refresh token pair 발급 |
| secret 관리 | OAuth client secret이 코드/문서/log/PR에 노출되지 않음 |

OAuth 구현 상세 근거 문서:

- `docs/08-members/member-여도현/기술-연구/google-oauth-login-api-구현-조사.md`

### 5.3 Password reset 구현 완료 기준

| 기능 | 완료 기준 |
|---|---|
| reset request | email 정규화, 존재 여부 노출 방지, reset token hash 저장, 만료 시각 저장 |
| reset confirm | token hash 검증, 만료/사용 여부 검증, password 변경, used-state 처리 |
| security revoke | confirm 성공 시 해당 user의 기존 refresh token 전부 revoke |
| email provider | `EmailSender` boundary를 두고 Google SMTP 구현체에 직접 결합하지 않음 |

## 6. 검증 체크리스트

### 6.1 문서 기준 검증

- [x] Sprint 1 Auth 범위가 signup/login/logout/me/OAuth/password reset으로 정리되어 있다.
- [x] JWT access token 30분, refresh token 14일 기준이 문서에 반영되어 있다.
- [x] refresh token hash 저장, rotation, logout invalidation 기준이 문서에 반영되어 있다.
- [x] password reset 성공, `SUSPENDED`/`DELETED` 상태 변경 시 refresh token revoke 기준이 문서에 반영되어 있다.
- [x] email `trim + lowercase` 정규화 기준이 문서에 반영되어 있다.
- [x] OAuth blind auto-link 금지 기준이 문서에 반영되어 있다.
- [x] login/OAuth callback/token refresh/`auth/me` ACTIVE-only 기준이 문서에 반영되어 있다.
- [x] access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie 권장 기준이 문서에 반영되어 있다.
- [x] secret/token/개인정보 비노출 기준이 문서에 반영되어 있다.

### 6.2 구현 PR 검증

- [ ] `./gradlew test`가 통과한다.
- [ ] 필요 시 `./gradlew build`가 통과한다.
- [ ] login 성공 시 access/refresh token pair가 발급된다.
- [ ] access token 만료 시간이 30분으로 설정되어 있다.
- [ ] refresh token 만료 시간이 14일로 설정되어 있다.
- [ ] refresh token 원문이 DB에 저장되지 않는다.
- [ ] refresh 성공 시 refresh token이 rotate된다.
- [ ] logout 후 기존 refresh token이 invalidation된다.
- [ ] password reset confirm 성공 후 해당 user의 기존 refresh token이 전부 revoke된다.
- [ ] `SUSPENDED`/`DELETED` user의 login/OAuth callback/token refresh/`auth/me`가 실패한다.
- [ ] signup/login/password reset/OAuth에서 email이 `trim + lowercase` 처리된다.
- [ ] OAuth callback에서 email만 같은 local user에 자동 연결하지 않는다.
- [ ] cookie 선택 시 `HttpOnly`, `SameSite=Lax`, 운영 `Secure` 기준이 적용된다.
- [ ] log, PR, 문서, 테스트 fixture에 secret/raw token/개인정보가 노출되지 않는다.

## 7. 남은 후속 작업

| 항목 | 후속 작업 | 관련 WBS |
|---|---|---|
| Refresh token reuse detection | 탈취 의심 reuse 탐지와 token family 전체 revoke 정책 결정 | Auth hardening |
| CSRF/Origin 방어 | cookie 기반 refresh/logout/password reset endpoint 보호 정책 결정 | Auth hardening |
| Endpoint별 authorization | `permitAll` 중심 구조에서 protected/public endpoint 기준 분리 | `3.1.1` 이후 RBAC |
| Rate limiting | login/OAuth/password reset abuse 방어 | Auth hardening |
| 상태 변경 revoke | `SUSPENDED`/`DELETED` 변경 API 도입 시 기존 refresh token revoke 연결 | `3.1.4` 이후 |

## 8. 최종 판단

`JWT/Auth hardening 정책 반영 확인`은 현재 시점에서 **Sprint 1 Auth 핵심 구현까지 반영 완료, production-ready hardening은 후속**으로 판단한다.

완료로 볼 수 있는 부분:

1. canonical 문서에 JWT/Auth hardening 정책이 반영되어 있다.
2. OAuth 구현 조사 문서에 1.3.2와 직접 연결되는 account linking, ACTIVE-only, JWT 발급 기준이 정리되어 있다.
3. 구현 PR과 E2E preflight 기준으로 access/refresh/logout/password reset/OAuth 핵심 흐름이 검증되어 있다.

완료로 보면 안 되는 부분:

1. refresh token reuse detection과 token family revoke 정책까지 완료된 것은 아니다.
2. CSRF/Origin 방어와 endpoint별 authorization 정책이 production-ready 수준으로 정리된 것은 아니다.
3. login/OAuth/password reset rate limiting은 아직 없다.
4. `SUSPENDED`/`DELETED` 상태 변경 API와 refresh revoke 연결은 후속 company/RBAC 작업 이후 확인해야 한다.

따라서 이 문서는 Sprint 1 Auth 핵심 구현 완료 상태와 후속 hardening gap을 구분하는 기준으로 사용한다.
