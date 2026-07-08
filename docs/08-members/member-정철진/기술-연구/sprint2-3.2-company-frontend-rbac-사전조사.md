> **작성자**: 정철진
> **작성일**: 2026-07-08
> **최종 수정**: 2026-07-08
> **버전**: v1.2
> **상태**: 설계 완료 — 구현 착수 전 §9 미결 사항만 팀 확인 필요
> **태그**: frontend, sprint-2, rbac, company, department, company-member, wbs-3.2

# Sprint 2 WBS 3.2~3.2.4 착수 전 사전조사

WBS 3.2("Company Frontend / Entry")부터 순서대로(3.2.1 → 3.2.2 → 3.2.3 → 3.2.4)
착수하기 전에, 선행 backend(3.1.1~3.1.4)가 실제로 준비됐는지 코드 기준으로
점검한 기록이다.

**업데이트(2026-07-08)**: §3의 핵심 블로커는 PR #60(`GET /api/v1/users/me/
company-memberships` 신규)으로 해소됐다. 남은 건 §4 스코프 확인뿐이다.

---

## 1. WBS 3.2 범위와 순서

| WBS | 내용 | 선행 조건 |
|---|---|---|
| 3.2.1 | S03/S04 진입 skeleton + membership context 연결 | Auth, Sprint 1.5 (Soft) |
| 3.2.2 | S13 회사 관리 화면 → Company API 실 연동 | 3.1.2 Company API |
| 3.2.3 | S14 구성원/부서/초대 상태 화면 확장 | 2.5(완료) + 3.1.3 Department API + 3.1.4 Company Member API |
| 3.2.4 | 권한별 navigation/action 노출 제한(RBAC) | 3.1.4 |

## 2. 선행 backend(3.1.1~3.1.4) 상태

| WBS | 내용 | PR | 상태 |
|---|---|---|---|
| 3.1.1 | 공통 auth context 구조 | #49 | 머지 |
| 3.1.2 | Company API (CRUD/stats) | #50 | 머지 |
| 3.1.3 | Department API | #54 | 머지 |
| 3.1.4 | Company Member API (목록/역할변경) | #56 | 머지 (팀장 리뷰에서 PATCH 경로/문서 불일치 지적 → 같은 PR에서 문서까지 수정 완료) |

표면적으로는 4개 다 끝났지만, §3의 블로커 때문에 실제로는 "회사/부서/구성원
CRUD API"만 준비된 상태고 **"로그인한 사용자가 자신의 소속·role을 아는 방법"은
아직 없다.**

---

## 3. 블로커 — 로그인/me 응답에 membership이 없다 (3.2.1 + 3.2.4 공통 원인)

### ✅ 해소됨 — PR #60 (`feature/user-company-memberships`, 여도현)

`/auth/login`/`/auth/me` 응답 계약은 그대로 두고, 별도 user-scoped 엔드포인트로
분리해서 해소했다(§3에서 제안했던 두 옵션 중 "신규 엔드포인트" 방식).

```
GET /api/v1/users/me/company-memberships
Authorization: Bearer {accessToken}

