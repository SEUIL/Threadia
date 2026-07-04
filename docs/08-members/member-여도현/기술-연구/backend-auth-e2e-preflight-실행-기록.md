> **작성자**: 여도현
> **작성일**: 2026-07-04
> **최종 수정**: 2026-07-04
> **버전**: v1.0
> **상태**: 검토 중
> **태그**: auth, backend, e2e, preflight, sprint-1

# Backend Auth E2E preflight 실행 기록

이 문서는 Sprint 1 `1.5.1 인증 흐름 end-to-end 테스트` 중 backend-only로 먼저 검증한 범위와 실행 방법을 정리한다. 공식 완료 기준은 WBS와 Sprint 운영 문서이며, 이 문서는 여도현 개인 기술 연구 기록이다.

이번 기록의 범위는 `backend/scripts/auth-e2e-preflight.sh`, 실제 Google OAuth backend callback smoke test, MailHog 기반 password reset positive confirm, frontend forgot/reset password 연동 E2E 검증이다. Invitation lookup 검증은 포함하지 않는다.

---

## 1. 작성 배경

`1.5.1 인증 흐름 end-to-end 테스트`는 전체 Auth flow를 실제 환경에서 검증하는 작업이다. 다만 현재는 아래 의존성이 남아 있어 전체 완료로 볼 수 없다.

| 항목 | 상태 | 영향 |
|---|---|---|
| Password reset API | WBS `1.4.1` 구현 반영 | request, confirm 실패/validation, MailHog 기반 positive confirm 검증 완료 |
| Frontend Auth 연동 | forgot/reset password 연동 확인 | browser 기반 password reset flow 검증 완료 |
| Google OAuth callback | 실제 Google 계정 기반 smoke test 완료 | callback, refresh cookie, refresh API, DB row 생성을 확인 |
| Invitation lookup | WBS `2.2` 범위 | Auth E2E preflight에서 제외 |

따라서 이번 작업은 backend가 단독으로 제공할 수 있는 Auth HTTP contract와 token/cookie 회귀 방지 검증을 먼저 자동화하는 방향으로 제한했다.

---

## 2. Preflight script

추가한 script는 다음 위치에 둔다.

```text
backend/scripts/auth-e2e-preflight.sh
```

script의 목적은 Postman 수동 검증 대신 반복 가능한 `curl` 기반 backend preflight를 제공하는 것이다. 임시 작업 디렉터리에 응답 body, header, cookie jar를 저장해 검증하고, 종료 시 삭제한다.

Token과 cookie raw value는 출력하지 않는다. 검증 결과는 `PASS`, `FAIL`, `BLOCKED`로만 표시한다.

---

## 3. 실행 전제

필수 command는 다음과 같다.

| Command | 용도 |
|---|---|
| `curl` | HTTP 요청과 status/header/cookie 확인 |
| `python3` | JSON field와 cookie jar parsing |

Backend server는 먼저 실행되어 있어야 한다. 기본 대상은 `http://localhost:8080`이다.

예시 실행 순서는 다음과 같다.

```bash
docker compose --env-file .env -f compose.mysql.yml up -d mysql

cd backend
./gradlew bootRun
```

SMTP credential 없이 로컬 backend-only preflight를 실행할 때는 Spring Boot actuator mail health가 `DOWN`이 될 수 있다. 이 경우 실제 Auth API 검증과 무관한 mail health만 끄고 실행한다.

```bash
cd backend
./gradlew bootRun --args='--management.health.mail.enabled=false'
```

다른 터미널에서 preflight를 실행한다.

```bash
bash backend/scripts/auth-e2e-preflight.sh
```

대상 URL을 바꾸려면 다음처럼 실행한다.

```bash
THREADIA_E2E_BASE_URL=http://localhost:8080 bash backend/scripts/auth-e2e-preflight.sh
```

---

## 4. 환경 변수

