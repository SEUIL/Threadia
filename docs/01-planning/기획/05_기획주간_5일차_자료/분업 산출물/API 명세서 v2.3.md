# Threadia API 명세서 v2.3

> **작성자**: Threadia 팀
> **작성일**: 2026-06-15
> **최종 수정**: 2026-06-16
> **버전**: v2.3
> **상태**: 확정
> **기준 문서**: API 명세서 초안 v1.4, 기능 분해표 v3.0, ERD v2.0, 보류 기능 최종표 v1.0

---

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

### 응답 형식

```json
{
  "data": {},
  "meta": { "requestId": "req_..." }
}
```

목록 응답은 `page`를 포함한다.

```json
{
  "data": [],
  "page": { "cursor": "next_cursor", "hasNext": true, "size": 20 },
  "meta": { "requestId": "req_..." }
}
```

### 공통 에러 코드

| HTTP | 코드 | 설명 |
|---:|---|---|
| 400 | VALIDATION_ERROR | 요청 형식/유효성 오류 |
| 401 | UNAUTHORIZED | 인증 필요 |
| 403 | FORBIDDEN | 권한 없음 |
| 404 | NOT_FOUND | 리소스 없음 |
| 409 | CONFLICT | 중복 데이터 |
| 409 | ALREADY_COMPANY_MEMBER | 이미 회사 소속 구성원 |

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
```

Sprint 1에서 실제 소셜 provider는 팀 설정으로 확정한다. API와 ERD는 provider 확장이 가능하도록 enum을 열어둔다.

---

## 3. 인증/Auth

| Method | Endpoint | 권한 | 설명 | 상태 |
|---|---|---|---|---|
| POST | `/api/v1/auth/signup` | 공개 | 이메일 회원가입 | MVP |
| POST | `/api/v1/auth/login` | 공개 | 이메일/비밀번호 로그인 | MVP |
| POST | `/api/v1/auth/logout` | A/P/C | 로그아웃 | MVP |
| GET | `/api/v1/auth/me` | A/P/C | 현재 사용자 컨텍스트 조회 | MVP |
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

- `email`: 필수, 이메일 형식, 중복 불가.
- `password`: 필수, 8자 이상, 영문+숫자 조합.
- `nickname`: 필수, 2~100자.

### 3.2 소셜 로그인

```http
GET /oauth2/authorization/{provider}
GET /api/v1/auth/oauth2/callback/{provider}
```

- provider는 `GOOGLE`, `KAKAO`, `NAVER` 중 팀 설정으로 활성화한다.
- provider 계정이 기존 이메일 계정과 일치하면 같은 `users` 계정에 연결한다.
- provider 식별자는 `oauth_accounts`에 저장한다.
- callback 성공 시 JWT accessToken과 사용자 컨텍스트를 반환하거나 프론트엔드 redirect URL에 token 교환 코드를 전달한다.

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

---

## 4. 사용자/User

| Method | Endpoint | 권한 | 설명 | 상태 |
|---|---|---|---|---|
| PATCH | `/api/v1/users/me/profile` | A/P/C | nickname, avatarUrl 수정 | MVP |

```json
{
  "nickname": "김민정",
  "avatarUrl": "https://.../profile.png"
}
```

프로필 사진은 파일 업로드 API가 아니라 `avatarUrl` 저장으로 시작한다. S3 사용은 확정됐지만, 범용 파일 업로드와 presigned URL API는 Sprint 1 이후에 구현한다.

---

## 5. 회사/부서/구성원

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
| POST | `/api/v1/companies/{companyId}/company-members` | C | 가입된 사용자 즉시 등록 |
| PATCH | `/api/v1/company-members/{companyMemberId}` | C | 역할/부서/상태 변경 |

가입되지 않은 이메일은 초대 테이블을 만들지 않고 `400 VALIDATION_ERROR`를 반환한다.

---

## 6. 프로젝트/스프린트

| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/companies/{companyId}/projects` | A/P/C | 프로젝트 목록 조회 |
| POST | `/api/v1/companies/{companyId}/projects` | C | 프로젝트 생성 |
| GET | `/api/v1/projects/{projectId}` | A/P/C | 프로젝트 상세 조회 |
| PATCH | `/api/v1/projects/{projectId}` | P/C | 프로젝트 정보 수정 |
| POST | `/api/v1/projects/{projectId}/archive` | P/C | 프로젝트 아카이빙 |
| GET | `/api/v1/projects/{projectId}/project-members` | A/P/C | 프로젝트 멤버 목록 |
| POST | `/api/v1/projects/{projectId}/project-members` | P/C | 프로젝트 멤버 추가 |
| PATCH | `/api/v1/project-members/{projectMemberId}` | P/C | 프로젝트 멤버 역할/상태 변경 |
| DELETE | `/api/v1/project-members/{projectMemberId}` | P/C | 프로젝트 멤버 제거 |
| GET | `/api/v1/projects/{projectId}/sprints` | A/P/C | Sprint 목록 |
| POST | `/api/v1/projects/{projectId}/sprints` | P/C | Sprint 생성 |
| GET | `/api/v1/sprints/{sprintId}` | A/P/C | Sprint 상세 |
| PATCH | `/api/v1/sprints/{sprintId}` | P/C | Sprint 수정 |
| POST | `/api/v1/sprints/{sprintId}/start` | P/C | Sprint 시작, 알림 발송 |
| POST | `/api/v1/sprints/{sprintId}/complete` | P/C | Sprint 종료 |
| POST | `/api/v1/sprints/{sprintId}/issues` | P/C | Sprint에 이슈 배정 |

