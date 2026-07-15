# Threadia API 명세서 v2.4

> **작성자**: Threadia 팀
> **작성일**: 2026-06-15
> **최종 수정**: 2026-07-15
> **버전**: Sprint 1.5 invitation 표시 필드 보강 + Sprint 3 기준선 반영 + company member scoped PATCH 동기화 + current-user membership context + project detail viewer response v1.6 + WBS 3.5 candidate Issue/Kanban 최소 계약 + WBS 4.2.1 profile Backend PATCH + WBS 4.3.1 PROJECT invitation Backend 계약 동기화 + Issue #108 project-scoped exact-email invitation candidate lookup
> **상태**: 확정
> **태그**: api, auth, invitation, contract
> **수정 근거**: Threadia-기획문서-검수리포트.md
> **기준 문서**: API 명세서 초안 v1.4, 기능 분해표 v3.1, ERD v2.0, 보류 기능 최종표 v1.0, Sprint 3 기준선 업데이트 제안, Sprint 3 Backlog 초안

---

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows이며, DB는 ERD 기반 `db/schema.sql`과 문서 변경이력으로 관리한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.

> **Sprint 3 기준선 메모**: 현재 API 기준은 Gate 3/4 완료 상태에 맞춰 Sprint 3 후보, Sprint 3 decision-needed, 유지 보류 항목을 분리한다. Sprint 1 Auth only, Sprint 1.5 company invitation, Sprint 2/Foundation reference 기록은 유지한다. S15는 `3.7~3.7.5` planning gate 이후 `4.10`에서 추적한다. attachment, document optimistic locking, advanced DM, PROJECT notification scope, project restore, permanent delete는 선행 결정 전 full endpoint contract나 request/response body를 만들지 않는다.


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
| PATCH | `/api/v1/companies/{companyId}/company-members/{companyMemberId}` | C | 역할/부서/상태 변경 |

Sprint 1.5부터 구성원 추가는 등록 사용자 exact email lookup 후 `invitations`를 생성한다. 가입되지 않은 이메일 대상 invitation, 외부 email 발송, token link 초대는 Sprint 1.5 범위가 아니며 user-facing membership create API로 즉시 소속을 만들지 않는다.

Sprint 1.5 invitation 검증을 위한 최초 회사와 `COMPANY_ADMIN`은 local/dev/demo seed로 준비한다. `POST /api/v1/companies`는 Sprint 1.5 구현 범위에 포함하지 않고 MVP/Sprint 2 reference로 유지하며, 운영 데이터에는 seed를 포함하지 않는다.

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
| GET | `/api/v1/projects/{projectId}/invitable-members?email={email}` | P/C | Issue #108 프로젝트 invitation 후보 exact email 조회. 초대 가능한 사용자 1명만 최소 응답으로 반환 |
| POST | `/api/v1/projects/{projectId}/invitations` | P/C | WBS 4.3.1 프로젝트 invitation 생성. 대상은 같은 회사 ACTIVE 구성원으로 제한하고 수락 전 membership 미생성 |
| GET | `/api/v1/projects/{projectId}/invitations` | P/C | WBS 4.3.1 프로젝트 invitation 목록 조회 |
| PATCH | `/api/v1/project-members/{projectMemberId}` | P/C | 프로젝트 멤버 역할/상태 변경 |
| DELETE | `/api/v1/project-members/{projectMemberId}` | P/C | 프로젝트 멤버 제거 |
| GET | `/api/v1/projects/{projectId}/sprints` | A/P/C | Sprint 목록 |
| POST | `/api/v1/projects/{projectId}/sprints` | P/C | Sprint 생성 |
| GET | `/api/v1/sprints/{sprintId}` | A/P/C | Sprint 상세 |
| PATCH | `/api/v1/sprints/{sprintId}` | P/C | Sprint 수정 |
| POST | `/api/v1/sprints/{sprintId}/start` | P/C | Sprint 시작, 알림 발송 |
| POST | `/api/v1/sprints/{sprintId}/complete` | P/C | Sprint 종료 |
| POST | `/api/v1/sprints/{sprintId}/issues` | P/C | Sprint에 이슈 배정 |

프로젝트 복원은 Sprint 3 decision-needed다. archive 프로젝트 복원 권한, UI, API 기준이 확정되기 전까지 full endpoint contract를 추가하지 않는다.

---

