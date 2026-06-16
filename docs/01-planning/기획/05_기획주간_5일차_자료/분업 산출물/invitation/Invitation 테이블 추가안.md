> **작성일**: 2026-06-16
> **최종 수정**: 2026-06-16
> **상태**: 제안 (도현 설계서 병합 + §9 절충안 확정, v4)
> **기준 문서**: ERD v2.0.md, API 명세서 v2.3.md, invitation_언급_정리.md, invitation-추가-영향도.md(여도현), 앱-내부-invitation-알림-연동-설계_도현.md(여도현), 사용자 시나리오(회사관리자/프로젝트관리자 초대 흐름)

# Invitation 테이블 추가안

## 1. 배경

`ERD v2.0.md`(3~4일차 확정)에서는 Invitation 테이블을 제거하고, 회사 구성원 추가를 즉시 등록 정책(가입자 즉시 등록 / 미가입 400 / 이미 소속 409)으로 단순화했다. 이후 발표자료 S14 화면에서 "초대 상태 표시" 요소가 빠진다는 의견으로 재도입이 검토됐다.

이 문서는 그 분석과, 이후 합의된 사용자 시나리오(회사관리자가 가입된 사용자를 초대하고, 대상자가 플랫폼 알림에서 수락/거절하는 흐름 — 프로젝트 관리자/구성원도 동일 구조), 그리고 여도현 작성 `앱-내부-invitation-알림-연동-설계_도현.md`의 알림 연동 설계를 병합해 **메일·토큰 없는 앱 내부 Invitation**의 추가안으로 정리한다.

## 2. 범위

| 포함 | 제외 |
|---|---|
| 가입된 사용자 대상 앱 내부 초대/수락/거절/취소 | 미가입 이메일 대상 초대, 메일 발송, 토큰, 미가입 onboarding |
| `PENDING / ACCEPTED / DECLINED / CANCELED / EXPIRED` 5단계 상태 | — |
| 회사 구성원 초대 + 프로젝트 멤버 초대 (동일 구조 공유) | — |
| 만료(EXPIRED) 처리 | — |

기존 즉시 등록 정책(가입자 즉시 `company_members`/`project_members` 생성)은 **완전히 대체**한다. 모든 신규 구성원 추가는 Invitation 생성 → 수락 경로를 거친다.

## 3. 핵심 설계 원칙 (도현 설계서 §3 채택)

### 3.1 Invitation과 Notification의 역할 분리

`notification_events`만으로 초대 상태를 표현하지 않는다. 초대 상태의 source of truth는 `invitations`이고, 알림 테이블은 전달/읽음 계층으로만 쓴다.

| 데이터 | source of truth |
|---|---|
| 초대 상태 (`PENDING/ACCEPTED/DECLINED/CANCELED/EXPIRED`) | `invitations` |
| 초대 수락 시 생성할 멤버십 정보 | `invitations` |
| 알림 제목/본문/이동 경로 | `notification_events` |
| 사용자가 알림을 읽었는지 여부 | `notification_recipients.read_at` |

`notification_recipients.read_at`은 알림 읽음 여부이지 초대 수락 여부가 아니다. 알림을 읽어도 초대는 여전히 `PENDING`일 수 있다.

### 3.2 Notification은 delivery/read layer로만 사용

초대 생성 시 `invitations` row를 만들고, 같은 transaction에서 `notification_events`/`notification_recipients`를 생성한다.

```text
초대 생성
→ invitations row 생성(PENDING)
→ notification_events row 생성(INVITATION_CREATED)
→ notification_recipients row 생성(invitee)
→ 초대 대상자는 S11 알림에서 초대 확인
→ 초대 상세 화면에서 수락/거절
```

## 4. 테이블 초안

```text
invitations
- id               BIGINT PK
- scope_type       ENUM(COMPANY, PROJECT), NOT NULL   -- 회사/프로젝트 초대 구분
- company_id       BIGINT FK -> companies.id, NOT NULL -- 모든 초대는 회사 범위 안에서 발생
- project_id       BIGINT FK -> projects.id, NULL      -- 프로젝트 초대일 때만 값
- inviter_user_id  BIGINT FK -> users.id, NOT NULL     -- 초대한 사용자
- invitee_user_id  BIGINT FK -> users.id, NOT NULL     -- 초대받은 가입 사용자
- role             ENUM, NOT NULL                       -- 수락 후 부여할 역할
- status           ENUM(PENDING, ACCEPTED, DECLINED, CANCELED, EXPIRED), NOT NULL, DEFAULT PENDING
- expires_at       DATETIME, NULL                       -- 만료 시각(Sprint 1은 nullable 허용)
- responded_at     DATETIME, NULL                       -- 수락/거절/취소 시각
- created_at       DATETIME, NOT NULL
- updated_at       DATETIME, NOT NULL
```