프로젝트 복원은 Sprint 1에서 제공하지 않는다.

---

## 7. 이슈/문서/그래프

| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/projects/{projectId}/issues` | A/P/C | 이슈 목록/칸반 조회 |
| POST | `/api/v1/projects/{projectId}/issues` | A/P/C | 이슈 생성 |
| POST | `/api/v1/projects/{projectId}/issues/from-message` | A/P/C | 프로젝트 메시지에서 이슈 생성 |
| GET | `/api/v1/issues/{issueId}` | A/P/C | 이슈 상세 조회 |
| PATCH | `/api/v1/issues/{issueId}` | A 제한/P/C | 이슈 수정 |
| PATCH | `/api/v1/issues/{issueId}/status` | A 제한/P/C | 이슈 상태 변경 |
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

이슈/문서 영구 삭제는 보류하고, Sprint 1은 휴지통/복원까지만 제공한다.

---

## 8. 쓰레드/채팅/DM

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

다자간 DM, DM 상대 read receipt, DM 메시지 삭제, DM 메시지 이슈 전환은 Sprint 1에서 제공하지 않는다.

---

## 9. 알림

| Method | Endpoint | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/notifications` | A/P/C | 본인 알림 목록 |
| GET | `/api/v1/notifications/unread-count` | A/P/C | unread count |
| POST | `/api/v1/notifications/{notificationRecipientId}/read` | 해당 수신 알림의 소유자 | 알림 읽음 처리 |
| POST | `/api/v1/notifications/read-all` | A/P/C | 전체 알림 읽음 처리 |
| GET | `/api/v1/notification-preferences` | A/P/C | 알림 설정 조회 |
| PUT | `/api/v1/notification-preferences` | A/P/C | 전체/이벤트별 on/off 설정 |

MVP는 `GLOBAL` scope만 허용한다. `PROJECT` scope 요청은 `PROJECT_NOTIFICATION_SCOPE_DEFERRED`로 처리한다.

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
| `GET /notification-preferences` | `scopeType=PROJECT` 요청 | 409 | `PROJECT_NOTIFICATION_SCOPE_DEFERRED` | 프로젝트별 알림 설정은 Sprint 1 범위에서 제공하지 않습니다. |
| `PUT /notification-preferences` | `notificationType=ALL` 외부 요청 | 400 | `VALIDATION_ERROR` | `notificationType=ALL`은 외부 API에서 사용할 수 없습니다. |
| `PUT /notification-preferences` | `enabled`와 `allEnabled`를 동시에 전달 | 400 | `VALIDATION_ERROR` | 알림 설정 요청 형식이 올바르지 않습니다. |

