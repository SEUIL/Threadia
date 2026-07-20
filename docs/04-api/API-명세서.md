# Threadia API 명세서 v3.0

> **작성자**: Threadia 팀
> **작성일**: 2026-06-15
> **최종 수정**: 2026-07-20
> **버전**: v3.12 Platform 회사 목록 API 계약 반영
> **상태**: 확정
> **태그**: api, auth, invitation, contract
> **수정 근거**: Threadia-기획문서-검수리포트.md
> **기준 문서**: API 명세서 초안 v1.4, 기능 분해표 v3.1, ERD v2.0, 보류 기능 최종표 v1.0, Sprint 3 기준선 업데이트 제안, Sprint 3 Backlog 초안

---

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows이며, DB는 ERD 기반 `db/schema.sql`과 문서 변경이력으로 관리한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.

> **Sprint 3 기준선 메모**: 현재 API 기준은 Gate 3/4 완료 상태에 맞춰 Sprint 3 후보, decision-needed, 유지 보류 항목을 분리한다. Sprint 1 Auth only, Sprint 1.5 company invitation, Sprint 2/Foundation reference 기록은 유지한다. S15 `4.10.1` support case, `4.10.3` typed system settings, `4.10.4` Backend lifecycle/audit-read, `4.8.1` notification inbox/read-state, `4.9.4.1` Search Backend 계약을 확정한다. `4.9.4.2` Frontend는 계획/0% 상태다. notification producer matrix, preferences, push/WebSocket, S15 Frontend, enforcement, dashboard, retention/cleanup은 후속 범위다.


## 1. 공통 규칙

### Base URL

```text
개발: http://localhost:8080/api/v1
운영: https://api.threadia.com/api/v1
```

### 인증

- JWT Bearer Token 사용.
- 헤더: `Authorization: Bearer <token>`.
- 인증 제외: 회원가입, 로그인, 소셜 로그인 redirect/callback, 비밀번호 재설정 요청/확인.
- 그 외 모든 요청은 인증 필수.
- 로그인, OAuth callback, token refresh, `/auth/me`는 `users.status = ACTIVE`인 사용자만 성공 처리한다. `SUSPENDED` 또는 `DELETED` 사용자는 인증을 거부한다.
- 사용자가 `SUSPENDED` 또는 `DELETED`로 변경될 때 해당 사용자의 기존 `refresh_tokens`를 revoke한다. 기존 access token은 별도 blacklist 없이 짧은 만료 시간에 따라 자연 만료시킨다.

### 응답 형식

```json
{
  "data": {},
  "meta": { "requestId": "req_..." }
}
```

pagination이 필요한 목록 응답은 `page`를 포함한다. endpoint별 응답 예시에서 `data.{resource}` wrapper를 명시한 소규모 scoped list는 해당 wrapper 계약을 따른다.

```json
{
  "data": [],
  "page": { "cursor": "next_cursor", "hasNext": true, "size": 20 },
  "meta": { "requestId": "req_..." }
}
```

### 시간과 이메일 정규화

- Threadia의 모든 서버 생성 시각과 만료 시각은 UTC 기준으로 생성, 저장, 비교한다.
- MySQL 컬럼 타입은 ERD의 `DATETIME`을 유지하되 애플리케이션 계층에서 UTC 값을 저장한다.
- API 응답의 시간 필드는 ISO-8601 UTC `Z` 형식으로 반환한다.
- 로그인 이메일은 모든 진입점에서 앞뒤 공백을 제거하고 lowercase로 정규화한 값을 기준으로 저장, 조회, 비교한다.
- 이메일 정규화 기준은 회원가입, 로그인, 비밀번호 재설정, OAuth 계정 연결, invitation 대상자 lookup에 동일하게 적용한다. `users.email`에는 정규화된 이메일만 저장한다.

### 공통 에러 코드

| HTTP | 코드 | 설명 |
|---:|---|---|
| 400 | VALIDATION_ERROR | 요청 형식/유효성 오류 |
| 401 | UNAUTHORIZED | 인증 필요 |
| 403 | FORBIDDEN | 권한 없음 |
| 404 | NOT_FOUND | 리소스 없음 |
| 409 | CONFLICT | 중복 데이터 |
| 409 | ALREADY_COMPANY_MEMBER | 이미 회사 소속 구성원 |
| 409 | COMPANY_MEMBER_SUSPENDED | 정지된 회사 구성원은 초대 수락으로 복구할 수 없음 |
| 409 | ALREADY_PROJECT_MEMBER | 이미 프로젝트 소속 구성원 |
| 409 | TARGET_NOT_ACTIVE_COMPANY_MEMBER | 대상 사용자가 활성 회사 구성원이 아님 |
| 409 | INVITATION_SELF_NOT_ALLOWED | 자기 자신에게 초대할 수 없음 |
| 409 | INVITATION_ALREADY_PENDING | 동일 scope와 대상 사용자에 대한 대기 중 초대가 이미 있음 |
| 409 | INVITATION_EXPIRED | 초대가 만료됨 |
| 409 | INVITATION_CANCELED | 초대가 취소됨 |
| 409 | INVITATION_INVALID_STATUS | 처리할 수 없는 초대 상태 |
| 409 | DOCUMENT_VERSION_CONFLICT | 문서 수정 optimistic locking 충돌. Sprint 3 decision-needed boundary code |
| 409 | THREAD_DEFERRED | WBS 4.7.1 Thread 구현 전 메시지 `threadId`를 지정함 |
| 409 | SYSTEM_SETTING_VERSION_CONFLICT | `expectedVersion`이 현재 setting version과 다름 |
| 409 | SYSTEM_SETTING_NO_CHANGE | replacement JSON이 현재 value와 동일함 |

### 사용자 요약 DTO

```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "avatarUrl": null
}
```

---

## 2. Enum

```text
authProvider:          LOCAL | GOOGLE | KAKAO | NAVER
companyMemberRole:     COMPANY_ADMIN | MEMBER
projectMemberRole:     PROJECT_ADMIN | MEMBER
projectStatus:         PLANNING | ACTIVE | PAUSED | COMPLETED | ARCHIVED
sprintStatus:          PLANNED | ACTIVE | COMPLETED
issueStatus:           BACKLOG | TODO | IN_PROGRESS | IN_REVIEW | DONE
issuePriority:         LOW | MEDIUM | HIGH | URGENT
issueSourceType:       MANUAL | MESSAGE
documentType:          DOC | MEETING_NOTE
notificationScopeType: GLOBAL
invitationStatus:      PENDING | ACCEPTED | DECLINED | CANCELED | EXPIRED
invitationScopeType:   COMPANY | PROJECT
invitationRoleByScope: COMPANY -> COMPANY_ADMIN | MEMBER, PROJECT -> PROJECT_ADMIN | MEMBER
platformAdminRole:      PLATFORM_ADMIN | SUPER_ADMIN
supportCaseType:        INQUIRY | REPORT
supportCaseStatus:      OPEN | IN_PROGRESS | RESOLVED | REJECTED | CLOSED
supportCaseTargetType:  USER | COMPANY | PROJECT | CONTENT
systemSettingKey:       SUPPORT_CASE_CATEGORIES | REPORT_REASON_CATEGORIES | ENFORCEMENT_REASON_PRESETS | ENFORCEMENT_DURATION_PRESETS | REPLY_TEMPLATES | ANNOUNCEMENT_BANNER | SUPPORT_CENTER_STATUS | RESULT_NOTIFICATION_POLICY
```

Phase A Sprint 1 구현 provider는 Google OAuth로 한정한다. API와 ERD의 KAKAO, NAVER enum은 후속 확장 reference로만 유지한다.

---

## 3. 인증/Auth

| Method | Endpoint | 권한 | 설명 | 상태 |
|---|---|---|---|---|
| POST | `/api/v1/auth/signup` | 공개 | 이메일 회원가입 | Sprint 1 확정 |
| POST | `/api/v1/auth/login` | 공개 | 이메일/비밀번호 로그인 | Sprint 1 확정 |
| POST | `/api/v1/auth/logout` | A/P/C | 로그아웃, refresh token invalidation | Sprint 1 확정 |
| POST | `/api/v1/auth/refresh` | 공개(cookie) | refresh token rotation 후 새 access token 발급 | Sprint 1 확정 |
| GET | `/api/v1/auth/me` | A/P/C | 현재 사용자 컨텍스트 조회 | Sprint 1 확정, `data.user` minimal Auth context |
| GET | `/oauth2/authorization/{provider}` | 공개 | 소셜 로그인 시작 | Sprint 1 확정 |
| GET | `/api/v1/auth/oauth2/callback/{provider}` | 공개 | 소셜 로그인 callback 처리 | Sprint 1 확정 |
| POST | `/api/v1/auth/password-reset/request` | 공개 | 비밀번호 재설정 요청 | Sprint 1 확정 |
| POST | `/api/v1/auth/password-reset/confirm` | 공개 | token 검증 후 비밀번호 변경 | Sprint 1 확정 |

### 3.1 회원가입

```http
POST /api/v1/auth/signup
```

```json
{
  "email": "minjeong@threadia.com",
  "password": "P@ssword1234",
  "nickname": "김민정",
  "avatarUrl": null
}
```

- `email`: 필수, 이메일 형식, 중복 불가. 저장·조회·비교 전 `trim + lowercase`로 정규화한다.
- `password`: 필수, 8자 이상, 영문+숫자 조합.
- `nickname`: 필수, 2~100자.

### 3.2 소셜 로그인

```http
GET /oauth2/authorization/{provider}
GET /api/v1/auth/oauth2/callback/{provider}
```

- Phase A Sprint 1 구현 provider는 `GOOGLE`이다. `KAKAO`, `NAVER`는 후속 확장 enum으로만 남긴다.
- 이메일만 같다는 이유로 provider 계정을 기존 로컬 계정에 blind auto-link하지 않는다. 이미 로그인한 사용자가 명시적으로 연결하거나, provider의 이메일 검증을 신뢰할 수 있는 경우에만 같은 `users` 계정에 연결한다.
- OAuth 계정 연결 판단에도 정규화된 이메일을 사용한다.
- provider 식별자는 `oauth_accounts`에 저장한다.
- callback 성공 시 `oauth_accounts`를 통해 `users`를 매핑하거나 생성한 뒤 Threadia refresh token cookie를 `Set-Cookie`로 내려주고 frontend success URL(`/hub`)로 redirect한다. OAuth callback endpoint는 이메일 login처럼 JSON body로 access token을 반환하지 않으며, access token은 URL query/fragment에도 노출하지 않는다. Frontend는 redirect 후 `POST /api/v1/auth/refresh`를 호출해 HttpOnly refresh cookie 기반으로 access token과 user context를 복원한다.

### 3.3 비밀번호 재설정

```http
POST /api/v1/auth/password-reset/request
```

```json
{ "email": "minjeong@threadia.com" }
```

```http
POST /api/v1/auth/password-reset/confirm
```

```json
{
  "token": "reset-token",
  "newPassword": "NewP@ssword1234"
}
```

- token은 `password_reset_tokens`에 해시로 저장한다.
- token은 만료 시각과 사용 시각을 가진다.
- 이메일 존재 여부는 응답 메시지로 노출하지 않는다.
- `password-reset/confirm` 성공 시 해당 사용자의 모든 기존 `refresh_tokens`를 revoke한다. 기존 access token은 별도 blacklist 없이 짧은 만료 시간에 따라 자연 만료시킨다.

### 3.7 Token refresh

```http
POST /api/v1/auth/refresh
```

- refresh token은 raw token이 아니라 hash로 서버에 저장한다.
- 성공 시 refresh token을 rotate하고 새 access token을 발급한다.
- 실패 또는 logout 시 해당 refresh token을 invalidation한다.
- token refresh는 `users.status = ACTIVE`인 사용자에게만 허용한다.
- Browser 저장 기준은 access token memory, refresh token `HttpOnly`, `SameSite=Lax`, `Path=/` cookie다. refresh token은 JSON response body에 노출하지 않고 `Set-Cookie`로만 전달한다. Login/refresh 성공 시 refresh token cookie `Max-Age=1209600`(14일)을 사용하고, HTTPS 운영 환경에서는 `Secure`를 사용하며 local development에서는 `Secure=false`를 허용한다. Logout은 대상 refresh token을 revoke하고 `Set-Cookie: refreshToken=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0`으로 browser cookie를 만료시킨다.

---

## 4. 사용자/User

> **Phase A Sprint 1 구현 범위**: 이 섹션은 후속 MVP reference다. Sprint 1 구현 target은 Auth API와 인증 DB에 한정한다.


| Method | Endpoint | 권한 | 설명 | 상태 |
|---|---|---|---|---|
| GET | `/api/v1/users/me/company-memberships` | A/P/C | 현재 사용자의 ACTIVE 회사 membership context 조회 | Sprint 3 frontend/RBAC context |
| PATCH | `/api/v1/users/me/profile` | A/P/C | nickname, avatarUrl 부분 수정 | WBS 4.2.1 Backend 구현 |

```json
{
  "nickname": "김민정",
  "avatarUrl": "https://.../profile.png"
}
```

프로필 사진은 파일 업로드 API가 아니라 `avatarUrl` 저장으로 시작한다. S3 사용은 확정됐지만, 범용 파일 업로드와 presigned URL API는 Sprint 1 이후에 구현한다.

---

## 5. 회사/부서/구성원

> **Phase A Sprint 1 구현 범위**: 이 섹션은 후속 MVP reference다. Sprint 1 구현 target은 Auth API와 인증 DB에 한정한다.


| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/companies` | 인증 | 회사 생성, 생성자가 COMPANY_ADMIN |
| GET | `/api/v1/companies/{companyId}` | A/P/C | 회사 정보 조회 |
| PATCH | `/api/v1/companies/{companyId}` | C | 회사 정보 수정 |
| POST | `/api/v1/companies/{companyId}/logo` | C | 회사 로고 URL 저장 |
| GET | `/api/v1/companies/{companyId}/stats` | C | 회사 구성원/부서/프로젝트 count |
| GET | `/api/v1/companies/{companyId}/departments` | C | 부서 목록 조회 |
| POST | `/api/v1/companies/{companyId}/departments` | C | 부서 생성 |
| PATCH | `/api/v1/departments/{departmentId}` | C | 부서 수정 |
| DELETE | `/api/v1/departments/{departmentId}` | C | 부서 삭제, 멤버 부서 NULL 처리 |
| GET | `/api/v1/companies/{companyId}/company-members` | C | 구성원 목록 조회 |
| POST | `/api/v1/companies/{companyId}/invitations` | C | Sprint 1.5 회사 초대 생성. 수락 전 membership 미생성 |
| PATCH | `/api/v1/companies/{companyId}/company-members/{companyMemberId}` | C | 역할/부서/상태 변경. `status: "LEFT"`로의 전환은 거절하고 DELETE 사용을 강제 |
| DELETE | `/api/v1/companies/{companyId}/company-members/{companyMemberId}` | C | 구성원 제거(soft — `status = LEFT`, `departmentId = null`). 해당 회사 내 active project membership도 함께 `REMOVED` 처리 |

Sprint 1.5부터 구성원 추가는 등록 사용자 exact email lookup 후 `invitations`를 생성한다. 가입되지 않은 이메일 대상 invitation, 외부 email 발송, token link 초대는 Sprint 1.5 범위가 아니며 user-facing membership create API로 즉시 소속을 만들지 않는다.

Sprint 1.5 invitation 검증은 local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명으로 진행했다. **(2026-07-18 갱신, `기획문서-변경이력.md` 2026-07-18 항목)** `POST /api/v1/companies`의 seed-only 제한은 해제됐다 — 로그인 사용자는 프론트에서 직접 회사를 생성할 수 있고 생성자는 자동 `COMPANY_ADMIN`이 된다. seed 기반 dev/demo 데이터 구성 자체는 계속 유효하다.

---

## 6. 프로젝트/스프린트

> **Phase A Sprint 1 구현 범위**: 이 섹션은 후속 MVP reference다. Sprint 1 구현 target은 Auth API와 인증 DB에 한정한다.


| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/companies/{companyId}/projects` | A/P/C | 프로젝트 목록 조회 |
| POST | `/api/v1/companies/{companyId}/projects` | C | 프로젝트 생성 |
| GET | `/api/v1/projects/{projectId}` | A/P/C | 프로젝트 상세 조회 |
| PATCH | `/api/v1/projects/{projectId}` | P/C | 프로젝트 정보 수정 |
| POST | `/api/v1/projects/{projectId}/archive` | P/C | 프로젝트 아카이빙 |
| GET | `/api/v1/projects/{projectId}/project-members` | A/P/C | 프로젝트 멤버 목록 |
| GET | `/api/v1/projects/{projectId}/invitable-members?email={email}` | P/C | Issue #108 프로젝트 invitation 후보 exact email 조회. 같은 회사 ACTIVE 구성원 중 초대 가능한 사용자 1명만 최소 응답으로 반환하고, 후보가 아니면 전부 404 |
| POST | `/api/v1/projects/{projectId}/invitations` | P/C | WBS 4.3.1 프로젝트 invitation 생성. 대상은 같은 회사 ACTIVE 구성원으로 제한하고 수락 전 membership 미생성 |
| GET | `/api/v1/projects/{projectId}/invitations` | P/C | WBS 4.3.1 프로젝트 invitation 목록 조회 |
| PATCH | `/api/v1/project-members/{projectMemberId}` | P/C | 프로젝트 멤버 역할/상태 변경 |
| DELETE | `/api/v1/project-members/{projectMemberId}` | P/C | 프로젝트 멤버 제거 |
| GET | `/api/v1/projects/{projectId}/sprints` | A/P/C | Sprint 목록 |
| POST | `/api/v1/projects/{projectId}/sprints` | P/C | Sprint 생성 |
| GET | `/api/v1/sprints/{sprintId}` | A/P/C | Sprint metadata 상세. 배정 이슈는 별도 Issue 목록 조회 |
| PATCH | `/api/v1/sprints/{sprintId}` | P/C | Sprint 수정 |
| POST | `/api/v1/sprints/{sprintId}/start` | P/C | Sprint 시작 |
| POST | `/api/v1/sprints/{sprintId}/complete` | P/C | Sprint 종료 |
| POST | `/api/v1/sprints/{sprintId}/issues` | P/C | Sprint에 이슈 배정 |

프로젝트 복원은 Sprint 3 decision-needed다. archive 프로젝트 복원 권한, UI, API 기준이 확정되기 전까지 full endpoint contract를 추가하지 않는다.

---

## 6-A. Invitation, Sprint 1.5 및 WBS 4.3.1

> **Sprint 1.5 구현 범위**: 등록 사용자 대상 app-internal invitation만 다룬다. 실제 email 발송, invitation token link, 미가입 사용자 초대, signup/onboarding auto-accept, resend는 포함하지 않는다.

| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/users/lookup?email={email}` | C | 등록 사용자 exact email lookup. Sprint 1.5 company invitation 대상 확인용. 입력 email은 `trim + lowercase` 정규화 후 조회 |
| GET | `/api/v1/projects/{projectId}/invitable-members?email={email}` | P/C | Issue #108 PROJECT invitation 후보 exact email lookup. 입력 email 정규화, 프로젝트 권한 및 생성 가능 조건 검증 후 최소 사용자 응답 반환. 조회 범위는 같은 회사 ACTIVE 구성원으로 한정하며 그 밖은 404 |
| POST | `/api/v1/companies/{companyId}/invitations` | C | 회사 invitation 생성 |
| GET | `/api/v1/companies/{companyId}/invitations` | C | 회사 invitation 목록 조회 |
| POST | `/api/v1/projects/{projectId}/invitations` | P/C | WBS 4.3.1 프로젝트 invitation 생성 |
| GET | `/api/v1/projects/{projectId}/invitations` | P/C | WBS 4.3.1 프로젝트 invitation 목록 조회 |
| GET | `/api/v1/users/me/invitations` | A/P/C | 내가 받은 COMPANY 및 PROJECT invitation 목록 조회 |
| POST | `/api/v1/invitations/{invitationId}/accept` | A/P/C | 저장된 scope에 따라 COMPANY 또는 PROJECT invitation 수락, membership 생성 또는 재활성화 |
| POST | `/api/v1/invitations/{invitationId}/decline` | A/P/C | 저장된 scope에 따라 COMPANY 또는 PROJECT invitation 거절 |
| POST | `/api/v1/invitations/{invitationId}/cancel` | C 또는 P/C | 저장된 scope에 따라 COMPANY는 C, PROJECT는 P/C 권한으로 invitation 취소 |

### Invitation 정책

- `status`는 `PENDING`, `ACCEPTED`, `DECLINED`, `CANCELED`, `EXPIRED` 중 하나다.
- 기본 만료 기간은 생성 시각 기준 UTC 7일이며 `expiresAt`은 null로 내려주지 않는다.
- read/accept/decline/cancel 시 lazy expiration을 확인한다. 필수 batch scheduler는 Sprint 1.5 범위가 아니다.
- 이미 회사 멤버면 `409 ALREADY_COMPANY_MEMBER`를 반환한다. 단, 기존 회사 membership이 `SUSPENDED` 상태이면 invitation accept로 복구하지 않고 `409 COMPANY_MEMBER_SUSPENDED`를 반환한다.
- WBS 4.3.1 PROJECT invitation은 대상자가 존재해야 하며, 존재하지만 `ACTIVE` user가 아니거나 해당 프로젝트 회사의 `ACTIVE` company member가 아니면 `409 TARGET_NOT_ACTIVE_COMPANY_MEMBER`를 반환한다. 대상 user가 아예 없으면 `404 NOT_FOUND`를 반환한다. 생성자는 자기 자신을 초대할 수 없으며 `409 INVITATION_SELF_NOT_ALLOWED`를 반환한다.
- WBS 4.3.1 PROJECT invitation은 이미 ACTIVE 프로젝트 멤버인 대상자에게 생성하거나 수락할 수 없으며 `409 ALREADY_PROJECT_MEMBER`를 반환한다.
- `(project, invitee, PROJECT, PENDING)` 중복 key는 하나의 대기 초대만 허용한다. 만료되지 않은 중복 생성은 `409 INVITATION_ALREADY_PENDING`을 반환하고, 만료된 대기 초대는 lazy expiration 후 새 초대를 생성할 수 있다.
- Issue #108 project-scoped 후보 조회는 부분 일치, 목록, pagination을 제공하지 않고 정규화된 email exact match만 허용한다. 서버는 email 조회 전에 요청자의 ACTIVE 상태와 해당 프로젝트 P/C 권한을 확인해 비권한자의 사용자 directory 열람을 막는다. C는 해당 프로젝트 회사의 ACTIVE `COMPANY_ADMIN`, P는 정확히 해당 프로젝트의 ACTIVE `PROJECT_ADMIN`이어야 한다.
- 후보 조회는 PROJECT invitation 생성과 같은 eligibility를 적용하되, **회사 경계 밖 정보를 노출하지 않도록 응답을 통일한다.** 대상은 ACTIVE user이면서 해당 프로젝트 회사의 ACTIVE company member여야 하며, 이 조건을 만족하지 않으면(미가입, 비ACTIVE, 다른 회사) 구분 없이 `404 NOT_FOUND`다. 갈라서 응답하면 P가 임의의 email로 서비스 가입 여부를 판별할 수 있다(user enumeration). 반면 자기 자신, ACTIVE 프로젝트 멤버, 만료되지 않은 PROJECT `PENDING` invitation 대상은 이미 같은 회사 안에서 확인 가능한 정보라 기존 `409` conflict code를 그대로 쓴다. 만료된 `PENDING` invitation은 조회 중 lazy expiration한 뒤 후보를 반환하며, 실제 생성 시 `POST /projects/{projectId}/invitations`가 eligibility를 다시 검증한다.
- 후보 조회는 아무것도 쓰지 않으므로 프로젝트 행에 비관적 락을 걸지 않는다(readOnly). eligibility의 최종 판정은 생성 endpoint가 담당한다.
- 기존 `GET /api/v1/users/lookup`의 C-only global lookup과 회사 구성원 목록 계약은 변경하지 않는다. project-scoped 후보 조회 성공 응답은 기존 4필드 `UserResponse`만 제공한다.
- 회사 invitation role은 `COMPANY_ADMIN` 또는 `MEMBER`만 허용한다. WBS 4.3.1 PROJECT invitation role은 `PROJECT_ADMIN` 또는 `MEMBER`만 허용한다.
- PROJECT invitation 수락 시 기존 프로젝트 membership이 없으면 새 row를 생성하고 `membershipCreated=true`를 반환한다. 기존 row가 `REMOVED`이면 그 row의 ID와 생성 시각을 보존한 채 초대 role과 `ACTIVE` 상태로 재활성화하며 `membershipCreated=false`를 반환한다.
- 공통 accept/decline/cancel endpoint는 request body의 scope 파라미터를 받지 않는다. access token과 `ACTIVE` 사용자 인증을 invitation ID 조회와 저장된 `scopeType` dispatch보다 먼저 검증하므로, 미인증 호출자는 invitation 존재 여부를 추론할 수 없다. invitation에 저장된 `scopeType`으로 COMPANY 또는 PROJECT 처리를 dispatch한다. PROJECT cancel은 같은 회사의 ACTIVE `COMPANY_ADMIN` 또는 ACTIVE `PROJECT_ADMIN`이 수행할 수 있다. `INVITE-01~04` company invitation 의미는 유지한다.
- Sprint 1.5의 invitation-related notification은 필요 시 내부 `notification_events` 원장 기록을 남길 수 있으나 완료 조건에는 포함하지 않는다. Push 알림, 별도 Invitation notification preference, S11 사용자 알림 UI 필수 구현은 Sprint 1.5 범위가 아니다.
- `activity_events`는 활동 원장, `notification_events`는 알림 사건 원본으로 별도 운영하고 통합 저장소로 합치지 않는다. 필요 시 activity 이벤트에서 notification 이벤트를 생성하는 연계만 허용한다.

---

## 7. 이슈/문서/그래프

> **Phase A Sprint 1 구현 범위**: 이 섹션은 후속 MVP reference다. Sprint 1 구현 target은 Auth API와 인증 DB에 한정한다.

> **WBS 4.4.1 Backend 계약**: 이슈는 고정 상태 `BACKLOG`, `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`를 사용한다. 커스텀 Kanban column/order, 메시지 기반 생성, 영구 삭제, notification, Frontend는 이 계약에 포함하지 않는다.


| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/projects/{projectId}/issues` | A/P/C | `status`, `sprintId`, `assigneeId`, `unassignedOnly`, `includeTrashed` filter를 지원하는 이슈 목록. pagination 없음 |
| POST | `/api/v1/projects/{projectId}/issues` | A/P/C | 이슈 생성. A는 title/description만, P/C는 priority/assignee/dueDate/sprint 지정 가능 |
| GET | `/api/v1/issues/{issueId}` | A/P/C | 이슈 상세 조회 |
| PATCH | `/api/v1/issues/{issueId}` | A/P/C | PATCH omitted=유지, 명시 null은 description/assignee/dueDate/sprintId만 clear. `sprintId`는 해제 전용으로 null만 허용하고 non-null은 400 |
| PATCH | `/api/v1/issues/{issueId}/status` | A 제한/P/C | A는 DONE 외 상태, P/C는 DONE 재오픈을 포함한 모든 상태 변경 |
| POST | `/api/v1/issues/{issueId}/trash` | P/C 또는 등록자 | 이슈 휴지통 이동 |
| POST | `/api/v1/issues/{issueId}/restore` | P/C 또는 등록자 | 이슈 복원 |
| GET | `/api/v1/projects/{projectId}/documents` | A/P/C | 문서 목록 |
| POST | `/api/v1/projects/{projectId}/documents` | A/P/C | 문서 생성 |
| GET | `/api/v1/documents/{documentId}` | A/P/C | 문서 상세 |
| PATCH | `/api/v1/documents/{documentId}` | 작성자/P/C | 문서 수정 |
| POST | `/api/v1/documents/{documentId}/trash` | 작성자/P/C | 문서 휴지통 이동 |
| POST | `/api/v1/documents/{documentId}/restore` | 작성자/P/C | 문서 복원 |
| GET | `/api/v1/projects/{projectId}/documents/graph` | A/P/C | 문서-이슈 그래프 데이터 |
| GET | `/api/v1/projects/{projectId}/document-issue-links` | A/P/C | 연결 목록 |
| POST | `/api/v1/projects/{projectId}/document-issue-links` | A/P/C | 연결 생성 |
| DELETE | `/api/v1/document-issue-links/{linkId}` | 생성자/P/C | 연결 해제 |
| GET | `/api/v1/issues/{issueId}/documents` | A/P/C | 이슈 기준 연결 문서 |
| GET | `/api/v1/documents/{documentId}/issues` | A/P/C | 문서 기준 연결 이슈 |

이슈 상태 변경은 같은 상태에 대해 200 no-op이며 history를 남기지 않는다. trashed 이슈의 상태/내용 변경은 `409 CONFLICT`다. trash/restore는 등록자 또는 P/C가 실행하며 idempotent 200이고 trash reason은 받지 않는다. `includeTrashed=true`은 active와 trashed를 함께 반환한다. 이슈의 `projectId`, `issueKey`, reporter, source 필드는 immutable이고 source는 `MANUAL`만 생성한다. 담당자와 Sprint는 같은 프로젝트의 ACTIVE project member 및 non-COMPLETED Sprint로 검증한다. COMPANY_ADMIN은 관리 action은 가능하지만 ACTIVE project member가 아니면 담당자가 될 수 없다.

