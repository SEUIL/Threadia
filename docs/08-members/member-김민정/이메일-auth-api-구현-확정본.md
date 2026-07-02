> **작성자**: 김민경 (WBS 1.3.1 Owner)
> **정리/공유**: 김민정
> **작성일**: 2026-07-02
> **최종 수정**: 2026-07-02
> **버전**: v1.2 (쿠키 `Path` 확정 반영, 팀장 컨펌 완료)
> **상태**: 팀장 컨펌 완료
> **태그**: auth, api, wbs-1.3.1

# WBS 1.3.1 이메일 회원가입/로그인 API 구현 (확정본)

> 기준 문서: `docs/04-api/API-명세서.md` 13.2절, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md`, `docs/05-architecture/프론트엔드-구조.md` 5.4절
> 이전 버전: `이메일 Auth API 구현.md` → `이메일 Auth API 구현_수정본.md` → 이 문서(쿠키 `Path` 확정 반영)
> **팀장 컨펌**: 완료 (2026-07-02). refresh token 쿠키 `Path=/` 통일 방향 승인. 이 문서(signup/login/logout/me)에는 별도 수정 요청 없음 — OAuth callback 관련 서술 수정 요청은 별도 공유 문서(`이메일-auth-api-확정-컨펌-및-공유.md`) 3절에 반영함.

## 이 문서에서 확정한 것

수정본 리뷰에서 나온 "실제 구현 시 추가로 챙겨야 할 것" 중 쿠키 `Path` 값이 `docs/05-architecture/프론트엔드-구조.md`(정철진, 확정)와 달랐던 부분을 다음과 같이 확정한다.

| 항목 | 수정본 값 | 확정 값 | 사유 |
|---|---|---|---|
| refresh token cookie `Path` | `/api/v1/auth` | **`/`** | `프론트엔드-구조.md` 5.4절이 이미 팀 간 참조되는 확정 계약이라, 새 값을 도입하기보다 기존 확정 문서에 맞추는 쪽이 변경 범위가 작고 향후 다른 팀원의 프론트 작업과 충돌할 위험이 없음 |

CORS/Cookie 나머지 항목은 `프론트엔드-구조.md` 5.4절 값을 그대로 채택한다 (아래 표).

| 항목 | 개발 환경 | 운영 환경 |
|---|---|---|
| `Access-Control-Allow-Origin` | `http://localhost:3000` | 운영 도메인(와일드카드 `*` 불가) |
| `Access-Control-Allow-Credentials` | `true` | `true` |
| `HttpOnly` | `true` | `true` |
| `SameSite` | `Lax` | `Lax` |
| `Secure` | `false` | `true` |
| `Path` | `/` | `/` |
| `Domain` | 생략(localhost) | `api.threadia.com` |

---

## 사용한 주요 테이블

- `users`
- `refresh_tokens`

## 구현 API

| 요구사항 ID | Method | Endpoint | 설명 | 인증 |
|---|---|---|---|---|
| AUTH-01 | POST | `/api/v1/auth/signup` | 이메일 회원가입 | 불필요 |
| AUTH-02 | POST | `/api/v1/auth/login` | 이메일 로그인 | 불필요 |
| AUTH-02 | POST | `/api/v1/auth/logout` | 로그아웃 | 필요 |
| AUTH-02 | GET | `/api/v1/auth/me` | 현재 로그인 사용자 조회 | 필요 |

## 핵심 구현 내용

- 회원가입/로그인 요청 email을 `trim + lowercase`로 정규화
- 회원가입 시 password를 BCrypt hash로 저장
- 회원가입 사용자는 `ACTIVE` 상태로 생성
- 회원가입 성공 응답에서는 토큰을 발급하지 않고 생성된 사용자 정보만 반환
- 로그인은 `ACTIVE` 사용자만 허용
- 로그인 성공 시 access token은 JWT로 발급하며 만료 시간은 30분(1800초)으로 설정
- refresh token은 JSON 응답 body에 노출하지 않고 `HttpOnly`, `SameSite=Lax`, `Path=/` cookie로 전달
- refresh token은 원문이 아닌 SHA-256 hash로 저장
- `/auth/me`는 token의 user id로 DB를 다시 조회하고 `ACTIVE` 사용자만 허용
- logout 시 cookie로 전달된 refresh token을 revoke 처리
- 모든 성공 응답은 `{ "data": ..., "meta": { "requestId": "req_..." } }` envelope 구조로 반환

