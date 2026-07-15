> **작성자**: 정철진
> **작성일**: 2026-06-16
> **최종 수정**: 2026-06-16
> **버전**: v1.1
> **상태**: 작성 중

# Sprint 1 프론트엔드 의존성 / 기술 스택 / DoD v1.1

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows이며, Sprint 1 DB migration은 `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens`만 포함한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.


이 문서는 Sprint 1 프론트엔드 작업 착수 전 필요한 범위, 기술 스택, 의존성, blocker, 완료 기준을 정리한다.

기준 문서:
- `Sprint 1_detail.md` v0.2
- `Sprint1_Backlog.md`
- `Sprint1_담당그룹_역할분담 v1.1.md`
- `API 명세서 v2.3`
- `작업 유형별 완료 조건 v2.md`
- `test-build-API-화면 확인 기준 v2.md`

---

## 1. Sprint 1 프론트엔드 범위

Sprint 1 프론트엔드는 인증 중심 화면과 인증 API 연동을 목표로 한다.

| 구분 | 결정 |
|---|---|
| 포함 | 로그인 화면, 회원가입 화면, 비밀번호 재설정 화면 |
| 연동 대상 | 회원가입, 로그인, 로그아웃, `/auth/me`, Google OAuth, 비밀번호 재설정 API |
| 인증 흐름 | 회원가입 -> 로그인 -> 로그아웃 -> Google OAuth -> 비밀번호 재설정 E2E 확인 |
| 제외/후순위 | 회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림 화면, 프로젝트 홈/대시보드, 목록 pagination 화면 구현 |

> 비밀번호 재설정은 화면까지 Sprint 1 Frontend 범위에 포함한다. 실제 이메일 발송 방식은 Google SMTP 실제 발송으로 확정하며, log/local console은 보조 debugging evidence로만 사용한다.

---

## 2. 기획 단계 확정 사항

| 항목 | 결정 | 비고 |
|---|---|---|
| Framework | React + Next.js | Sprint 1 Frontend 구조 기준 |
| Routing | 미정 | App Router vs Pages Router는 기획 최종 전까지 확정 예정 |
| Package manager | npm | 설치/실행 명령은 npm 기준으로 통일 |
| Language | TypeScript | 사용 확정 |
| Styling | Tailwind CSS | 사용 확정 |
| 인증 방식 | JWT Bearer Token | API 명세서 v2.3 기준 |
| JWT 저장 위치 | cookie 또는 memory 방향 | `localStorage`는 우선 선택지에서 제외 |
| refresh token | 사용 | access token 30분, refresh token 14일, `POST /api/v1/auth/refresh`, refresh token hash 저장, rotation, logout invalidation 기준 확정 |
| Google OAuth | Sprint 1 포함 | 앱 등록과 redirect URI는 개발 직전에 진행 |
| 비밀번호 재설정 이메일 | Google SMTP 실제 발송 | 수신 email, reset token hash, expiration, used-state 확인. log/local console은 보조 evidence |
| API 단일 응답 | `{ data, meta }` | API 명세서 v2.3 기준 |
| API 에러 응답 | `{ error: { code, message, details }, meta }` | API 명세서 v2.3 기준 |

---

## 3. 개발 중 확정 / 확인할 항목

| 항목 | 상태 | 확인 시점 | 담당 |
|---|---|---|---|
| App Router vs Pages Router | 미정 | 기획 최종 전 또는 Sprint 1 시작 전 | 정철진, 팀 전체 |
| JWT 저장 세부 방식 | cookie 또는 memory 중 확정 필요 | Auth 구현 전 | 황스일, 여도현, 김태준, 정철진 |
| refresh token 만료/갱신 전략 | access token 30분, refresh token 14일, refresh endpoint와 rotation 확정 | Sprint 1 Auth 구현 | 황스일, 여도현, 김태준 |
| Google OAuth 앱 등록 | 개발 직전 진행 | OAuth API 구현 전 | 황스일, 김민정, 여도현 |
| Google OAuth redirect URI | 개발 직전 확정 | OAuth API 구현 전 | 황스일, 김민정, 여도현, 정철진 |
| OAuth callback 처리 방식 | 확인 필요 | Backend callback 구현 시 | 여도현, 정철진, 황스일 |
| `/auth/me` 최소 사용 context | `data.user.id/email/nickname/avatarUrl` | 로그인 유지/보호 route 구현 전 | 김민경, 정철진, 황스일 |
| 실제 Auth API 응답 | 확인 필요 | Backend 구현 후 | 김민경, 정철진, 황스일 |
| Zustand/TanStack Query | 개발 중 판단 | API 연동 구조가 보일 때 | 정철진 |
| query key 기준 | 개발 중 판단 | TanStack Query 도입 시 | 정철진 |
| pagination | 후순위 | 목록 화면 구현 시 | 정철진, API 담당 |