## 6-A. Invitation, Sprint 1.5 및 WBS 4.3.1

> **Sprint 1.5 구현 범위**: 등록 사용자 대상 app-internal invitation만 다룬다. 실제 email 발송, invitation token link, 미가입 사용자 초대, signup/onboarding auto-accept, resend는 포함하지 않는다.

| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/users/lookup?email={email}` | C | 등록 사용자 exact email lookup. Sprint 1.5 company invitation 대상 확인용. 입력 email은 `trim + lowercase` 정규화 후 조회 |
| GET | `/api/v1/projects/{projectId}/invitable-members?email={email}` | P/C | Issue #108 PROJECT invitation 후보 exact email lookup. 입력 email 정규화, 프로젝트 권한 및 생성 가능 조건 검증 후 최소 사용자 응답 반환 |
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
- 후보 조회는 PROJECT invitation 생성과 같은 eligibility를 적용한다. 대상은 ACTIVE user이면서 해당 프로젝트 회사의 ACTIVE company member여야 하고, 자기 자신, ACTIVE 프로젝트 멤버, 만료되지 않은 PROJECT `PENDING` invitation 대상은 각각 기존 `409` conflict code로 거절한다. 만료된 `PENDING` invitation은 조회 중 lazy expiration한 뒤 후보를 반환하며, 실제 생성 시 `POST /projects/{projectId}/invitations`가 eligibility를 다시 검증한다.
- 기존 `GET /api/v1/users/lookup`의 C-only global lookup과 회사 구성원 목록 계약은 변경하지 않는다. project-scoped 후보 조회 성공 응답은 기존 4필드 `UserResponse`만 제공한다.
- 회사 invitation role은 `COMPANY_ADMIN` 또는 `MEMBER`만 허용한다. WBS 4.3.1 PROJECT invitation role은 `PROJECT_ADMIN` 또는 `MEMBER`만 허용한다.
- PROJECT invitation 수락 시 기존 프로젝트 membership이 없으면 새 row를 생성하고 `membershipCreated=true`를 반환한다. 기존 row가 `REMOVED`이면 그 row의 ID와 생성 시각을 보존한 채 초대 role과 `ACTIVE` 상태로 재활성화하며 `membershipCreated=false`를 반환한다.
- 공통 accept/decline/cancel endpoint는 request body의 scope 파라미터를 받지 않는다. access token과 `ACTIVE` 사용자 인증을 invitation ID 조회와 저장된 `scopeType` dispatch보다 먼저 검증하므로, 미인증 호출자는 invitation 존재 여부를 추론할 수 없다. invitation에 저장된 `scopeType`으로 COMPANY 또는 PROJECT 처리를 dispatch한다. PROJECT cancel은 같은 회사의 ACTIVE `COMPANY_ADMIN` 또는 ACTIVE `PROJECT_ADMIN`이 수행할 수 있다. `INVITE-01~04` company invitation 의미는 유지한다.
- Sprint 1.5의 invitation-related notification은 필요 시 내부 `notification_events` 원장 기록을 남길 수 있으나 완료 조건에는 포함하지 않는다. Push 알림, 별도 Invitation notification preference, S11 사용자 알림 UI 필수 구현은 Sprint 1.5 범위가 아니다.
- `activity_events`는 활동 원장, `notification_events`는 알림 사건 원본으로 별도 운영하고 통합 저장소로 합치지 않는다. 필요 시 activity 이벤트에서 notification 이벤트를 생성하는 연계만 허용한다.

---

## 7. 이슈/문서/그래프

> **Phase A Sprint 1 구현 범위**: 이 섹션은 후속 MVP reference다. Sprint 1 구현 target은 Auth API와 인증 DB에 한정한다.

> **WBS 3.5 candidate 구현 계약**: `GET/POST /api/v1/projects/{projectId}/issues`, `PATCH /api/v1/issues/{issueId}/status`만 후보 구현 범위다. `POST` 요청은 `title`만 받고 새 이슈는 `BACKLOG`로 생성한다. 세 endpoint의 이슈 응답 객체는 `id`, `issueKey`, `title`, `status`만 포함한다. `description`, `priority`, `assigneeUserId`, `sprintId`, `dueDate`, `sourceType` 등 full Issue contract 필드와 이슈 상세 수정은 이번 후보 구현에서 제외한다.


| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/projects/{projectId}/issues` | A/P/C | WBS 3.5 candidate 이슈 목록/칸반 조회. 응답은 최소 이슈 객체만 제공 |
| POST | `/api/v1/projects/{projectId}/issues` | A/P/C | WBS 3.5 candidate title-only 이슈 생성. `BACKLOG`로 생성 |
| POST | `/api/v1/projects/{projectId}/issues/from-message` | A/P/C | 프로젝트 메시지에서 이슈 생성 |
| GET | `/api/v1/issues/{issueId}` | A/P/C | 이슈 상세 조회 |
| PATCH | `/api/v1/issues/{issueId}` | A 제한/P/C | full Issue contract 기준 이슈 수정. WBS 3.5 candidate 제외 |
| PATCH | `/api/v1/issues/{issueId}/status` | A 제한/P/C | WBS 3.5 candidate 이슈 상태 변경 |
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

