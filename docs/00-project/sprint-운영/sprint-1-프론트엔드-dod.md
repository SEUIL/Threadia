> **작성자**: 정철진
> **작성일**: 2026-06-16
> **최종 수정**: 2026-06-27
> **버전**: 개발주간 DB정책 반영 v1
> **상태**: 확정
> **태그**: sprint-1, frontend, dod, auth
> **수정 근거**: Threadia-기획문서-검수리포트.md, DB-schema-관리-정책.md

# Sprint 1 프론트엔드 의존성 / 기술 스택 / DoD v1.1

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows다. DB schema는 `docs/03-design/ERD.md`와 `db/schema.sql`을 기준으로 관리하고, Spring Boot/JPA는 schema 생성이 아니라 mapping 검증에 사용한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.


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
| JWT 저장 위치 | access token은 memory, refresh token은 `HttpOnly`, `SameSite=Lax` cookie 권장 | `localStorage`는 우선 선택지에서 제외 |
| refresh token | 사용 | access token 30분, refresh token 14일, `POST /api/v1/auth/refresh`, refresh token hash 저장, rotation, logout invalidation 기준 확정 |
| Google OAuth | Sprint 1 포함 | 앱 등록과 redirect URI는 개발 직전에 진행 |
| 비밀번호 재설정 이메일 | Google SMTP 실제 발송 | 수신 email, reset token hash, expiration, used-state 확인. log/local console은 보조 evidence |
| API 단일 응답 | `{ data, meta }` | API 명세서 v2.3 기준 |
| API 에러 응답 | `{ error: { code, message, details }, meta }` | API 명세서 v2.3 기준 |
| 이메일 정규화 | `trim + lowercase` | signup, login, password reset, OAuth, invitation lookup과 일치 |
| 인증 성공 조건 | `users.status = ACTIVE` | login, OAuth callback, token refresh, `/auth/me` 기준 |

---

## 3. 개발 중 확정 / 확인할 항목

| 항목 | 상태 | 확인 시점 | 담당 |
|---|---|---|---|
| App Router vs Pages Router | 미정 | 기획 최종 전 또는 Sprint 1 시작 전 | 정철진, 팀 전체 |
| JWT 저장 세부 방식 | access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie 권장 기준 확인 | Auth 구현 전 | 황스일, 여도현, 김태준, 정철진 |
| refresh token 만료/갱신 전략 | access token 30분, refresh token 14일, refresh endpoint와 rotation 확정 | Sprint 1 Auth 구현 | 황스일, 여도현, 김태준 |
| Google OAuth 앱 등록 | 개발 직전 진행 | OAuth API 구현 전 | 황스일, 김민정, 여도현 |
| Google OAuth redirect URI | 개발 직전 확정 | OAuth API 구현 전 | 황스일, 김민정, 여도현, 정철진 |
| OAuth callback 처리 방식 | 확인 필요 | Backend callback 구현 시 | 여도현, 정철진, 황스일 |
| `/auth/me` 최소 사용 context | `data.user.id/email/nickname/avatarUrl` | 회사/프로젝트 context와 `companies` 배열은 Sprint 1 Auth-only FE blocker가 아님 | 김민경, 정철진, 황스일 |
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
| `/auth/me` 응답 | Sprint 1은 `data.user.id/email/nickname/avatarUrl`만 사용 | company/project/unread/DM count와 `companies` 배열은 후속 MVP reference |


### 5.1 Sprint 1 Auth-only 응답 처리 기준

- 로그인 성공 응답은 Sprint 1 Auth-only 기준으로 회사/프로젝트 context를 요구하지 않는다. `companies: []`이거나 company/project context가 아직 없어도 로그인 성공 처리를 막지 않는다.
- `/auth/me`는 `data.user.id/email/nickname/avatarUrl` 최소 context를 로그인 유지와 보호 route 판단에 사용한다.
- login, OAuth callback, token refresh, `/auth/me` 실패가 non-ACTIVE 상태로 발생하면 client token/session 상태를 정리하고 재로그인을 유도한다.
- `password-reset/confirm` 성공 후 기존 `refresh_tokens`가 revoke되므로 Frontend는 기존 로그인 상태를 신뢰하지 않고 재로그인을 유도한다.

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
| 비밀번호 재설정 화면 | reset request/confirm API, Google SMTP | 실제 수신 email, reset token hash, expiration, used-state, confirm 후 기존 client session 정리와 재로그인 필요 상태 확인 | E2E 검증 범위 확정 | 김태준, 정철진, 황스일 | 확정 |
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
| API 일치 | API 명세서 v2.3의 Auth request/response/error 형식, email 정규화, ACTIVE 사용자 기준과 일치한다 | API 요청/응답 예시 |
| 보안 | token, secret, password, 개인정보가 코드, 문서, 로그에 노출되지 않는다 | secret 비노출 확인 |
| 후속 조치 | 미완료 또는 미확인 항목은 사유, owner, 처리 시점을 기록한다 | TODO 또는 blocker 기록 |