---

## 4. 작업 일정

| Day | Frontend 관련 작업 | 완료 조건 |
|---|---|---|
| Day 1 | React + Next 프로젝트 구조 생성, npm 기준 실행 확인, routing 기준 후보 정리 | `npm run dev` 또는 합의된 dev script로 기본 route 진입 확인 |
| Day 2 | 로그인/회원가입/비밀번호 재설정 화면 layout 구현, JWT 정책 회의 결과 확인 | 화면 UI 초안 완료, token 저장/갱신 방향 확인 |
| Day 3 | 인증 화면 마무리, API 연동 준비, OAuth callback 처리 방식 확인 | API 응답 형식 기준으로 연동 준비 완료 |
| Day 4 | 로그인/회원가입/logout/me/비밀번호 재설정 API 실제 연동 | 브라우저에서 주요 인증 흐름 동작 확인 |
| Day 5 | 인증 흐름 E2E 확인, 버그 수정, 6일차 보고 입력값 정리 | 회원가입 -> 로그인 -> 로그아웃 -> OAuth -> 비밀번호 재설정 흐름 확인 결과 기록 |

---

## 5. API 연동 기준

기준 문서: `API 명세서 v2.3`

이 문서에서는 API v2.3 전체가 아니라 Auth 섹션과 인증 연동에 필요한 공통 응답 형식을 우선 적용한다.

| 항목 | 내용 | 상태 |
|---|---|---|
| Base URL (dev) | `http://localhost:8080/api/v1` | 확정 |
| 단일 응답 형식 | `{ data, meta }` | 확정 |
| 목록 응답 형식 | `{ data, page, meta }` | API 기준 존재, Sprint 1 인증 FE 직접 blocker 아님 |
| error response | `{ error: { code, message, details }, meta }` | 확정 |
| `/auth/me` 응답 | Sprint 1은 `data.user.id/email/nickname/avatarUrl`만 사용 | company/project/unread/DM count는 후속 MVP reference |

Auth 연동 대상:
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- `GET /oauth2/authorization/{provider}`
- `GET /api/v1/auth/oauth2/callback/{provider}`
- `POST /api/v1/auth/password-reset/request`
- `POST /api/v1/auth/password-reset/confirm`

---

## 6. 의존성 / Blocker 정리
### 6.1 영역별 의존성 / Blocker

| 영역 | 의존성 | Blocker | 영향 | 해결 담당 | 결정 시점 | 상태 |
|---|---|---|---|---|---|---|
| Frontend 구조 | React + Next, TypeScript, Tailwind CSS, npm | App Router vs Pages Router 미정 | route/layout 구조 결정 지연 가능 | 정철진, 팀 전체 | Sprint 1 시작 전 | 미결 |
| Auth 정책 | JWT Bearer access token + refresh token | access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie 권장. access 30분, refresh 14일, rotation/logout invalidation | 로그인 상태 유지와 API 연동 기준 확정 | 황스일 Owner, 여도현 지원, 김태준 Reviewer, 정철진 확인 | Day 2 | 확정 |
| OAuth | Google OAuth, Client ID/Secret, redirect URI | 앱 등록과 redirect URI를 개발 직전에 진행 | 소셜 로그인 API 구현 전 준비 필요 | 황스일 Owner, 김민정 지원, 여도현 Reviewer | OAuth 개발 전 | 예정 |
| Auth API 구현 | signup/login/logout/me, OAuth, password reset API | 실제 구현 응답과 API v2.3 일치 여부 확인 필요 | FE-BE 병렬 작업 지연 | 김민경, 여도현, 김태준, 정철진 | Day 3~4 | 확인 필요 |
| 비밀번호 재설정 이메일 | Google SMTP 실제 발송 | secret 비노출 필요 | reset request E2E 검증 기준 확정 | 김태준, 황스일, 정철진 | Day 4 전 | 확정 |