이슈/문서 영구 삭제는 Sprint 3 decision-needed다. 참조 무결성, audit, 보존 기간, 권한 기준이 확정되기 전까지 full endpoint contract를 추가하지 않는다. WBS 3.5 candidate의 최소 이슈 계약은 이슈 상세, 담당자, 우선순위, 마감일, Sprint, from-message, 문서, thread, notification 연결을 확정하거나 구현하지 않는다.

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
| POST | `/api/v1/threads/{threadId}/comments` | A/P/C | 댓글 작성, 멘션 알림 생성 |
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

1:1 DM core는 Sprint 3 후보다. 다자간 DM, DM room + participants 일반화, DM 상대 read receipt, DM 메시지 삭제, DM 메시지 이슈 전환은 선행 결정 전 full endpoint contract를 만들지 않는다.

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

알림 목록, 읽음 처리, unread count는 Sprint 3 후보이며 primary WBS는 `NOTI-01`=`4.8`이다. `GLOBAL` scope preference는 현재 계약으로 유지한다. `PROJECT` scope preference는 Sprint 3 decision-needed API boundary이며 실제 지원 기준 확정 전에는 `PROJECT_NOTIFICATION_SCOPE_DEFERRED`로 처리한다. `NOTI-02/03`은 현재 계약/reference row로 유지하고 요구사항추적표 primary WBS backfill 대상에서 제외한다.

`notificationRecipientId`는 `notification_recipients.id`를 의미한다. API는 로그인 사용자 소유의 수신 알림만 읽음 처리한다.

알림 원본 저장소는 MySQL의 `notification_events`, `notification_recipients`, `notification_preferences`를 사용한다. Redis는 unread count와 실시간 push 보조 cache가 필요해진 뒤 조건부 확장으로 검토한다. NoSQL 알림/활동 저장소는 Sprint 1에서 제공하지 않는다.

### 9.1 Notifications Error Cases

| API | 상황 | HTTP | `error.code` | message 예시 |
|---|---|---:|---|---|
| `GET /notifications` | `type` enum 값 오류 | 400 | `VALIDATION_ERROR` | 알림 조회 조건이 올바르지 않습니다. |
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
| GET | `/api/v1/search` | A/P/C | 프로젝트 내부 통합 검색 | Sprint 3 decision-needed |
| GET | `/api/v1/projects/{projectId}/dashboard` | A/P/C | 프로젝트 홈/대시보드 summary | Sprint 3 후보 |

### 10.1 활동 피드

- MySQL `activity_events`를 원장으로 사용한다.
- STOMP push topic: `/topic/projects.{projectId}.activity`.
- push 메시지는 새 이벤트 신호만 보내고, 클라이언트는 REST API로 feed를 재조회한다.
- Redis cache key 예시: `activity:project:{projectId}:latest`.

### 10.2 통합 검색

- 1차 검색 대상: Issue, Document, Thread, Project Message.
- MySQL `LIKE` 또는 FULLTEXT를 우선 사용한다.
- MySQL `LIKE` 또는 FULLTEXT 우선 여부는 Sprint 3 decision-needed다. Typesense/Meilisearch는 기본 계약에 넣지 않는다.

### 10.3 프로젝트 대시보드

- progress, recentIssues, recentDocuments, recentActivities, sprintSummary를 반환한다.
- 기본은 MySQL aggregate query다.
- 성능 문제가 확인되면 `project_dashboard_summaries`와 Redis cache를 추가한다.

---