## 구현 범위

- 회원가입/로그인/로그아웃/내 정보 조회 API 구현
- email 정규화 및 사용자 조회/저장 로직 구현
- BCrypt 비밀번호 암호화 적용
- JWT access token 발급 및 Bearer 인증 적용
- refresh token HttpOnly cookie 발급, hash 저장 및 logout revoke 처리
- validation 및 공통 에러 응답 처리
- Spring Security 설정 및 JWT 관련 설정 추가
- CORS 설정: `Access-Control-Allow-Origin=http://localhost:3000`, `Access-Control-Allow-Credentials=true` (개발 환경)

## API 요청/응답 예시

### 1. 회원가입

```http
POST /api/v1/auth/signup
Content-Type: application/json
```

```json
{
  "email": " USER@example.com ",
  "password": "password1234",
  "nickname": "김민경"
}
```

성공 응답:

```http
201 Created
```

```json
{
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "nickname": "김민경",
      "avatarUrl": null
    }
  },
  "meta": {
    "requestId": "req_..."
  }
}
```

### 2. 로그인

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "USER@example.com",
  "password": "password1234"
}
```

성공 응답:

```http
200 OK
Set-Cookie: refreshToken=<refreshToken>; HttpOnly; SameSite=Lax; Path=/; Max-Age=1209600
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
      "nickname": "김민경",
      "avatarUrl": null
    },
    "companies": []
  },
  "meta": {
    "requestId": "req_..."
  }
}
```

### 3. 로그아웃

```http
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
Cookie: refreshToken=<refreshToken>
```

성공 응답:

```http
200 OK
Set-Cookie: refreshToken=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0
```

```json
{
  "data": {
    "success": true
  },
  "meta": {
    "requestId": "req_..."
  }
}
```

### 4. 내 정보 조회

```http
GET /api/v1/auth/me
Authorization: Bearer <accessToken>
```

성공 응답:

```http
200 OK
```

```json
{
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "nickname": "김민경",
      "avatarUrl": null
    }
  },
  "meta": {
    "requestId": "req_..."
  }
}
```

## curl 검증 예시

### 회원가입

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": " USER@example.com ",
    "password": "password1234",
    "nickname": "김민경"
  }'
```

### 로그인

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "email": "user@example.com",
    "password": "password1234"
  }'
```

### 내 정보 조회

```bash
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <accessToken>"
```

### 로그아웃

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <accessToken>" \
  -b cookies.txt
```

## 검증 시나리오

| 항목 | 기대 결과 |
|---|---|
| 공백/대문자가 포함된 email로 회원가입 | `trim + lowercase` 처리 후 저장 |
| 같은 email로 중복 회원가입 | `409 Conflict` |
| 회원가입 성공 | `201 Created`와 `data.user` 응답, 토큰 미발급 |
| 올바른 email/password로 로그인 | access token 응답 및 refresh token HttpOnly cookie(`Path=/`) 발급 |
| 로그인 응답 body 확인 | `refreshToken` 미포함, `companies: []`, `expiresIn: 1800` 포함 |
| 잘못된 password로 로그인 | `401 Unauthorized` |
| `SUSPENDED` 또는 `DELETED` 사용자 로그인 | `401 Unauthorized` |
| access token 없이 `/auth/me` 호출 | `401 Unauthorized` |
| 정상 access token으로 `/auth/me` 호출 | `data.user.id/email/nickname/avatarUrl` 응답 |
| `/auth/logout` 호출 | cookie refresh token revoke 처리 및 `success: true` 응답 |
| CORS: `http://localhost:3000` 외 origin에서 credentials 요청 | 차단됨 |

## 완료 조건 반영 여부

| 완료 조건 | 반영 여부 |
|---|---|
| signup 구현 | 완료 |
| login 구현 | 완료 |
| logout 구현 | 완료 |
| me 구현 | 완료 |
| email trim+lowercase | 완료 |
| ACTIVE 사용자 인증 기준 | 완료 |
| access token 30분 정책 | 완료 |
| refresh token HttpOnly cookie 정책 | 완료 |
| refresh token cookie `Path` 프론트-백엔드 일치 (`/`) | 완료 |
| 공통 `data/meta` 응답 envelope | 완료 |
| Postman 또는 curl 검증 가능 | 완료 |