### 6.2 프론트엔드 기능별 의존성 / Blocker

| 기능 | 의존성 | Blocker | 영향 | 해결 담당 | 상태 |
|---|---|---|---|---|---|
| Frontend 프로젝트 구조 | React + Next, TypeScript, Tailwind CSS, npm | App Router vs Pages Router 미정 | Day 1 route/layout 기준 지연 | 정철진 Owner, 김민정 Reviewer | 미결 |
| 회원가입 화면 | signup API, validation, error response | 실제 API 응답 확인 필요 | 화면 연동 지연 | 정철진, 김민경 | 확인 필요 |
| 로그인 화면 | login API, JWT, refresh token 정책 | access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie 권장 | 로그인 상태 처리 기준 확정 | 정철진, 황스일, 여도현 | 확정 |
| 로그아웃 | logout API, token/session 초기화 기준 | logout 후 client state 정리 방식 확인 필요 | 로그인 상태가 화면에 남을 수 있음 | 정철진, 김민경 | 확인 필요 |
| `/auth/me` | JWT claim, minimal user context DTO | `data.user.id/email/nickname/avatarUrl`만 사용 | 로그인 유지, 보호 route 처리 기준 확정 | 정철진, 김민경 | 확정 |
| Google OAuth | Google OAuth provider, redirect URI, callback API | 앱 등록과 callback 처리 방식 확인 필요 | 소셜 로그인 흐름 지연 | 황스일, 김민정, 여도현, 정철진 | 예정 |
| 비밀번호 재설정 화면 | reset request/confirm API, Google SMTP | 실제 수신 email, reset token hash, expiration, used-state 확인 | E2E 검증 범위 확정 | 김태준, 정철진, 황스일 | 확정 |
| Pagination | `{ data, page, meta }`, cursor page 응답 | Sprint 1 인증 FE의 직접 blocker 아님 | 후속 목록 화면 구현 시 영향 | 정철진, API 담당 | 후순위 |

---

## 7. DoD / 검증 기준
### 7.1 공통 DoD

| 기준 | 완료 조건 | 최소 증빙 |
|---|---|---|
| 범위 | 로그인/회원가입/비밀번호 재설정 화면과 인증 연동 범위만 반영한다 | 작업 범위 기록 |
| 실행 | Frontend dev server가 실행되고 대상 route에 진입할 수 있다 | 실행 명령, 확인 route |
| 정상 케이스 | 대표 성공 흐름을 브라우저에서 확인한다 | 브라우저 확인 기록 또는 캡처 |
| 실패 케이스 | 필수값 누락, 인증 실패, token 없음/만료 등 주요 실패 조건을 확인한다 | 실패 케이스 확인 기록 |
| API 일치 | API 명세서 v2.3의 Auth request/response/error 형식과 일치한다 | API 요청/응답 예시 |
| 보안 | token, secret, password, 개인정보가 코드, 문서, 로그에 노출되지 않는다 | secret 비노출 확인 |
| 후속 조치 | 미완료 또는 미확인 항목은 사유, owner, 처리 시점을 기록한다 | TODO 또는 blocker 기록 |

### 7.2 화면별 DoD

