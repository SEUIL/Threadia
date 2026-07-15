> **작성자**: 김민정 (WBS 1.4.3 Owner)
> **작성일**: 2026-07-04
> **최종 수정**: 2026-07-04
> **버전**: v1.1
> **상태**: 작성 중
> **태그**: auth, api, postman, wbs-1.4.3

# WBS 1.4.3 인증 API 전체 요청/응답 예시 정리

> 기준 문서: `docs/04-api/API-명세서.md` 13.2절, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md`
> 관련 요구사항: AUTH-01 ~ AUTH-04
> 이 문서는 signup/login/refresh/logout/me(이메일 Auth API 확정본에 이미 정리됨)에 **refresh**와 **Google OAuth**를 추가하고, 비밀번호 재설정의 현재 구현 상태를 명시해 "인증 API 전체"를 한 번에 볼 수 있도록 정리한다.
>
> **이 문서만으로 안 되는 부분**: signup/login/logout/me의 실제 요청/응답 JSON, curl 검증법, 검증 시나리오는 여기 없다 → `이메일-auth-api-구현-확정본.md` 참고. 아래 §1 표의 "구현 상태" 칸에서도 해당 endpoint마다 그 문서로 바로 연결한다.

---

## 1. 대상 API 현황

| 요구사항 ID | Method | Endpoint | 설명 | 인증 | 구현 상태 |
|---|---|---|---|---|---|
| AUTH-01 | POST | `/api/v1/auth/signup` | 이메일 회원가입 | 불필요 | 완료 → [예시](./이메일-auth-api-구현-확정본.md#1-회원가입) |
| AUTH-02 | POST | `/api/v1/auth/login` | 이메일 로그인 | 불필요 | 완료 → [예시](./이메일-auth-api-구현-확정본.md#2-로그인) |
| AUTH-02 | POST | `/api/v1/auth/refresh` | access token 재발급 | refreshToken cookie | 완료 → 이 문서 §2 |
| AUTH-02 | POST | `/api/v1/auth/logout` | 로그아웃 | Bearer + refreshToken cookie | 완료 → [예시](./이메일-auth-api-구현-확정본.md#3-로그아웃) |
| AUTH-02 | GET | `/api/v1/auth/me` | 현재 로그인 사용자 조회 | Bearer | 완료 → [예시](./이메일-auth-api-구현-확정본.md#4-내-정보-조회) |
| AUTH-03 | GET | `/oauth2/authorization/google` | Google OAuth 시작 | 불필요 | 완료 → 이 문서 §3 |
| AUTH-03 | GET | `/api/v1/auth/oauth2/callback/google` | Google OAuth callback | Google 세션 | 완료 → 이 문서 §3 |
| AUTH-04 | POST | `/api/v1/auth/password-reset/request` | 비밀번호 재설정 요청 | 불필요 | 완료 → 이 문서 §4 |
| AUTH-04 | POST | `/api/v1/auth/password-reset/confirm` | 비밀번호 재설정 확정 | 불필요 | 완료 → 이 문서 §4 |

---

## 2. Token Refresh (신규)

### 요청

```http
POST /api/v1/auth/refresh
Cookie: refreshToken=<refreshToken>
```

body 없음. `Authorization` 헤더도 필요 없다 — refreshToken cookie만으로 처리된다.

### 성공 응답

```http
200 OK
Set-Cookie: refreshToken=<rotated-refreshToken>; HttpOnly; SameSite=Lax; Path=/; Max-Age=1209600
```

```json
{
  "data": {
    "accessToken": "eyJ...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "nickname": "active",
      "avatarUrl": null
    },
    "companies": []
  },
  "meta": { "requestId": "req_..." }
}
```

login 응답과 동일한 `LoginData` 구조를 그대로 재사용한다. refresh 성공 시 refresh token을 **rotate**하므로 매 호출마다 새 refreshToken cookie가 내려온다(기존 토큰은 재사용 불가).

### 실패 조건 (`AuthenticationFailedException` → 401)

| 조건 | 비고 |
|---|---|
| refreshToken cookie 없음 | |
| DB에 없는 토큰 해시 | 위조/오탈자 |
| 이미 revoke된 토큰 (logout 처리됨) | |
| 만료된 토큰(`expires_at` 지남) | |
| 사용자 상태가 `ACTIVE`가 아님(SUSPENDED/DELETED) | |

### curl 검증

```bash
# 1) 로그인해서 refreshToken 쿠키 확보
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"active@example.com","password":"password1234"}' \
  -c cookies.txt