### 7.2 화면별 DoD

| 화면/흐름 | 완료 조건 | 최소 증빙 |
|---|---|---|
| 프로젝트 구조 | npm 기준으로 설치/실행 가능하고 기본 route가 열린다 | `npm run dev` 또는 합의된 dev script 실행 기록 |
| 회원가입 화면 | 필수 입력, validation, 성공/실패 응답 처리가 동작한다 | 회원가입 API 요청/응답 예시, 화면 확인 기록 |
| 로그인 화면 | 이메일/비밀번호 로그인 성공, 실패, loading 상태가 구분된다 | 로그인 API 요청/응답 예시, token 처리 확인 |
| 로그아웃 | 로그아웃 후 token/session과 화면 상태가 정리된다 | logout API 응답, 로그아웃 후 화면 확인 |
| `/auth/me` | 로그인 유지 확인 또는 보호 route 진입 판단에 사용된다. `data.user` 최소 context만 요구한다 | `/auth/me` 응답 예시, 화면 분기 확인 |
| Google OAuth | OAuth 시작과 callback 처리 방식이 API 기준과 맞는다 | redirect/callback 확인 기록 |
| 비밀번호 재설정 | reset request 이메일 발송과 reset confirm 흐름이 동작하고, confirm 후 기존 refresh token revoke에 맞춰 재로그인이 필요하다 | SMTP 발송 확인, reset API 요청/응답 예시 |

### 7.3 완료로 보지 않는 경우

| 경우 | 이유 |
|---|---|
| route 진입 또는 브라우저 확인 기록이 없다 | 사용자 관점 동작을 확인할 수 없다 |
| 정상 케이스만 확인했다 | 실패 케이스에서 사용자가 막힐 수 있다 |
| API 변경 또는 연동인데 요청/응답 예시가 없다 | API 명세와 실제 동작 일치 여부를 확인할 수 없다 |
| 공식 token 저장 기준(access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie), email 정규화, ACTIVE 사용자 기준을 반영하지 않고 인증 구현을 완료 처리했다 | 보안 및 유지보수 리스크가 남는다 |
| Google SMTP 또는 OAuth secret이 문서/로그에 노출되었다 | 보안 사고 가능성이 있다 |
| 미확인 항목의 owner와 처리 시점이 없다 | 후속 작업으로 추적되지 않는다 |

---

## 8. Sprint 1 착수 전 체크리스트

- [x] Sprint 1 FE 범위: 로그인/회원가입/비밀번호 재설정 화면 포함
- [ ] App Router vs Pages Router 결정
- [x] package manager: npm
- [x] TypeScript 사용
- [x] Tailwind CSS 사용
- [x] JWT 저장 세부 방식: access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie 권장
- [x] refresh token 사용
- [x] refresh token 만료 14일, refresh endpoint, rotation 전략 확정
- [ ] Google OAuth 앱 등록
- [ ] Google OAuth redirect URI 확정
- [ ] Google OAuth callback 처리 방식 확인
- [x] `/auth/me` Sprint 1 최소 context 확인
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
| JWT 저장 세부 구현 | access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie 권장안을 Backend CORS/cookie/security 설정과 함께 확인 | Day 2 JWT 정책 회의 |
| Zustand/TanStack Query | 인증 연동 규모를 보고 도입 여부 판단 가능 | API 연동 전 |
| query key 기준 | TanStack Query 도입 시점에 정리 가능 | API hook 작성 시 |
| pagination / `useInfiniteQuery` | Sprint 1 인증 FE 직접 범위가 아님 | 목록 화면 구현 시 |
| Three.js | 인증 중심 Sprint 1의 필수 blocker가 아님 | 3D 허브 작업 시 |

---

## Phase B invitation 반영 메모