| 환경 변수 | 기본값 | 용도 |
|---|---|---|
| `THREADIA_E2E_BASE_URL` | `http://localhost:8080` | preflight 대상 backend base URL |
| `THREADIA_E2E_REQUIRE_OAUTH` | `false` | `true`면 OAuth start와 callback readiness가 통과하지 않을 때 실패 처리 |
| `THREADIA_E2E_MAILHOG_API_URL` | 없음 | 설정 시 MailHog API에서 reset email을 읽어 positive confirm까지 검증 |
| `THREADIA_E2E_CONNECT_TIMEOUT` | `3` | `curl` connection timeout 초 단위 |
| `THREADIA_E2E_MAX_TIME` | `10` | `curl` 요청 전체 timeout 초 단위 |

OAuth start를 강제 검증하려면 backend 실행 환경에 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`이 필요하다.

Password reset positive confirm까지 자동 검증하려면 local SMTP capture가 필요하다. 예시는 MailHog 기준이다.

```bash
docker run -d --rm --name threadia-mailhog \
  -p 127.0.0.1:1025:1025 \
  -p 127.0.0.1:8025:8025 \
  mailhog/mailhog

cd backend
./gradlew bootRun --args='--management.health.mail.enabled=false --spring.mail.host=127.0.0.1 --spring.mail.port=1025 --spring.mail.username= --spring.mail.password= --spring.mail.properties.mail.smtp.auth=false --spring.mail.properties.mail.smtp.starttls.enable=false --threadia.mail.from=no-reply@threadia.local'

THREADIA_E2E_MAILHOG_API_URL=http://127.0.0.1:8025 \
  ./scripts/auth-e2e-preflight.sh
```

---

## 5. 검증 범위

| 순서 | 검증 | 기대 결과 |
|---|---|---|
| 1 | `/actuator/health` | `200` |
| 2 | signup | `201` |
| 3 | signup email normalization | trim/lowercase 적용 |
| 4 | duplicate signup | `409` |
| 5 | login | `200` |
| 6 | login response body | `refreshToken` 없음 |
| 7 | login refresh cookie | `refreshToken` cookie 존재 |
| 8 | bad login | `401` |
| 9 | refresh | `200` |
| 10 | refresh response body | `refreshToken` 없음 |
| 11 | refresh rotated cookie | 새 `refreshToken` cookie 존재 |
| 12 | refresh rotation | login cookie와 refresh cookie가 다름 |
| 13 | old refresh reuse | `401` |
| 14 | `/api/v1/auth/me` | `200` |
| 15 | `/api/v1/auth/me` email | login user와 일치 |
| 16 | `/api/v1/auth/me` without token | `401` |
| 17 | logout | `200` |
| 18 | logout clear cookie | `Set-Cookie: refreshToken=...Max-Age=0` |
| 19 | revoked refresh after logout | `401` |
| 20 | OAuth start | Google redirect `302` 또는 `BLOCKED` |
| 21 | OAuth callback readiness | `redirect_uri` path가 `/api/v1/auth/oauth2/callback/google` 또는 `BLOCKED` |
| 22 | OAuth actual callback | `BLOCKED` |
| 23 | password reset request | `200` |
| 24 | password reset missing user request | `200` |
| 25 | password reset bad token confirm | `401` |
| 26 | password reset weak password confirm | `400` |
| 27 | password reset raw token capture | MailHog 설정 시 token 존재 확인 |
| 28 | password reset positive confirm | `200` |
| 29 | password reset token reuse | `401` |
| 30 | reset 후 old password login | `401` |
| 31 | reset 후 new password login | `200` |

핵심 성공 기준은 Auth core flow가 모두 `PASS`로 끝나고, 마지막에 아래 메시지가 출력되는 것이다.

```text
PASS backend auth e2e preflight completed without printing tokens or cookie values
```

---

## 6. 제외 항목

### 6.1 Password reset positive confirm

Password reset API는 WBS `1.4.1 비밀번호 재설정 API 구현` 반영 후 request와 confirm 실패/validation contract까지 preflight에 포함했다. 실제 positive confirm은 raw reset token이 email body로만 전달되고 DB에는 hash만 저장되므로, local SMTP capture가 필요하다.

```text
THREADIA_E2E_MAILHOG_API_URL=http://127.0.0.1:8025 ./scripts/auth-e2e-preflight.sh
```

MailHog API URL을 설정하지 않으면 기존처럼 positive confirm만 `BLOCKED`로 남긴다. Token raw value는 script output에 출력하지 않고 존재 여부만 표시한다.

### 6.2 OAuth callback

Preflight는 `/oauth2/authorization/google`의 시작 redirect와 Google redirect URL 내부 `redirect_uri` path를 확인한다. `redirect_uri`는 `/api/v1/auth/oauth2/callback/google`이어야 한다.

다만 실제 Google login, authorization code 교환, OAuth account 저장, refresh token DB 저장, browser session까지는 자동 완료로 보지 않는다. 이 부분은 real `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, Google Console redirect URI, browser session이 필요하므로 `BLOCKED`로 남긴다.