200 →
{
  "data": {
    "companyMemberships": [
      { "companyMemberId": 1, "companyId": 10, "companyName": "Threadia",
        "role": "COMPANY_ADMIN", "status": "ACTIVE" }
    ]
  },
  "meta": { "requestId": "..." }
}
```

- **`status = ACTIVE`만 반환**한다(`CompanyMemberRepository.
  findByUserIdAndStatusWithCompanyOrderByCompanyIdAscCompanyMemberIdAsc`,
  `join fetch companyMember.company`로 N+1 방지). SUSPENDED/LEFT 회사는
  목록에서 자동으로 빠지므로, 3.2.4에서 "정지/탈퇴한 회사는 아예 안 보인다"는
  동작을 프론트가 따로 필터링할 필요 없이 그대로 얻는다.
- 인증은 `authContext.userId()`(토큰에서 파싱된 본인 ID)만 사용 — 다른
  유저의 membership을 열거할 파라미터 자체가 없다.
- `/auth/me` 응답에 `companyMemberships`가 안 섞여 들어가는 걸 회귀
  테스트로 고정해뒀다(`MeControllerTest`에
  `jsonPath("$.data.companyMemberships").doesNotExist()` 추가).
- 인증 실패 케이스(헤더 없음/Bearer 아님/만료/비활성 유저) 전부 401로
  테스트 커버됨.

이제 3.2.1/3.2.4 설계는 이 엔드포인트를 로그인 직후(또는 AuthContext 마운트
시점)에 한 번 호출해서 `companyId → role` 맵을 만드는 방식으로 잡으면 된다.
`AuthContext.tsx`에 `companyMemberships` state를 추가하고, RailShell/
HubOverlay/members 페이지의 `MOCK_ROLE`/`MOCK_COMPANY_ID`를 이 값으로
교체하는 게 다음 단계다.

### 증상 (해소 전 기록, 배경 참고용)

- `LoginData.companies`가 여전히 `List.of()` 하드코딩이다
  (`backend/src/main/java/com/threadia/backend/auth/dto/LoginData.java`).
- `MeData`는 `UserResponse`(id/email/nickname/avatarUrl)만 담고 있다
  (`backend/src/main/java/com/threadia/backend/user/MeData.java`).
- API-명세서에도 이미 "Sprint 1 companyMemberships는 후속 MVP reference"로
  문서화돼 있었지만, 그 "후속"이 3.1.1~3.1.4 어디에도 실제로 배정되지 않았다.

### 근본 원인 — 조회할 방법 자체가 없다

- `CompanyMemberRepository`에는 `findByCompany_IdAndUser_Id(companyId, userId)`
  (단건, **companyId를 이미 알아야만** 쓸 수 있음)만 있고, "유저 기준 전체
  소속 회사 목록"을 조회하는 메서드가 없다.
- `CompanyController`에도 "내 회사 목록" 엔드포인트가 없다
  (`POST /companies`, `GET/{id}`, `PATCH/{id}`, `GET/{id}/stats` 뿐).
- `GET /companies/{companyId}`(`CompanyService.get()`)는 `requireActiveMember`만
  체크해서 일반 멤버도 호출은 되지만, 응답(`CompanyResponse`)에 role 필드가
  없다.
- role이 들어있는 유일한 응답은 `GET /companies/{companyId}/company-members`
  (`CompanyMemberResponse`)인데, 이 엔드포인트는 `requireCompanyAdmin`이라
  **정작 자기 role을 모르는 일반 멤버(A/P)는 호출 자체가 403으로 막힌다.**

### 왜 하나의 블로커인가

3.2.1(소속 회사·프로젝트 진입점 표시)과 3.2.4(role 기반 UI 노출 제한)가
각각 다른 문제처럼 보이지만, 둘 다 "로그인한 사용자의 companyId+role 목록"이
있으면 동시에 풀린다. 백엔드에 전달할 때 두 건이 아니라 **하나의 요청**으로
정리하는 게 맞다:

> `/auth/login`, `/auth/me` 응답에 `companyMemberships:
> [{ companyId, companyName, role }]`를 채워달라. (또는 별도
> `GET /users/me/company-memberships` 신규 엔드포인트.)

---

## 4. 스코프 확인이 필요한 것 (블로커 아님)

**`POST /companies`(회사 생성)가 이미 열려 있다.** 인증만 되면 별도 권한
체크 없이 누구나 새 회사를 만들 수 있고, 생성자는 즉시 그 회사의
`COMPANY_ADMIN`이 된다(`CompanyService.create()`). 화면정의서 §S13은
"생성 모드는 MVP/Sprint 2 reference"로 문서화돼 있어서, 3.2.2에서 이
생성 폼을 실제로 만들지 아니면 이번에도 seed 회사만 쓰고 미룰지 팀 확인이
필요하다.

참고로 생성 응답(`CompanyCreateData`)에는 `CompanyMemberSummaryResponse
{id, role, status}`가 포함돼 있어서, **방금 만든 회사에 한해서는** role을
즉시 알 수 있다. 다만 기존에 이미 소속돼 있던 회사에는 이 정보가 없어서
§3 블로커의 우회책은 못 된다.

---

## 5. 3.2.3용 API 계약 — 확정, 단 설계 시 주의할 점 1가지

### Department API (3.1.3, PR #54)

- `GET/POST /companies/{companyId}/departments`, `PATCH/DELETE /departments/{departmentId}`
- 삭제 시 `company_members.department_id`를 bulk update로 NULL 처리 후 삭제 — 정상
- 회사별 부서명 unique(DB 제약 + 서비스 체크 이중 방어) — 정상
- IDOR 방지: update/delete는 URL에 companyId 없이 department 엔티티에서
  실제 회사를 유도(`department.getCompany().getId()`) — 안전

### Company Member API (3.1.4, PR #56)

- `GET /companies/{companyId}/company-members?status=&departmentId=&q=`
- `PATCH /companies/{companyId}/company-members/{companyMemberId}`
  (canonical 문서와 경로 불일치가 있었으나 같은 PR에서 문서까지 수정 완료됨)
- 응답: `CompanyMemberResponse{id, companyId, departmentId, role, status,
  joinedAt, createdAt, updatedAt, user}` — 부서 **이름**은 없고 `departmentId`만
  있으므로 화면에서 Department API 응답과 매핑해서 표시해야 한다(PR 작성자도
  명시).
- 비즈니스 규칙(서버가 다 막아줌): 마지막 활성 `COMPANY_ADMIN` 변경 차단(409),
  `LEFT`→`ACTIVE` 직접 재활성화 차단(409), 타 회사 department 지정/구성원
  접근 차단(404).

### ⚠️ 설계 시 주의: PATCH가 partial update가 아니라 full-replace다

```java
CompanyMemberUpdateRequest(@NotNull role, @NotNull status, Long departmentId)
```

`role`과 `status`가 둘 다 필수라서, **호출 한 번에 세 필드를 항상 다
같이 보내야 한다.** 지금 `members/page.tsx`의 mock 코드는 역할 select와
부서 select가 완전히 독립된 onChange 핸들러로 되어 있어서 이 계약과 안
맞는다. 3.2.3에서는 두 select를 다루더라도 PATCH 호출 시점엔 "현재
role+status+departmentId 전체"를 합쳐서 보내는 방식으로 다시 짜야 한다.
`status`(ACTIVE/SUSPENDED/LEFT) 변경 UI 자체가 지금 화면에 아예 없다는
점도 같이 고려해야 한다(최소한 "현재 status 유지" 로직은 필수).

---

## 6. 참고만 — blocking 아님

- **Project API(3.3)는 domain/repository만 있고 Controller/Service가 없다**
  (`backend/.../project/domain/*.java`, `repository/*.java`만 존재).
  3.2.1 완료조건에 "프로젝트 진입점 표시"도 있지만, 원래부터 3.2.1은
  3.3에 의존하지 않게 설계돼 있어서 프로젝트 쪽은 이번에도 mock 유지하면
  된다.
- 부서 삭제와 구성원 부서 변경이 동시에 일어나는 동시성 edge(FK violation이
  500으로 노출될 수 있음)는 팀장이 PR #56 리뷰에서 이미 지적했고 "후속 보강
  후보"로 non-blocking 분류됨.

---

## 7. 다음 액션 (제안)

1. ~~§3 블로커(로그인/me membership 채우기)를 팀에 전달~~ — PR #60으로 해소,
   merge 완료.
2. §4 스코프(회사 생성 UI 포함 여부)는 여전히 팀 확인 필요.
3. `AuthContext.tsx`에 `companyMemberships` state 추가 +
   `GET /users/me/company-memberships` 연동을 3.2.1의 첫 작업으로 잡는다
   (§3 "다음 단계" 참고). 이후 RailShell/HubOverlay/members 페이지의
   `MOCK_ROLE`/`MOCK_COMPANY_ID`를 실제 값으로 순차 교체.
4. 3.2.3 설계 시 §5의 PATCH full-replace 제약을 처음부터 반영한다.

---

## 8. 상세 설계 (구현 착수 기준)

착수 순서 3.2.1 → 3.2.2 → 3.2.3 → 3.2.4에 맞춰 파일 단위로 정리한다.
화면정의서 §S03(2026-07-08 PR #60 갱신분)의 설계 메모를 기준으로 삼는다.

### 8.1 신규/수정 파일 개요

| 파일 | 구분 | 내용 |
|---|---|---|
| `types/company.types.ts` | 신규 | `CompanyMembership`, `CompanyResponse`, `CompanyStatsResponse`, `DepartmentResponse`, `CompanyMemberResponse` 등 backend record와 1:1 매핑 |
| `services/company.service.ts` | 신규 | `getMyMemberships`, `get`, `create`, `update`, `stats` |
| `services/department.service.ts` | 신규 | `list`, `create`, `update`, `remove` |
| `services/company-member.service.ts` | 신규 | `list`, `update`(full-replace) |
| `contexts/AuthContext.tsx` | 수정 | `companies`/`CompanyContext` 제거 → `companyMemberships` state로 교체(§8.2) |
| `types/auth.types.ts` | 수정 | `CompanyContext` 제거(죽은 필드였음, §8.2 근거) |
| `app/(app)/hub/page.tsx`, `components/hub/HubOverlay.tsx` | 수정 | `MOCK_COMPANY_ID`/`MOCK_ROLE` 제거, `companyMemberships` 기준 분기(§8.3) |
| `components/app-shell/RailShell.tsx` | 수정 | `MOCK_ROLE` 제거, `useParams().companyId` 기준으로 `companyMemberships`에서 role 조회 |
| `app/(app)/[companyId]/settings/page.tsx` | 수정 | mock → Company/Department API 실 연동(§8.4) |
| `app/(app)/[companyId]/members/page.tsx` | 수정 | `MOCK_MEMBERS`/`MOCK_DEPARTMENTS` → Company Member/Department API 실 연동(§8.5) |

### 8.2 AuthContext 통합 — `companies`를 `companyMemberships`로 교체

기존 `AuthContext.tsx`의 `companies: CompanyContext[]` state는 `LoginResponse.
companies`(백엔드가 영구히 `List.of()`만 반환하도록 확정된 필드, §3 "증상"
참고)를 채우던 죽은 코드였다. 새로 대체하지 않고 **그대로 제거**하고, PR #60
엔드포인트 기준의 `companyMemberships` state로 바꾼다.

```ts
// types/auth.types.ts
export interface CompanyMembership {
  companyMemberId: number;
  companyId: number;
  companyName: string;
  role: "COMPANY_ADMIN" | "MEMBER";
  status: "ACTIVE" | "SUSPENDED" | "LEFT"; // 응답은 ACTIVE만 오지만 타입은 전체 유지
}
```

```ts
// contexts/AuthContext.tsx
const [companyMemberships, setCompanyMemberships] = useState<CompanyMembership[]>([]);

// refresh(마운트 시)와 login() 성공 직후 둘 다에서 이어서 호출한다.
// 실패해도 로그인 자체를 막지 않는다 — 빈 배열 유지 후 §8.3의
// "소속 없음" 분기로 자연스럽게 흘러가면 된다.
try {
  const { data } = await companyService.getMyMemberships();
  setCompanyMemberships(data.data.companyMemberships);
} catch {
  setCompanyMemberships([]);
}
```

logout 시 `companyMemberships`도 `[]`로 초기화한다(기존 `companies` 초기화와
동일 위치).

### 8.3 3.2.1 — S03 허브 진입점

화면정의서 §S03 설계 메모 그대로 분기한다:

- `companyMemberships.length > 0` → 정상 진입. **다중 회사 소속 시 어떤
  회사로 진입할지는 화면정의서에 명시가 없다** — 우선 `companyMemberships[0]`
  (첫 번째, 서버가 `companyId asc`로 정렬해서 줌)로 진입하고, 회사 전환 UI는
  범위 밖으로 미룬다. **팀 확인 필요**(§9로 이관).
- `companyMemberships.length === 0` → "회사 소속 없음" 상태 화면 + 회사
  생성/초대 수락(`/invitations`, 이미 구현됨) 안내. 이 안내 화면 자체는
  신규로 만들어야 한다(기존에 없음).
- `HubOverlay`/`RailShell`의 `MOCK_COMPANY_ID`를 `companyMemberships[0].companyId`로,
  `MOCK_ROLE`을 그 회사의 `role`로 교체한다.
- 화면정의서 규칙대로 **A/P는 회사관리 메뉴 숨김, C만 노출** — 지금 코드도
  이미 `MOCK_ROLE === "C"`로 조건부 렌더링하던 자리라 값만 실제 role로
  바꾸면 된다(로직 구조 변경 불필요).

### 8.4 3.2.2 — S13 회사 관리 (`settings/page.tsx`)

| mock | 교체 대상 |
|---|---|
| `MOCK_COMPANY` + `saveEdit`의 TODO | `companyService.get(companyId)` 초기 로드, `update(companyId, draft)`로 저장 |
| `MOCK_STATS_BY_STATUS`, `MOCK_MEMBER_COUNT` | `companyService.stats(companyId)` — `CompanyStatsData{companyId, memberCount, departmentCount, projectCount, projectStatusCounts: Map<String,Long>}` 확인 완료. `projectStatusCounts`를 `MOCK_STATS_BY_STATUS`와 동일한 키(`PLANNING`/`ACTIVE`/...)로 그대로 매핑하면 된다 |
| `MOCK_DEPARTMENTS` + `addDepartment`/`removeDepartment`의 TODO | `departmentService.list/create/remove` |
| "생성 모드"(회사가 아예 없을 때) | §4 스코프 확인 결과에 따라 포함 여부 결정. **팀 확인 전엔 관리 모드만 구현.** |

### 8.5 3.2.3 — S14 구성원·부서 (`members/page.tsx`)

| mock | 교체 대상 |
|---|---|
| `MOCK_MEMBERS`, `filteredMembers`(클라이언트 검색) | `companyMemberService.list(companyId, { q, status, departmentId })` — 서버 필터로 이전 가능(§5), 다만 목록 규모가 작으면 클라이언트 검색 유지도 무방 |
| `MOCK_DEPARTMENTS`(부서 select 옵션) | `departmentService.list(companyId)` — S13에서 만든 것과 같은 서비스 재사용 |
| `setMemberRole`, `setMemberDepartment`(독립 핸들러 2개) | **§5에서 지적한 PATCH full-replace 문제.** 두 핸들러를 하나의 `updateMember(memberId, partial)`로 통합하고, 호출 시점에 해당 멤버의 현재 `role`/`status`/`departmentId`를 합쳐서 `companyMemberService.update()`에 전체 페이로드로 보낸다. `status` 변경 UI(활성/정지/탈퇴)는 지금 화면에 없으므로, 최소한 "현재 status 유지"는 반드시 구현하고, status 변경 UI 자체를 이번에 추가할지는 범위 확인 필요. |
| `canManageMembers`(현재 `MOCK_ROLE`) | §8.2의 `companyMemberships` 기준 실제 role로 교체 |

### 8.6 3.2.4 — RBAC UI 노출 제한, 적용 대상 전수 조사

`MOCK_ROLE` 상수를 쓰는 파일을 전부 실제 role로 교체 대상으로 잡는다
(3.2.3 진행 중 재검색해서 최종 확정):

- `components/app-shell/RailShell.tsx`, `components/hub/HubOverlay.tsx` — §8.3에서 이미 처리
- `app/(app)/[companyId]/members/page.tsx` — §8.5에서 이미 처리
- `app/(app)/[companyId]/projects/page.tsx` — `MOCK_ROLE`로 "회사 전체/소속 프로젝트만" 문구, 프로젝트 생성 버튼 노출 분기(C만) 하던 부분
- `app/(app)/[companyId]/[projectId]/backlog/page.tsx`, `board/page.tsx` — `MOCK_ROLE`로 스프린트/이슈 관리 권한 분기. **다만 이 두 화면은 Project Member API(3.3.4, 프로젝트 단위 role)가 별도 필요할 수 있어, 회사 레벨 role만으로 충분한지 3.2.4 착수 시 재확인.**

### 8.7 검증 계획

기존 invitation 작업과 동일한 방식(로컬 backend + seed 계정 + 브라우저
실측)으로 진행한다. 추가로 이번엔:

- 소속 회사 0개/1개/2개 이상 각각 seed해서 §8.3 분기를 전부 확인
- A/P/C 각 role로 로그인해서 §8.6 대상 화면의 메뉴/버튼 노출 차이를 실측
- 3.2.3의 PATCH 통합 호출이 role만 바꿀 때 department가 안 날아가는지,
  department만 바꿀 때 role이 안 날아가는지 교차 확인(§5 회귀 방지)

---

## 9. 미결 — 팀 확인 필요 (설계 진행 중 보류한 것)

1. 다중 회사 소속 시 허브 진입 회사 선택 UX (§8.3)
2. S13 "생성 모드" 포함 여부 (§4, §8.4)
3. S14 구성원 `status`(정지/탈퇴) 변경 UI 포함 여부 (§8.5)
4. backlog/board의 role 분기가 회사 레벨로 충분한지, 프로젝트 레벨 role이
   필요한지 (§8.6)

관련: [[s14-invitation-프론트-연동-설계]]