## 11. MVP 보류/Stub 목록

| 구분 | API/기능 | 처리 | 이유 |
|---|---|---|---|
| Sprint 3 decision-needed | S15 플랫폼 관리자 | full endpoint 미정 | `3.7~3.7.5` planning gate와 Go/No-Go handoff 이후 `4.10`에서 상세 기능 ID, ERD/API/화면 영향도 확인 필요 |
| Sprint 3 decision-needed | 다자간 DM 방 | full endpoint 미정 | DM room + participants 모델 결정 필요 |
| 보류 | DM 메시지 삭제 | 미제공 | 전송/조회/unread 중심 |
| Sprint 3 decision-needed | DM 상대 read receipt | full endpoint 미정 | read receipt 정책 결정 필요 |
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
- S15(`3.7~3.7.5` planning gate 이후 `4.10`), attachment, document optimistic locking, advanced DM, PROJECT notification scope, project restore, permanent delete의 full API 계약.

---

## 13. 요청/응답 JSON 예시

이 섹션은 v2.4의 endpoint, 권한, 보류 범위를 유지한 상태에서 구현자가 참고할 수 있는 JSON 예시를 보강한다.

- `v1.4`의 예시는 형식 참고용으로만 사용한다.
- 실제 계약 기준은 이 문서의 endpoint 표와 v2.2에서 확정한 정책이다.
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

Request Body:

```json
{ "startedAt": "2026-06-17T09:00:00Z" }
```

#### POST `/api/v1/sprints/{sprintId}/complete`

Request Body:

```json
{
  "completedAt": "2026-06-30T18:00:00Z",
  "summary": "Sprint 1 목표 기능 skeleton 완료"
}
```

#### POST `/api/v1/sprints/{sprintId}/issues`

Request Body:

```json
{
  "issueIds": [1000, 1001, 1002]
}
```


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

- 대상 user가 없으면 `404 NOT_FOUND`, 대상이 ACTIVE user 또는 같은 회사 ACTIVE member가 아니면 `409 TARGET_NOT_ACTIVE_COMPANY_MEMBER`를 반환한다.
- self, ACTIVE 프로젝트 멤버, live pending PROJECT invitation은 각각 `409 INVITATION_SELF_NOT_ALLOWED`, `409 ALREADY_PROJECT_MEMBER`, `409 INVITATION_ALREADY_PENDING`을 반환한다.
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

`companyName`, `inviterUserId`, `inviterNickname`, `inviteeEmail`, `inviteeNickname`은 초대 화면 표시용 필드다. Action 처리와 권한 검증에는 `id`, `scopeType`, `companyId`, `projectId`, `inviteeUserId`를 사용한다.

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

> **WBS 3.5 candidate**: 목록의 각 이슈 객체는 `id`, `issueKey`, `title`, `status`만 포함한다.

Response 200:

```json
{
  "data": {
    "issues": [
      {
        "id": 1000,
        "issueKey": "THD-1",
        "title": "로그인 API 구현",
        "status": "BACKLOG"
      }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/projects/{projectId}/issues`

> **WBS 3.5 candidate**: `title`만 허용하며, `description`, `priority`, `assigneeUserId`, `sprintId`, `dueDate`는 받지 않는다. 생성 상태는 항상 `BACKLOG`다.

Request Body:

```json
{
  "title": "로그인 API 구현"
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
      "status": "BACKLOG"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/projects/{projectId}/issues/from-message`

Request Body:

```json
{
  "messageId": 7000,
  "title": "채팅에서 도출된 API 수정",
  "description": "프로젝트 채팅 메시지를 기반으로 이슈를 생성한다.",
  "priority": "MEDIUM",
  "assigneeUserId": 2,
  "sprintId": 300
}
```

#### PATCH `/api/v1/issues/{issueId}`

> full Issue contract 수정 예시는 WBS 3.5 candidate 범위 밖의 reference다.

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

#### PATCH `/api/v1/issues/{issueId}/status`