# 2) 쿠키만으로 refresh
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -b cookies.txt -c cookies.txt -i
```

seed 계정(`active@example.com` / `password1234`) 기준 로컬에서 실제로 실행해 200 응답과 rotate된 쿠키를 확인함 (2026-07-04).

---

## 3. Google OAuth 로그인

### 흐름

```
브라우저 GET /oauth2/authorization/google
  -> Google 로그인/동의 화면
  -> Google이 GET /api/v1/auth/oauth2/callback/google로 리다이렉트 (authorization code 포함)
  -> backend가 code 교환 + 사용자 매핑/생성 + JWT 발급
  -> 성공: Set-Cookie(refreshToken) + 302 리다이렉트 http://localhost:3000/hub
  -> 실패: 302 리다이렉트 http://localhost:3000/login?oauth=failed (Set-Cookie 없음)
```

### OAuth 성공 후 accessToken 전달 — 확정

`GoogleOAuthLoginService.login()`이 accessToken을 포함한 `LoginData`를 만들지만, `OAuth2LoginSuccessHandler`는 그중 `refreshCookie()`만 응답에 반영하고 accessToken은 버린다.

```java
OAuthLoginResult result = googleOAuthLoginService.login(oauth2User);
response.addHeader(HttpHeaders.SET_COOKIE, result.refreshCookie().toString());
response.sendRedirect(properties.getSuccessRedirectUrl());
// result.data().accessToken()은 여기서 버려진다
```

`contexts/AuthContext.tsx`(정철진, WBS 1.4.2, PR #25)가 앱 로드 시 `POST /api/v1/auth/refresh`를 refreshToken cookie만으로 호출해 세션(accessToken+user)을 복원한다. Google OAuth 로그인도 동일한 `refreshToken` cookie를 심어주므로, `/hub` 리다이렉트 후 앱이 마운트되는 시점에 이 로직이 그대로 적용되어 accessToken을 받아온다.

```ts
// contexts/AuthContext.tsx — AuthProvider의 세션 복원 useEffect
const { data } = await axios.post<ApiResponse<LoginResponse>>(
  `${API_BASE}/api/v1/auth/refresh`, {}, { withCredentials: true }
);
setAccessToken(data.data.accessToken);
setUser(data.data.user);
```

**참고**: 이건 "일반 새로고침 세션 복원"을 위해 만든 로직이 OAuth 흐름에도 부수적으로 적용되는 것이지, OAuth를 위해 의도적으로 설계된 전용 처리는 아니다.

### 성공/실패 리다이렉트 대상 (`AuthOAuthProperties`)

| 결과 | 리다이렉트 URL |
|---|---|
| 성공 | `http://localhost:3000/hub` (+ `Set-Cookie: refreshToken=...`) |
| 실패 | `http://localhost:3000/login?oauth=failed` (Set-Cookie 없음) |

### 실패 조건

- 계정이 `ACTIVE` 상태가 아님 (`OAuthLoginException`)
- 이미 같은 이메일로 가입된 계정에 blind auto-link 시도 (`OAuthLoginException`) — 정책상 자동 연결 금지

### 검증 방법

Google 로그인/동의 단계는 실제 사람이 브라우저로 진행해야 해서 curl로 완전 자동화할 수 없다. `/oauth2/authorization/google`을 직접 호출하면 Google 인증 URL로 정상 리다이렉트되는 것까지는 curl로 확인 가능하다:

```bash
curl -i http://localhost:8080/oauth2/authorization/google
# -> 302, Location: https://accounts.google.com/o/oauth2/v2/auth?...client_id=...
```

나머지(콜백 이후 흐름)는 `GoogleOAuthLoginServiceTest`, `OAuth2LoginSuccessHandlerTest`, `OAuth2LoginFailureHandlerTest` 단위 테스트로 커버되어 있다.

---

## 4. 비밀번호 재설정 (AUTH-04)