> **생성일**: 2026-06-16
> **상태**: 공식 결정 반영 완료
> **기준**: `.sisyphus/evidence/task-5-invitation-impact-matrix.md`의 affected row와 2026-06-16 공식 decision set을 사용한다.

### 반영 범위

1. Phase B는 Sprint 1.5의 등록 사용자 대상 app-internal invitation 정책을 반영한다.
2. Sprint 1은 Auth-only이며 invitation을 구현하지 않는다.
3. Sprint 1.5부터 회사 멤버 추가는 즉시 membership 생성이 아니라 `invitations` 생성과 대상자 accept 후 membership 생성으로 처리한다. 프로젝트 멤버 추가와 project invitation은 Sprint 2로 분리한다.
4. Sprint 2는 project invitation, project membership 생성, actual invitee email sending, token link acceptance, unregistered onboarding, resend, email template, delivery failure handling, batch cleanup/operations의 후보 시점이며 확정 구현 commitment는 아니다.

### invitation 공식 결정

1. Sprint 1.5 포함 범위는 `invitations` table, `PENDING / ACCEPTED / DECLINED / CANCELED / EXPIRED`, company invitation, exact email lookup for registered users, received invitation list, accept/decline/cancel, company membership creation on accept다.
2. Sprint 1.5 제외 범위는 project invitation, project membership creation, actual email sending to invitees, invitation token link, unregistered-user invitation, signup/onboarding auto-accept, resend다.
3. `notification_events`에는 필요 시 `INVITATION_CREATED`, `INVITATION_ACCEPTED`, `INVITATION_DECLINED`, `MEMBER_ADDED`를 내부 event 원장으로 기록할 수 있으나 Sprint 1.5 완료 조건에는 포함하지 않는다. Push 알림, 별도 Invitation notification preference, S11 사용자 알림 UI 필수 구현은 Sprint 1.5 범위가 아니다.
4. Sprint 1.5에서는 user-facing `notification_preferences`에 별도 Invitation category를 노출하지 않고 기존 global notification preference policy를 따른다.
5. `EXPIRED`와 `expires_at`을 유지하고 기본 만료 기간은 7일로 둔다. read/accept/decline/cancel 시 lazy expiration을 확인하며, 만료된 `PENDING` invitation accept는 `409 INVITATION_EXPIRED`로 거절한다.
6. Sprint 1.5에는 필수 batch scheduler를 추가하지 않는다. Batch cleanup/sweeper는 Sprint 2 이후 admin list hygiene, retention, metrics, operational cleanup 필요에 따라 검토한다.

### Phase C 추가 동기화 결정

1. Sprint 1.5 invitation 검증은 local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명을 사용한다. `POST /api/v1/companies`는 Sprint 1.5 구현 범위에 포함하지 않고 MVP/Sprint 2 reference로 유지하며 운영 데이터에는 seed를 포함하지 않는다.
2. `activity_events`는 활동 원장, `notification_events`는 알림 사건 원본으로 별도 운영한다. 두 저장소를 통합하지 않고, 필요 시 activity 이벤트에서 notification 이벤트를 생성하는 연계만 허용한다.
3. 서버 생성 시각과 만료 시각은 UTC 기준으로 생성, 저장, 비교한다. ERD의 `DATETIME` 타입은 유지하고 API 응답은 ISO-8601 UTC `Z` 형식으로 반환한다.
4. 로그인 이메일은 모든 진입점에서 `trim + lowercase`로 정규화한 값을 저장, 조회, 비교한다. 이 기준은 signup, login, password reset, OAuth 계정 연결, invitation lookup에 동일하게 적용한다.
5. OAuth 계정 연결은 이메일만 같다는 이유로 blind auto-link하지 않는다. 이미 로그인한 사용자이거나 provider 이메일 검증을 신뢰할 수 있는 경우에만 기존 `users` 계정과 연결한다.
6. `password-reset/confirm` 성공 시 해당 사용자의 모든 기존 `refresh_tokens`를 revoke한다. 기존 access token은 별도 blacklist 없이 짧은 만료 시간에 따라 자연 만료시킨다.
7. login, OAuth callback, token refresh, `/auth/me`는 `users.status = ACTIVE`인 사용자만 성공 처리한다. `SUSPENDED` 또는 `DELETED` 사용자는 인증을 거부하며, 상태 변경 시 기존 `refresh_tokens`를 revoke한다.