- 토큰/이메일 컬럼 없음 — `invitee_user_id`로 가입자를 직접 참조한다. (초대 대상 지정 방식은 §9 후순위 항목 참고)

### 4.1 제약 조건

| 제약 | 기준 |
|---|---|
| 회사 초대 중복 방지 | 같은 `company_id`, `invitee_user_id`에 `PENDING` 초대는 하나만 |
| 프로젝트 초대 중복 방지 | 같은 `project_id`, `invitee_user_id`에 `PENDING` 초대는 하나만 |
| 프로젝트 초대 대상 | 이미 같은 회사의 active `company_members`인 사용자만 허용 (도현 §4.3 채택) |
| 수락 중복 방지 | 수락 API는 transaction + row lock으로 처리 |

### 4.2 상태 전이

`PENDING → ACCEPTED`(대상자) / `PENDING → DECLINED`(대상자) / `PENDING → CANCELED`(초대자·관리자) / `PENDING → EXPIRED`(만료). `ACCEPTED/DECLINED/CANCELED/EXPIRED`는 terminal 상태.

> **구현 메모 (ERD/API 명세에는 노출하지 않음)**: 거절(`DECLINED`)·취소(`CANCELED`)는 같은 "PENDING → terminal" 전이 로직을 공유하고 액터 권한만 다르다 — 거절은 `invitee_user_id` 본인만, 취소는 `inviter_user_id` 또는 관리자만. 동시성(취소와 수락 동시 진입)은 상태 변경 시 row lock + `WHERE status='PENDING'` 단일 UPDATE로 한 번만 성공하도록 처리한다.

## 5. 알림 연동 (도현 설계서 §5 채택)

### 5.1 초대 생성 알림

```text
notification_events
- type: INVITATION_CREATED
- company_id: invitation.company_id
- project_id: invitation.project_id (nullable)
- actor_user_id: inviter_user_id
- target_type: INVITATION
- target_id: invitation.id
- title: "초대가 도착했습니다"
- body: "OO님이 OO 회사/프로젝트에 초대했습니다"
- route: "/invitations/{invitationId}"
- payload_json: { "invitationId": 1, "scopeType": "COMPANY" }
```

`notification_recipients`에는 초대받은 사용자 1명만 넣는다(`read_at: null`).

### 5.2 수락/거절 후 알림

기존 알림 recipient를 수정하지 않고 새 이벤트를 만든다.

| 이벤트 | 수신자 | 목적 |
|---|---|---|
| `INVITATION_ACCEPTED` | 초대한 사용자 | 초대가 수락됐음을 알림 |
| `INVITATION_DECLINED` | 초대한 사용자 | 초대가 거절됐음을 알림 |
| `MEMBER_ADDED` | 초대 대상자 또는 관련 관리자 | 실제 멤버십 생성 결과 알림 |

> 위 4개 이벤트(`INVITATION_CREATED/ACCEPTED/DECLINED`, `MEMBER_ADDED`)는 `notification_events.event_type` ENUM에 값만 추가한다. **테이블 구조 변경 없음.** `notification_preferences`에 별도 카테고리로 노출할지는 Sprint 1 GLOBAL scope 제약과 함께 추가 논의.

