> **작성자**: 정철진
> **작성일**: 2026-07-07
> **최종 수정**: 2026-07-07
> **버전**: v1.0
> **상태**: 작성 중 — 구현 착수 전 기준 문서
> **태그**: frontend, invitation, s14, sprint-1.5, api-contract, rbac, nextjs

# S14 Invitation 프론트 연동 설계 (WBS 2.5)

이 문서는 WBS 2.5 "S14 구성원·권한 관리 화면 invitation 연결"의 프론트엔드 구현
설계를 정한다. Invitation 흐름이 발신(회사 관리자)과 수신(초대 대상자)으로 나뉘고
상태 전이·에러 코드가 많아, 구현 전에 화면 분리, API 계약, 상태 매핑, 범위 경계를
문서로 고정한다.

기준 백엔드는 `dev` 기준 PR #47(회사 초대 생성/목록), #48(응답 흐름 accept/
decline/cancel + 표시 필드), #50(Company API)이 머지된 상태다.

---

## 1. 화면과 라우팅

Invitation은 발신측과 수신측의 접근 주체·회사 컨텍스트가 다르므로 화면을 둘로 나눈다.
이 구조는 `docs/05-architecture/프론트엔드-구조.md`에 최초 설계 시점부터 잡혀 있던 것을
그대로 구현한다.

| 화면 | 경로 | 접근 | 회사 컨텍스트 | 역할 |
|---|---|---|---|---|
| S14 수신 | `/invitations` | 로그인 사용자 전원 (A/P/C) | 없음 (company-agnostic) | 내가 받은 초대 목록 + 수락/거절 |
| S14 발신·관리 | `/{companyId}/members` | C (COMPANY_ADMIN) | `{companyId}` 하위 | 이메일 조회·초대 생성·보낸 초대 목록·취소 + (구성원 목록) |

### 수신측을 회사 컨텍스트 밖에 두는 이유

초대를 "받았지만 아직 수락 안 한" 사용자는 그 회사의 membership이 아직 없다. 따라서
`/{companyId}/...` 하위 라우트로는 진입시킬 수 없다(`/auth/me`에 해당 companyId
membership이 없고, 네비게이션도 그 회사로 보낼 수 없다). 아무 회사에도 소속되지 않은
신규 사용자가 첫 초대를 받는 경우까지 포함해야 하므로, 수신 목록은 로그인 직후 접근
가능한 company-agnostic 경로 `/invitations`에 둔다.

### INVITE-03 접근 주체 정리

INVITE-03(초대 수락/거절)의 대상은 A/P/C 전원이다. 화면정의서 §S14는 접근 사용자를
`C`로만 표기하지만, 이는 발신·관리 화면(`/{companyId}/members`) 기준이다. 수신 화면
`/invitations`는 전원 접근으로 분리해 이 충돌을 해소한다.

---

## 2. API 계약 (dev 머지 기준 확정)

### 2.1 엔드포인트와 담당 WBS

| 기능 | 엔드포인트 | WBS | 백엔드 상태 |
|---|---|---|---|
| 등록 사용자 이메일 조회 | `GET /api/v1/users/lookup?email=` | 2.2 | 머지 완료 |
| 회사 초대 생성 | `POST /api/v1/companies/{companyId}/invitations` | 2.3 | 머지 완료 |
| 보낸 초대 목록 | `GET /api/v1/companies/{companyId}/invitations` | 2.3 | 머지 완료 |
| 받은 초대 목록 | `GET /api/v1/users/me/invitations` | 2.4 | 머지 완료 |
| 초대 수락 | `POST /api/v1/invitations/{invitationId}/accept` | 2.4 | 머지 완료 |
| 초대 거절 | `POST /api/v1/invitations/{invitationId}/decline` | 2.4 | 머지 완료 |
| 초대 취소 | `POST /api/v1/invitations/{invitationId}/cancel` | 2.4 | 머지 완료 |
| **구성원 목록 조회** | `GET /api/v1/companies/{companyId}/company-members` | **3.1.4** | **미착수 (Sprint 2)** |
| **구성원 역할/부서 변경** | `PATCH /api/v1/company-members/{companyMemberId}` | **3.1.4** | **미착수 (Sprint 2)** |