| 화면/흐름 | 완료 조건 | 최소 증빙 |
|---|---|---|
| 프로젝트 구조 | npm 기준으로 설치/실행 가능하고 기본 route가 열린다 | `npm run dev` 또는 합의된 dev script 실행 기록 |
| 회원가입 화면 | 필수 입력, validation, 성공/실패 응답 처리가 동작한다 | 회원가입 API 요청/응답 예시, 화면 확인 기록 |
| 로그인 화면 | 이메일/비밀번호 로그인 성공, 실패, loading 상태가 구분된다 | 로그인 API 요청/응답 예시, token 처리 확인 |
| 로그아웃 | 로그아웃 후 token/session과 화면 상태가 정리된다 | logout API 응답, 로그아웃 후 화면 확인 |
| `/auth/me` | 로그인 유지 확인 또는 보호 route 진입 판단에 사용된다 | `/auth/me` 응답 예시, 화면 분기 확인 |
| Google OAuth | OAuth 시작과 callback 처리 방식이 API 기준과 맞는다 | redirect/callback 확인 기록 |
| 비밀번호 재설정 | reset request 이메일 발송과 reset confirm 흐름이 동작한다 | SMTP 발송 확인, reset API 요청/응답 예시 |

### 7.3 완료로 보지 않는 경우

| 경우 | 이유 |
|---|---|
| route 진입 또는 브라우저 확인 기록이 없다 | 사용자 관점 동작을 확인할 수 없다 |
| 정상 케이스만 확인했다 | 실패 케이스에서 사용자가 막힐 수 있다 |
| API 변경 또는 연동인데 요청/응답 예시가 없다 | API 명세와 실제 동작 일치 여부를 확인할 수 없다 |
| 공식 token 저장 기준(access token memory, refresh token HttpOnly SameSite=Lax cookie)을 반영하지 않고 인증 구현을 완료 처리했다 | 보안 및 유지보수 리스크가 남는다 |
| Google SMTP 또는 OAuth secret이 문서/로그에 노출되었다 | 보안 사고 가능성이 있다 |
| 미확인 항목의 owner와 처리 시점이 없다 | 후속 작업으로 추적되지 않는다 |

---

## 8. Sprint 1 착수 전 체크리스트

- [x] Sprint 1 FE 범위: 로그인/회원가입/비밀번호 재설정 화면 포함
- [ ] App Router vs Pages Router 결정
- [x] package manager: npm
- [x] TypeScript 사용
- [x] Tailwind CSS 사용
- [ ] JWT 저장 세부 방식: cookie 또는 memory 중 확정
- [x] refresh token 사용
- [x] refresh token 만료 14일, refresh endpoint, rotation 전략 확정
- [ ] Google OAuth 앱 등록
- [ ] Google OAuth redirect URI 확정
- [ ] Google OAuth callback 처리 방식 확인
- [ ] `/auth/me` Sprint 1 최소 context 확인
- [ ] 로그인/회원가입/logout/me API 성공 응답 예시 확인
- [x] 비밀번호 재설정 email 발송 방식 Google SMTP 실제 발송 확정
- [ ] 비밀번호 재설정 이메일 발송 E2E 확인
- [ ] error response JSON 실제 구현 확인
- [ ] pagination은 후속 목록 화면 blocker로 이동 처리

---

## 9. 개발 중으로 넘기는 항목

| 항목 | 넘기는 이유 | 처리 시점 |
|---|---|---|
| OAuth 앱 등록/redirect URI | 개발 직전 실제 환경 기준으로 등록하는 것이 안전함 | OAuth API 구현 전 |
| OAuth callback token 전달 방식 | Backend 구현 방식과 맞춰야 함 | 소셜 로그인 API 구현 시 |
| JWT cookie vs memory 세부 구현 | Backend CORS/cookie/security 설정과 함께 결정해야 함 | Day 2 JWT 정책 회의 |
| Zustand/TanStack Query | 인증 연동 규모를 보고 도입 여부 판단 가능 | API 연동 전 |
| query key 기준 | TanStack Query 도입 시점에 정리 가능 | API hook 작성 시 |
| pagination / `useInfiniteQuery` | Sprint 1 인증 FE 직접 범위가 아님 | 목록 화면 구현 시 |
| Three.js | 인증 중심 Sprint 1의 필수 blocker가 아님 | 3D 허브 작업 시 |