> **WBS 3.5 candidate**: 응답 이슈 객체는 `id`, `issueKey`, `title`, `status`만 포함한다.

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
      "status": "IN_PROGRESS"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/issues/{issueId}/trash`

Request Body:

```json
{ "reason": "중복 이슈" }
```

#### POST `/api/v1/issues/{issueId}/restore`

Response 200:

```json
{
  "data": {
    "issueId": 1000,
    "trashedAt": null
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

#### PATCH `/api/v1/documents/{documentId}`

> 문서 optimistic locking은 Sprint 3 decision-needed다. 이 예시는 현재 문서 수정 body 기준이며 `version` 또는 `revision` 필드를 확정하지 않는다.

Request Body:

```json
{
  "title": "로그인 API 설계 메모 v2",
  "content": "검증 기준과 에러 응답 예시를 추가한다."
}
```

#### POST `/api/v1/documents/{documentId}/trash`

Request Body:

```json
{ "reason": "문서 통합으로 인한 보관" }
```

#### POST `/api/v1/documents/{documentId}/restore`

Response 200:

```json
{
  "data": {
    "documentId": 5000,
    "trashedAt": null
  },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/projects/{projectId}/documents/graph`

Response 200:

```json
{
  "data": {
    "nodes": [
      { "id": "document-5000", "type": "DOCUMENT", "label": "로그인 API 설계 메모" },
      { "id": "issue-1000", "type": "ISSUE", "label": "로그인 API 구현" }
    ],
    "edges": [
      { "source": "document-5000", "target": "issue-1000", "type": "DOCUMENT_ISSUE" }
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
  "issueId": 1000,
  "linkType": "RELATED"
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

#### PATCH `/api/v1/threads/{threadId}`

Request Body:

```json
{ "topic": "로그인 API 검증 논의" }
```

#### POST `/api/v1/threads/{threadId}/comments`

Request Body:

```json
{
  "content": "@김민정 실패 케이스도 같이 확인해주세요.",
  "mentionedUserIds": [2]
}
```

#### POST `/api/v1/projects/{projectId}/messages`

Request Body:

```json
{
  "content": "로그인 API 응답 형식 확인했습니다.",
  "threadId": null
}
```

#### POST `/api/v1/projects/{projectId}/messages/mark-read`

Request Body:

```json
{
  "lastReadMessageId": 7000,
  "readAt": "2026-06-16T10:00:00Z"
}
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

#### POST `/api/v1/direct-messages/rooms/{roomId}/messages`

Request Body:

```json
{ "content": "API 명세서 v2.3 확인 부탁드립니다." }
```

#### POST `/api/v1/direct-messages/rooms/{roomId}/mark-read`

Request Body:

```json
{
  "lastReadDirectMessageId": 9100,
  "readAt": "2026-06-16T10:00:00Z"
}
```

Response 200:

```json
{
  "data": {
    "roomId": 9000,
    "lastReadDirectMessageId": 9100,
    "unreadCount": 0
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

1. Sprint 1.5 invitation 검증은 local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명을 사용한다. `POST /api/v1/companies`는 Sprint 1.5 구현 범위에 포함하지 않고 MVP/Sprint 2 reference로 유지하며 운영 데이터에는 seed를 포함하지 않는다.
2. `activity_events`는 활동 원장, `notification_events`는 알림 사건 원본으로 별도 운영한다. 두 저장소를 통합하지 않고, 필요 시 activity 이벤트에서 notification 이벤트를 생성하는 연계만 허용한다.
3. 서버 생성 시각과 만료 시각은 UTC 기준으로 생성, 저장, 비교한다. ERD의 `DATETIME` 타입은 유지하고 API 응답은 ISO-8601 UTC `Z` 형식으로 반환한다.
4. 로그인 이메일은 모든 진입점에서 `trim + lowercase`로 정규화한 값을 저장, 조회, 비교한다. 이 기준은 signup, login, password reset, OAuth 계정 연결, invitation lookup에 동일하게 적용한다.
5. OAuth 계정 연결은 이메일만 같다는 이유로 blind auto-link하지 않는다. 이미 로그인한 사용자이거나 provider 이메일 검증을 신뢰할 수 있는 경우에만 기존 `users` 계정과 연결한다.
6. `password-reset/confirm` 성공 시 해당 사용자의 모든 기존 `refresh_tokens`를 revoke한다. 기존 access token은 별도 blacklist 없이 짧은 만료 시간에 따라 자연 만료시킨다.
7. login, OAuth callback, token refresh, `/auth/me`는 `users.status = ACTIVE`인 사용자만 성공 처리한다. `SUSPENDED` 또는 `DELETED` 사용자는 인증을 거부하며, 상태 변경 시 기존 `refresh_tokens`를 revoke한다.