---

## 10. 조건부 확장 API

| Method | Endpoint | 권한 | 설명 | 상태 |
|---|---|---|---|---|
| GET | `/api/v1/projects/{projectId}/activity-events` | A/P/C | 프로젝트 활동 피드 조회 | 조건부 확장 |
| GET | `/api/v1/search` | A/P/C | 프로젝트 내부 통합 검색 | 조건부 확장 |
| GET | `/api/v1/projects/{projectId}/dashboard` | A/P/C | 프로젝트 홈/대시보드 summary | 조건부 확장 |

### 10.1 활동 피드

- MySQL `activity_events`를 원장으로 사용한다.
- STOMP push topic: `/topic/projects.{projectId}.activity`.
- push 메시지는 새 이벤트 신호만 보내고, 클라이언트는 REST API로 feed를 재조회한다.
- Redis cache key 예시: `activity:project:{projectId}:latest`.

### 10.2 통합 검색

- 1차 검색 대상: Issue, Document, Thread, Project Message.
- MySQL `LIKE` 또는 FULLTEXT를 우선 사용한다.
- Typesense/Meilisearch는 Sprint 1 보류다.

### 10.3 프로젝트 대시보드

- progress, recentIssues, recentDocuments, recentActivities, sprintSummary를 반환한다.
- 기본은 MySQL aggregate query다.
- 성능 문제가 확인되면 `project_dashboard_summaries`와 Redis cache를 추가한다.

---

## 11. MVP 보류/Stub 목록

| 구분 | API/기능 | 처리 | 이유 |
|---|---|---|---|
| 보류 | 다자간 DM 방 | 미제공 | Sprint 2 |
| 보류 | DM 메시지 삭제 | 미제공 | 전송/조회/unread 중심 |
| 보류 | DM 상대 read receipt | 미제공 | Sprint 2 |
| 보류 | DM 메시지→이슈 전환 | `DM_MESSAGE_CANNOT_CREATE_ISSUE` | DM은 사적 소통 |
| 보류 | Thread→DM 공유 | `THREAD_DM_SHARE_DEFERRED` | 정책 미정 |
| 보류 | Thread→Issue 추가 연결 | `THREAD_ISSUE_LINK_DEFERRED` | 데이터 모델 미정 |
| 보류 | 이슈/문서 영구 삭제 | `PERMANENT_DELETE_DEFERRED` | 참조 보존 정책 필요 |
| 보류 | 프로젝트 복원 | `PROJECT_RESTORE_DEFERRED` | 복원 UI 미정 |
| 보류 | 프로젝트별 알림 설정 | `PROJECT_NOTIFICATION_SCOPE_DEFERRED` | GLOBAL only MVP |
| 보류 | NoSQL 알림/활동 저장소 | 미제공 | MySQL 원장 사용, Sprint 1 제외 |
| 제외 | Redis 원본 저장 | 미제공 | Redis는 cache/push 보조로만 사용 |
| 선택 | 범용 파일 첨부 | `/api/v1/attachments` 미제공 | S3 사용 확정, 구현은 Sprint 1 이후 |
| 보류 | Typesense/Meilisearch | 미제공 | 검색 고도화 후보 |

---

## 12. 6일차 이월 기술 검토

- JWT refresh token 사용 여부.
- 이슈 목록 페이지네이션 방식.
- 이슈 상태 변경 시 알림 자동 발송 로직.
- `from-message` 엔드포인트에서 thread 자동 생성 여부.
- 메시지 목록 실시간 업데이트 방식.
- DM 방 unread count 처리 방식.
- `activity_events`와 `notification_events`의 운영상 분리/통합 기준.

---

## 13. 요청/응답 JSON 예시

이 섹션은 v2.2의 endpoint, 권한, 보류 범위를 유지한 상태에서 구현자가 참고할 수 있는 JSON 예시를 보강한다.