## 6. API 초안

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/v1/users/lookup?email=` | C | 회사 초대 대상 이메일 완전 일치 조회 (§9 절충안) |
| POST | `/api/v1/companies/{companyId}/invitations` | C | 회사 구성원 초대 생성 |
| POST | `/api/v1/projects/{projectId}/invitations` | P/C | 프로젝트 멤버 초대 생성 |
| GET | `/api/v1/companies/{companyId}/invitations?status=` | C | 회사 초대 목록(관리자) |
| GET | `/api/v1/projects/{projectId}/invitations?status=` | P/C | 프로젝트 초대 목록(관리자) |
| GET | `/api/v1/me/invitations?status=` | A/P/C | 내가 받은 초대 목록 |
| POST | `/api/v1/invitations/{invitationId}/accept` | 대상자 본인 | 수락 → 멤버십 생성, `ACCEPTED` |
| POST | `/api/v1/invitations/{invitationId}/decline` | 대상자 본인 | 거절 → `DECLINED` |
| POST | `/api/v1/invitations/{invitationId}/cancel` | 초대자/관리자 | 취소 → `CANCELED` |

초대 대상 지정 방식은 §9에서 절충안(C)으로 확정했다. 회사 초대는 `GET /api/v1/users/lookup?email=`로 완전 일치 조회 후 확인 카드를 보여주고, 그 결과로 받은 `inviteeUserId`를 초대 생성 request body에 담아 보낸다. 프로젝트 초대는 기존 회사 구성원 목록에서 선택하므로 별도 조회 API가 필요 없다.

```json
{ "inviteeUserId": 12, "role": "MEMBER" }
```

**`GET /api/v1/users/lookup?email=`** (회사 초대 전용)

```json
// 일치하는 사용자 있음 — 200
{
  "data": {
    "user": { "id": 12, "email": "member@threadia.com", "nickname": "팀원", "avatarUrl": null }
  },
  "meta": { "requestId": "req_..." }
}

// 일치하는 사용자 없음 — 404 NOT_FOUND
{
  "error": { "code": "NOT_FOUND", "message": "가입하지 않은 사용자입니다.", "details": {} },
  "meta": { "requestId": "req_..." }
}
```

- 부분 문자열 검색이 아니라 이메일 완전 일치만 허용한다(자동완성 없음).
- 미가입 사용자는 404로 명시적으로 알린다 — Threadia는 미가입자 대상 초대/메일 발송 흐름이 없어 숨길 실익이 없고, 관리자가 오타인지 미가입인지 구분할 수 있어야 한다.

### 6.1 수락 처리 단계 (도현 §6.4 채택)

1. invitation row를 lock한다.
2. 현재 사용자가 `invitee_user_id`와 일치하는지 확인.
3. `status = PENDING`인지 확인.
4. 만료 여부 확인.
5. 회사 초대면 `company_members` 생성.
6. 프로젝트 초대면 active `company_members` 확인 후 `project_members` 생성.
7. invitation 상태를 `ACCEPTED`로 변경.
8. 초대자에게 `INVITATION_ACCEPTED` 알림 생성.

### 6.2 예외 케이스 (도현 §10 채택)

| 케이스 | 처리 |
|---|---|
| 초대 대상자가 미가입 | `users/lookup`에서 `404 NOT_FOUND`(§9 확정) |
| 이미 회사 구성원 | 409 `ALREADY_COMPANY_MEMBER` |
| 이미 프로젝트 멤버 | 409 `ALREADY_PROJECT_MEMBER` |
| 이미 pending 초대 존재 | 409 `INVITATION_ALREADY_PENDING` |
| 대상자 아닌 사용자가 수락/거절 | 403 `FORBIDDEN` |
| 만료된 초대 수락 | 409 `INVITATION_EXPIRED` |
| 취소된 초대 수락 | 409 `INVITATION_CANCELED` |
| 동시에 두 번 수락 | transaction + row lock으로 한 번만 성공 |

### 6.3 API 명세서 v2.3 반영 시 변경 사항

> 아래는 팀 합의 후 `API 명세서 v2.3.md`에 실제로 반영할 변경 내역이다. (현재 API 명세서 원본은 건드리지 않았다. 합의 전까지 변경 내용은 이 문서에만 둔다.)

**(1) §2 Enum 추가**

```text
invitationStatus:    PENDING | ACCEPTED | DECLINED | CANCELED | EXPIRED
invitationScopeType: COMPANY | PROJECT
```

**(2) §5 회사/부서/구성원 — 엔드포인트 교체**

| 기존 (제거) | 대체 |
|---|---|
| `POST /api/v1/companies/{companyId}/company-members` (가입자 즉시 등록) | 초대 경로(§6 표)로 대체. 멤버 생성은 invitation accept 시점에 서버 내부에서만 수행 |

"가입되지 않은 이메일은 초대 테이블을 만들지 않고 400 반환한다" 문구는 정책이 반대로 바뀌어 삭제/수정. 새 정책: 미가입 사용자는 초대 대상이 될 수 없음(검색 결과 제외 또는 400 — §9 후순위).

**(3) §6 프로젝트/스프린트 — 엔드포인트 교체**

| 기존 (제거) | 대체 |
|---|---|
| `POST /api/v1/projects/{projectId}/project-members` (즉시 추가) | 초대 경로(§6 표)로 대체. accept 시 `project_members` 생성 |

**(4) §9 알림 — notificationType(event_type ENUM)에 값 추가**

`INVITATION_CREATED`, `INVITATION_ACCEPTED`, `INVITATION_DECLINED`, `MEMBER_ADDED` (테이블 구조 변경 없음, §5 참고)

**(5) 요청/응답 JSON 예시**

회사 초대 생성 — `POST /api/v1/companies/{companyId}/invitations`:

```json
// 요청
{ "inviteeUserId": 2, "role": "MEMBER" }