WBS 1.4.1(Owner 김태준, PR #29 `feature/password-reset-backend`)이 `dev`에 merge되어 구현 완료됐다. 아래는 실제 로컬 backend에 curl로 직접 검증한 결과다 (2026-07-04).

### 4.1 재설정 요청

```http
POST /api/v1/auth/password-reset/request
```

```json
{ "email": "user@example.com" }
```

### 성공 응답

```json
{ "data": { "success": true }, "meta": { "requestId": "req_..." } }
```

**가입되지 않은 이메일로 요청해도 동일하게 `200 {"success":true}`를 반환한다** — 계정 존재 여부를 응답으로 노출하지 않기 위한 설계이며, 실제로 존재하는 이메일/존재하지 않는 이메일 둘 다 요청해 응답이 동일함을 확인했다.

내부적으로는 `ACTIVE` 상태이면서 로컬 비밀번호가 설정된 사용자(소셜 전용 계정 제외)에게만 재설정 token을 발급하고 이메일을 보낸다. token은 SHA-256 해시로 `password_reset_tokens`에 저장되며, 새 token 발급 시 그 사용자의 기존 미사용 token은 모두 사용 처리된다.

### 4.2 재설정 확정

```http
POST /api/v1/auth/password-reset/confirm
```

```json
{
  "token": "<이메일로 전달된 raw token>",
  "newPassword": "NewP@ssword1234"
}
```

### 성공 응답

```json
{ "data": { "success": true }, "meta": { "requestId": "req_..." } }
```

성공 시:
- 사용자 비밀번호가 새 값으로 변경된다 (BCrypt 인코딩)
- 사용한 token은 `used_at`이 기록되어 재사용 불가
- **해당 사용자의 기존 `refresh_tokens`가 전부 revoke된다** — 재설정 이전에 로그인했던 세션은 refresh가 더 이상 불가능해진다

### 실패 조건 (`AuthenticationFailedException` → 401)

| 조건 | 비고 |
|---|---|
| DB에 없는 token 해시 | 위조/오탈자 |
| 이미 사용된 token (`used_at` 존재) | 재사용 차단 |
| 만료된 token (`expires_at` 지남, 기본 30분) | `threadia.password-reset.expiration-minutes` |
| 사용자 상태가 `ACTIVE`가 아님 | |

`newPassword`는 signup과 동일한 규칙(8~72자, 영문+숫자 조합, BCrypt 72바이트 경계 방어)을 따르며 어기면 `password-reset/confirm` 자체가 400 VALIDATION_ERROR로 거부된다.

### curl 검증 (2026-07-04, SMTP 미설정 환경이라 token은 DB에 직접 삽입해 검증)

```bash
# 1) 테스트 계정 생성 + 로그인(재설정 전 refresh token 확보)
curl -s -X POST http://localhost:8080/api/v1/auth/signup -H "Content-Type: application/json" \
  -d '{"email":"pwreset-test@example.com","password":"password1234","nickname":"pwreset-test"}'
curl -s -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"email":"pwreset-test@example.com","password":"password1234"}'

# 2) 재설정 요청 (실제 이메일 발송은 로컬 SMTP 미설정으로 로그에만 warn, DB에는 token hash 정상 저장됨)
curl -s -X POST http://localhost:8080/api/v1/auth/password-reset/request -H "Content-Type: application/json" \
  -d '{"email":"pwreset-test@example.com"}'

# 3) 재설정 확정 (raw token은 이메일 발송분을 못 받으므로, 알려진 raw token의 SHA-256 해시를 DB에 직접 심어 검증)
curl -s -X POST http://localhost:8080/api/v1/auth/password-reset/confirm -H "Content-Type: application/json" \
  -d '{"token":"<raw token>","newPassword":"newpassword5678"}'

# 4) 검증: 이전 refresh token은 401, 구비밀번호 로그인은 401, 신규 비밀번호 로그인은 200
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/refresh -b cookies.txt
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"pwreset-test@example.com","password":"password1234"}'
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"pwreset-test@example.com","password":"newpassword5678"}'
```

실제 실행 결과: request(존재/미존재 이메일 동일 200) → confirm(200) → 동일 token 재사용(401) → 구비밀번호 로그인(401) → 신규 비밀번호 로그인(200) → 재설정 전 refresh token(401, revoke 확인)까지 전체 라이프사이클 정상 동작 확인.

### 테스트 커버리지

`PasswordResetServiceTest`(9건) + `PasswordResetControllerTest`(5건) — 정상 케이스, 미존재/만료/사용됨/비활성 사용자 token, 소셜 전용 계정(로컬 비밀번호 없음) 케이스, validation 실패 케이스까지 커버.

---

## 5. 완료 조건 반영 여부 (WBS 1.4.3 기준: "인증 API 전체 요청/응답 예시 정리")

| 항목 | 반영 여부 |
|---|---|
| signup/login/logout/me 요청/응답 예시 | 완료 — §1 표에서 `이메일-auth-api-구현-확정본.md`로 링크 연결 |
| refresh 요청/응답 예시 | 완료 |
| Google OAuth 흐름/리다이렉트 계약 정리 | 완료 |
| OAuth accessToken 전달 방식 | **확정** — 정철진 `AuthContext` 세션 복원 로직(PR #25) |
| 비밀번호 재설정 | 완료 — WBS 1.4.1(김태준, PR #29) merge 확인 후 §4에 요청/응답 예시와 curl 검증 결과 반영 |
| Postman 컬렉션 실물 파일 | 미작성 (문서형 예시로 대체, WBS상 "또는" 조건 충족) |

**종합 진척률**: 90% — AUTH-01~04(signup/login/refresh/logout/me/OAuth/비밀번호 재설정) 문서화 자체는 완료했으나, **Reviewer 리뷰 전이라 100%로 표기하지 않는다**. 리뷰 승인 후 100%로 갱신한다.