- `v1.4`의 예시는 형식 참고용으로만 사용한다.
- 실제 계약 기준은 이 문서의 endpoint 표와 v2.2에서 확정한 정책이다.
- `PATCH` 요청은 변경할 필드만 전달한다.
- S3 파일 저장 API, Redis/NoSQL 알림 저장 API, 프로젝트별 알림 설정은 Sprint 1에서 제공하지 않는다.

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

```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "email": "minjeong@threadia.com",
      "nickname": "김민정",
      "avatarUrl": null
    },
    "companies": [
      {
        "companyId": 10,
        "companyName": "Threadia Inc.",
        "role": "COMPANY_ADMIN"
      }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

#### POST `/api/v1/auth/logout`

Response 200:

```json
{
  "data": { "success": true },
  "meta": { "requestId": "req_..." }
}
```

#### GET `/api/v1/auth/me`

Response 200:

```json
{
  "data": {
    "user": {
      "id": 1,
      "email": "minjeong@threadia.com",
      "nickname": "김민정",
      "avatarUrl": null
    },
    "companyMemberships": [
      {
        "companyId": 10,
        "companyName": "Threadia Inc.",
        "role": "COMPANY_ADMIN",
        "status": "ACTIVE"
      }
    ],
    "projectMemberships": [
      {
        "projectId": 20,
        "projectKey": "THD",
        "projectName": "Threadia MVP",
        "role": "PROJECT_ADMIN",
        "status": "ACTIVE"
      }
    ],
    "unreadNotificationCount": 3,
    "dmUnreadCount": 2
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

#### PATCH `/api/v1/users/me/profile`

Request Body:

```json
{
  "nickname": "김민정",
  "avatarUrl": "https://.../profile.png"
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
      "createdAt": "2026-06-16T09:00:00"
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

#### POST `/api/v1/companies/{companyId}/company-members`

Request Body:

```json
{
  "userId": 2,
  "departmentId": 1,
  "role": "MEMBER",
  "status": "ACTIVE"
}
```

Response 201:

```json
{
  "data": {
    "companyMember": {
      "id": 101,
      "companyId": 10,
      "departmentId": 1,
      "role": "MEMBER",
      "status": "ACTIVE",
      "user": {
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

#### PATCH `/api/v1/company-members/{companyMemberId}`

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

#### POST `/api/v1/projects/{projectId}/project-members`

Request Body:

```json
{
  "companyMemberId": 101,
  "role": "MEMBER",
  "status": "ACTIVE"
}
```

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
{ "startedAt": "2026-06-17T09:00:00" }
```

#### POST `/api/v1/sprints/{sprintId}/complete`

Request Body:

```json
{
  "completedAt": "2026-06-30T18:00:00",
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

### 13.6 Issue / Document / Graph

#### POST `/api/v1/projects/{projectId}/issues`

Request Body:

```json
{
  "title": "로그인 API 구현",
  "description": "이메일/비밀번호 로그인 API를 구현한다.",
  "status": "TODO",
  "priority": "HIGH",
  "assigneeUserId": 2,
  "sprintId": 300,
  "dueDate": "2026-06-21"
}
```

Response 201:

```json
{
  "data": {
    "issue": {
      "id": 1000,
      "projectId": 20,
      "title": "로그인 API 구현",
      "status": "TODO",
      "priority": "HIGH",
      "sourceType": "MANUAL"
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

Request Body:

```json
{ "status": "IN_PROGRESS" }
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
  "readAt": "2026-06-16T10:00:00"
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
  "readAt": "2026-06-16T10:00:00"
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
      "createdAt": "2026-06-16T10:00:00"
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
    "readAt": "2026-06-16T10:05:00"
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

`notificationType=ALL`은 외부 API에서 사용하지 않는다. 프로젝트별 설정인 `scopeType=PROJECT`는 Sprint 1에서 `PROJECT_NOTIFICATION_SCOPE_DEFERRED`로 처리한다.
