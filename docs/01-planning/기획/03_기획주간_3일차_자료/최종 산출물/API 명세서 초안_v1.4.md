# Threadia API 명세서 초안 v1.4

> 작성일: 2026-06-12 / 수정일: 2026-06-12
> 기준 문서: API 명세서 초안 - DAO 최종수정본, ERD 초안 v1.4
> **v1.4 변경 사항:** `displayName` → `nickname` 변경 / `Invitation` 테이블 제거 (즉시 등록 정책 확정) / `messages.updated_at` 제거 (수정 API 미제공) / `includeTrashed` 기본값 `false` 명시 / `/api/v1` prefix 통일 / `{ data, meta }` 응답 형식 확정

---

## 공통 규칙

### Base URL

```
개발: http://localhost:8080/api/v1
운영: https://api.threadia.com/api/v1
```

### 인증

- JWT Bearer Token 사용
- 헤더: `Authorization: Bearer <token>`
- 인증 제외: `POST /auth/signup`, `POST /auth/login`
- 그 외 모든 요청에 필수

### 공통 응답 형식

**성공 (단일)**
```json
{
  "data": {},
  "meta": { "requestId": "req_..." }
}
```

**성공 (목록)**
```json
{
  "data": [],
  "page": { "cursor": "next_cursor", "hasNext": true, "size": 20 },
  "meta": { "requestId": "req_..." }
}
```

**실패**
```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "프로젝트 접근 권한이 없습니다.",
    "details": {}
  }
}
```

### 공통 에러 코드

| HTTP 상태 | 코드 | 설명 |
|-----------|------|------|
| 400 | VALIDATION_ERROR | 요청 형식/유효성 오류 |
| 401 | UNAUTHORIZED | 인증 필요 |
| 403 | FORBIDDEN | 권한 없음 |
| 404 | NOT_FOUND | 리소스 없음 |
| 409 | CONFLICT | 중복 데이터 |
| 409 | ALREADY_COMPANY_MEMBER | 이미 회사 소속 구성원 |

### 공통 사용자 요약 DTO

```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "avatarUrl": null
}
```

적용 위치: 로그인/me 응답의 `user`, 구성원 목록의 `user`, 이슈의 `reporter`/`assignee`, 메시지/댓글의 `author`, DM의 `otherUser`

### 권한 약어

- **A**: 일반 사용자 (`project_members.role = MEMBER`)
- **P**: 프로젝트 관리자 (`project_members.role = PROJECT_ADMIN`)
- **C**: 회사 관리자 (`company_members.role = COMPANY_ADMIN`)

---

## 1. 권한 정책

### 역할 판정 함수

```text
isCompanyMember(userId, companyId):
  exists CompanyMember(company_id=companyId, user_id=userId, status=ACTIVE)

isCompanyAdmin(userId, companyId):
  exists CompanyMember(company_id=companyId, user_id=userId, role=COMPANY_ADMIN, status=ACTIVE)

canAccessProject(userId, projectId):
  exists ProjectMember(project_id=projectId, user_id=userId, status=ACTIVE)
  OR isCompanyAdmin(userId, project.company_id)

isProjectAdmin(userId, projectId):
  exists ProjectMember(project_id=projectId, user_id=userId, role=PROJECT_ADMIN, status=ACTIVE)
  OR isCompanyAdmin(userId, project.company_id)

canTrashOrRestoreIssue(userId, issueId):
  isProjectAdmin(userId, issue.project_id)
  OR issue.reporter_user_id = userId

canTrashOrRestoreDocument(userId, documentId):
  isProjectAdmin(userId, document.project_id)
  OR document.author_user_id = userId
```

### 역할별 write 권한 요약

| 기능 | A | P | C |
|------|:-:|:-:|:-:|
| 회사 정보/부서/구성원 관리 | ✗ | ✗ | ✓ |
| 프로젝트 생성 | ✗ | ✗ | ✓ |
| 프로젝트 설정/아카이빙 | ✗ | ✓ | ✓ |
| 프로젝트 멤버 관리 | ✗ | ✓ | ✓ |
| 스프린트 생성/시작/종료 | ✗ | ✓ | ✓ |
| 이슈 생성/조회 | ✓ | ✓ | ✓ |
| 이슈 담당자/우선순위/마감일 지정 | ✗ | ✓ | ✓ |
| 이슈 상태 이동 (DONE 제외) | ✓ | ✓ | ✓ |
| 이슈 휴지통/복원 | 본인 등록 이슈만 | ✓ | ✓ |
| 문서 작성/수정 | ✓ | ✓ | ✓ |
| 문서 휴지통/복원 | 본인 작성 문서만 | ✓ | ✓ |
| 이슈-문서 연결 | ✓ | ✓ | ✓ |
| 프로젝트 채팅/DM | ✓ | ✓ | ✓ |
| 알림 목록/읽음/개인 설정 | ✓ | ✓ | ✓ |

---

## 2. Enum 값 목록

```text
companyMemberRole:     COMPANY_ADMIN | MEMBER
projectMemberRole:     PROJECT_ADMIN | MEMBER
projectStatus:         PLANNING | ACTIVE | PAUSED | COMPLETED | ARCHIVED
sprintStatus:          PLANNED | ACTIVE | COMPLETED
issueStatus:           BACKLOG | TODO | IN_PROGRESS | IN_REVIEW | DONE
issuePriority:         LOW | MEDIUM | HIGH | URGENT
issueSourceType:       MANUAL | MESSAGE
documentType:          DOC | MEETING_NOTE
notificationType:      ISSUE_ASSIGNED | ISSUE_STATUS_CHANGED | THREAD_COMMENTED | MENTIONED |
                       DOCUMENT_UPDATED | DOCUMENT_LINKED_TO_ISSUE | SPRINT_STARTED | SPRINT_ENDED |
                       PROJECT_MESSAGE | DIRECT_MESSAGE | MEMBER_INVITED | ROLE_CHANGED
notificationScopeType: GLOBAL
```

---

## 3. 인증 (Auth)

### 3-1. 회원가입

```
POST /api/v1/auth/signup
```

**권한:** 비로그인

**Request Body**
```json
{
  "email": "minjeong@threadia.com",
  "password": "P@ssword1234",
  "nickname": "김민정",
  "avatarUrl": null
}
```

**Response 201**
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

**Validation**
- `email`: 필수, 이메일 형식, 중복 불가
- `password`: 필수, 8자 이상, 영문+숫자 조합
- `nickname`: 필수, 2~100자

---

### 3-2. 로그인

```
POST /api/v1/auth/login
```

**권한:** 비로그인

**Request Body**
```json
{
  "email": "minjeong@threadia.com",
  "password": "P@ssword1234"
}
```

**Response 200**
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
      { "companyId": 10, "companyName": "Threadia Inc.", "role": "COMPANY_ADMIN" }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