// 응답 201
{
  "data": {
    "invitation": {
      "id": 500,
      "scopeType": "COMPANY",
      "companyId": 10,
      "projectId": null,
      "role": "MEMBER",
      "status": "PENDING",
      "inviteeUser": { "id": 2, "email": "member@threadia.com", "nickname": "팀원" },
      "expiresAt": null,
      "createdAt": "2026-06-16T10:00:00"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

수락 — `POST /api/v1/invitations/{invitationId}/accept` (이 시점에 `company_members` 생성):

```json
{
  "data": {
    "invitation": { "id": 500, "status": "ACCEPTED", "respondedAt": "2026-06-16T11:00:00" },
    "companyMember": {
      "id": 101, "companyId": 10, "departmentId": null,
      "role": "MEMBER", "status": "ACTIVE",
      "user": { "id": 2, "email": "member@threadia.com", "nickname": "팀원", "avatarUrl": null }
    }
  },
  "meta": { "requestId": "req_..." }
}
```

거절 — `POST .../decline` / 취소 — `POST .../cancel`:

```json
// decline 응답 200
{ "data": { "invitation": { "id": 500, "status": "DECLINED", "respondedAt": "2026-06-16T11:00:00" } }, "meta": { "requestId": "req_..." } }
// cancel 응답 200
{ "data": { "invitation": { "id": 500, "status": "CANCELED", "respondedAt": "2026-06-16T11:00:00" } }, "meta": { "requestId": "req_..." } }
```

프로젝트 초대 생성 — `POST /api/v1/projects/{projectId}/invitations` (`scopeType=PROJECT`, accept 시 `project_members` 생성, 대상은 같은 회사 active company_members만):

```json
// 요청
{ "inviteeUserId": 2, "role": "MEMBER" }

// 응답 201
{
  "data": {
    "invitation": {
      "id": 600,
      "scopeType": "PROJECT",
      "companyId": 10,
      "projectId": 20,
      "role": "MEMBER",
      "status": "PENDING",
      "inviteeUser": { "id": 2, "email": "member@threadia.com", "nickname": "팀원" },
      "expiresAt": null,
      "createdAt": "2026-06-16T10:00:00"
    }
  },
  "meta": { "requestId": "req_..." }
}
```

## 7. 검토한 대안 (도현 설계서 §9 채택)

별도 `invitations` 테이블 없이 `company_members`/`project_members`에 `PENDING` 상태를 추가하는 방식도 가능하나, Threadia는 권한 판정이 `company_members`/`project_members` 기준이라 pending row가 실제 멤버십과 섞이는 문제가 생긴다.

| 항목 | pending membership 방식의 문제 |
|---|---|
| 권한 판정 | 권한 query에서 `status=ACTIVE` 조건 누락 시 pending 사용자가 권한 획득 |
| 멤버 목록 | 실제 구성원과 초대 대기자를 계속 구분해야 함 |
| 취소/거절 이력 | membership row가 거절/취소 이력까지 떠안음 |
| 프로젝트 초대 | 회사 pending과 프로젝트 pending이 얽혀 상태 전이 복잡 |
| 감사 추적 | 누가 언제 어떤 역할로 초대했는지 분리 어려움 |

→ **결론: `invitations`를 별도 source of truth로 둔다.** 테이블 하나 늘지만 초대 상태와 실제 권한을 분리해 RBAC 오류 가능성을 낮춘다.

```text
invitations              = 초대 요청과 상태
company_members/project_members = 수락 이후 실제 권한
notification_events      = 초대 사실 전달
notification_recipients  = 누가 알림을 받고 읽었는지
```

## 8. 다른 테이블 영향 (구조 변경 없음)

| 테이블 | 영향 | 구조 변경 |
|---|---|---|
| `company_members` | INSERT 시점만 "즉시" → "수락 시"로 이동 | 없음 |
| `project_members` | 동일 | 없음 |
| `notification_events` | `event_type` ENUM에 invitation 이벤트 4종 추가 | 없음(값 추가) |
| `notification_preferences` | 새 카테고리 노출 여부 검토 | 없음(미정) |

신규 테이블은 `invitations` 하나뿐이며, 기존 테이블을 단방향 FK로 참조만 한다(기존 테이블에 FK 컬럼 추가 없음).

## 9. 초대 대상 지정 방식 (확정 — 절충안 C)

A(이메일 입력 → 서버 내부 변환, 조회 API 없음)와 B(자유 검색 UI → `inviteeUserId`)를 비교한 결과, 둘 다 단독으로는 채택하지 않고 **절충안(C)**으로 확정한다.

| | A | B | C(채택) |
|---|---|---|---|
| 대상 확인 UX | 보낸 뒤에야 실패 확인 | 검색 중 닉네임/아바타로 확인 | 조회 결과로 확인 카드 표시 |
| 프론트 구현 | 단순(텍스트 입력) | 복잡(디바운스 검색+드롭다운) | 단순(입력+조회 버튼+결과 카드) |
| Enumeration 리스크 | 거의 없음 | 부분 문자열 자동완성 시 brute-force 가능 | A와 동등 — 완전 일치만 허용, 자동완성 없음 |

**범위별 처리**

- **프로젝트 초대**: 대상이 이미 같은 회사 active `company_members`이므로, 기존 회사 구성원 목록에서 선택하는 B 방식을 그대로 쓴다. 별도 검색 API 불필요, enumeration 리스크 없음(관리자가 이미 볼 수 있는 목록이라서).
- **회사 초대**: 대상이 아직 회사에 없는 신규 가입자라 범위를 좁힐 수 없다. `GET /api/v1/users/lookup?email=` 완전 일치 조회로 1명만 반환하고, 확인 카드를 보여준 뒤 `inviteeUserId`로 초대를 생성한다(§6 참고).

**미가입 사용자 처리(확정)**

조회 결과 없음 → `404 NOT_FOUND` + "가입하지 않은 사용자입니다." 명시. 검색 결과에서 조용히 제외하지 않는다. Threadia는 미가입자 대상 초대/메일 발송 흐름이 없어 이메일 존재 여부를 숨길 실익이 없고, 관리자가 오타와 미가입을 구분할 수 있어야 한다는 점을 우선한다.

## 10. 확정된 결정 사항

1. 즉시 등록 정책 완전 대체.
2. 회사/프로젝트 초대는 `invitations` 테이블/플로우 공유 (`scope_type`, `project_id` nullable).
3. status 5단계: `PENDING/ACCEPTED/DECLINED/CANCELED/EXPIRED` (만료 포함).
4. 거절 명칭은 `DECLINED`/`decline` (초대 도메인 실무 표준 + 도현 설계서 일치).
5. scope 구분 컬럼명 `scope_type`.
6. 알림은 도현 §3/§5 설계(역할 분리 + 4개 이벤트) 채택.
7. 프로젝트 초대 대상은 active `company_members`로 제한.
8. 설계 대안(pending membership)은 RBAC 오염 위험으로 기각.
9. 초대 대상 지정 방식은 절충안(C) 확정 — 프로젝트 초대는 기존 회사 구성원 목록에서 선택(B), 회사 초대는 이메일 완전 일치 조회(`GET /api/v1/users/lookup?email=`) 후 확인 카드 표시.
10. 미가입 사용자는 조회 시 `404 NOT_FOUND`로 명시한다(검색 결과에서 조용히 제외하지 않음).

## 11. 다음 단계

이 제안서에 대한 팀 합의 후, `ERD v2.0.md`와 `API 명세서 v2.3.md`에 반영하는 작업은 별도 승인을 받아 진행한다(두 문서 모두 상태가 "확정"이며, `docs/CLAUDE.md` 규칙상 01-planning 자료는 사용자 확인 없이 수정하지 않는다).