구성원 목록 조회와 역할/부서 변경은 2.5 범위가 아니라 Sprint 2(3.1.4 백엔드,
3.2.3 프론트)다. 따라서 `/{companyId}/members`의 구성원 목록·역할/부서 select는
이번 작업에서 실 API로 붙이지 않고 mock 상태를 유지한다.

### 2.2 응답 DTO — `InvitationResponse`

```
id             : Long     초대 식별자 (action 처리용)
scopeType      : String   "COMPANY" | "PROJECT" (Sprint 1.5는 COMPANY만)
companyId      : Long     회사 식별자 (action/권한 처리용)
companyName    : String   회사명 (표시용)
projectId      : Long     프로젝트 초대 시에만, COMPANY 초대는 null
inviterUserId  : Long     초대자 식별자
inviterNickname: String   초대자 닉네임 (표시용)
inviteeUserId  : Long     초대 대상 식별자
inviteeEmail   : String   초대 대상 이메일 (표시용)
inviteeNickname: String   초대 대상 닉네임 (표시용)
role           : String   "COMPANY_ADMIN" | "MEMBER"
status         : String   상태 (아래 §3)
expiresAt      : String   ISO-8601 UTC, 기본 생성 +7일
```

`companyName`, `inviterNickname`, `inviteeEmail`, `inviteeNickname`은 화면 표시용,
`id`/`companyId`/`projectId`/`inviterUserId`/`inviteeUserId`는 action·권한 처리용이다.
(이 표시용 필드는 PR #48 리뷰 요청으로 추가됐다.)

### 2.3 목록 응답 래퍼

받은 초대 목록과 보낸 초대 목록 모두 **페이지네이션 없는 scoped wrapper**를 쓴다.
초기에 문서상 `me/invitations`가 `{data:[...], page:{...}}`로 잘못 적혀 있었으나
실제 코드(`CompanyInvitationsData`) 기준으로 정합화됐다.

```json
{
  "data": { "invitations": [ /* InvitationResponse[] */ ] },
  "meta": { "requestId": "req_..." }
}
```

초대 생성 응답은 단건 래퍼다.

```json
{ "data": { "invitation": { /* InvitationResponse */ } }, "meta": { "requestId": "..." } }
```

### 2.4 action 응답 — `InvitationActionData`

accept / decline / cancel 공통.

```json
{ "data": { "invitationId": 501, "status": "ACCEPTED", "membershipCreated": true }, "meta": {...} }
```

`membershipCreated`는 accept에서만 의미가 있다. 신규 membership 생성이면 `true`,
기존 `LEFT` membership 재활성화면 `false`다.

---

## 3. Invitation 상태와 전이

| 상태 | 표시 | 수신 화면 동작 | 발신 화면 동작 |
|---|---|---|---|
| PENDING | 대기 중 | [수락]/[거절] 노출 | [취소] 노출 |
| ACCEPTED | 수락됨 | action 없음 | action 없음 |
| DECLINED | 거절됨 | action 없음 | action 없음 |
| CANCELED | 취소됨 | action 없음 | action 없음 |
| EXPIRED | 만료됨 | action 없음 | action 없음 |

- 만료는 lazy expiration이다. 백엔드가 목록 조회(`list`/`listMine`)와 action 시점에
  `expirePendingIfNeeded`로 만료된 PENDING을 EXPIRED로 전이한다. 따라서 프론트는
  응답 `status`를 신뢰하면 된다.
- 다만 목록 조회 후 화면에 오래 머무는 동안 만료될 수 있으므로, 수신 화면의 수락
  버튼은 방어적으로 `expiresAt < now`일 때 비활성화한다(백엔드도 `INVITATION_EXPIRED`로
  이중 방어).
- accept 시 membership 생성은 accept 시점에만 일어난다. `SUSPENDED` membership은
  accept로 복구되지 않고 `COMPANY_MEMBER_SUSPENDED`로 거절, `LEFT`는 재활성화된다.

---

## 4. 에러 코드 매핑 (전부 HTTP 409)

| code | 발생 지점 | 사용자 메시지(안) |
|---|---|---|
| `ALREADY_COMPANY_MEMBER` | 초대 생성 / accept | 이미 회사에 소속된 사용자입니다. |
| `INVITATION_ALREADY_PENDING` | 초대 생성 | 이미 대기 중인 초대가 있습니다. |
| `COMPANY_MEMBER_SUSPENDED` | accept | 정지된 구성원은 초대 수락으로 복구할 수 없습니다. |
| `INVITATION_EXPIRED` | accept / decline / cancel | 만료된 초대입니다. |
| `INVITATION_CANCELED` | accept / decline | 이미 취소된 초대입니다. |
| `INVITATION_INVALID_STATUS` | accept / decline / cancel | 처리할 수 없는 초대 상태입니다. |

lookup 실패(미등록 이메일)는 404 계열로, "등록된 사용자를 찾을 수 없습니다"로 안내하고
Sprint 1.5 범위상 외부 이메일 초대는 미지원임을 함께 표시한다.

---

## 5. 코드 구조

### 5.1 신규 파일

- `frontend/services/invitation.service.ts` — `auth.service.ts`와 동일 패턴.
  `apiClient`(Bearer 자동 첨부)를 그대로 쓴다.

```ts
export const invitationService = {
  listMine:    () => apiClient.get("/api/v1/users/me/invitations"),
  accept:  (id: number) => apiClient.post(`/api/v1/invitations/${id}/accept`),
  decline: (id: number) => apiClient.post(`/api/v1/invitations/${id}/decline`),
  cancel:  (id: number) => apiClient.post(`/api/v1/invitations/${id}/cancel`),
  listCompany: (companyId: string) =>
    apiClient.get(`/api/v1/companies/${companyId}/invitations`),
  create: (companyId: string, body: { inviteeUserId: number; role: "COMPANY_ADMIN" | "MEMBER" }) =>
    apiClient.post(`/api/v1/companies/${companyId}/invitations`, body),
  lookup: (email: string) =>
    apiClient.get("/api/v1/users/lookup", { params: { email } }),
};
```

- `frontend/types/invitation.ts` — `InvitationResponse`를 백엔드 record와 1:1 매핑.
- `frontend/app/(app)/invitations/page.tsx` — 현재 빈 스켈레톤을 실제 수신 화면으로 구현.

### 5.2 공용 추출

현재 `members/page.tsx`에 있는 `STATUS_LABEL` / `STATUS_CLASS`(PENDING~EXPIRED)를
공용 모듈로 분리해 수신·발신 두 화면이 공유한다.

### 5.3 수정 파일

- `frontend/app/(app)/[companyId]/members/page.tsx` — 초대 관련(lookup/create/
  listCompany/cancel)을 mock에서 실 API로 교체. 보낸 초대는 `inviteeEmail`/
  `inviteeNickname`으로 표시. 구성원 목록·역할/부서 변경은 mock 유지(§2.1).
  COMPANY_ADMIN만 발신 UI를 노출하도록 role 분기.

---

## 6. 구현 범위 경계 (명시)

### 이번(2.5) 포함
- `/invitations` 수신 화면 신규 구현: 받은 초대 조회 + accept/decline
- `/{companyId}/members` 발신 흐름 실 연동: lookup → create, 보낸 초대 목록, cancel
- role 기반 발신 UI 노출 분기
- 상태 표시(5종)와 409 에러 매핑

### 이번 제외 (사유)
- 구성원 목록 조회·역할/부서 변경 → Sprint 2 (백엔드 3.1.4 미착수)
- 프로젝트 초대(`/projects/{id}/invitations`) → Sprint 2
- 실제 이메일 발송, 초대 token link, 미가입 사용자 초대, resend → Sprint 1.5 범위 밖
- 구성원 목록에서 DM 시작(CHAT-02 연계)의 실 연동 → 채팅 foundation 이후

---

## 7. 검증 계획 (Sprint 1 Frontend DoD 7.1~7.3 기준)

- 실행: 로컬 백엔드 + seed 회사/COMPANY_ADMIN 기준으로 발신→수신→수락까지 브라우저 확인
- 정상 케이스: 초대 생성 → 대상자 `/invitations`에서 조회 → 수락 시 membership 생성
- 실패 케이스: 이미 소속(`ALREADY_COMPANY_MEMBER`), 대기중 중복(`INVITATION_ALREADY_PENDING`),
  만료(`INVITATION_EXPIRED`) 각각 화면 메시지 확인
- 보안: access token은 memory 보관(기존 axios 패턴), 초대 응답에 secret 없음 확인
- 후속조치: 구성원 목록/역할변경 mock 유지 사유를 화면 주석과 이 문서에 기록