Sprint는 `PLANNED → ACTIVE → COMPLETED` 단방향이며 생성은 항상 PLANNED다. P/C만 생성·수정·시작·완료·일괄 배정할 수 있고 수정은 PLANNED에만 허용한다. 프로젝트별 ACTIVE Sprint는 하나이며 서버 UTC로 `startedAt`/`completedAt`을 기록한다. `GET /api/v1/sprints/{sprintId}`는 Sprint metadata만 반환하며 배정 이슈를 포함하지 않는다. 배정 이슈는 `GET /api/v1/projects/{projectId}/issues?sprintId={sprintId}`로 별도 조회한다. `unassignedOnly=true`는 Sprint가 없는 이슈만 반환하며 `sprintId`와 함께 전달하면 `400 VALIDATION_ERROR`다. 배정은 같은 프로젝트의 non-trashed issue만 대상으로 하고 status를 바꾸지 않는다. 배정 해제는 `PATCH /api/v1/issues/{issueId}`에 `sprintId: null`을 보내며(Issue #113), 그 endpoint는 해제만 허용하고 non-null `sprintId`는 `400`으로 거절해 배정 경로를 위 POST 하나로 유지한다. 완료 시 DONE을 제외한 모든 배정 issue는 원자적으로 `BACKLOG`로 이동하고 Sprint를 clear한다. notification/event 발행은 하지 않는다.

문서 정책: 문서 `type`은 `DOC` 또는 `MEETING_NOTE`이고, `title`은 trim 후 최대 200자이며 `content`는 Markdown 원문이다. 목록은 `type`, `q`, `includeTrashed`(기본 `false`)를 지원하며 기존 표시 순서(id 오름차순)를 유지한다. 문서 조회/생성은 A/P/C, 수정·trash·restore는 작성자 또는 P/C가 수행한다. trash/restore는 idempotent다. 문서와 이슈가 모두 요청 `projectId`에 속해야 link를 만들 수 있고, 중복 link 또는 어느 한 endpoint가 trashed이면 `409 CONFLICT`다. trashed endpoint의 기존 link는 project link list, 기본 graph, reverse read에서 숨긴다.

그래프는 `DOCUMENT`, `ISSUE` node만 반환한다. trashed 문서·이슈와 그 endpoint를 참조하는 edge는 반환하지 않는다. `GET /documents/{documentId}/issues`는 canonical full `IssueResponse` 객체 목록, `GET /issues/{issueId}/documents`는 문서 목록을 반환한다.

문서 optimistic locking은 Sprint 3 decision-needed다. `DOCUMENT_VERSION_CONFLICT`는 boundary error code로만 둔다. request body의 `version` 또는 `revision` 필드, conflict response body, UI 해소 방식은 아직 확정 계약이 아니다.

파일/이미지 첨부는 Sprint 3 decision-needed다. `/api/v1/attachments` full endpoint, S3 presigned URL request/response, metadata schema, 권한, 크기, MIME 정책은 선행 결정 후 추가한다.

---

## 8. 쓰레드/채팅/DM

> **Phase A Sprint 1 구현 범위**: 이 섹션은 후속 MVP reference다. Sprint 1 구현 target은 Auth API와 인증 DB에 한정한다.


| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/projects/{projectId}/threads` | A/P/C | 대상별 쓰레드 조회 |
| POST | `/api/v1/projects/{projectId}/threads` | A/P/C | 쓰레드 생성/get-or-create |
| GET | `/api/v1/threads/{threadId}` | A/P/C | 쓰레드 상세 |
| PATCH | `/api/v1/threads/{threadId}` | 작성자/P/C | 쓰레드 topic 수정 |
| POST | `/api/v1/threads/{threadId}/comments` | A/P/C | append-only 댓글 작성. 멘션 알림은 범위 밖 |
| GET | `/api/v1/projects/{projectId}/messages` | A/P/C | 프로젝트 메시지 목록 |
| POST | `/api/v1/projects/{projectId}/messages` | A/P/C | 프로젝트 메시지 작성 |
| GET | `/api/v1/messages/{messageId}` | A/P/C | 메시지 상세 |
| POST | `/api/v1/projects/{projectId}/messages/mark-read` | A/P/C | 채팅방 읽음 위치 갱신 |
| GET | `/api/v1/projects/{projectId}/messages/unread-count` | A/P/C | 채팅방 unread count |
| GET | `/api/v1/companies/{companyId}/direct-messages/rooms` | A/P/C | DM 방 목록 |
| POST | `/api/v1/companies/{companyId}/direct-messages/rooms` | A/P/C | DM 방 생성/get-or-create |
| GET | `/api/v1/direct-messages/rooms/{roomId}/messages` | 참여자 | DM 메시지 목록 |
| POST | `/api/v1/direct-messages/rooms/{roomId}/messages` | 참여자 | DM 메시지 전송 |
| POST | `/api/v1/direct-messages/rooms/{roomId}/mark-read` | 참여자 | DM 읽음 위치 갱신 |
| GET | `/api/v1/direct-messages/unread-count` | A/P/C | 전체 DM unread count |

### 8.1 WBS 4.7.3.1 1:1 DM REST 정책

- 이 구현은 1:1 room만 지원한다. room은 `(companyId, min(userId), max(userId))`로 정규화하고 unique constraint로 하나만 유지한다. self-DM은 `400 VALIDATION_ERROR`다.
- room 목록과 생성은 path `companyId`의 현재 사용자와 상대 사용자가 모두 ACTIVE company member일 때만 허용한다. 존재하지 않거나 inactive인 상대 사용자는 `400 VALIDATION_ERROR`다.
- room의 메시지 목록, 전송, mark-read는 현재 ACTIVE 사용자가 room 참여자이며 room 회사의 ACTIVE member일 때만 허용한다. 비참여자, 다른 회사 membership, 존재하지 않는 room은 모두 `404 NOT_FOUND`로 처리해 room 존재 여부를 노출하지 않는다.
- `POST /companies/{companyId}/direct-messages/rooms`는 신규 room이면 `201`, 기존 normalized pair room이면 `200`을 반환한다. unique-key race는 insert 후 pair 재조회로 처리한다.
- 메시지 content는 trim 후 1~10,000자다. 메시지 목록은 생성 시각과 ID 오름차순으로 반환한다.
- mark-read body는 `lastReadDirectMessageId`만 받는다. client `readAt`은 허용하지 않으며 server UTC로만 저장한다. 포인터는 같은 room 메시지여야 하고 뒤로 이동하지 않는다.
- unread는 현재 사용자가 보낸 메시지를 제외하고 개인별 `dm_room_user_states.last_read_direct_message_id`로 계산한다. 상대방 read receipt는 제공하지 않는다.

1:1 DM core는 WBS 4.7.3.1 Backend 구현 범위다. 다자간 DM, DM room + participants 일반화, DM 상대 read receipt는 이번 개발 기간 보류다. 영구 제외하지 않으며 향후 재검토 전 endpoint, request/response contract, schema, frontend 구현을 추가하지 않는다. DM 메시지 삭제와 DM 메시지 이슈 전환도 full endpoint contract를 만들지 않는다.

---

## 9. 알림

> **Phase A Sprint 1 구현 범위**: 이 섹션은 후속 MVP reference다. Sprint 1 구현 target은 Auth API와 인증 DB에 한정한다.


| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/notifications` | A/P/C | 본인 알림 목록 |
| GET | `/api/v1/notifications/unread-count` | A/P/C | unread count |
| POST | `/api/v1/notifications/{notificationRecipientId}/read` | 해당 수신 알림의 소유자 | 알림 읽음 처리 |
| POST | `/api/v1/notifications/read-all` | A/P/C | 전체 알림 읽음 처리 |
| GET | `/api/v1/notification-preferences` | A/P/C | 알림 설정 조회 |
| PUT | `/api/v1/notification-preferences` | A/P/C | 전체/이벤트별 on/off 설정 |

알림 목록, 읽음 처리, unread count의 Backend 구현 row는 `4.8.1`, 후속 S11 Frontend 연동 row는 `4.8.2`다. 요구사항 추적의 primary WBS는 child가 아니라 `NOTI-01`=`4.8`을 유지한다. `GLOBAL` scope preference는 reference 계약으로만 유지한다. `PROJECT` scope preference는 Sprint 3 decision-needed API boundary이며 실제 지원 기준 확정 전에는 `PROJECT_NOTIFICATION_SCOPE_DEFERRED`로 처리한다. `NOTI-02/03`은 현재 계약/reference row로 유지하고 요구사항추적표 primary WBS backfill 대상에서 제외한다.

`notificationRecipientId`는 `notification_recipients.id`를 의미한다. API는 로그인 사용자 소유의 수신 알림만 읽음 처리한다.

WBS `4.8.1`은 MySQL의 기존 `notification_events`, `notification_recipients`만 JPA mapping하여 inbox/read-state core를 제공한다. `notification_preferences` API와 producer wiring/event-recipient matrix는 이번 slice에 포함하지 않는다. Redis, push/WebSocket, NoSQL 알림 저장소도 제공하지 않는다.

### 9.1 WBS 4.8.1 Notification Core 정책

- 알림 `type` catalog는 `ISSUE_ASSIGNED`, `ISSUE_STATUS_CHANGED`, `THREAD_COMMENTED`, `MENTIONED`, `DOCUMENT_UPDATED`, `DOCUMENT_LINKED_TO_ISSUE`, `SPRINT_STARTED`, `SPRINT_ENDED`, `PROJECT_MESSAGE`, `DIRECT_MESSAGE`, `MEMBER_ADDED`, `ROLE_CHANGED`로 고정한다.
- target type catalog는 `ISSUE`, `DOCUMENT`, `MESSAGE`, `THREAD`, `DIRECT_MESSAGE_ROOM`, `PROJECT`, `SPRINT`로 고정한다.
- `GET /notifications` query는 선택적 `companyId`, `projectId`, `type`, `unreadOnly`, `cursor`, `size`를 받는다. `unreadOnly` 기본값은 `false`, `size` 기본값은 20이며 허용 범위는 1~100이다.
- 목록은 `notification_recipients.created_at DESC, notification_recipients.id DESC` keyset 순서다. cursor는 opaque URL-safe HMAC 서명값이며 사용자 ID, 정규화된 `companyId`/`projectId`/`type`/`unreadOnly`, 마지막 `(createdAt,id)`를 결합한다. 변조되거나 다른 사용자·filter에서 재사용한 cursor는 `400 VALIDATION_ERROR`다.
- 모든 목록, unread count, read-all query/update는 `recipient_user_id = current active user`를 강제한다. filter 없는 본인 inbox 조회는 허용한다.
- `companyId` filter는 해당 회사 ACTIVE membership이 필요하다. `projectId` filter는 프로젝트 회사의 ACTIVE `COMPANY_ADMIN` 또는 ACTIVE company member이면서 ACTIVE project member여야 한다. 두 filter를 함께 보내면 프로젝트의 회사와 일치해야 하며 불일치는 `400 VALIDATION_ERROR`다.
- `GET /notifications/unread-count`는 선택적 `companyId`, `projectId`, `type`을 목록과 같은 scope 규칙으로 적용하고 `read_at IS NULL`인 본인 row만 계산한다.
- 개별 read는 `notification_recipients.id`를 잠근 뒤 본인 소유인지 검사한다. 없는 ID는 404, 다른 사용자 소유 ID는 403이다. 최초 호출에만 server UTC `read_at`을 기록하고 반복 호출은 기존 `readAt`을 보존하는 idempotent 동작이다.
- read-all body의 `companyId`, `projectId`, `type`은 모두 선택적이며 빈 object는 본인의 모든 unread 알림을 대상으로 한다. scope와 type 검증 후 `read_at IS NULL`인 matching row만 한 번에 갱신하고 실제 변경 row 수를 `updatedCount`로 반환한다.
- 이번 Backend slice는 기존 알림 row를 조회·읽음 처리하는 consumer boundary다. 알림 producer, event publisher, event/recipient matrix, preferences, Push/WebSocket/Redis와 Frontend 구현 완료를 의미하지 않는다.

### 9.2 Notifications Error Cases

| API | 상황 | HTTP | `error.code` | message 예시 |
|---|---|---:|---|---|
| `GET /notifications` | `type` enum 값 오류 | 400 | `VALIDATION_ERROR` | 알림 조회 조건이 올바르지 않습니다. |
| `GET /notifications` | `size`가 1~100 범위를 벗어남 | 400 | `VALIDATION_ERROR` | 알림 목록 size는 1 이상 100 이하여야 합니다. |
| `GET /notifications` | cursor 변조 또는 사용자/filter 재사용 | 400 | `VALIDATION_ERROR` | 알림 목록 cursor가 올바르지 않습니다. |
| `GET /notifications` | `companyId`와 `projectId`의 회사 불일치 | 400 | `VALIDATION_ERROR` | companyId와 projectId의 회사 범위가 일치하지 않습니다. |
| `GET /notifications` | 접근 권한 없는 `companyId` 또는 `projectId` 지정 | 403 | `FORBIDDEN` | 해당 범위의 알림을 조회할 수 없습니다. |
| `GET /notifications/unread-count` | 접근 권한 없는 `companyId` 또는 `projectId` 지정 | 403 | `FORBIDDEN` | 해당 범위의 알림 수를 조회할 수 없습니다. |
| `POST /notifications/{notificationRecipientId}/read` | `notificationRecipientId`가 존재하지 않음 | 404 | `NOT_FOUND` | 알림을 찾을 수 없습니다. |
| `POST /notifications/{notificationRecipientId}/read` | 조회된 수신 알림의 `user_id`가 현재 사용자와 다름 | 403 | `FORBIDDEN` | 본인 알림만 읽음 처리할 수 있습니다. |
| `POST /notifications/read-all` | `type` enum 값 오류 | 400 | `VALIDATION_ERROR` | 전체 읽음 처리 조건이 올바르지 않습니다. |
| `GET /notification-preferences` | `scopeType=PROJECT` 요청 | 409 | `PROJECT_NOTIFICATION_SCOPE_DEFERRED` | 프로젝트별 알림 설정은 Sprint 3 decision-needed입니다. |
| `PUT /notification-preferences` | `notificationType=ALL` 외부 요청 | 400 | `VALIDATION_ERROR` | `notificationType=ALL`은 외부 API에서 사용할 수 없습니다. |
| `PUT /notification-preferences` | `enabled`와 `allEnabled`를 동시에 전달 | 400 | `VALIDATION_ERROR` | 알림 설정 요청 형식이 올바르지 않습니다. |

---

## 10. 조건부 확장 API

> **Phase A Sprint 1 구현 범위**: 이 섹션은 후속 MVP reference다. Sprint 1 구현 target은 Auth API와 인증 DB에 한정한다.


| Method | Endpoint | 권한 | 설명 | 상태 |
|---|---|---|---|---|
| GET | `/api/v1/projects/{projectId}/activity-events` | A/P/C | 프로젝트 활동 피드 조회 | Sprint 3 후보 |
| GET | `/api/v1/search` | A/P/C | 프로젝트 내부 통합 검색 | WBS `4.9.4.1` Backend 구현 완료 |
| GET | `/api/v1/projects/{projectId}/dashboard` | A/P/C | 프로젝트 홈/대시보드 summary | Sprint 3 후보 |

### 10.1 활동 피드

- MySQL `activity_events`를 원장으로 사용한다.
- STOMP push topic: `/topic/projects.{projectId}.activity`.
- push 메시지는 새 이벤트 신호만 보내고, 클라이언트는 REST API로 feed를 재조회한다.
- Redis cache key 예시: `activity:project:{projectId}:latest`.

### 10.2 WBS 4.9.4.1 통합 검색 Backend 정책

`GET /api/v1/search`는 인증된 ACTIVE 사용자가 권한 있는 하나의 프로젝트 안에서 통합 검색을 수행하는 Backend 계약이다. 검색 방식은 WBS `4.9.2.1` 완료 결정에 따라 MySQL `LIKE`를 사용한다. `4.9.2`와 `4.9.4`는 진행 중 50% parent이며, `4.9.2.2`와 `4.9.4.2` Frontend row는 계획/0% 상태다.

#### Request

```http
GET /api/v1/search?projectId=20&q=로그인&size=20
```

| Parameter | 필수 | 규칙 |
|---|---|---|
| `projectId` | 필수 | 검색 범위가 되는 프로젝트 ID다. |
| `q` | 필수 | 앞뒤 공백을 제거한 nonblank 문자열이며 최대 100자다. |
| `cursor` | 선택 | 서버가 발급한 opaque signed cursor다. |
| `size` | 선택 | 양수만 허용한다. 기본값은 20이고 50을 초과하면 50으로 cap한다. |

#### 검색 대상과 가시성

- source type은 `ISSUE`, `DOCUMENT`, `THREAD`, `PROJECT_MESSAGE` 네 가지다.
- Issue는 `issueKey`, `title`, `description`을 검색하며 trashed Issue는 제외한다.
- Document는 `title`, `content`를 검색하며 trashed Document는 제외한다.
- Thread는 `topic`과 replies를 검색한다.
- Project Message는 top-level 메시지만 검색한다. Thread reply, DM 메시지, 다른 프로젝트 메시지는 검색 대상이 아니다.
- 모든 source는 기존 ACTIVE company membership과 ACTIVE project membership, 또는 기존 company/project admin authorization을 먼저 적용한다. 권한 없는 프로젝트와 항목은 결과에 노출하지 않는다.

#### Query, 정렬, cursor

- query는 parameterized literal `LIKE`로 처리한다. `%`, `_`, `\\`는 literal로 검색되도록 escape하고 SQL 문자열을 조합하지 않는다.
- 결과는 `(sortAt DESC, type ASC, id DESC)`로 정렬한다. `sortAt`은 source의 검색 정렬 시각이며, 같은 값에서도 중복이나 누락 없이 순서를 결정한다.
- cursor는 사용자 ID, `projectId`, trim된 `q`, 마지막 `(sortAt, type, id)`와 sort definition에 결합한 signed 값이다. 변조하거나 다른 사용자, 프로젝트, query에서 재사용하면 `400 VALIDATION_ERROR`다.

#### Response

성공 응답은 `ApiResponse<SearchData>`이며 `data.results`와 `data.nextCursor`를 사용한다. 모든 result는 `id`, `type`, `title`, `snippet`, `sortAt`만 포함한다. source별 원본 field나 `projectId`는 반환하지 않는다. 다음 page가 없으면 `data.nextCursor`는 `null`이다.

```json
{
  "data": {
    "results": [
      {
        "id": 101,
        "type": "ISSUE",
        "title": "로그인 오류",
        "snippet": "로그인 화면에서 오류가 발생한다.",
        "sortAt": "2026-07-17T03:00:00Z"
      }
    ],
    "nextCursor": "next_cursor"
  },
  "meta": { "requestId": "req_..." }
}
```

#### Error cases

| 상황 | HTTP | `error.code` |
|---|---:|---|
| `projectId` 또는 `q` 누락, blank `q`, 100자 초과 query, 양수가 아닌 `size` | 400 | `VALIDATION_ERROR` |
| 변조된 cursor 또는 다른 사용자/project/query에 재사용한 cursor | 400 | `VALIDATION_ERROR` |
| 인증되지 않았거나 ACTIVE 사용자가 아님 | 401 | `UNAUTHORIZED` |
| 기존 authorization 정책상 프로젝트 검색 권한이 없음 | 403 | `FORBIDDEN` |

#### 비범위

MySQL FULLTEXT, 외부 검색 엔진, Redis, DB schema/index 변경, Frontend, 한국어 형태소 분석, 초성·오타·동의어·fuzzy·autocomplete 검색은 이 Backend slice에 포함하지 않는다.

### 10.3 프로젝트 대시보드

- progress, recentIssues, recentDocuments, recentActivities, sprintSummary를 반환한다.
- 기본은 MySQL aggregate query다.
- 성능 문제가 확인되면 `project_dashboard_summaries`와 Redis cache를 추가한다.

---

## 10-A. WBS 4.10.1 Support Case Backend

플랫폼 처리 API는 `/api/v1/platform` namespace를 사용한다. 모든 endpoint는 JWT Bearer token이 필요하다. platform action 인가는 `platform_admins`에서만 판정하며 회사 또는 프로젝트 membership role은 사용하지 않는다.

| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/v1/support-cases` | 인증 ACTIVE 사용자 | 문의/신고 생성 |
| GET | `/api/v1/support-cases` | 인증 ACTIVE 사용자 | 본인 case 목록 |
| GET | `/api/v1/support-cases/{supportCaseId}` | reporter 본인 | 본인 case 상세와 public reply |
| GET | `/api/v1/platform/support-cases` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | 전체 case 목록 |
| GET | `/api/v1/platform/support-cases/{supportCaseId}` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | case 상세와 public reply |
| POST | `/api/v1/platform/support-cases/{supportCaseId}/public-replies` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | 사용자-facing 답변 생성 |
| GET | `/api/v1/platform/support-cases/{supportCaseId}/internal-notes` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | 내부 메모 조회 |
| POST | `/api/v1/platform/support-cases/{supportCaseId}/internal-notes` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | 내부 메모 생성 |
| PATCH | `/api/v1/platform/support-cases/{supportCaseId}/status` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | 상태 전이 |

### 10-A.1 생성과 mutation request

```json
{
  "type": "REPORT",
  "title": "신고 제목",
  "body": "신고 내용",
  "targetType": "CONTENT",
  "targetId": "0b7fd3f9-c84f-4f15-b020-a942a9463c55",
  "reason": "초기 신고 접수"
}
```

- `INQUIRY`는 `targetType`, `targetId`를 사용하지 않는다.
- `REPORT`는 `targetType`이 필수이고 `targetId`는 nullable UUID다. target domain FK와 존재 검증은 WBS `4.10.1`에서 수행하지 않는다.
- 생성, public reply, internal note, 상태 변경 request는 mutation body와 별개인 `reason`을 필수로 받는다. reason은 trim 후 3~500자다.
- public reply와 internal note 생성 body는 `{ "body": "...", "reason": "..." }`, 상태 변경 body는 `{ "status": "IN_PROGRESS", "reason": "..." }`다.

### 10-A.2 조회 응답과 가시성

case object는 `id`, `reporterUserId`, `type`, `status`, `targetType`, `targetId`, `title`, `body`, `createdAt`, `updatedAt`을 반환한다. 상세 응답은 아래 shape를 사용한다.

```json
{
  "data": {
    "supportCase": {
      "id": 10,
      "reporterUserId": 1,
      "type": "INQUIRY",
      "status": "OPEN",
      "targetType": null,
      "targetId": null,
      "title": "계정 문의",
      "body": "문의 내용",
      "createdAt": "2026-07-15T03:00:00Z",
      "updatedAt": "2026-07-15T03:00:00Z"
    },
    "publicReplies": []
  },
  "meta": { "requestId": "req_..." }
}
```

일반 사용자 목록·상세 query와 DTO는 internal note를 포함하지 않는다. internal note는 platform 전용 endpoint의 `data.internalNotes`에서만 반환한다. 다른 사용자의 case detail은 ownership을 노출하지 않도록 `404 NOT_FOUND`로 처리한다.

### 10-A.3 상태와 audit

허용 전이는 `OPEN→IN_PROGRESS/REJECTED/CLOSED`, `IN_PROGRESS→RESOLVED/REJECTED/CLOSED`, `RESOLVED→CLOSED`, `REJECTED→CLOSED`다. `CLOSED`는 terminal이며 같은 상태 전이와 reopen은 `409 CONFLICT`다.

각 mutation은 actor, authority source/role, action, target, reason, before/after state, UTC timestamp, `supportCaseId`를 immutable audit에 같은 transaction으로 저장한다. audit 저장 실패 시 mutation을 rollback한다. 처리 결과 notification은 생성하지 않는다.

---

## 10-B. WBS 4.10.3 System Settings Backend

system settings는 `/api/v1/platform/system-settings` namespace를 사용한다. 모든 endpoint는 JWT Bearer token과 `platform_admins` 기반 authority를 요구하며 company/project membership은 권한 근거가 아니다.

| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/platform/system-settings` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | 전체 8개 code-owned catalog value 조회 |
| GET | `/api/v1/platform/system-settings/{key}` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | 한 code-owned catalog value 조회 |
| PUT | `/api/v1/platform/system-settings/{key}` | `SUPER_ADMIN` | 한 key의 JSON value를 whole-document replacement |

### 10-B.1 Catalog 및 default

허용 path key는 `SUPPORT_CASE_CATEGORIES`, `REPORT_REASON_CATEGORIES`, `ENFORCEMENT_REASON_PRESETS`, `ENFORCEMENT_DURATION_PRESETS`, `REPLY_TEMPLATES`, `ANNOUNCEMENT_BANNER`, `SUPPORT_CENTER_STATUS`, `RESULT_NOTIFICATION_POLICY`로 정확히 8개다. unknown key는 `400 VALIDATION_ERROR`이며 arbitrary key 생성·삭제·부분 수정 API는 제공하지 않는다.

clean-rebuild seed는 schema version `1`이고 policy examples만 이용한다. support category는 `ACCOUNT`, `COMPANY`, `PROJECT`, `BUG`, `OTHER`, report reason은 `SPAM`, `IMPERSONATION`, `MALICIOUS_CONTENT`, `PERMISSION_ABUSE`, enforcement duration은 1일·7일·30일·무기한(null)이며, banner는 disabled, support center는 `OPEN`, result notification 두 항목은 enabled다.

JSON value의 top-level/object field는 key별 code-owned validator가 검증한다. categories/reasons는 `{ "categories"|"presets": [{ "code", "label" }] }`, duration은 `{ "presets": [{ "code", "durationDays" }] }`, template는 `{ "templates": [{ "code", "title", "body" }] }`, banner는 `{ "enabled", "message" }`, support center는 `{ "status", "message" }`, notification policy는 `{ "notifyOnPublicReply", "notifyOnCaseStatusChange" }` shape다. unknown JSON field, 타입 오류, blank required string, non-positive duration은 `400 VALIDATION_ERROR`다.

### 10-B.2 Response와 replacement

`GET /api/v1/platform/system-settings` response는 `data.settings` 배열을, `GET /api/v1/platform/system-settings/{key}` response는 같은 object를 `data`로 반환한다. `valueSchemaVersion`은 현재 1이며 `version`은 write의 optimistic concurrency 값이다. 존재하지 않는 유효 key는 `404 NOT_FOUND`다.

```json
{
  "data": {
    "settings": [
      {
        "id": 1,
        "key": "SUPPORT_CENTER_STATUS",
        "value": { "status": "OPEN", "message": "문의 접수가 가능합니다." },
        "valueSchemaVersion": 1,
        "version": 1,
        "createdAt": "2026-07-16T00:00:00Z",
        "updatedAt": "2026-07-16T00:00:00Z"
      }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

`PUT /api/v1/platform/system-settings/SUPPORT_CENTER_STATUS` request는 value 전체, positive `expectedVersion`, trim 후 3~500자인 `reason`을 모두 요구한다.

```json
{
  "value": {
    "status": "DELAYED",
    "message": "문의 답변이 지연되고 있습니다."
  },
  "expectedVersion": 1,
  "reason": "운영 인력 부재에 따른 응답 지연 안내"
}
```

성공하면 `data`에 갱신된 setting을 반환한다. setting row를 transaction 내 pessimistic lock으로 읽어 stale `expectedVersion`은 `409 SYSTEM_SETTING_VERSION_CONFLICT`, 동일 JSON value는 `409 SYSTEM_SETTING_NO_CHANGE`으로 구분한다. 성공 replacement만 version을 정확히 한 번 증가시키며, immutable `admin_action_audits`에는 setting key, schema version, replacement 전/후 persisted version만 같은 transaction으로 기록한다. setting value 전체는 audit snapshot에 저장하지 않는다. audit write 실패 시 replacement는 rollback한다. notification dispatch, enforcement execution, OAuth/runtime/feature-flag setting은 이 API 범위에 포함하지 않는다.

---

## 10-C. WBS 4.10.4 Platform Admin Lifecycle

| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/platform/actor` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | 현재 platform actor identity 조회 |
| GET | `/api/v1/platform/admins` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | 활성·revoke 이력을 포함한 관리자 목록 |
| POST | `/api/v1/platform/admins` | `SUPER_ADMIN` | ACTIVE 사용자에게 platform role 부여 |
| POST | `/api/v1/platform/admins/{platformAdminId}/revoke` | `SUPER_ADMIN` | 관리자 권한 soft revoke |
| POST | `/api/v1/platform/admins/{platformAdminId}/reactivate` | `SUPER_ADMIN` | revoke 이력 재활성화 |
| PATCH | `/api/v1/platform/admins/{platformAdminId}/role` | `SUPER_ADMIN` | ACTIVE 관리자 role 변경 |
| GET | `/api/v1/platform/audits` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | redacted platform audit keyset 조회 |
| GET | `/api/v1/platform/companies` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | 회사 목록 keyset 조회 |
| PATCH | `/api/v1/platform/companies/{companyId}/status` | `PLATFORM_ADMIN`, `SUPER_ADMIN` | 회사 상태 변경과 immutable audit 기록 |

- lifecycle write는 singleton lock row를 먼저 `PESSIMISTIC_WRITE`로 획득한 뒤 token actor와 ACTIVE `platform_admins` row를 새 DB query로 재인가한다. 캐시된 authority로 write하지 않는다.
- revoke는 `status=REVOKED`, `revokedAt`을 기록하며 revoked row는 모든 platform API authority에서 제외된다. reactivate는 ACTIVE 사용자만 가능하며 `reactivatedAt`을 기록한다.
- 마지막 ACTIVE `SUPER_ADMIN`은 revoke하거나 `PLATFORM_ADMIN`으로 demote할 수 없다. 모든 lifecycle mutation은 3~500자 reason과 같은 transaction의 immutable audit를 요구한다.
- audit write와 read-side response는 action/target allowlist를 중앙에서 적용한다. support case snapshot에는 raw `title`/`body`를, system setting snapshot에는 `value` 전체를 저장하지 않는다.
- lifecycle conflict는 `LAST_ACTIVE_SUPER_ADMIN`, `DUPLICATE_PLATFORM_ADMIN_HISTORY`, `PLATFORM_ADMIN_ALREADY_ACTIVE`, `PLATFORM_ADMIN_REVOKED`, `PLATFORM_ADMIN_ROLE_NO_CHANGE`의 명시적 `409` error code를 사용한다.

### 10-C.1 Audit read

`GET /api/v1/platform/audits`는 ACTIVE `PLATFORM_ADMIN` 또는 `SUPER_ADMIN`만 호출할 수 있으며 company/project membership을 authorization 근거로 사용하지 않는다. query filter는 선택적 `actorUserId`, `action`, `targetType`, `targetId`, `supportCaseId`, `createdFrom`, `createdTo`, `cursor`, `size`다. ID는 양수여야 하고 `targetId`에는 `targetType`이 필요하다. 시간은 UTC ISO-8601이고 범위는 `[createdFrom, createdTo)`이며 from은 to보다 늦을 수 없다. 기본 size는 20, 최대는 100이다.

정렬은 `(createdAt DESC, id DESC)`의 keyset이며 cursor는 HMAC-SHA256 opaque 값으로 requester user ID와 정규화된 모든 filter 및 마지막 key를 묶는다. cursor 변조, 다른 사용자 또는 filter 재사용은 `400 VALIDATION_ERROR`다. payload에는 audit snapshot, reason, email/name 등 PII를 넣지 않는다.

응답 `data`는 `audits`, `nextCursor`, `hasNext`, `size`를 포함한다. 각 audit은 `id`, `supportCaseId`, `actorUserId`, `actorPlatformAdminId`, `authoritySource`, `authorityRole`, `action`, `targetType`, `targetId`, `reason`, `beforeState`, `afterState`, `createdAt`만 반환한다. persisted state는 object JSON만 parse한 뒤 중앙 sanitizer를 다시 적용하고 scalar allowlist만 반환한다. null은 null, malformed/non-object state는 `{}`이며 raw snapshot text, email, name은 반환하지 않는다.

audit record의 3년 보존 정책은 유지한다. cleanup/purge/archive job, legacy data sanitation/migration은 이 API 범위가 아니다.

### 10-C.2 Platform company operations

`GET /api/v1/platform/companies`와 `PATCH /api/v1/platform/companies/{companyId}/status`는 ACTIVE `PLATFORM_ADMIN` 또는 `SUPER_ADMIN`만 호출할 수 있다. authorization은 `platform_admins`만 사용하며 company/project membership은 필요하지도, 권한 근거도 아니다.

#### GET `/api/v1/platform/companies`

| Parameter | 필수 | 규칙 |
|---|---|---|
| `status` | 선택 | `ACTIVE` 또는 `INACTIVE`, 대소문자 무관. 다른 값은 `400 VALIDATION_ERROR`다. |
| `q` | 선택 | blank는 미지정으로 처리한다. 앞뒤 공백 제거 후 회사명 대소문자 무관 부분 일치이며 최대 150자다. |
| `cursor` | 선택 | 서버가 발급한 opaque HMAC-SHA256 cursor다. |
| `size` | 선택 | 기본 20, 허용 범위 1~100이다. |

결과는 `id DESC`로 정렬한다. cursor가 있으면 마지막 회사 ID보다 작은 row만 조회하며 `size + 1` 방식으로 다음 page를 판정한다. cursor는 고정 domain `PLATFORM_COMPANY`, version, requester user ID, 정규화된 `status`, 원문 회사명을 포함하지 않는 정규화된 `q` binding, 마지막 company ID에 서명 결합한다. 변조, 다른 requester/filter/domain의 cursor 재사용, malformed cursor는 모두 `400 VALIDATION_ERROR`다.

성공 응답은 `ApiResponse`의 `data.companies`, `data.nextCursor`, `data.hasNext`, `data.size`를 반환한다. `companies`의 각 항목은 기존 `CompanyResponse`(`id`, `name`, `representativeName`, `description`, `industryCategory`, `logoUrl`, `status`, `createdAt`, `updatedAt`)다. 다음 page가 없으면 `nextCursor`는 `null`이고 `hasNext`는 `false`다. member count, membership, project 정보와 별도 description 확장은 반환하지 않는다.

```json
{
  "data": {
    "companies": [
      {
        "id": 42,
        "name": "Threadia",
        "representativeName": "홍길동",
        "description": "협업 플랫폼 회사",
        "industryCategory": "IT",
        "logoUrl": null,
        "status": "ACTIVE",
        "createdAt": "2026-07-20T00:00:00Z",
        "updatedAt": "2026-07-20T00:00:00Z"
      }
    ],
    "nextCursor": "opaque_cursor",
    "hasNext": true,
    "size": 20
  },
  "meta": { "requestId": "req_..." }
}
```

#### PATCH `/api/v1/platform/companies/{companyId}/status`

기존 상태 변경 계약은 유지한다. request body는 `status`와 trim 후 3~500자인 `reason`을 받으며, `status`는 `ACTIVE` 또는 `INACTIVE`다. 같은 상태 요청은 `409 CONFLICT`, 없는 회사는 `404 NOT_FOUND`다. 성공 시 기존 `CompanyData` wrapper의 `data.company`를 반환하고, 상태 전/후와 reason을 포함한 immutable platform audit를 같은 transaction에 기록한다.

| API | 상황 | HTTP | `error.code` |
|---|---|---:|---|
| `GET /platform/companies` | 잘못된 `status`, 150자 초과 `q`, 1~100 밖 `size`, malformed/tampered/requester/filter/domain 불일치 cursor | 400 | `VALIDATION_ERROR` |
| `GET /platform/companies` | JWT Bearer token 없음 또는 invalid | 401 | `UNAUTHORIZED` |
| `GET /platform/companies` | ACTIVE platform admin이 아님 | 403 | `FORBIDDEN` |
| `PATCH /platform/companies/{companyId}/status` | 잘못된 request 또는 reason | 400 | `VALIDATION_ERROR` |
| `PATCH /platform/companies/{companyId}/status` | 동일 상태 | 409 | `CONFLICT` |
| `PATCH /platform/companies/{companyId}/status` | 회사 없음 | 404 | `NOT_FOUND` |

## 11. MVP 보류/Stub 목록

| 구분 | API/기능 | 처리 | 이유 |
|---|---|---|---|
| 후속 범위 | S15 Backend 이후 범위 | `4.10.4` Backend lifecycle/audit-read 구현 완료 | 사용자/회사 enforcement, dashboard/notification 확장, S15 Frontend, retention/cleanup은 이번 slice에서 제외 |
| 이번 개발 기간 보류 | 다자간 DM 방 | full endpoint 미정 | 향후 재검토 전 endpoint, request/response contract, schema, frontend 구현을 추가하지 않음 |
| 보류 | DM 메시지 삭제 | 미제공 | 전송/조회/unread 중심 |
| 이번 개발 기간 보류 | DM 상대 read receipt | full endpoint 미정 | 향후 재검토 전 endpoint, request/response contract, schema, frontend 구현을 추가하지 않음 |
| 보류 | DM 메시지→이슈 전환 | `DM_MESSAGE_CANNOT_CREATE_ISSUE` | DM은 사적 소통 |
| 보류 | Thread→DM 공유 | `THREAD_DM_SHARE_DEFERRED` | 정책 미정 |
| 보류 | Thread→Issue 추가 연결 | `THREAD_ISSUE_LINK_DEFERRED` | 데이터 모델 미정 |
| Sprint 3 decision-needed | 문서 optimistic locking | `DOCUMENT_VERSION_CONFLICT` boundary만 유지 | version/revision 정책 결정 필요 |
| Sprint 3 decision-needed | 이슈/문서 영구 삭제 | `PERMANENT_DELETE_DEFERRED` | 참조 무결성, audit, 보존 기간, 권한 기준 필요 |
| Sprint 3 decision-needed | 프로젝트 복원 | `PROJECT_RESTORE_DEFERRED` | 복원 권한, UI, API 기준 필요 |
| Sprint 3 decision-needed | 프로젝트별 알림 설정 | `PROJECT_NOTIFICATION_SCOPE_DEFERRED` API boundary | PROJECT scope 지원 기준 필요. 요구사항추적표 primary WBS row로 backfill하지 않는다 |
| 보류 | NoSQL 알림/활동 저장소 | 미제공 | MySQL 원장 사용, Sprint 1 제외 |
| 제외 | Redis 원본 저장 | 미제공 | Redis는 cache/push 보조로만 사용 |
| Sprint 3 decision-needed | 범용 파일 첨부 | `/api/v1/attachments` full contract 미정 | S3 presigned URL, metadata, 권한, 크기, MIME 정책 필요 |
| 보류 | Typesense/Meilisearch | 미제공 | 검색 고도화 후보 |

---

## 12. 6일차 이월 기술 검토

- JWT Bearer access token과 refresh token을 함께 사용한다. Access token lifetime은 30분, refresh token lifetime은 14일이며, refresh token은 서버에 hash로 저장하고 성공한 refresh 시 rotation, logout 시 invalidation한다. Browser는 access token memory 저장, refresh token `HttpOnly`, `SameSite=Lax` cookie 저장을 권장하며 HTTPS 환경에서는 `Secure`를 사용한다.
- 이슈 목록 페이지네이션 방식.
- 이슈 상태 변경 시 알림 자동 발송 로직.
- `from-message` 엔드포인트에서 thread 자동 생성 여부.
- 메시지 목록 실시간 업데이트 방식.
- DM 방 unread count 처리 방식.
- `activity_events`와 `notification_events`는 분리 운영한다. activity 이벤트가 notification 이벤트를 생성하는 조건은 구현 단계에서 이벤트별로 제한한다.
- S15 `4.10.1`, `4.10.3`, `4.10.4` Backend는 구현 완료 기준이다. enforcement, dashboard/notification 확장, S15 Frontend, retention/cleanup과 attachment, document optimistic locking, advanced DM, PROJECT notification scope, project restore, permanent delete의 full API 계약은 후속 범위다.

---

## 13. 요청/응답 JSON 예시

이 섹션은 v2.6 endpoint, 권한, 보류 범위를 기준으로 구현자가 참고할 수 있는 JSON 예시를 제공한다.

- `v1.4`의 예시는 형식 참고용으로만 사용한다.
- 실제 계약 기준은 이 문서의 endpoint 표와 v2.6 정책이다.
- `PATCH` 요청은 변경할 필드만 전달한다.
- S3 파일 저장 API, Redis/NoSQL 알림 저장 API, 프로젝트별 알림 설정은 현재 full contract로 제공하지 않는다. Sprint 3 decision-needed 항목은 선행 결정 후 request/response 예시를 추가한다.

### 13.1 공통 Error Response

```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "프로젝트 접근 권한이 없습니다.",
    "details": {}
  },
  "meta": { "requestId": "req_..." }
}
```

### 13.2 Auth

#### POST `/api/v1/auth/signup`

Request Body:

```json
{
  "email": "minjeong@threadia.com",
  "password": "P@ssword1234",
  "nickname": "김민정",
  "avatarUrl": null
}
```

Response 201:

```json
{
  "data": {
    "user": {
      "id": 1,
      "email": "minjeong@threadia.com",
      "nickname": "김민정",
      "avatarUrl": null
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/auth/login`

Request Body:

```json
{
  "email": "minjeong@threadia.com",
  "password": "P@ssword1234"
}
```

Response 200:

> **Sprint 1 공식 결정**: login 응답의 `companies`는 Sprint 1에서 빈 배열이다. 회사/프로젝트 membership context는 Sprint 1.5 이후 invitation accept 또는 후속 MVP reference에서 채운다.

```http
Set-Cookie: refreshToken=<refreshToken>; HttpOnly; SameSite=Lax; Path=/; Max-Age=1209600
```

```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 1,
      "email": "minjeong@threadia.com",
      "nickname": "김민정",
      "avatarUrl": null
    },
    "companies": []
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/auth/logout`

Response 200:

```http
Set-Cookie: refreshToken=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0
```

```json
{
  "data": { "success": true },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/auth/me`

> **Sprint 1 공식 결정**: `/auth/me`의 응답은 `data.user` 아래 `id`, `email`, `nickname`, `avatarUrl`만 포함하는 minimal Auth context다. `companyMemberships`는 `/api/v1/users/me/company-memberships`에서 별도로 조회하고, `projectMemberships`, `unreadNotificationCount`, `dmUnreadCount`는 후속 MVP reference 예시다.

Response 200:

```json
{
  "data": {
    "user": {
      "id": 1,
      "email": "minjeong@threadia.com",
      "nickname": "김민정",
      "avatarUrl": null
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/auth/password-reset/request`

Request Body:

```json
{ "email": "minjeong@threadia.com" }
```

#### POST `/api/v1/auth/password-reset/confirm`

Request Body:

```json
{
  "token": "reset-token",
  "newPassword": "NewP@ssword1234"
}
```

### 13.3 User

#### GET `/api/v1/users/me/company-memberships`

현재 로그인 사용자의 ACTIVE 회사 membership context만 반환한다. Frontend S03/RBAC context는 이 endpoint를 기준으로 회사 선택, 회사관리 메뉴 노출, 초기 라우팅을 결정한다. `/api/v1/auth/me`와 `/api/v1/auth/login` 응답 shape는 변경하지 않는다.

Response 200:

```json
{
  "data": {
    "companyMemberships": [
      {
        "companyMemberId": 101,
        "companyId": 10,
        "companyName": "Threadia",
        "role": "COMPANY_ADMIN",
        "status": "ACTIVE"
      }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

ACTIVE membership이 없으면 `companyMemberships`는 빈 배열이다.

#### PATCH `/api/v1/users/me/profile`

현재 인증된 `ACTIVE` 사용자 본인의 `nickname`, `avatarUrl`만 부분 수정한다. `/api/v1/auth/me`는 read-only minimal Auth context를 유지하고, `GET /api/v1/users/me/company-memberships`와 결합하지 않는다. email, status, password는 이 endpoint에서 수정할 수 없다.

- `nickname`과 `avatarUrl` 중 하나 이상을 포함해야 한다. 빈 객체는 `400 VALIDATION_ERROR`다.
- `nickname`을 포함하면 `null`일 수 없고 2~100자여야 한다.
- `avatarUrl`을 포함하면 최대 500자다. URL 형식 validation은 하지 않는다.
- 포함하지 않은 필드는 기존 값을 보존한다. `"avatarUrl": null`은 avatar를 명시적으로 제거한다.
- 지원하지 않는 request field는 기존 Jackson request parsing 정책에 따라 거부한다.
- Authorization 헤더 누락/형식 오류/유효하지 않은 token, 존재하지 않거나 `ACTIVE`가 아닌 사용자는 기존 `401 UNAUTHORIZED` error envelope을 반환한다.

Request Body:

```json
{
  "nickname": "김민정",
  "avatarUrl": "https://.../profile.png"
}
```

avatar만 제거하는 Request Body:

```json
{
  "avatarUrl": null
}
```

Response 200:

```json
{
  "data": {
    "user": {
      "id": 1,
      "email": "minjeong@threadia.com",
      "nickname": "김민정",
      "avatarUrl": "https://.../profile.png"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

### 13.4 Company / Department / Company Member

#### POST `/api/v1/companies`

Request Body:

```json
{
  "name": "Threadia Inc.",
  "representativeName": "황스일",
  "description": "개발팀을 위한 협업 플랫폼",
  "industryCategory": "IT/소프트웨어",
  "logoUrl": null
}
```

Response 201:

```json
{
  "data": {
    "company": {
      "id": 10,
      "name": "Threadia Inc.",
      "representativeName": "황스일",
      "description": "개발팀을 위한 협업 플랫폼",
      "industryCategory": "IT/소프트웨어",
      "logoUrl": null,
      "createdAt": "2026-06-16T09:00:00Z"
    },
    "companyMember": {
      "id": 100,
      "role": "COMPANY_ADMIN",
      "status": "ACTIVE"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### PATCH `/api/v1/companies/{companyId}`

Request Body:

```json
{
  "name": "Threadia Corp.",
  "representativeName": "황스일",
  "description": "업데이트된 설명",
  "industryCategory": "IT/소프트웨어",
  "logoUrl": "https://.../logo.png"
}
```

#### POST `/api/v1/companies/{companyId}/logo`

Request Body:

```json
{ "logoUrl": "https://.../logo.png" }
```

> Sprint 1은 파일 업로드 API가 아니라 URL 저장으로 시작한다. S3 파일 저장 구현과 presigned URL API는 Sprint 1 이후로 둔다.

#### POST `/api/v1/companies/{companyId}/departments`

Request Body:

```json
{ "name": "디자인팀" }
```

#### PATCH `/api/v1/departments/{departmentId}`

Request Body:

```json
{ "name": "UX/디자인팀" }
```

#### DELETE `/api/v1/departments/{departmentId}`

Response 200:

```json
{
  "data": {
    "deleted": true,
    "affectedMemberCount": 3
  },
  "meta": { "requestId": "req_..." }
}
```

#### Deprecated/Internal Note: POST `/api/v1/companies/{companyId}/company-members`

> Sprint 1.5부터 이 endpoint는 user-facing 구성원 생성 API로 제공하지 않는다.
> 회사 구성원 추가 흐름은 `POST /api/v1/companies/{companyId}/invitations`로 invitation을 만들고, 대상자가 `POST /api/v1/invitations/{invitationId}/accept`를 호출할 때 membership을 생성한다.
> 기존 membership의 역할, 부서, 상태 변경은 `PATCH /api/v1/companies/{companyId}/company-members/{companyMemberId}`에서 처리한다.

#### PATCH `/api/v1/companies/{companyId}/company-members/{companyMemberId}`

Request Body:

```json
{
  "departmentId": 2,
  "role": "COMPANY_ADMIN",
  "status": "ACTIVE"
}
```

`status: "LEFT"`는 이 endpoint로 전환할 수 없다 — `409 CONFLICT`를 반환하고 DELETE endpoint 사용을 요구한다.

#### DELETE `/api/v1/companies/{companyId}/company-members/{companyMemberId}`

Response 200:

```json
{
  "data": {
    "removed": true,
    "removedProjectMemberCount": 2
  },
  "meta": { "requestId": "req_..." }
}
```

- 요청자는 대상 회사의 ACTIVE `COMPANY_ADMIN`이어야 한다.
- 대상 company member는 `status = LEFT`, `departmentId = null`로 전환된다(soft remove, hard delete 아님).
- 같은 트랜잭션에서 대상 사용자의 해당 회사 내 ACTIVE project membership을 모두 `REMOVED`로 전환한다.
- 자기 자신 제거, 이미 `LEFT`인 멤버 재제거, 마지막 ACTIVE `COMPANY_ADMIN` 제거는 각각 `409 CONFLICT`를 반환한다.
- 대상이 어느 프로젝트의 마지막 ACTIVE `PROJECT_ADMIN`이면 해당 프로젝트를 이유로 `409 CONFLICT`를 반환하고 아무것도 변경하지 않는다.
- 회사 멤버 제거와 project member 변경/생성/초대 수락 경로의 lock 순서는 company → company member → project member(오름차순) → project(오름차순)로 고정해 교착을 막는다.

### 13.5 Project / Sprint / Project Member

#### POST `/api/v1/companies/{companyId}/projects`

Request Body:

```json
{
  "name": "Threadia MVP",
  "projectKey": "THD",
  "description": "Threadia MVP 개발 프로젝트",
  "status": "PLANNING",
  "startDate": "2026-06-17",
  "endDate": "2026-07-31"
}
```

Response 201:

```json
{
  "data": {
    "project": {
      "id": 20,
      "companyId": 10,
      "name": "Threadia MVP",
      "projectKey": "THD",
      "status": "PLANNING"
    },
    "projectMember": {
      "id": 200,
      "role": "PROJECT_ADMIN",
      "status": "ACTIVE"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/projects/{projectId}`

Response 200:

```json
{
  "data": {
    "project": {
      "id": 20,
      "companyId": 10,
      "name": "Threadia MVP",
      "projectKey": "THD",
      "description": "Threadia MVP 개발 프로젝트",
      "status": "ACTIVE",
      "startDate": "2026-06-17",
      "endDate": "2026-07-31",
      "createdAt": "2026-06-17T00:00:00Z",
      "updatedAt": "2026-07-09T00:00:00Z"
    },
    "viewer": {
      "companyRole": "MEMBER",
      "projectRole": "PROJECT_ADMIN",
      "canManageProject": true
    }
  },
  "meta": { "requestId": "req_..." }
}
```

`viewer`는 현재 요청자의 회사/프로젝트 역할과 프로젝트 관리 가능 여부를 나타낸다. 회사 관리자는 `companyRole = "COMPANY_ADMIN"`, 프로젝트 멤버가 아니면 `projectRole = null`일 수 있다. 프로젝트 통계는 이 응답에 포함하지 않고 project member 목록 또는 별도 stats 계약에서 처리한다.

#### PATCH `/api/v1/projects/{projectId}`

Request Body:

```json
{
  "name": "Threadia MVP Sprint 1",
  "description": "Sprint 1 범위로 조정된 프로젝트",
  "status": "ACTIVE",
  "startDate": "2026-06-17",
  "endDate": "2026-07-31"
}
```

#### POST `/api/v1/projects/{projectId}/archive`

Request Body:

```json
{ "reason": "MVP 종료 후 보관" }
```

Response 200:

```json
{
  "data": {
    "projectId": 20,
    "status": "ARCHIVED"
  },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/projects/{projectId}/project-members`

Response 200:

```json
{
  "data": {
    "projectMembers": [
      {
        "id": 200,
        "projectId": 20,
        "role": "MEMBER",
        "companyRole": "COMPANY_ADMIN",
        "status": "ACTIVE",
        "joinedAt": "2026-07-07T00:00:00Z",
        "createdAt": "2026-07-07T00:00:00Z",
        "updatedAt": "2026-07-07T00:00:00Z",
        "user": {
          "id": 2,
          "email": "member@example.com",
          "nickname": "김민정",
          "avatarUrl": null
        }
      }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

`role`은 프로젝트 membership 역할이고, `companyRole`은 같은 회사의 ACTIVE company membership 역할이다. `companyRole`은 `COMPANY_ADMIN`, `MEMBER`, `null` 중 하나이며, S12 멤버 목록은 이 값을 사용해 프로젝트 role이 `MEMBER`여도 회사 관리자임을 표시한다.

#### Deprecated/Internal Note: POST `/api/v1/projects/{projectId}/project-members`

> Sprint 1.5에서는 이 endpoint와 프로젝트 멤버 invitation 흐름을 user-facing API로 제공하지 않는다.
> 프로젝트 멤버 추가 흐름은 WBS 4.3.1 구현에서 `POST /api/v1/projects/{projectId}/invitations`로 PROJECT scope invitation을 만들고, 대상자가 `POST /api/v1/invitations/{invitationId}/accept`를 호출할 때 membership을 생성하거나 기존 REMOVED membership을 재활성화한다. `INVITE-01~04` company invitation과는 분리한다.
> 기존 프로젝트 membership의 역할과 상태 변경은 `PATCH /api/v1/project-members/{projectMemberId}`에서 처리한다.

#### PATCH `/api/v1/project-members/{projectMemberId}`

Request Body:

```json
{
  "role": "PROJECT_ADMIN",
  "status": "ACTIVE"
}
```

Response 200:

```json
{
  "data": {
    "projectMember": {
      "id": 200,
      "projectId": 20,
      "role": "PROJECT_ADMIN",
      "companyRole": "COMPANY_ADMIN",
      "status": "ACTIVE",
      "joinedAt": "2026-07-07T00:00:00Z",
      "createdAt": "2026-07-07T00:00:00Z",
      "updatedAt": "2026-07-20T00:00:00Z",
      "user": {
        "id": 2,
        "email": "member@example.com",
        "nickname": "김민정",
        "avatarUrl": null
      }
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### DELETE `/api/v1/project-members/{projectMemberId}`

Response 200:

```json
{
  "data": { "removed": true },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/projects/{projectId}/sprints`

Request Body:

```json
{
  "name": "Sprint 1",
  "goal": "인증, 프로젝트, 이슈 API skeleton 완성",
  "startDate": "2026-06-17",
  "endDate": "2026-06-30"
}
```

#### PATCH `/api/v1/sprints/{sprintId}`

Request Body:

```json
{
  "name": "Sprint 1 - Backend Skeleton",
  "goal": "Spring Boot 기반 API skeleton 완성",
  "startDate": "2026-06-17",
  "endDate": "2026-06-30"
}
```

#### POST `/api/v1/sprints/{sprintId}/start`

Request Body: 없음

#### POST `/api/v1/sprints/{sprintId}/complete`

Request Body:

```json
{
  "summary": "Sprint 1 목표 기능 skeleton 완료"
}
```

#### GET `/api/v1/sprints/{sprintId}`

Sprint의 `id`, `projectId`, `name`, `goal`, `status`, 기간과 lifecycle timestamp/summary만 반환하는 metadata endpoint다. 배정 이슈 배열은 반환하지 않으며 `GET /api/v1/projects/{projectId}/issues?sprintId={sprintId}`로 조회한다.

#### POST `/api/v1/sprints/{sprintId}/issues`

Request Body:

```json
{
  "issueIds": [1000, 1001, 1002]
}
```

`issueIds`는 null 또는 빈 배열일 수 없고, 각 원소도 null일 수 없다. 위반 시 `400 VALIDATION_ERROR`를 반환한다.

### 13.5-A Invitation

#### GET `/api/v1/users/lookup?email=member@threadia.com`

Response 200:

```json
{
  "data": {
    "user": {
      "id": 2,
      "email": "member@threadia.com",
      "nickname": "팀원",
      "avatarUrl": null
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/companies/{companyId}/invitations`

Request Body:

```json
{
  "inviteeUserId": 2,
  "role": "MEMBER"
}
```

Response 201:

```json
{
  "data": {
    "invitation": {
      "id": 501,
      "scopeType": "COMPANY",
      "companyId": 10,
      "companyName": "Threadia",
      "projectId": null,
      "projectName": null,
      "inviterUserId": 1,
      "inviterNickname": "초대자1",
      "inviteeUserId": 2,
      "inviteeEmail": "member@threadia.com",
      "inviteeNickname": "사용자1",
      "role": "MEMBER",
      "status": "PENDING",
      "expiresAt": "2026-06-24T10:00:00Z"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/companies/{companyId}/invitations`

Response 200:

```json
{
  "data": {
    "invitations": [
      {
        "id": 501,
        "scopeType": "COMPANY",
        "companyId": 10,
        "companyName": "Threadia",
        "projectId": null,
        "projectName": null,
        "inviterUserId": 1,
        "inviterNickname": "초대자1",
        "inviteeUserId": 2,
        "inviteeEmail": "member@threadia.com",
        "inviteeNickname": "사용자1",
        "role": "MEMBER",
        "status": "PENDING",
        "expiresAt": "2026-06-24T10:00:00Z"
      }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/projects/{projectId}/invitable-members?email=member@threadia.com`

> **Issue #108 구현 계약**: 해당 프로젝트 회사의 ACTIVE C 또는 정확히 해당 프로젝트의 ACTIVE P만 호출할 수 있다. 권한을 먼저 검증한 뒤 `trim + lowercase` 정규화 email로 exact match하며, 목록 또는 부분 검색은 제공하지 않는다.
>
> 조회 범위는 **해당 프로젝트 회사의 ACTIVE company member로 한정**한다. 회사 경계 밖 email은 가입 여부와 무관하게 동일하게 `404`이며, 전역 email 조회는 계속 C 전용(`GET /api/v1/users/lookup`)이다. 아무것도 쓰지 않는 조회이므로 프로젝트 행을 잠그지 않는다.

Response 200:

```json
{
  "data": {
    "user": {
      "id": 2,
      "email": "member@threadia.com",
      "nickname": "팀원",
      "avatarUrl": null
    }
  },
  "meta": { "requestId": "req_..." }
}
```

- **초대 후보가 아니면 전부 `404 NOT_FOUND`로 통일한다.** 미가입 email, ACTIVE가 아닌 user, 다른 회사 소속 user를 호출자가 구분할 수 없어야 한다 — 갈라서 응답하면 P가 임의의 email로 서비스 가입 여부를 알아낼 수 있다(user enumeration). 이 endpoint의 권한은 프로젝트 범위이므로 회사 경계 밖은 존재 여부조차 노출하지 않는다.
- self, ACTIVE 프로젝트 멤버, live pending PROJECT invitation은 각각 `409 INVITATION_SELF_NOT_ALLOWED`, `409 ALREADY_PROJECT_MEMBER`, `409 INVITATION_ALREADY_PENDING`을 반환한다. 이들은 모두 이미 같은 회사 안에서 확인되는 정보라 노출 문제가 없다.
- 생성 endpoint(`POST /projects/{projectId}/invitations`)는 `inviteeUserId`를 받으므로 `409 TARGET_NOT_ACTIVE_COMPANY_MEMBER`를 그대로 유지한다.
- 만료된 pending invitation은 lazy expiration 후 후보 조회를 허용한다. 후보 조회와 invitation 생성 사이에 상태가 바뀔 수 있으므로 생성 endpoint가 모든 eligibility를 다시 검증한다.

#### POST `/api/v1/projects/{projectId}/invitations`

> **WBS 4.3.1 구현 계약**: P/C가 `inviteeUserId`와 `PROJECT_ADMIN | MEMBER` role로 PROJECT invitation을 생성한다. request body에 `scopeType`은 받지 않으며, scope는 endpoint로 결정된다.

Request Body:

```json
{
  "inviteeUserId": 2,
  "role": "MEMBER"
}
```

Response 201:

```json
{
  "data": {
    "invitation": {
      "id": 601,
      "scopeType": "PROJECT",
      "companyId": 10,
      "companyName": "Threadia",
      "projectId": 20,
      "projectName": "Threadia 웹",
      "inviterUserId": 1,
      "inviterNickname": "초대자1",
      "inviteeUserId": 2,
      "inviteeEmail": "member@threadia.com",
      "inviteeNickname": "사용자1",
      "role": "MEMBER",
      "status": "PENDING",
      "expiresAt": "2026-06-24T10:00:00Z"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/projects/{projectId}/invitations`

> P/C만 PROJECT scope invitation 목록을 조회한다. 목록은 생성 시각 내림차순이며, 조회 시 만료된 `PENDING` invitation은 lazy expiration으로 `EXPIRED` 상태가 된다.

Response 200:

```json
{
  "data": {
    "invitations": [
      {
        "id": 601,
        "scopeType": "PROJECT",
        "companyId": 10,
        "companyName": "Threadia",
        "projectId": 20,
        "projectName": "Threadia 웹",
        "inviterUserId": 1,
        "inviterNickname": "초대자1",
        "inviteeUserId": 2,
        "inviteeEmail": "member@threadia.com",
        "inviteeNickname": "사용자1",
        "role": "MEMBER",
        "status": "PENDING",
        "expiresAt": "2026-06-24T10:00:00Z"
      }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/users/me/invitations`

> 받은 COMPANY와 PROJECT invitation을 모두 생성 시각 내림차순으로 반환한다. 조회 시 만료된 `PENDING` invitation은 lazy expiration으로 `EXPIRED` 상태가 된다.

Response 200:

```json
{
  "data": {
    "invitations": [
      {
        "id": 501,
        "scopeType": "COMPANY",
        "companyId": 10,
        "companyName": "Threadia",
        "projectId": null,
        "projectName": null,
        "inviterUserId": 1,
        "inviterNickname": "초대자1",
        "inviteeUserId": 2,
        "inviteeEmail": "member@threadia.com",
        "inviteeNickname": "사용자1",
        "role": "MEMBER",
        "status": "PENDING",
        "expiresAt": "2026-06-24T10:00:00Z"
      },
      {
        "id": 601,
        "scopeType": "PROJECT",
        "companyId": 10,
        "companyName": "Threadia",
        "projectId": 20,
        "projectName": "Threadia 웹",
        "inviterUserId": 1,
        "inviterNickname": "초대자1",
        "inviteeUserId": 2,
        "inviteeEmail": "member@threadia.com",
        "inviteeNickname": "사용자1",
        "role": "MEMBER",
        "status": "PENDING",
        "expiresAt": "2026-06-24T10:00:00Z"
      }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

`companyName`, `projectName`, `inviterUserId`, `inviterNickname`, `inviteeEmail`, `inviteeNickname`은 초대 화면 표시용 필드다. Action 처리와 권한 검증에는 `id`, `scopeType`, `companyId`, `projectId`, `inviteeUserId`를 사용한다.

`projectName`은 PROJECT scope에서만 채우고 COMPANY scope는 `null`이다. 초대 대상자는 아직 프로젝트 member가 아니라 `GET /api/v1/projects/{projectId}`가 `403`이므로, 이 필드가 없으면 수신 화면이 어느 프로젝트 초대인지 표시할 수 없다.

#### POST `/api/v1/invitations/{invitationId}/accept`

> 저장된 `scopeType`이 COMPANY면 기존 회사 invitation 정책을 적용한다. PROJECT면 초대 대상자만 수락할 수 있고, 수락 시에도 대상자는 해당 프로젝트 회사의 ACTIVE company member여야 한다.

Response 200:

```json
{
  "data": {
    "invitationId": 501,
    "status": "ACCEPTED",
    "membershipCreated": true
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/invitations/{invitationId}/decline`

> 저장된 `scopeType`의 초대 대상자만 거절할 수 있다.

Response 200:

```json
{
  "data": { "invitationId": 501, "status": "DECLINED", "membershipCreated": false },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/invitations/{invitationId}/cancel`

> 저장된 `scopeType`이 COMPANY면 C, PROJECT면 P/C 권한으로 PENDING invitation만 취소할 수 있다.

Response 200:

```json
{
  "data": { "invitationId": 501, "status": "CANCELED", "membershipCreated": false },
  "meta": { "requestId": "req_..." }
}
```

### 13.6 Issue / Document / Graph

#### GET `/api/v1/projects/{projectId}/issues`

목록의 각 이슈 객체는 full Issue contract를 따른다. `id`, `issueKey`, `title`, `status`, `description`, `priority`, `assigneeUserId`, `reporterUserId`, `sprintId`, `dueDate`, `sourceType`, `trashedAt`, `restoredAt`을 반환하며 시간 필드는 UTC `Z` 형식이다.

`reporterUserId`는 표시용이 아니라 **권한 판정용**이다 — trash/restore가 P/C 또는 등록자에게 허용되므로, 클라이언트가 이 값과 현재 사용자를 비교해야 "내가 등록한 이슈" 경로를 판정할 수 있다.

- `unassignedOnly=true`는 `sprintId`가 없는 이슈만 조회한다.
- `unassignedOnly=true`와 `sprintId`를 함께 전달하면 `400 VALIDATION_ERROR`다.

Response 200:

```json
{
  "data": {
    "issues": [
      {
        "id": 1000,
        "issueKey": "THD-1",
        "title": "로그인 API 구현",
        "status": "BACKLOG",
        "description": "로그인 API 구현과 테스트를 진행한다.",
        "priority": "HIGH",
        "assigneeUserId": 2,
        "reporterUserId": 1,
        "sprintId": 300,
        "dueDate": "2026-06-22",
        "sourceType": "MANUAL",
        "trashedAt": null,
        "restoredAt": null
      }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/projects/{projectId}/issues`

`title`은 필수이며 최대 200자다. A는 `title`, `description`만 지정할 수 있고 P/C는 `priority`, `assigneeUserId`, `sprintId`, `dueDate`도 지정할 수 있다. 생성 상태는 항상 `BACKLOG`다.

Request Body:

```json
{
  "title": "로그인 API 구현",
  "description": "로그인 API 구현과 테스트를 진행한다.",
  "priority": "HIGH",
  "assigneeUserId": 2,
  "sprintId": 300,
  "dueDate": "2026-06-22"
}
```

Response 201:

```json
{
  "data": {
    "issue": {
      "id": 1000,
      "issueKey": "THD-1",
      "title": "로그인 API 구현",
      "status": "BACKLOG",
      "description": "로그인 API 구현과 테스트를 진행한다.",
      "priority": "HIGH",
      "assigneeUserId": 2,
      "reporterUserId": 1,
      "sprintId": 300,
      "dueDate": "2026-06-22",
      "sourceType": "MANUAL",
      "trashedAt": null,
      "restoredAt": null
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### PATCH `/api/v1/issues/{issueId}`

변경할 필드만 전달한다. omitted 필드는 유지하고 명시 `null`은 `description`, `assigneeUserId`, `dueDate`, `sprintId`만 clear한다.

Request Body:

```json
{
  "title": "로그인 API 구현 및 테스트",
  "description": "로그인 API와 실패 케이스 테스트를 함께 구현한다.",
  "priority": "URGENT",
  "assigneeUserId": 3,
  "dueDate": "2026-06-22"
}
```

**`sprintId`는 해제 전용이다.** 명시 `null`이면 이슈를 Sprint에서 떼어 백로그로 되돌리고, non-null 값을 담으면 `400 VALIDATION_ERROR`다. 배정은 `POST /api/v1/sprints/{sprintId}/issues` 하나로만 한다 — 배정 경로를 둘로 나누지 않기 위한 계약이다. `sprintId`도 관리 전용 필드라 A가 담아 보내면 `403`이다(A는 `title`/`description`만 수정할 수 있다).

Request Body (Sprint 해제):

```json
{ "sprintId": null }
```

#### PATCH `/api/v1/issues/{issueId}/status`

응답 이슈 객체는 full Issue contract를 따른다.

Request Body:

```json
{ "status": "IN_PROGRESS" }
```

Response 200:

```json
{
  "data": {
    "issue": {
      "id": 1000,
      "issueKey": "THD-1",
      "title": "로그인 API 구현",
      "status": "IN_PROGRESS",
      "description": "로그인 API 구현과 테스트를 진행한다.",
      "priority": "HIGH",
      "assigneeUserId": 2,
      "reporterUserId": 1,
      "sprintId": 300,
      "dueDate": "2026-06-22",
      "sourceType": "MANUAL",
      "trashedAt": null,
      "restoredAt": null
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/issues/{issueId}/trash`

Request body 없음.

#### POST `/api/v1/issues/{issueId}/restore`

Response 200:

```json
{
  "data": {
    "issue": {
      "id": 1000,
      "issueKey": "THD-1",
      "title": "로그인 API 구현",
      "status": "BACKLOG",
      "description": "로그인 API 구현과 테스트를 진행한다.",
      "priority": "HIGH",
      "assigneeUserId": 2,
      "reporterUserId": 1,
      "sprintId": 300,
      "dueDate": "2026-06-22",
      "sourceType": "MANUAL",
      "trashedAt": null,
      "restoredAt": "2026-07-14T00:00:00Z"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/projects/{projectId}/documents`

Request Body:

```json
{
  "title": "로그인 API 설계 메모",
  "type": "DOC",
  "content": "로그인 API의 요청/응답과 실패 케이스를 정리한다."
}
```

`title`, `type`, `content`는 모두 필수다. `title`은 trim 후 1~200자이며, `content`는 Markdown 원문이다. Response 201의 `data.document`는 `id`, `projectId`, `authorUserId`, `title`, `type`, `content`, `trashedAt`, `restoredAt`, `createdAt`, `updatedAt`을 반환한다. 시간 필드는 ISO-8601 UTC `Z` 문자열이다.

#### GET `/api/v1/projects/{projectId}/documents`

Query parameter: `type=DOC|MEETING_NOTE`, `q`, `includeTrashed=false`. 응답은 `data.documents` 배열이며, 기본 조회는 trashed 문서를 제외한다.

#### GET `/api/v1/documents/{documentId}`

Response 200은 `data.document`에 `id`, `projectId`, `authorUserId`, `title`, `type`, `content`, `trashedAt`, `restoredAt`, `createdAt`, `updatedAt`을 반환한다. 시간 필드는 ISO-8601 UTC `Z` 문자열이다.

```json
{
  "data": {
    "document": {
      "id": 5000,
      "projectId": 20,
      "authorUserId": 1,
      "title": "로그인 API 설계 메모",
      "type": "DOC",
      "content": "# 로그인 API\n\n요청과 응답을 정리한다.",
      "trashedAt": null,
      "restoredAt": null,
      "createdAt": "2026-07-15T00:00:00Z",
      "updatedAt": "2026-07-15T00:00:00Z"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### PATCH `/api/v1/documents/{documentId}`

> 문서 optimistic locking은 Sprint 3 decision-needed다. 이 예시는 현재 문서 수정 body 기준이며 `version` 또는 `revision` 필드를 확정하지 않는다.

Request Body:

```json
{
  "title": "로그인 API 설계 메모 v2",
  "type": "DOC",
  "content": "검증 기준과 에러 응답 예시를 추가한다."
}
```

`title`, `type`, `content`는 모두 필수이고, 지정하지 않은 필드를 보존하는 partial update는 제공하지 않는다. Response 200은 생성과 같은 `data.document` shape를 반환한다.

#### POST `/api/v1/documents/{documentId}/trash`

Request body는 없다. Response 200은 `data.document`의 전체 문서 shape를 반환하며, 최초 trash에서 `trashedAt`은 UTC `Z` 시각이 된다. 이미 trashed인 문서를 다시 trash해도 같은 응답 shape로 성공한다.

#### POST `/api/v1/documents/{documentId}/restore`

Request body는 없다. Response 200은 `data.document`의 전체 문서 shape를 반환하며, restore 후 `trashedAt`은 `null`이다. 이미 active인 문서를 다시 restore해도 같은 응답 shape로 성공한다.

#### GET `/api/v1/projects/{projectId}/documents/graph`

Response 200:

```json
{
  "data": {
    "nodes": [
      { "id": 5000, "type": "DOCUMENT", "title": "로그인 API 설계 메모", "issueKey": null, "status": null },
      { "id": 1000, "type": "ISSUE", "title": "로그인 API 구현", "issueKey": "THD-1", "status": "BACKLOG" }
    ],
    "edges": [
      { "linkId": 7000, "sourceDocumentId": 5000, "targetIssueId": 1000 }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/projects/{projectId}/document-issue-links`

Request Body:

```json
{
  "documentId": 5000,
  "issueId": 1000
}
```

Response 201:

```json
{
  "data": {
    "link": {
      "id": 7000,
      "projectId": 20,
      "documentId": 5000,
      "issueId": 1000,
      "createdByUserId": 1
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/projects/{projectId}/document-issue-links`

Response 200:

```json
{
  "data": {
    "links": [
      {
        "id": 7000,
        "projectId": 20,
        "documentId": 5000,
        "issueId": 1000,
        "createdByUserId": 1
      }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

#### DELETE `/api/v1/document-issue-links/{linkId}`

Response 200:

```json
{
  "data": { "deleted": true },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/issues/{issueId}/documents`

Response 200은 `data.documents`에 연결된 untrashed 문서만 반환한다.

#### GET `/api/v1/documents/{documentId}/issues`

Response 200은 `data.issues`에 연결된 untrashed 이슈만 반환한다. 이슈 객체는 현재 canonical `IssueResponse` full shape를 사용한다.

### 13.7 Thread / Message / DM

#### POST `/api/v1/projects/{projectId}/threads`

Request Body:

```json
{
  "targetType": "ISSUE",
  "targetId": 1000,
  "topic": "로그인 API 논의"
}
```

대상별 대표 Thread를 명시적으로 get-or-create한다. 처음 생성하면 `201`, 기존 대표 Thread를 반환하면 `200`이다. Issue/Document/Message의 생성은 Thread를 자동 생성하지 않는다.

Response `data.thread`은 `id`, `projectId`, `targetType`, `targetId`, `topic`, `createdByUserId`, `createdAt`, `updatedAt`을 반환한다.

#### GET `/api/v1/projects/{projectId}/threads?targetType={ISSUE|DOCUMENT|MESSAGE}&targetId={targetId}`

`targetType`과 `targetId`는 모두 필수다. 요청 프로젝트·대상과 일치하는 대표 Thread 하나를 `data.thread`로 반환한다. project-wide Thread 목록과 pagination은 제공하지 않는다. Thread가 아직 없으면 `404 NOT_FOUND`다.

#### GET `/api/v1/threads/{threadId}`

접근 가능한 Thread 상세를 `data.thread`로 반환한다.

#### PATCH `/api/v1/threads/{threadId}`

Request Body:

```json
{ "topic": "로그인 API 검증 논의" }
```

`topic`은 `null` 또는 trim 후 1~200자다. `null`은 topic을 제거한다. Thread 생성자 또는 P/C만 수정할 수 있다.

#### GET `/api/v1/threads/{threadId}/comments`

생성 순서(`id ASC`)의 append-only 댓글을 `data.comments`로 반환한다.

#### POST `/api/v1/threads/{threadId}/comments`

Request Body:

```json
{
  "content": "실패 케이스도 같이 확인해주세요."
}
```

Response 201의 `data.comment`은 `id`, `threadId`, `authorUserId`, `content`, `createdAt`을 반환한다.

#### POST `/api/v1/projects/{projectId}/messages`

Request Body:

```json
{
  "content": "로그인 API 응답 형식 확인했습니다.",
  "threadId": null
}
```

Response 201:

```json
{
  "data": {
    "message": {
      "id": 7000,
      "projectId": 20,
      "authorUserId": 1,
      "content": "로그인 API 응답 형식 확인했습니다.",
      "threadId": null,
      "createdAt": "2026-07-15T10:00:00Z"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/projects/{projectId}/messages`

Query parameters: `cursor`(선택, 서버가 서명한 opaque cursor), `limit`(선택, 기본 50, 1~100).

Response 200:

```json
{
  "data": {
    "messages": [
      {
        "id": 7002,
        "projectId": 20,
        "authorUserId": 2,
        "content": "검증 결과를 공유합니다.",
        "threadId": null,
        "createdAt": "2026-07-15T10:02:00Z"
      }
    ],
    "nextCursor": "<server-signed-opaque-cursor>"
  },
  "meta": { "requestId": "req_..." }
}
```

마지막 page의 `nextCursor`는 `null`이다. cursor의 payload 또는 signature가 손상됐거나 현재 path project와 다르면 `400 VALIDATION_ERROR`다.

#### GET `/api/v1/messages/{messageId}`

Response 200은 `POST /projects/{projectId}/messages`의 `data.message`와 같은 6개 필드를 반환한다. 경로 message가 없으면 `404 NOT_FOUND`, 해당 project 접근 권한이 없으면 `403 FORBIDDEN`이다.

#### POST `/api/v1/projects/{projectId}/messages/mark-read`

Request Body:

```json
{ "lastReadMessageId": 7000 }
```

Response 200:

```json
{
  "data": {
    "projectId": 20,
    "lastReadMessageId": 7000,
    "unreadCount": 0
  },
  "meta": { "requestId": "req_..." }
}
```

`lastReadMessageId`가 path project에 속하지 않으면 `403 FORBIDDEN`이다. `lastReadAt`은 request field가 아니며 서버 UTC 시각만 저장한다.

#### GET `/api/v1/projects/{projectId}/messages/unread-count`

Response 200:

```json
{
  "data": {
    "projectId": 20,
    "unreadCount": 3
  },
  "meta": { "requestId": "req_..." }
}
```

#### WBS 4.7.2.1 Project Chat Backend 정책

- `GET /api/v1/projects/{projectId}/messages`는 최신순 `(createdAt DESC, id DESC)` keyset pagination을 사용한다. `cursor`는 HMAC signature가 붙은 응답의 opaque `nextCursor`만 사용할 수 있고, 다른 project의 cursor, 손상된 payload/signature는 `400 VALIDATION_ERROR`다. 기본 `limit`은 50, 허용 범위는 1~100이며 응답은 `data.messages`, `data.nextCursor`를 제공한다.
- `POST /api/v1/projects/{projectId}/messages`는 1~10,000자 범위의 비어 있지 않은 `content`만 작성한다. 메시지는 immutable이며 edit/delete endpoint를 제공하지 않는다. `threadId`는 WBS 4.7.1 완료 전 반드시 `null`이고, non-null 요청은 `409 THREAD_DEFERRED`다.
- `POST /api/v1/projects/{projectId}/messages/mark-read`는 path project의 메시지만 pointer로 허용한다. 서버 UTC 시각으로 읽음 시각을 기록하며 client `readAt`은 받지 않는다. 동일 사용자의 `(projectId, userId)` 상태는 upsert되고, pointer는 `(createdAt, id)` 순서에서 뒤로 이동하지 않는다.
- `GET /api/v1/projects/{projectId}/messages/unread-count`와 mark-read 응답의 unread count는 저장하지 않고 계산한다. 본인이 작성한 메시지는 제외하며, 현재 pointer 이후 메시지만 센다.
- 모든 endpoint는 ACTIVE 회사 `COMPANY_ADMIN`이면 해당 회사의 모든 project에 접근할 수 있고, 그 외 사용자는 ACTIVE project membership이 필요하다. 서버 생성 시각과 응답 시간은 UTC ISO-8601 `Z` 형식이다.
- list의 잘못된 `limit`, 10,000자를 초과하거나 blank인 `content`, missing `lastReadMessageId`, malformed/tampered cursor는 `400 VALIDATION_ERROR`다. 없는 message는 `404 NOT_FOUND`, 접근 불가 project/message는 `403 FORBIDDEN`, non-null `threadId`는 `409 THREAD_DEFERRED`다.

#### WBS 4.7.1.1 Thread Core Backend 정책

- Thread 생성은 `POST /api/v1/projects/{projectId}/threads`만 수행한다. Issue, Document, Message 생성 및 조회는 Thread를 자동 생성하지 않는다.
- 대상은 `ISSUE`, `DOCUMENT`, `MESSAGE`만 허용하고, 존재하며 path project에 속해야 한다. `(targetType, targetId)` DB unique key와 `INSERT IGNORE` 후 재조회로 경합 시에도 대표 Thread 하나만 반환한다.
- `GET /api/v1/projects/{projectId}/threads`는 필수 target query의 대표 Thread 조회 전용이다. project-wide list/pagination은 범위 밖이다.
- 모든 Thread read/create/comment endpoint는 ACTIVE 회사 `COMPANY_ADMIN` 또는 ACTIVE project member만 수행한다. topic 변경은 Thread 생성자 또는 P/C만 수행한다.
- 댓글은 1~10,000자 `content`를 trim해 생성 순서로 append-only 저장한다. 댓글 수정/삭제, 멘션 파싱·저장·notification/event 발행은 제공하지 않는다.
- topic은 생성 시 선택이고, 수정 요청의 `null`은 제거를 의미한다. non-null topic은 trim 후 1~200자여야 한다. 모든 서버 시간 및 응답 시간은 UTC ISO-8601 `Z` 형식이다.

#### GET `/api/v1/companies/{companyId}/direct-messages/rooms`

현재 사용자가 참여한 해당 회사의 1:1 room만 반환한다. 각 room은 상대 사용자와 현재 사용자 기준 unread count를 포함한다.

#### POST `/api/v1/companies/{companyId}/direct-messages/rooms`

Request Body:

```json
{
  "otherUserId": 2
}
```

Response 201 / 200:

```json
{
  "data": {
    "room": {
      "id": 9000,
      "companyId": 10,
      "otherUser": {
        "id": 2,
        "email": "member@threadia.com",
        "nickname": "팀원",
        "avatarUrl": null
      }
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/direct-messages/rooms/{roomId}/messages`

참여자만 생성 시각/ID 오름차순 메시지 목록을 조회할 수 있다.

#### POST `/api/v1/direct-messages/rooms/{roomId}/messages`

Request Body:

```json
{ "content": "API 명세서 v2.3 확인 부탁드립니다." }
```

#### POST `/api/v1/direct-messages/rooms/{roomId}/mark-read`

Request Body:

```json
{
  "lastReadDirectMessageId": 9100
}
```

Response 200:

```json
{
  "data": {
    "roomId": 9000,
    "lastReadDirectMessageId": 9100,
    "unreadCount": 0,
    "readAt": "2026-06-16T10:00:00Z"
  },
  "meta": { "requestId": "req_..." }
}
```

### 13.8 Notification

#### GET `/api/v1/notifications`

Query Params 예시:

```text
companyId=10&projectId=20&type=MENTIONED&unreadOnly=true&cursor=next_cursor&size=20
```

`companyId`, `projectId`, `type`, `unreadOnly`, `cursor`, `size`는 모두 선택적이다. `unreadOnly=false`, `size=20`이 기본값이다. `cursor`는 client가 해석하거나 조립하지 않는 opaque 값이며 같은 사용자와 같은 정규화 filter에서만 사용할 수 있다.

Response 200:

```json
{
  "data": [
    {
      "notificationRecipientId": 8000,
      "notificationEventId": 8100,
      "type": "MENTIONED",
      "title": "새 멘션이 있습니다.",
      "body": "로그인 API 논의에서 언급되었습니다.",
      "readAt": null,
      "target": {
        "type": "THREAD",
        "id": 6000
      },
      "createdAt": "2026-06-16T10:00:00Z"
    }
  ],
  "page": { "cursor": "next_cursor", "hasNext": true, "size": 20 },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/notifications/unread-count`

선택적 query `companyId`, `projectId`, `type`을 지원하며 목록과 같은 scope/type 검증을 적용한다.

Response 200:

```json
{
  "data": {
    "unreadCount": 3
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/notifications/{notificationRecipientId}/read`

Response 200:

```json
{
  "data": {
    "notificationRecipientId": 8000,
    "readAt": "2026-06-16T10:05:00Z"
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/notifications/read-all`

Request Body:

```json
{
  "type": "MENTIONED",
  "companyId": 10,
  "projectId": 20
}
```

세 필드는 모두 선택적이다. `{}`는 현재 ACTIVE 사용자의 모든 unread 알림을 읽음 처리한다. 이미 읽은 row의 `readAt`은 변경하지 않는다.

Response 200:

```json
{
  "data": { "updatedCount": 5 },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/notification-preferences`

Response 200:

```json
{
  "data": [
    {
      "notificationType": "ISSUE_ASSIGNED",
      "scopeType": "GLOBAL",
      "enabled": true
    },
    {
      "notificationType": "MENTIONED",
      "scopeType": "GLOBAL",
      "enabled": true
    }
  ],
  "meta": { "requestId": "req_..." }
}
```

#### PUT `/api/v1/notification-preferences`

Request Body:

```json
{
  "notificationType": "MENTIONED",
  "scopeType": "GLOBAL",
  "enabled": true
}
```

Response 200:

```json
{
  "data": {
    "preference": {
      "notificationType": "MENTIONED",
      "scopeType": "GLOBAL",
      "enabled": true
    }
  },
  "meta": { "requestId": "req_..." }
}
```

전체 알림 on/off 변경은 아래 형식을 사용한다.

```json
{
  "scopeType": "GLOBAL",
  "allEnabled": false
}
```

`notificationType=ALL`은 외부 API에서 사용하지 않는다. 프로젝트별 설정인 `scopeType=PROJECT`는 Sprint 3 decision-needed API boundary이며 실제 지원 기준 확정 전까지 `PROJECT_NOTIFICATION_SCOPE_DEFERRED`로 처리한다. 요구사항추적표 primary WBS row로는 backfill하지 않는다.

---

## Phase B invitation 반영 메모

> **생성일**: 2026-06-16
> **상태**: 공식 결정 반영 완료
> **기준**: `.sisyphus/evidence/task-5-invitation-impact-matrix.md`의 affected row와 2026-06-16 공식 decision set을 사용한다.

> **현재 기준선 주석**: 이 섹션은 2026-06-16/Sprint 1.5 결정 기록이다. 현재 Gate 3/4 완료 기준선은 project invitation을 Sprint 3 `PROJECT-MEMBER-01~02` 후보로 다루며, `INVITE-01~04` company invitation을 project invitation으로 재사용하지 않는다.

### 반영 범위

1. Phase B는 Sprint 1.5의 등록 사용자 대상 app-internal invitation 정책을 반영한다.
2. Sprint 1은 Auth-only이며 invitation을 구현하지 않는다.
3. Sprint 1.5부터 회사 멤버 추가는 즉시 membership 생성이 아니라 `invitations` 생성과 대상자 accept 후 membership 생성으로 처리한다. 프로젝트 멤버 추가와 project invitation은 당시 Sprint 2 후보로 분리한 과거 결정 기록이며, 현재 기준선에서는 Sprint 3 `PROJECT-MEMBER-01~02` 후보로 다룬다.
4. 당시 Sprint 2는 project invitation, project membership 생성, actual invitee email sending, token link acceptance, unregistered onboarding, resend, email template, delivery failure handling, batch cleanup/operations의 후보 시점이며 확정 구현 commitment는 아니었다. 현재 project invitation baseline은 Sprint 3 `PROJECT-MEMBER-01~02` 후보이고, `INVITE-01~04`는 company invitation 전용이다.

### invitation 공식 결정

1. Sprint 1.5 포함 범위는 `invitations` table, `PENDING / ACCEPTED / DECLINED / CANCELED / EXPIRED`, company invitation, exact email lookup for registered users, received invitation list, accept/decline/cancel, company membership creation on accept다.
2. Sprint 1.5 제외 범위는 project invitation, project membership creation, actual email sending to invitees, invitation token link, unregistered-user invitation, signup/onboarding auto-accept, resend다.
3. `notification_events`에는 필요 시 `INVITATION_CREATED`, `INVITATION_ACCEPTED`, `INVITATION_DECLINED`, `MEMBER_ADDED`를 내부 event 원장으로 기록할 수 있으나 Sprint 1.5 완료 조건에는 포함하지 않는다. Push 알림, 별도 Invitation notification preference, S11 사용자 알림 UI 필수 구현은 Sprint 1.5 범위가 아니다.
4. Sprint 1.5에서는 user-facing `notification_preferences`에 별도 Invitation category를 노출하지 않고 기존 global notification preference policy를 따른다.
5. `EXPIRED`와 `expires_at`을 유지하고 기본 만료 기간은 7일로 둔다. read/accept/decline/cancel 시 lazy expiration을 확인하며, 만료된 `PENDING` invitation accept는 `409 INVITATION_EXPIRED`로 거절한다.
6. Sprint 1.5에는 필수 batch scheduler를 추가하지 않는다. Batch cleanup/sweeper는 Sprint 2 이후 admin list hygiene, retention, metrics, operational cleanup 필요에 따라 검토한다.

### Phase C 추가 동기화 결정

1. Sprint 1.5 invitation 검증은 local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명을 사용했다. **(2026-07-18 갱신, `기획문서-변경이력.md` 2026-07-18 항목)** `POST /api/v1/companies`의 seed-only 제한은 해제됐다 — 로그인 사용자는 프론트에서 직접 회사를 생성할 수 있고 생성자는 자동 `COMPANY_ADMIN`이 된다. seed 기반 dev/demo 데이터 구성 자체는 계속 유효하다. 회사 생성 이후 추가 소속 확대는 계속 invitation 전용이다.
2. `activity_events`는 활동 원장, `notification_events`는 알림 사건 원본으로 별도 운영한다. 두 저장소를 통합하지 않고, 필요 시 activity 이벤트에서 notification 이벤트를 생성하는 연계만 허용한다.
3. 서버 생성 시각과 만료 시각은 UTC 기준으로 생성, 저장, 비교한다. ERD의 `DATETIME` 타입은 유지하고 API 응답은 ISO-8601 UTC `Z` 형식으로 반환한다.
4. 로그인 이메일은 모든 진입점에서 `trim + lowercase`로 정규화한 값을 저장, 조회, 비교한다. 이 기준은 signup, login, password reset, OAuth 계정 연결, invitation lookup에 동일하게 적용한다.
5. OAuth 계정 연결은 이메일만 같다는 이유로 blind auto-link하지 않는다. 이미 로그인한 사용자이거나 provider 이메일 검증을 신뢰할 수 있는 경우에만 기존 `users` 계정과 연결한다.
6. `password-reset/confirm` 성공 시 해당 사용자의 모든 기존 `refresh_tokens`를 revoke한다. 기존 access token은 별도 blacklist 없이 짧은 만료 시간에 따라 자연 만료시킨다.
7. login, OAuth callback, token refresh, `/auth/me`는 `users.status = ACTIVE`인 사용자만 성공 처리한다. `SUSPENDED` 또는 `DELETED` 사용자는 인증을 거부하며, 상태 변경 시 기존 `refresh_tokens`를 revoke한다.