`THREADIA_E2E_REQUIRE_OAUTH=true`를 설정하면 Google redirect가 아닐 때 실패로 전환할 수 있다.

### 6.3 Frontend E2E

이번 작업은 backend-only다. Browser에서 access token 저장, refresh cookie 포함 요청, CORS credential 동작, route guard는 별도 frontend E2E에서 확인해야 한다.

### 6.4 User lookup

`GET /api/v1/users/lookup?email=...`은 invitation flow의 등록 사용자 exact email lookup API다. Sprint 1.5 invitation / WBS `2.2` 범위이므로 Auth E2E preflight에서 제외한다.

---

## 7. 검증 결과

이번 script 기준으로 확인한 결과는 다음과 같다.

| 검증 | 결과 |
|---|---|
| `bash -n backend/scripts/auth-e2e-preflight.sh` | 통과 |
| `cd backend && ./gradlew test` | `BUILD SUCCESSFUL` |
| Backend 실행 후 `./scripts/auth-e2e-preflight.sh` | 통과 |
| `bash-language-server` diagnostics | 문제 없음 |
| 5축 review | 최종 PASS |
| OAuth callback readiness 보강 | 추가 |
| 실제 Google OAuth backend callback smoke | 통과 |
| Password reset request/negative confirm preflight | 통과 |
| MailHog 기반 password reset positive confirm | 통과 |
| reset token one-time reuse 방지 | 통과 |
| reset 후 old/new password login | 통과 |
| Frontend forgot/reset password Playwright E2E | 통과 |
| 최신 `dev` 기준 backend preflight 재실행 | 통과 |

초기 code quality review에서는 `curl` 호출에 timeout과 transport failure-safe status capture가 부족하다는 지적이 있었다. 이후 `THREADIA_E2E_CONNECT_TIMEOUT`, `THREADIA_E2E_MAX_TIME`, `curl_status()`를 추가해 hang 위험을 줄였다.

---

## 8. 진행률 판단

`1.5.1 인증 흐름 end-to-end 테스트` 전체 기준 진행률은 `90%`로 기록하는 것이 적절하다.

근거는 다음과 같다.

| 구분 | 판단 |
|---|---|
| Backend Auth core flow | 자동 preflight 완료 |
| Token/cookie 회귀 방지 | 자동 preflight 완료 |
| OAuth start redirect | 자동 preflight 완료 또는 OAuth env 없으면 `BLOCKED` |
| OAuth callback readiness | `redirect_uri` path 자동 확인 추가 |
| OAuth 실제 login/callback | 실제 Google 계정 기반 backend callback smoke 통과 |
| OAuth DB 저장 | 최근 `oauth_accounts`, OAuth user, `refresh_tokens` row 생성 확인 |
| Password reset | request, missing user request, invalid token confirm, weak password confirm, positive confirm, token reuse, old/new password login 자동 검증 완료 |
| Frontend 연동 | forgot/reset password browser E2E 통과 |

즉, backend 단독으로 검증 가능한 핵심 Auth 흐름, 실제 Google OAuth backend callback smoke, password reset positive confirm, frontend forgot/reset password 연동은 진행했다. 남은 범위는 전체 frontend Auth flow의 브라우저 통합 검증과 팀 reviewer 확인이다.