---

### 3-3. 로그아웃

```
POST /api/v1/auth/logout
```

**권한:** A/P/C

**Response 200**
```json
{
  "data": { "success": true },
  "meta": { "requestId": "req_..." }
}
```

---

### 3-4. 현재 사용자 컨텍스트 조회

```
GET /api/v1/auth/me
```

**권한:** A/P/C

> 현재 사용자의 계정, 소속, 권한, unread count를 반환. `User.role` 필드는 반환하지 않음.

**Response 200**
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

---

## 4. 회사 (Companies)

### 4-1. 회사 생성

```
POST /api/v1/companies
```

**권한:** 로그인 사용자 (생성자가 자동으로 COMPANY_ADMIN)

**Request Body**
```json
{
  "name": "Threadia Inc.",
  "representativeName": "황스일",
  "description": "개발팀을 위한 협업 플랫폼",
  "industryCategory": "IT/소프트웨어",
  "logoUrl": null
}
```

**Response 201**
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
      "createdAt": "2026-06-12T09:00:00"
    },
    "companyMember": { "role": "COMPANY_ADMIN" }
  },
  "meta": { "requestId": "req_..." }
}
```

**Validation**
- `name`: 필수
- `representativeName`: 필수

---

### 4-2. 회사 정보 조회

```
GET /api/v1/companies/{companyId}
```

**권한:** 해당 회사 소속 A/P/C

**Response 200** — 회사 정보 반환 (수정은 C만 가능)

---

### 4-3. 회사 정보 수정

```
PATCH /api/v1/companies/{companyId}
```

**권한:** C

**Request Body** (변경할 필드만)
```json
{
  "name": "Threadia Corp.",
  "representativeName": "황스일",
  "description": "업데이트된 설명",
  "industryCategory": "IT/소프트웨어",
  "logoUrl": "https://s3.../logo/1.png"
}
```

**Response 200** — 수정된 회사 정보 반환

---

### 4-4. 회사 로고 저장

```
POST /api/v1/companies/{companyId}/logo
```

**권한:** C

**Request Body**
```json
{ "logoUrl": "https://s3.../logo/1.png" }
```

> MVP 권장: URL 직접 저장. S3 presigned URL은 선택 사항.

**Response 200** — `logoUrl`, `uploadMode` 반환

---

### 4-5. 회사 관리 통계

```
GET /api/v1/companies/{companyId}/stats
```

**권한:** C

**Response 200**
```json
{
  "data": {
    "memberCount": 8,
    "departmentCount": 3,
    "projectCount": 4,
    "projectStatusCounts": { "ACTIVE": 2, "PLANNING": 1, "ARCHIVED": 1 }
  },
  "meta": { "requestId": "req_..." }
}
```

---

## 5. 부서 (Departments)

### 5-1. 부서 목록 조회

```
GET /api/v1/companies/{companyId}/departments
```

**권한:** C

**Query Params**
- `includeMemberCount`: `true` | `false` (optional)

**Response 200**
```json
{
  "data": [
    { "id": 1, "name": "개발팀", "memberCount": 4 },
    { "id": 2, "name": "기획팀", "memberCount": 2 }
  ],
  "meta": { "requestId": "req_..." }
}
```

---

### 5-2. 부서 생성

```
POST /api/v1/companies/{companyId}/departments
```

**권한:** C

**Request Body**
```json
{ "name": "디자인팀" }
```

**Response 201** — 생성된 부서 반환

> MVP는 단일 depth. hierarchy 구조는 MVP 이후 검토.

---

### 5-3. 부서 수정

```
PATCH /api/v1/departments/{departmentId}
```

**권한:** C

**Request Body**
```json
{ "name": "UX/디자인팀" }
```

**Response 200** — 수정된 부서 반환

---

### 5-4. 부서 삭제

```
DELETE /api/v1/departments/{departmentId}
```

**권한:** C

**Query Params**
- `memberHandlingPolicy`: `SET_NULL` (기본값)

> 소속 구성원의 `department_id = NULL` 처리 후 삭제.

**Response 200**
```json
{
  "data": { "deleted": true, "affectedMemberCount": 3 },
  "meta": { "requestId": "req_..." }
}
```

---

## 6. 회사 구성원 (Company Members)

### 6-1. 구성원 목록 조회

```
GET /api/v1/companies/{companyId}/company-members
```

**권한:** C

**Query Params**
- `status`: `ACTIVE` | `SUSPENDED` | `LEFT` (optional)
- `departmentId`: 부서 ID (optional)
- `q`: 검색어 (optional)

**Response 200**
```json
{
  "data": [
    {
      "id": 1,
      "user": {
        "id": 3,
        "email": "user@threadia.com",
        "nickname": "여도현",
        "avatarUrl": null
      },
      "role": "MEMBER",
      "status": "ACTIVE",
      "department": { "id": 1, "name": "개발팀" },
      "joinedAt": "2026-06-12T09:00:00"
    }
  ],
  "meta": { "requestId": "req_..." }
}
```

---

### 6-2. 구성원 추가 (즉시 등록)

```
POST /api/v1/companies/{companyId}/company-members
```

**권한:** C

**Request Body**
```json
{
  "email": "newhire@example.com",
  "defaultCompanyRole": "MEMBER",
  "departmentId": 1
}
```

**케이스별 처리:**

| 케이스 | 응답 |
|--------|------|
| 가입된 사용자 이메일 → 즉시 구성원 등록 | 201 Created |
| 이미 소속된 사용자 이메일 | 409 Conflict / ALREADY_COMPANY_MEMBER |
| 가입되지 않은 이메일 | 400 Bad Request ("먼저 회원가입이 필요합니다") |

**Response 201**
```json
{
  "data": {
    "id": 5,
    "user": {
      "id": 7,
      "email": "newhire@example.com",
      "nickname": "신규 팀원",
      "avatarUrl": null
    },
    "role": "MEMBER",
    "status": "ACTIVE",
    "joinedAt": "2026-06-12T10:00:00"
  },
  "meta": { "requestId": "req_..." }
}
```

> **즉시 등록 정책:** Invitation 테이블 없음. 가입된 사용자만 즉시 등록 가능. 미가입 이메일은 400 Bad Request.

---

### 6-3. 구성원 역할/부서/상태 변경

```
PATCH /api/v1/company-members/{companyMemberId}
```

**권한:** C

**Request Body** (변경할 필드만)
```json
{
  "role": "COMPANY_ADMIN",
  "departmentId": 2,
  "status": "ACTIVE"
}
```

> 마지막 COMPANY_ADMIN 제거 방지.

**Response 200** — 수정된 구성원 정보 반환

---

## 7. 프로젝트 (Projects)

### 7-1. 프로젝트 목록 조회

```
GET /api/v1/companies/{companyId}/projects
```

**권한:** A/P/C

> C는 회사 전체, A/P는 ProjectMember로 소속된 프로젝트만 조회.

**Query Params**
- `status`: `PLANNING` | `ACTIVE` | `PAUSED` | `COMPLETED` | `ARCHIVED` (optional)
- `q`: 검색어 (optional)

**Response 200**
```json
{
  "data": [
    {
      "id": 20,
      "projectKey": "THD",
      "name": "Threadia MVP",
      "status": "ACTIVE",
      "memberCount": 6,
      "issueCount": 12,
      "myRole": "PROJECT_ADMIN"
    }
  ],
  "meta": { "requestId": "req_..." }
}
```

---

### 7-2. 프로젝트 생성

```
POST /api/v1/companies/{companyId}/projects
```

**권한:** C (COMPANY_ADMIN only)

**Request Body**
```json
{
  "projectKey": "THD",
  "name": "Threadia MVP",
  "description": "6주 MVP 개발 프로젝트",
  "startDate": "2026-06-12",
  "endDate": "2026-07-25",
  "initialPmUserIds": [1, 2],
  "status": "PLANNING"
}
```

**Response 201** — 생성된 프로젝트 및 초기 프로젝트 멤버 반환

---

### 7-3. 프로젝트 상세 조회

```
GET /api/v1/projects/{projectId}
```

**권한:** A/P/C

**Response 200** — 프로젝트 정보, 내 역할(`myProjectRole`), 통계(`stats`) 반환

---

### 7-4. 프로젝트 정보 수정

```
PATCH /api/v1/projects/{projectId}
```

**권한:** P/C

**Request Body** (변경할 필드만)
```json
{
  "name": "Threadia MVP v2",
  "description": "업데이트된 설명",
  "status": "ACTIVE",
  "startDate": "2026-06-12",
  "endDate": "2026-08-01"
}
```

**Response 200** — 수정된 프로젝트 반환

---

### 7-5. 프로젝트 아카이빙

```
POST /api/v1/projects/{projectId}/archive
```

**권한:** P/C

**Request Body**
```json
{ "reason": "MVP 완료" }
```

**Response 200**
```json
{
  "data": {
    "id": 20,
    "status": "ARCHIVED",
    "archivedAt": "2026-07-25T18:00:00"
  },
  "meta": { "requestId": "req_..." }
}
```

> **프로젝트 복원:** MVP 보류 (`PROJECT_RESTORE_DEFERRED`).

---

## 8. 프로젝트 구성원 (Project Members)

### 8-1. 프로젝트 멤버 목록 조회

```
GET /api/v1/projects/{projectId}/project-members
```

**권한:** A/P/C

**Query Params**
- `role`: `PROJECT_ADMIN` | `MEMBER` (optional)
- `status`: `ACTIVE` | `REMOVED` (optional)

**Response 200** — 멤버 목록 반환 (`user`, `role`, `status` 포함)

---

### 8-2. 프로젝트 멤버 추가

```
POST /api/v1/projects/{projectId}/project-members
```

**권한:** P/C

**Request Body**
```json
{
  "companyMemberId": 5,
  "role": "MEMBER"
}
```

> 같은 회사의 active `company_members` 사용자만 추가 가능.

**Response 201** — 추가된 프로젝트 멤버 반환

---

### 8-3. 프로젝트 멤버 역할/상태 변경

```
PATCH /api/v1/project-members/{projectMemberId}
```

**권한:** P/C

**Request Body** (변경할 필드만)
```json
{
  "role": "PROJECT_ADMIN",
  "status": "ACTIVE"
}
```

> 최소 1명 PROJECT_ADMIN 유지 정책 필요.

**Response 200** — 수정된 프로젝트 멤버 반환

---

### 8-4. 프로젝트 멤버 제거

```
DELETE /api/v1/project-members/{projectMemberId}
```

**권한:** P/C

> 담당 이슈 보유 멤버 제거 정책은 구현 시 결정 필요.

**Response 200**
```json
{
  "data": { "removed": true },
  "meta": { "requestId": "req_..." }
}
```

---

## 9. 스프린트 (Sprints)

### 9-1. 스프린트 목록 조회

```
GET /api/v1/projects/{projectId}/sprints
```

**권한:** A/P/C

**Query Params**
- `status`: `PLANNED` | `ACTIVE` | `COMPLETED` (optional)

**Response 200**
```json
{
  "data": [
    {
      "id": 1,
      "name": "Sprint 1",
      "goal": "인증 및 기본 구조 완성",
      "status": "ACTIVE",
      "startsAt": "2026-06-17T00:00:00",
      "endsAt": "2026-06-21T23:59:59"
    }
  ],
  "meta": { "requestId": "req_..." }
}
```

---

### 9-2. 스프린트 생성

```
POST /api/v1/projects/{projectId}/sprints
```

**권한:** P/C

**Request Body**
```json
{
  "name": "Sprint 1",
  "goal": "인증 및 기본 프로젝트 구조 완성",
  "startsAt": "2026-06-17T00:00:00",
  "endsAt": "2026-06-21T23:59:59",
  "issueIds": [1, 2, 3]
}
```

**Response 201** — 생성된 스프린트 반환

---

### 9-3. 스프린트 상세 조회

```
GET /api/v1/sprints/{sprintId}
```

**권한:** A/P/C

**Response 200** — 스프린트 정보 및 소속 이슈 목록 반환

---

### 9-4. 스프린트 정보 수정

```
PATCH /api/v1/sprints/{sprintId}
```

**권한:** P/C

**Request Body** (변경할 필드만)
```json
{
  "name": "Sprint 1 (수정)",
  "goal": "업데이트된 목표",
  "endsAt": "2026-06-22T23:59:59"
}
```

**Response 200** — 수정된 스프린트 반환

---

### 9-5. 스프린트 시작

```
POST /api/v1/sprints/{sprintId}/start
```

**권한:** P/C

**Request Body**
```json
{ "notifyMembers": true }
```

**Response 200**
```json
{
  "data": {
    "sprint": { "id": 1, "status": "ACTIVE" },
    "notificationSummary": { "sent": 5 }
  },
  "meta": { "requestId": "req_..." }
}
```

---

### 9-6. 스프린트 종료

```
POST /api/v1/sprints/{sprintId}/complete
```

**권한:** P/C

**Request Body**
```json
{ "moveOpenIssuesTo": "BACKLOG" }
```

> `moveOpenIssuesTo`: `BACKLOG` 또는 다음 스프린트 ID

**Response 200** — `sprint.status = COMPLETED` 반환

---

### 9-7. 스프린트에 이슈 배정

```
POST /api/v1/sprints/{sprintId}/issues
```

**권한:** P/C

**Request Body**
```json
{ "issueIds": [4, 5, 6] }
```

> `issue.project_id` 일치 검증 필수.

**Response 200** — 배정된 이슈 목록 반환

---

## 10. 이슈 (Issues)

### 10-1. 이슈 목록 조회 (칸반/백로그)

```
GET /api/v1/projects/{projectId}/issues
```

**권한:** A/P/C

**Query Params**
- `status`: `BACKLOG` | `TODO` | `IN_PROGRESS` | `IN_REVIEW` | `DONE` (optional)
- `sprintId`: 스프린트 ID (없으면 백로그)
- `assigneeId`: 담당자 사용자 ID (optional)
- `includeTrashed`: `true` | `false` (optional, **기본값: false**)

> `includeTrashed=false`(기본값)이면 `trashed_at IS NULL`인 이슈만 반환.

**Response 200**
```json
{
  "data": [
    {
      "id": 1,
      "issueKey": "THD-1",
      "title": "로그인 API 구현",
      "status": "IN_PROGRESS",
      "priority": "HIGH",
      "assignee": { "id": 3, "email": "user@threadia.com", "nickname": "여도현", "avatarUrl": null },
      "dueDate": "2026-06-20",
      "sprintId": 1
    }
  ],
  "meta": { "requestId": "req_..." }
}
```

---

### 10-2. 이슈 생성

```
POST /api/v1/projects/{projectId}/issues
```

**권한:** A/P/C

**Request Body**
```json
{
  "title": "칸반 보드 UI 구현",
  "description": "드래그앤드롭 지원 5단계 칸반",
  "status": "BACKLOG",
  "priority": "MEDIUM",
  "assigneeUserId": 2,
  "sprintId": 1,
  "dueDate": "2026-06-25"
}
```

> `assigneeUserId`, `priority`, `dueDate` 지정은 P/C만 가능.
> `assigneeUserId`는 해당 프로젝트의 ProjectMember여야 함.

**Response 201** — 생성된 이슈 반환

---

### 10-3. 메시지에서 이슈 생성

```
POST /api/v1/projects/{projectId}/issues/from-message
```

**권한:** A/P/C

**Request Body**
```json
{
  "messageId": 55,
  "title": "ERD 최종 검토 필요",
  "description": "채팅에서 제기된 이슈",
  "priority": "HIGH",
  "assigneeUserId": 3,
  "dueDate": "2026-06-20"
}
```

> `message.project_id = projectId` 검증 필수.
> DM 메시지는 이슈 생성 원본 불가 (`DM_MESSAGE_CANNOT_CREATE_ISSUE`).

**Response 201**
```json
{
  "data": {
    "issue": {
      "id": 7,
      "issueKey": "THD-7",
      "title": "ERD 최종 검토 필요",
      "status": "BACKLOG",
      "sourceType": "MESSAGE",
      "sourceMessageId": 55,
      "sourceMessagePreview": "ERD 검토 완료했습니다!"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

---

### 10-4. 이슈 상세 조회

```
GET /api/v1/issues/{issueId}
```

**권한:** A/P/C

**Query Params**
- `includeLinks`: `true` | `false` (optional)
- `includeThread`: `true` | `false` (optional)
- `includeSourceMessage`: `true` | `false` (optional)

**Response 200**
```json
{
  "data": {
    "issue": {
      "id": 1,
      "projectId": 20,
      "issueKey": "THD-1",
      "title": "로그인 API 구현",
      "description": "# 로그인 API\n\n## 요구사항\n- JWT 발급...",
      "status": "IN_PROGRESS",
      "priority": "HIGH",
      "reporter": { "id": 1, "email": "user@threadia.com", "nickname": "황스일", "avatarUrl": null },
      "assignee": { "id": 3, "email": "user@threadia.com", "nickname": "여도현", "avatarUrl": null },
      "dueDate": "2026-06-20",
      "sourceType": "MANUAL",
      "sourceMessageId": null,
      "trashedAt": null,
      "createdAt": "2026-06-12T09:00:00",
      "updatedAt": "2026-06-12T09:00:00"
    },
    "documents": [{ "id": 2, "title": "API 설계 회의록" }],
    "statusHistory": [
      { "fromStatus": null, "toStatus": "BACKLOG", "changedAt": "2026-06-12T09:00:00" }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

---

### 10-5. 이슈 수정

```
PATCH /api/v1/issues/{issueId}
```

**권한:** A(제한)/P/C

**역할별 수정 가능 필드:**

| 필드 | A | P/C |
|------|:-:|:---:|
| `title` | ✅ | ✅ |
| `description` | ✅ | ✅ |
| `assigneeUserId` | ❌ | ✅ |
| `priority` | ❌ | ✅ |
| `dueDate` | ❌ | ✅ |
| `sprintId` | ❌ | ✅ |

> A가 제한된 필드를 포함해 요청 시 → 403 Forbidden

**Request Body** (변경할 필드만)
```json
{
  "title": "로그인 API 구현 (수정)",
  "description": "업데이트된 설명"
}
```

**Response 200** — 수정된 이슈 반환

---

### 10-6. 이슈 상태 변경

```
PATCH /api/v1/issues/{issueId}/status
```

**권한:** A(제한)/P/C

**역할별 허용 상태 이동:**

| 역할 | 허용 범위 |
|------|-----------|
| A | `BACKLOG` → `TODO` → `IN_PROGRESS` → `IN_REVIEW` |
| P/C | 전체 (`IN_REVIEW` → `DONE` 포함) |

**Request Body**
```json
{ "toStatus": "IN_REVIEW" }
```

**Response 200** — 수정된 이슈 및 상태 이력 반환

---

### 10-7. 이슈 휴지통 이동

```
POST /api/v1/issues/{issueId}/trash
```

**권한:** P/C 또는 이슈 등록자 본인

**Request Body**
```json
{ "reason": "중복 이슈" }
```

**Response 200**
```json
{
  "data": { "id": 1, "trashedAt": "2026-06-12T10:00:00" },
  "meta": { "requestId": "req_..." }
}
```

---

### 10-8. 이슈 복원

```
POST /api/v1/issues/{issueId}/restore
```

**권한:** P/C 또는 이슈 등록자 본인

**Response 200**
```json
{
  "data": { "id": 1, "trashedAt": null },
  "meta": { "requestId": "req_..." }
}
```

> **영구 삭제:** MVP 미제공 (`PERMANENT_DELETE_DEFERRED`).

---

## 11. 문서 (Documents)

### 11-1. 문서 목록 조회

```
GET /api/v1/projects/{projectId}/documents
```

**권한:** A/P/C

**Query Params**
- `type`: `DOC` | `MEETING_NOTE` (optional)
- `q`: 검색어 (optional)
- `includeTrashed`: `true` | `false` (optional, **기본값: false**)

> `includeTrashed=false`(기본값)이면 `trashed_at IS NULL`인 문서만 반환.

**Response 200** — 문서 목록 반환 (`id`, `title`, `type`, `author`, `updatedAt`, `linkedIssueCount`)

---

### 11-2. 문서 생성

```
POST /api/v1/projects/{projectId}/documents
```

**권한:** A/P/C

**Request Body**
```json
{
  "title": "Sprint 1 회의록",
  "bodyMarkdown": "# Sprint 1 킥오프\n\n## 목표\n...",
  "documentType": "MEETING_NOTE"
}
```

**Response 201** — 생성된 문서 반환

---

### 11-3. 문서 상세 조회

```
GET /api/v1/documents/{documentId}
```

**권한:** A/P/C

**Query Params**
- `includeIssueLinks`: `true` | `false` (optional)
- `includeThread`: `true` | `false` (optional)

**Response 200** — 문서 정보, 연결 이슈 목록, 쓰레드 반환

---

### 11-4. 문서 수정

```
PATCH /api/v1/documents/{documentId}
```

**권한:** A(작성자)/P/C

**Request Body** (변경할 필드만)
```json
{
  "title": "Sprint 1 회의록 (수정)",
  "bodyMarkdown": "# Sprint 1 킥오프\n\n(업데이트됨)",
  "documentType": "MEETING_NOTE"
}
```

**Response 200** — 수정된 문서 반환. `DOCUMENT_UPDATED` 알림 후보 생성.

---

### 11-5. 문서 휴지통 이동

```
POST /api/v1/documents/{documentId}/trash
```

**권한:** P/C 또는 문서 작성자 본인

**Request Body**
```json
{ "reason": "불필요한 회의록" }
```

**Response 200** — `document.trashedAt` 반환

---

### 11-6. 문서 복원

```
POST /api/v1/documents/{documentId}/restore
```

**권한:** P/C 또는 문서 작성자 본인

**Response 200** — `document.trashedAt = null` 반환

> **영구 삭제:** MVP 미제공 (`PERMANENT_DELETE_DEFERRED`).

---

### 11-7. 그래프 데이터 조회

```
GET /api/v1/projects/{projectId}/documents/graph
```

**권한:** A/P/C

**Query Params**
- `includeIsolated`: `true` | `false` (optional)
- `documentType`: `DOC` | `MEETING_NOTE` (optional)

**Response 200**
```json
{
  "data": {
    "nodes": [
      { "id": "doc_1", "type": "DOCUMENT", "label": "회의록", "documentType": "MEETING_NOTE" },
      { "id": "issue_7", "type": "ISSUE", "label": "로그인 오류", "status": "IN_PROGRESS" }
    ],
    "edges": [
      { "id": "link_10", "source": "doc_1", "target": "issue_7", "linkType": "REFERENCE" }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

---

## 12. 이슈-문서 연결 (Document-Issue Links)

### 12-1. 연결 목록 조회

```
GET /api/v1/projects/{projectId}/document-issue-links
```

**권한:** A/P/C

**Query Params**
- `issueId`: 이슈 ID (optional)
- `documentId`: 문서 ID (optional)

**Response 200** — 연결 목록 반환

---

### 12-2. 연결 생성

```
POST /api/v1/projects/{projectId}/document-issue-links
```

**권한:** A/P/C

**Request Body**
```json
{
  "issueId": 1,
  "documentId": 2,
  "linkType": "REFERENCE"
}
```

> `issue.project_id = document.project_id = projectId` 검증 필수.
> `linkType`은 기본 `REFERENCE`. MVP UI에서 선택하지 않음.

**Response 201** — 생성된 연결 및 이슈/문서 요약 반환

---

### 12-3. 연결 해제

```
DELETE /api/v1/document-issue-links/{linkId}
```

**권한:** A/P/C (생성자 또는 P/C 권장)

**Response 200**
```json
{
  "data": { "deleted": true },
  "meta": { "requestId": "req_..." }
}
```

---

### 12-4. 이슈 기준 연결 문서 조회

```
GET /api/v1/issues/{issueId}/documents
```

**권한:** A/P/C

**Response 200** — 연결된 문서 목록 및 링크 정보 반환

---

### 12-5. 문서 기준 연결 이슈 조회

```
GET /api/v1/documents/{documentId}/issues
```

**권한:** A/P/C

**Response 200** — 연결된 이슈 목록 및 링크 정보 반환

---

## 13. 쓰레드 (Threads)

### 13-1. 쓰레드 조회 (대상별)

```
GET /api/v1/projects/{projectId}/threads
```

**권한:** A/P/C

**Query Params**
- `targetType`: `ISSUE` | `DOCUMENT` | `MESSAGE` (필수)
- `targetId`: 대상 ID (필수)

**Response 200** — 쓰레드 및 댓글 목록 반환

---

### 13-2. 쓰레드 생성 (get-or-create)

```
POST /api/v1/projects/{projectId}/threads
```

**권한:** A/P/C

**Request Body**
```json
{
  "targetType": "ISSUE",
  "targetId": 1,
  "topic": "로그인 API 구현 논의"
}
```

> UNIQUE `(targetType, targetId)` — 이미 존재하면 기존 쓰레드 반환.
> polymorphic target은 애플리케이션 레이어에서 검증.

**Response 201 / 200** — 쓰레드 반환

---

### 13-3. 쓰레드 상세 조회

```
GET /api/v1/threads/{threadId}
```

**권한:** A/P/C

**Response 200**
```json
{
  "data": {
    "thread": {
      "id": 1,
      "targetType": "ISSUE",
      "targetId": 1,
      "topic": "로그인 API 논의",
      "projectId": 20,
      "createdAt": "2026-06-12T09:00:00"
    },
    "comments": [
      {
        "id": 1,
        "author": { "id": 2, "email": "user@threadia.com", "nickname": "정철진", "avatarUrl": null },
        "body": "UI 컴포넌트 구조 확인 완료했습니다.",
        "decisionFlag": false,
        "createdAt": "2026-06-12T10:00:00",
        "updatedAt": "2026-06-12T10:00:00"
      }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

---

### 13-4. 쓰레드 topic 수정

```
PATCH /api/v1/threads/{threadId}
```

**권한:** 작성자 또는 P/C

**Request Body**
```json
{ "topic": "로그인 API 구현 최종 논의" }
```

**Response 200** — 수정된 쓰레드 반환

---

### 13-5. 쓰레드 댓글 작성

```
POST /api/v1/threads/{threadId}/comments
```

**권한:** A/P/C

**Request Body**
```json
{
  "body": "API 스펙 검토 완료. LGTM!",
  "mentionedUserIds": [3, 4],
  "decisionFlag": false
}
```

> 멘션 대상에 `MENTIONED` 알림 생성.
> 일반 댓글은 target 관련자(작성자/reporter/assignee/기존 댓글 참여자)에게만 `THREAD_COMMENTED` 알림 — 전체 프로젝트 멤버 fan-out 금지.

**Response 201** — 작성된 댓글 반환

> **삭제 불가 정책:** 댓글 삭제 API 미제공. 맥락 보존 목적.
> **Thread→DM 공유:** MVP 보류 (`THREAD_DM_SHARE_DEFERRED`).
> **Thread→Issue 추가 연결:** MVP 보류 (`THREAD_ISSUE_LINK_DEFERRED`).

---

## 14. 채팅 (Messages)

### 14-1. 메시지 목록 조회

```
GET /api/v1/projects/{projectId}/messages
```

**권한:** A/P/C

**Query Params**
- `cursor`: 페이지네이션 커서 (마지막 메시지 ID)
- `size`: 페이지 크기 (기본 20)
- `afterMessageId`: 특정 메시지 이후 조회

**Response 200**
```json
{
  "data": [
    {
      "id": 1,
      "author": { "id": 1, "email": "user@threadia.com", "nickname": "황스일", "avatarUrl": null },
      "body": "오늘 ERD 초안 리뷰 합시다",
      "createdAt": "2026-06-12T09:00:00"
    }
  ],
  "page": { "cursor": null, "hasNext": false, "size": 20 },
  "meta": { "requestId": "req_..." }
}
```

> **삭제 불가 정책:** 삭제 API 미제공.
> **수정 불가 정책:** 수정 API 미제공 (MVP).

---

### 14-2. 메시지 작성

```
POST /api/v1/projects/{projectId}/messages
```

**권한:** A/P/C

**Request Body**
```json
{
  "body": "ERD 검토 완료했습니다!",
  "mentionedUserIds": [3]
}
```

**Response 201** — 작성된 메시지 반환. `PROJECT_MESSAGE` 알림 후보 생성.

---

### 14-3. 메시지 상세 조회

```
GET /api/v1/messages/{messageId}
```

**권한:** A/P/C (해당 프로젝트 접근 가능자)

**Response 200** — 메시지 본문, 연결 쓰레드, 이슈 전환 정보 반환

---

### 14-4. 채팅방 읽음 위치 갱신

```
POST /api/v1/projects/{projectId}/messages/mark-read
```

**권한:** A/P/C

**Request Body**
```json
{ "lastReadMessageId": 42 }
```

> 내 `ProjectChatMemberState.last_read_message_id` upsert. 상대 read receipt 아님.

**Response 200**
```json
{
  "data": { "unreadCount": 0, "lastReadAt": "2026-06-12T10:00:00" },
  "meta": { "requestId": "req_..." }
}
```

---

### 14-5. Unread count 조회

```
GET /api/v1/projects/{projectId}/messages/unread-count
```

**권한:** A/P/C

**Response 200**
```json
{
  "data": { "unreadCount": 5 },
  "meta": { "requestId": "req_..." }
}
```

---

## 15. 1:1 DM (Direct Messages)

### 15-1. DM 방 목록 조회

```
GET /api/v1/companies/{companyId}/direct-messages/rooms
```

**권한:** A/P/C

**Response 200**
```json
{
  "data": [
    {
      "roomId": 1,
      "otherUser": { "id": 3, "email": "user@threadia.com", "nickname": "여도현", "avatarUrl": null },
      "lastMessage": "내일 오전 회의 괜찮으세요?",
      "lastMessageAt": "2026-06-12T09:00:00",
      "unreadCount": 2
    }
  ],
  "meta": { "requestId": "req_..." }
}
```

---

### 15-2. DM 방 생성 또는 조회 (get-or-create)

```
POST /api/v1/companies/{companyId}/direct-messages/rooms
```

**권한:** A/P/C (같은 회사 active 구성원)

**Request Body**
```json
{ "targetUserId": 3 }
```

**생성 규칙:**
```text
- currentUserId = targetUserId이면 400 VALIDATION_ERROR
- targetUserId가 같은 company active member가 아니면 403
- user1_id = min(currentUserId, targetUserId), user2_id = max(...)
- UNIQUE(company_id, user1_id, user2_id) 기준 get-or-create
```

**Response 201 / 200**
```json
{
  "data": {
    "roomId": 1,
    "otherUser": { "id": 3, "email": "user@threadia.com", "nickname": "여도현", "avatarUrl": null },
    "unreadCount": 0
  },
  "meta": { "requestId": "req_..." }
}
```

---

### 15-3. DM 메시지 목록 조회

```
GET /api/v1/direct-messages/rooms/{roomId}/messages
```

**권한:** 해당 DM 방 참여자

**Query Params**
- `cursor`: 페이지네이션 커서
- `size`: 기본 20

**Response 200**
```json
{
  "data": [
    {
      "id": 1,
      "sender": { "id": 1, "email": "user@threadia.com", "nickname": "황스일", "avatarUrl": null },
      "body": "내일 오전 회의 괜찮으세요?",
      "createdAt": "2026-06-12T09:00:00"
    }
  ],
  "page": { "cursor": null, "hasNext": false, "size": 20 },
  "meta": { "requestId": "req_..." }
}
```

---

### 15-4. DM 메시지 전송

```
POST /api/v1/direct-messages/rooms/{roomId}/messages
```

**권한:** 해당 DM 방 참여자

**Request Body**
```json
{ "body": "네, 10시에 봐요!" }
```

**Response 201** — 전송된 DM 메시지 반환. `DIRECT_MESSAGE` 알림 후보 생성.

> **삭제 불가 정책:** DM 메시지 삭제 API 미제공.
> **DM→이슈 전환 불가:** 요청 시 `DM_MESSAGE_CANNOT_CREATE_ISSUE` 반환.
> **Thread→DM 공유:** MVP 보류 (`THREAD_DM_SHARE_DEFERRED`).

---

### 15-5. DM 읽음 위치 갱신

```
POST /api/v1/direct-messages/rooms/{roomId}/mark-read
```

**권한:** 해당 DM 방 참여자

**Request Body**
```json
{ "lastReadMessageId": 10 }
```

> `DmRoomUserState.last_read_message_id` upsert. 상대 read receipt 아님.

**Response 200**
```json
{
  "data": { "unreadCount": 0, "lastReadAt": "2026-06-12T10:00:00" },
  "meta": { "requestId": "req_..." }
}
```

---

### 15-6. 전체 DM Unread count 조회

```
GET /api/v1/direct-messages/unread-count
```

**권한:** A/P/C

**Query Params**
- `companyId`: 회사 ID (optional)

**Response 200**
```json
{
  "data": {
    "totalUnreadCount": 3,
    "unreadCountByRoom": [
      { "roomId": 1, "unreadCount": 2 },
      { "roomId": 3, "unreadCount": 1 }
    ]
  },
  "meta": { "requestId": "req_..." }
}
```

---

## 16. 알림 (Notifications)

### 16-1. 알림 목록 조회

```
GET /api/v1/notifications
```

**권한:** A/P/C (본인 알림만)

**Query Params**
- `companyId`: 회사 ID (optional)
- `projectId`: 프로젝트 ID (optional)
- `type`: `notificationType` enum 값 (optional)
- `unreadOnly`: `true` | `false` (optional)
- `cursor`: 페이지네이션 커서

**Response 200**
```json
{
  "data": {
    "notifications": [
      {
        "recipientId": 1,
        "eventId": 100,
        "type": "ISSUE_ASSIGNED",
        "title": "이슈가 나에게 배정되었습니다",
        "body": "황스일님이 '로그인 API 구현'을 배정했습니다.",
        "target": { "type": "ISSUE", "id": 1, "projectId": 20 },
        "route": "/projects/20/issues/1",
        "readAt": null,
        "createdAt": "2026-06-12T10:00:00"
      }
    ]
  },
  "page": { "cursor": null, "hasNext": false, "size": 20 },
  "meta": { "requestId": "req_..." }
}
```

---

### 16-2. Unread count 조회

```
GET /api/v1/notifications/unread-count
```

**권한:** A/P/C

**Query Params**
- `companyId`: 회사 ID (optional)
- `projectId`: 프로젝트 ID (optional)

**Response 200**
```json
{
  "data": {
    "totalUnread": 5,
    "byType": { "ISSUE_ASSIGNED": 2, "THREAD_COMMENTED": 3 }
  },
  "meta": { "requestId": "req_..." }
}
```

---

### 16-3. 알림 읽음 처리

```
POST /api/v1/notifications/{recipientId}/read
```

**권한:** 해당 알림 수신자

**Response 200**
```json
{
  "data": { "readAt": "2026-06-12T10:05:00" },
  "meta": { "requestId": "req_..." }
}
```

---

### 16-4. 전체 알림 읽음 처리

```
POST /api/v1/notifications/read-all
```

**권한:** A/P/C

**Request Body** (optional)
```json
{
  "companyId": 10,
  "projectId": 20,
  "type": "ISSUE_ASSIGNED"
}
```

**Response 200**
```json
{
  "data": { "updatedCount": 5 },
  "meta": { "requestId": "req_..." }
}
```

---

### 16-5. 알림 설정 조회

```
GET /api/v1/notification-preferences
```

**권한:** A/P/C

**Query Params**
- `scopeType`: `GLOBAL` (MVP는 GLOBAL만 허용)
- `notificationType`: `notificationType` enum 값 (optional)

**Response 200** — 개인 알림 설정 목록 반환

---

### 16-6. 알림 설정 변경

```
PUT /api/v1/notification-preferences
```

**권한:** A/P/C

**이벤트별 on/off:**
```json
{
  "scopeType": "GLOBAL",
  "notificationType": "DIRECT_MESSAGE",
  "enabled": true
}
```

**전체 on/off:**
```json
{
  "scopeType": "GLOBAL",
  "allEnabled": false
}
```

> `scopeType=PROJECT` 요청 → `PROJECT_NOTIFICATION_SCOPE_DEFERRED` 반환.
> 외부 API에 `notificationType=ALL` 노출 금지. 전체 on/off는 GLOBAL row 일괄 update로 처리.

**Response 200** — `preference` 또는 `{ updatedCount }` 반환

---

## 17. API 엔드포인트 요약

| # | Method | Path | 설명 | 권한 |
|---|--------|------|------|------|
| 1 | POST | /api/v1/auth/signup | 회원가입 | 공개 |
| 2 | POST | /api/v1/auth/login | 로그인 | 공개 |
| 3 | POST | /api/v1/auth/logout | 로그아웃 | 인증 |
| 4 | GET | /api/v1/auth/me | 사용자 컨텍스트 조회 | A/P/C |
| 5 | POST | /api/v1/companies | 회사 생성 | 인증 |
| 6 | GET | /api/v1/companies/{id} | 회사 정보 조회 | A/P/C |
| 7 | PATCH | /api/v1/companies/{id} | 회사 정보 수정 | C |
| 8 | POST | /api/v1/companies/{id}/logo | 회사 로고 저장 | C |
| 9 | GET | /api/v1/companies/{id}/stats | 회사 관리 통계 | C |
| 10 | GET | /api/v1/companies/{id}/departments | 부서 목록 | C |
| 11 | POST | /api/v1/companies/{id}/departments | 부서 생성 | C |
| 12 | PATCH | /api/v1/departments/{did} | 부서 수정 | C |
| 13 | DELETE | /api/v1/departments/{did} | 부서 삭제 | C |
| 14 | GET | /api/v1/companies/{id}/company-members | 구성원 목록 | C |
| 15 | POST | /api/v1/companies/{id}/company-members | 구성원 추가 (즉시 등록) | C |
| 16 | PATCH | /api/v1/company-members/{cmid} | 구성원 역할/부서/상태 변경 | C |
| 17 | GET | /api/v1/companies/{id}/projects | 프로젝트 목록 | A/P/C |
| 18 | POST | /api/v1/companies/{id}/projects | 프로젝트 생성 | C |
| 19 | GET | /api/v1/projects/{id} | 프로젝트 상세 | A/P/C |
| 20 | PATCH | /api/v1/projects/{id} | 프로젝트 수정 | P/C |
| 21 | POST | /api/v1/projects/{id}/archive | 프로젝트 아카이빙 | P/C |
| 22 | GET | /api/v1/projects/{id}/project-members | 프로젝트 멤버 목록 | A/P/C |
| 23 | POST | /api/v1/projects/{id}/project-members | 프로젝트 멤버 추가 | P/C |
| 24 | PATCH | /api/v1/project-members/{pmid} | 프로젝트 멤버 역할/상태 변경 | P/C |
| 25 | DELETE | /api/v1/project-members/{pmid} | 프로젝트 멤버 제거 | P/C |
| 26 | GET | /api/v1/projects/{id}/sprints | 스프린트 목록 | A/P/C |
| 27 | POST | /api/v1/projects/{id}/sprints | 스프린트 생성 | P/C |
| 28 | GET | /api/v1/sprints/{sid} | 스프린트 상세 | A/P/C |
| 29 | PATCH | /api/v1/sprints/{sid} | 스프린트 수정 | P/C |
| 30 | POST | /api/v1/sprints/{sid}/start | 스프린트 시작 | P/C |
| 31 | POST | /api/v1/sprints/{sid}/complete | 스프린트 종료 | P/C |
| 32 | POST | /api/v1/sprints/{sid}/issues | 스프린트에 이슈 배정 | P/C |
| 33 | GET | /api/v1/projects/{id}/issues | 이슈 목록/칸반 조회 | A/P/C |
| 34 | POST | /api/v1/projects/{id}/issues | 이슈 생성 | A/P/C |
| 35 | POST | /api/v1/projects/{id}/issues/from-message | 메시지에서 이슈 생성 | A/P/C |
| 36 | GET | /api/v1/issues/{iid} | 이슈 상세 | A/P/C |
| 37 | PATCH | /api/v1/issues/{iid} | 이슈 수정 (필드별 권한) | A/P/C |
| 38 | PATCH | /api/v1/issues/{iid}/status | 이슈 상태 변경 (A: DONE 불가) | A/P/C |
| 39 | POST | /api/v1/issues/{iid}/trash | 이슈 휴지통 이동 | A/P/C |
| 40 | POST | /api/v1/issues/{iid}/restore | 이슈 복원 | A/P/C |
| 41 | GET | /api/v1/projects/{id}/documents | 문서 목록 | A/P/C |
| 42 | POST | /api/v1/projects/{id}/documents | 문서 생성 | A/P/C |
| 43 | GET | /api/v1/documents/{did} | 문서 상세 | A/P/C |
| 44 | PATCH | /api/v1/documents/{did} | 문서 수정 | A/P/C |
| 45 | POST | /api/v1/documents/{did}/trash | 문서 휴지통 이동 | A/P/C |
| 46 | POST | /api/v1/documents/{did}/restore | 문서 복원 | A/P/C |
| 47 | GET | /api/v1/projects/{id}/documents/graph | 그래프 데이터 | A/P/C |
| 48 | GET | /api/v1/projects/{id}/document-issue-links | 연결 목록 | A/P/C |
| 49 | POST | /api/v1/projects/{id}/document-issue-links | 연결 생성 | A/P/C |
| 50 | DELETE | /api/v1/document-issue-links/{lid} | 연결 해제 | A/P/C |
| 51 | GET | /api/v1/issues/{iid}/documents | 이슈 기준 연결 문서 | A/P/C |
| 52 | GET | /api/v1/documents/{did}/issues | 문서 기준 연결 이슈 | A/P/C |
| 53 | GET | /api/v1/projects/{id}/threads | 쓰레드 조회 | A/P/C |
| 54 | POST | /api/v1/projects/{id}/threads | 쓰레드 생성/get-or-create | A/P/C |
| 55 | GET | /api/v1/threads/{tid} | 쓰레드 상세 | A/P/C |
| 56 | PATCH | /api/v1/threads/{tid} | 쓰레드 topic 수정 | 작성자/P/C |
| 57 | POST | /api/v1/threads/{tid}/comments | 댓글 작성 | A/P/C |
| 58 | GET | /api/v1/projects/{id}/messages | 메시지 목록 | A/P/C |
| 59 | POST | /api/v1/projects/{id}/messages | 메시지 작성 | A/P/C |
| 60 | GET | /api/v1/messages/{mid} | 메시지 상세 | A/P/C |
| 61 | POST | /api/v1/projects/{id}/messages/mark-read | 채팅방 읽음 갱신 | A/P/C |
| 62 | GET | /api/v1/projects/{id}/messages/unread-count | Unread count | A/P/C |
| 63 | GET | /api/v1/companies/{id}/direct-messages/rooms | DM 방 목록 | A/P/C |
| 64 | POST | /api/v1/companies/{id}/direct-messages/rooms | DM 방 생성/조회 | A/P/C |
| 65 | GET | /api/v1/direct-messages/rooms/{rid}/messages | DM 메시지 목록 | 참여자 |
| 66 | POST | /api/v1/direct-messages/rooms/{rid}/messages | DM 메시지 전송 | 참여자 |
| 67 | POST | /api/v1/direct-messages/rooms/{rid}/mark-read | DM 읽음 갱신 | 참여자 |
| 68 | GET | /api/v1/direct-messages/unread-count | 전체 DM unread | A/P/C |
| 69 | GET | /api/v1/notifications | 알림 목록 | A/P/C |
| 70 | GET | /api/v1/notifications/unread-count | Unread count | A/P/C |
| 71 | POST | /api/v1/notifications/{rid}/read | 알림 읽음 처리 | 수신자 |
| 72 | POST | /api/v1/notifications/read-all | 전체 읽음 처리 | A/P/C |
| 73 | GET | /api/v1/notification-preferences | 알림 설정 조회 | A/P/C |
| 74 | PUT | /api/v1/notification-preferences | 알림 설정 변경 | A/P/C |

---

## MVP 보류/Stub 목록

| 구분 | API/기능 | 처리 | 이유 |
|------|----------|------|------|
| 보류 | 다자간 DM 방 | 미제공 | 1:1 DM만 MVP |
| 보류 | DM 메시지 삭제 | 미제공 | 전송/조회/unread 중심 |
| 보류 | DM 상대 read receipt | 미제공 | unread count와 별개 |
| 보류 | DM 메시지→이슈 전환 | `DM_MESSAGE_CANNOT_CREATE_ISSUE` | DM은 사적 소통 |
| 보류 | Thread→DM 공유 | `THREAD_DM_SHARE_DEFERRED` | sourceThreadCommentId/정책 미정 |
| 보류 | Thread→Issue 추가 연결 | `THREAD_ISSUE_LINK_DEFERRED` | 데이터 모델 미정 |
| 보류 | 이슈/문서 영구 삭제 | `PERMANENT_DELETE_DEFERRED` | 참조 보존 정책 필요 |
| 보류 | 프로젝트 복원 | `PROJECT_RESTORE_DEFERRED` | 아카이브 복원 UI 미정 |
| 보류 | 프로젝트별 알림 설정 | `PROJECT_NOTIFICATION_SCOPE_DEFERRED` | GLOBAL only MVP |
| 선택 | 범용 파일 첨부 | `/api/v1/attachments` MVP 이후 | 범용 S3 첨부는 과대 |
| 선택 | 통합 검색 | `/api/v1/search` stub 가능 | 핵심 흐름 외 |
| 선택 | 활동 피드 | `/api/v1/activity-events` stub 가능 | 내부 원장 선택 |

---

*검토 필요 사항:*
- [ ] JWT 만료/갱신 방식 결정 (refresh token 사용 여부)
- [ ] `/api/v1/projects/{id}/issues` 페이지네이션 방식 (offset vs. cursor)
- [ ] 이슈 상태 변경 시 알림 자동 발송 로직 서비스 레이어 확인
- [ ] `from-message` 엔드포인트에서 thread 자동 생성 여부 결정
- [ ] 메시지 목록 실시간 업데이트 방식 (polling vs. SSE — MVP에서는 polling 권장)
- [ ] DM 방 unread count 처리 방식 — `DmRoomUserState` 집계 vs 서비스 레이어 계산
- [ ] `activity_events`와 `notification_events` 통합 여부
