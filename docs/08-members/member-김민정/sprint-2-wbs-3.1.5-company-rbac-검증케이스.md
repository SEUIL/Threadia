> **작성자**: 김민정 (WBS 3.1.5 Owner)
> **작성일**: 2026-07-07
> **최종 수정**: 2026-07-09
> **버전**: v2.7
> **상태**: 검토 요청
> **태그**: sprint-2, wbs-3.1.5, company, rbac, qa

# Sprint 2 WBS 3.1.5 Company/RBAC Backend 검증 케이스

> 기준: 구글 시트 WBS 탭 3.1.5 row, `docs/02-requirements/기능-시나리오-API-ERD-추적표.md`(COMPANY-01/02/04, DEPT-01~04, MEMBER-01/03/04, RBAC-01), `docs/04-api/API-명세서.md` 13.4절
> WBS 3.1.5(Company/RBAC Backend 검증 evidence 정리, Owner 김민정)의 착수 가능 조건은 "검증 케이스 선작성 가능; 최종 evidence는 구현 결과 후"다. 선행 WBS(3.1.1~3.1.4, Owner 여도현/김민경)가 끝나기 전까지는 이 문서의 케이스 목록만 준비하고, 실제 실행 결과는 구현 완료 후 채운다.

---

## 0. WBS 3.1.5 진행 현황 (본인 self-report)

| WBS | 진척률 | 비고 |
|---|---|---|
| 3.1.5 | 100% | §2 검증 케이스 22개(19-A/19-B 포함) 전부 실행 완료(22개 정상 통과). §3 DB 정합성 포인트도 실행 중 확인 완료. #4/#7 버그는 PR #66(fix/company-api-qa, 2026-07-09 merge)로 수정되어 Apidog 재검증까지 완료함(§2.1 #4/#7 참고). 초안에 있던 프로젝트 관리자 지정 케이스는 WBS 3.3.x(Project) 범위라 [`sprint-2-wbs-3.3.5-project-backend-검증케이스.md`](./sprint-2-wbs-3.3.5-project-backend-검증케이스.md)로 이관함 |

---

## 1. 착수 조건 확인

| WBS | 작업 | Owner | 진척률(시트 기준) | 비고 |
|---|---|---|---|---|
| 3.1.1 | Backend 공통 구조와 auth context 기준 확정 | **김민경** (시트상 여도현→김민경으로 재배정 확인, 2026-07-08) | **100% (완료)** | dev(`dfb248f`)의 `AuthContext`/`AuthContextService`/`CompanyMembershipService` merge와 일치. PR #49 Reviewer review + merge로 확정, 시트에도 반영 완료 |
| 3.1.2 | 회사 조회/생성/수정/통계 API 구현 | 김민경 | **100% (완료, §2.1 버그 2건 PR #66로 수정 및 재검증 완료 — 2026-07-09)** | dev(`eb8bb4c`)에 `CompanyController`, `CompanyService`, `CompanyControllerTest`, `CompanyServiceTest` merge 확인(2026-07-07)과 일치. §2.1 Apidog 실행 결과 발견된 #4(존재 확인보다 권한 확인이 먼저 실행되는 순서 버그), #7(로고 저장 endpoint 미구현 + 404 매핑 누락)은 xaphxe가 PR #66(`fix/company-api-qa`, Closes #63)으로 수정, 리뷰(김민정) 3라운드 반영 후 2026-07-09 dev merge(`776ca26`). Apidog 재실행으로 #4/#7 모두 정상 동작 재확인함 — 상세는 §2.1 표와 비고 참고 |
| 3.1.3 | 부서 목록/생성/수정/삭제 API 구현 | 김민경 | **100% (완료, 부팀장 merge 인정)** | PR #54 merge(2026-07-08). `DepartmentController`(조회/생성/수정/삭제 전부), `DepartmentService`, `DepartmentRepository`, exception 클래스까지 전부 존재. merge는 **부팀장이자 리뷰어인 정철진(cjfwls39)**이 직접 수행 — GitHub 상 별도 APPROVED review 코멘트는 없으나, 부팀장(리뷰어)의 직접 merge를 리뷰어 merge 조건 충족으로 인정한다. §2.2 실행 완료. **추가(2026-07-09)**: §2.1 #4 순서 버그와 동일한 패턴이 `list()`/`create()`에서 발견되어 이슈 #67로 분리, PR #68(xaphxe 작성)로 수정됨. PR #68도 정철진(cjfwls39)이 직접 merge(`e17c147`) — 순서 버그는 §2.2 #9/#10 재검증으로 해소됐고, 부팀장 merge 기준으로 3.1.3 완료 인정 |
| 3.1.4 | 회사 구성원 목록과 역할/부서/상태 변경 API 구현 | 여도현 | **100% (완료)** | PR #56 merge(2026-07-08). **Reviewer(황스일/SEUIL)가 review(APPROVED) 후 본인이 직접 merge까지 완료** — 새 규칙에 정확히 부합하는 100% 확정 사례. 타 회사 department 지정 차단, 마지막 활성 COMPANY_ADMIN 변경 차단, LEFT 직접 재활성화 차단까지 구현됨(canonical 문서 경로 동기화 이슈 1건은 review 중 발견되어 반영 완료) |

진척률 갱신 규칙(2026-07-08 변경): 진행중~99%는 각 Owner 본인, **100% 확정은 Reviewer가 해당 PR을 review + merge 완료한 시점**(부팀장이자 리뷰어인 철진의 직접 merge도 리뷰어 merge로 인정). **3.1.1~3.1.4가 모두 100% 확정됐으므로 §2 전체 케이스(§2.1 회사 API·§2.2 부서 API·§2.3/§2.3-A 회사 구성원 API·§2.4 RBAC 공통) 실행이 완료됐다.**

---

## 2. 검증 케이스

### 2.1 회사 API (3.1.2 대상, 요구사항 COMPANY-01/02/04)

| # | endpoint | 케이스 | 종류 | 기대 결과 | 확인 결과 | 비고 |
|---|---|---|---|---|---|---|
| 1 | `POST /api/v1/companies` | 정상 생성 (name/representativeName/description/industryCategory) | 정상 | 201, `company` 생성 + 생성자 `companyMember.role = COMPANY_ADMIN` | **정상 통과** | Apidog 실행(2026-07-08) 201 확인, `companyId=6` 생성 |
| 2 | `POST /api/v1/companies` | 필수 필드 누락(name 없음) | 실패 | 400 `VALIDATION_ERROR` | **정상 통과** | Apidog 실행(2026-07-08) 400 확인 |
| 3 | `GET /api/v1/companies/{companyId}` | 소속 사용자(A/P/C) 조회 | 정상 | 200, 회사 정보 반환 | **정상 통과** | Apidog 실행(2026-07-08) 200 확인 (`companyId=6`) |
| 4 | `GET /api/v1/companies/{companyId}` | 존재하지 않는 companyId | 실패 | 404 `NOT_FOUND` | **정상 통과(재검증, 2026-07-09)** | **최초 발견(2026-07-08)**: 403 `FORBIDDEN` 반환. 원인은 `CompanyService.get()`이 권한 확인을 존재 확인보다 먼저 실행하던 순서 문제. **수정(PR #66, 2026-07-09 dev merge `776ca26`)**: `인증(토큰 검증+ACTIVE 사용자) → 존재 확인 → 권한 확인` 순서로 통일. 무효 토큰(`invalid-token-abc`)으로 재확인 시 401도 정상 반환되어, 익명/무효 토큰이 존재 확인 단계까지 가지 못하는 것도 확인함. **재검증**: `dev` 최신 코드로 로컬 백엔드 재기동 후 Apidog 재실행 → 404 `NOT_FOUND` 확인. `CompanyService.update()`, `stats()`, `updateLogo()`도 동일 순서로 함께 수정됨. **`DepartmentService`(3곳)의 동일 패턴은 이번 PR 범위 밖이라 별도 이슈([#67](https://github.com/SEUIL/Threadia/issues/67))로 분리** → xaphxe가 PR #68(`fix/department-api-qa`)로 `list()`/`create()`의 순서를 동일하게 수정, cjfwls39가 2026-07-09 05:22 merge(`e17c147`), 이슈 #67 자동 close. Apidog 재검증 결과는 §2.2 #9/#10 비고 참고 |
| 5 | `PATCH /api/v1/companies/{companyId}` | C 역할 사용자가 name/description 등 수정 | 정상 | 200, 변경 반영 | **정상 통과** | Apidog 실행(2026-07-08) 200 확인, description 변경 반영 |
| 6 | `PATCH /api/v1/companies/{companyId}` | A/P 역할 사용자가 수정 시도 | 실패 | 403 `FORBIDDEN` | **정상 통과** | Apidog 실행(2026-07-08) 403 확인. 최초 확인 시점엔 member1이 아직 회사 6에 실제로 가입되지 않은 상태(비멤버)였음이 나중에 DB 확인으로 드러나, invitation accept 재실행(`membershipCreated: true` 확인) 후 정식 MEMBER 신분으로 재확인함. RBAC-01과 겹치는 케이스 |
| 7 | `POST /api/v1/companies/{companyId}/logo` | logoUrl 저장 | 정상 | 200, `logoUrl` 갱신 | **정상 통과(재검증, 2026-07-09)** | **최초 발견(2026-07-08)**: 500 `INTERNAL_ERROR`. 원인 2가지 — (1) `/logo` endpoint 자체가 미구현, (2) 매핑 안 된 URL이 `NoResourceFoundException`으로 처리되는데 `GlobalExceptionHandler`가 이를 catch-all로 흘려보내 500이 됨(잘못된 URL `/api/v1/companies/7/accept`로도 동일 재현). **수정(PR #66)**: `POST /{companyId}/logo` endpoint 신규 구현(`CompanyLogoRequest`에 `@NotBlank @Size(max=500) @URL` 검증), `GlobalExceptionHandler`에 `NoResourceFoundException → 404` 매핑 추가. 리뷰 중 추가로 발견된 문제(`PATCH /{companyId}`의 `CompanyUpdateRequest.logoUrl`이 느슨한 검증으로 로고를 암묵적으로 지울 수 있던 것)도 같은 PR에서 함께 정리 — `update()`에서 `logoUrl` 처리 로직을 제거해 로고 변경은 `/logo` 전용 API로만 책임 분리함(로고 삭제 API는 신규 기능으로 범위 제외, 필요 시 별도 이슈). **재검증(Apidog, dev 최신)**: ① `POST /logo` 정상 body → 200, `logoUrl` 갱신 확인 ② `logoUrl: ""` / `"not-a-url"` → 400 `VALIDATION_ERROR` 확인 ③ `GET /api/v1/companies/6/accept`(오타 URL) → 404 `NOT_FOUND` 확인(기존 500에서 전환) ④ `PATCH /{companyId}`에 `logoUrl`을 같이 보내도 무시되고 기존 값 유지되는 것 확인 |
| 8 | `GET /api/v1/companies/{companyId}/stats` | C 역할 사용자 통계 조회 | 정상 | 200, 구성원/부서/프로젝트 count 반환 | **정상 통과** | Apidog 실행(2026-07-08) `accessToken`(C) 200, `memberAccessToken`(정식 MEMBER, invitation accept 완료 확인 후 재실행) 403 모두 기대값 확인 — PR #50 review에서 지적됐던 "stats는 C 권한" 이슈가 실제로 반영됐음을 재확인. COMPANY-04 |

### 2.2 부서 API (3.1.3 대상, 요구사항 DEPT-01~04)

| # | endpoint | 케이스 | 종류 | 기대 결과 | 확인 결과 | 비고 |
|---|---|---|---|---|---|---|
| 9 | `GET /api/v1/companies/{companyId}/departments` | C 역할 목록 조회 | 정상 | 200, 부서 목록 반환 | **정상 통과** | Apidog 실행(2026-07-08) 200 확인. DEPT-01. **추가 검증(2026-07-09)**: §2.1 #4에서 발견된 순서 버그가 `list()`에도 있어 존재하지 않는 companyId 조회 시 403이 나던 것을 PR #68로 수정 — 재검증 결과 404 `NOT_FOUND` 확인 |
| 10 | `POST /api/v1/companies/{companyId}/departments` | 신규 부서명으로 생성 | 정상 | 201, 부서 생성 | **정상 통과** | Apidog 실행(2026-07-08) 201 확인, `departmentId=1` 생성. DEPT-02. **추가 검증(2026-07-09)**: `create()`도 동일 순서 버그가 있었으나 PR #68로 수정 — 존재하지 않는 companyId로 재검증 시 404 `NOT_FOUND` 확인 |
| 11 | `POST /api/v1/companies/{companyId}/departments` | 회사 내 중복 부서명으로 생성 | 실패 | 409 `CONFLICT` | **정상 통과** | Apidog 실행(2026-07-08) 409 확인. "회사별 부서명 unique" 완료 조건 직접 검증 |
| 12 | `PATCH /api/v1/departments/{departmentId}` | 부서명 변경 | 정상 | 200, 변경 반영 | **정상 통과** | Apidog 실행(2026-07-08) 200 확인. DEPT-03 |
| 13 | `DELETE /api/v1/departments/{departmentId}` | 소속 멤버 있는 부서 삭제 | 정상 | 200, `affectedMemberCount` > 0, 해당 `company_members.department_id = NULL` | **정상 통과** | Apidog 실행(2026-07-08) 200, `affectedMemberCount: 1` 확인. DB 직접 조회로 `company_members.department_id = NULL`, `departments` row 삭제 둘 다 재확인 완료. DEPT-04 |
| 14 | `DELETE /api/v1/departments/{departmentId}` | 존재하지 않는 departmentId | 실패 | 404 `NOT_FOUND` | **정상 통과** | Apidog 실행(2026-07-08) 404 `NOT_FOUND` 확인. **(정정)** 이전에 §2.1 #4와 동일 패턴(403 예상)으로 경고했었으나 틀린 예측이었음 — `DepartmentService.delete()`는 `list()/create()/update()`와 반대로 **존재 확인(`departmentForUpdate`)을 권한 확인보다 먼저 실행**하는 구조라 #4 버그 패턴이 적용되지 않음. 실제 실행으로 정정 확인 |

### 2.3 회사 구성원 API (3.1.4 대상, 요구사항 MEMBER-01/03/04)

> **경로 정정(2026-07-08)**: 문서 초안에는 `PATCH /api/v1/company-members/{companyMemberId}`로 적었으나, 실제 구현(`CompanyMemberController.java:51`)은 `PATCH /api/v1/companies/{companyId}/company-members/{companyMemberId}`로 **companyId가 경로에 포함**된다. 아래 표는 실제 경로로 정정했다.

| # | endpoint | 케이스 | 종류 | 기대 결과 | 확인 결과 | 비고 |
|---|---|---|---|---|---|---|
| 15 | `GET /api/v1/companies/{companyId}/company-members` | C 역할 목록 조회 | 정상 | 200, 역할/부서/상태 포함 목록 반환 | **정상 통과** | Apidog 실행(2026-07-08) 200 확인. MEMBER-01 |
| 16 | `PATCH /api/v1/companies/{companyId}/company-members/{companyMemberId}` | role/department/status 정상 변경 | 정상 | 200, 변경 반영 | **정상 통과** | Apidog 실행(2026-07-08) 200 확인, `departmentId: 1`로 변경 반영. MEMBER-03 |
| 17 | `PATCH /api/v1/companies/{companyId}/company-members/{companyMemberId}` | A/P 역할 사용자가 변경 시도 | 실패 | 403 `FORBIDDEN` | **정상 통과** | Apidog 실행(2026-07-08) 403 확인. RBAC-01과 겹치는 케이스 |
| 18 | `PATCH /api/v1/companies/{companyId}/company-members/{companyMemberId}` | 존재하지 않는 companyMemberId | 실패 | 404 `NOT_FOUND` | **정상 통과** | Apidog 실행(2026-07-08) 404 확인. **(정정)** 이전에 §2.1 #4와 동일 패턴(403 예상)으로 경고했었으나 틀린 예측이었음 — 예측대로 `update()`는 path의 `companyId`(항상 유효)로 권한 체크를 통과한 뒤 `companyMemberForUpdate(companyMemberId, companyId)`에서 존재하지 않는 companyMemberId를 정상적으로 잡아 404를 던지는 구조로 확인 |

> **범위 정정(2026-07-09)**: 초안에 있던 프로젝트 관리자 지정 케이스(`PATCH /api/v1/project-members/{projectMemberId}`, PROJECT_ADMIN 최소 1명 유지)는 WBS 3.1.5(Company/RBAC) 범위가 아니라 **WBS 3.3.x(Project) 범위**다. 해당 케이스는 [`sprint-2-wbs-3.3.5-project-backend-검증케이스.md`](./sprint-2-wbs-3.3.5-project-backend-검증케이스.md) §2.3(#17)에서 이미 동일하게 추적하므로 이 문서에서는 제외한다.

### 2.3-A Invitation accept 연계 membership 상태 전이 (WBS 2.4 정책, PR #48/#53 반영)

`company_members`는 3.1.4 API뿐 아니라 invitation accept(WBS 2.4)로도 상태가 바뀐다. PR #53(`docs/05-architecture/백엔드-구조.md`)에 문서화된 정책 기준으로 아래 2개 케이스를 추가한다.

| # | endpoint | 케이스 | 종류 | 기대 결과 | 확인 결과 | 비고 |
|---|---|---|---|---|---|---|
| 19-A | `POST /api/v1/invitations/{invitationId}/accept` | 기존 `LEFT` 상태 membership이 있는 사용자가 재초대를 accept | 정상 | 새 row를 만들지 않고 기존 membership을 `ACTIVE`로 재활성화 | **정상 통과** | Apidog 실행(2026-07-08) member2(companyMemberId=8)를 LEFT 전환 후 재초대(invitationId=10) accept → `membershipCreated: false` 확인. DB로 `company_members` row 개수 불변(3개 유지, id=8 그대로 ACTIVE 재활성화)까지 재확인 완료 |
| 19-B | `POST /api/v1/invitations/{invitationId}/accept` | 기존 `SUSPENDED` 상태 membership이 있는 사용자가 accept 시도 | 실패 | 재활성화하지 않고 `409 COMPANY_MEMBER_SUSPENDED` | **정상 통과** | Apidog 실행(2026-07-08) member1(companyMemberId=7)을 SUSPENDED로 전환 후 재초대(invitationId=8) accept 시도 → 409 확인. SUSPENDED 사용자가 초대 수락만으로 우회 활성화되면 안 됨 |

### 2.4 RBAC 공통 검증 (RBAC-01)

| # | 케이스 | 종류 | 기대 결과 | 확인 결과 | 비고 |
|---|---|---|---|---|---|
| 20 | `users` 테이블에 `role` 단일 필드가 없음을 DB 스키마로 확인 | DB | `users.role` 컬럼 부재 | **정상 통과** | `DESCRIBE users`(2026-07-08) 결과 `id/email/password_hash/nickname/avatar_url/status/created_at/updated_at`만 존재, `role` 컬럼 없음 확인. ERD 기준과 일치 |
| 21 | 회사 관련 API 전체에서 권한 판정이 `company_members.role` 기준으로만 동작 | 코드/API | C 미만 역할은 관리 action에서 일괄 403 | **정상 통과** | §2.1 #6, §2.3 #17에서 확인한 개별 403 케이스 취합 — 둘 다 `company_members.role`(MEMBER)이 COMPANY_ADMIN 미만이라 일관되게 403 처리됨을 확인 |

`확인 결과`는 `정상 통과 / 실패(버그) / 확인 불가` 중 하나로 표기한다. `실패(버그)`면 비고에 재현 방법과 관련 PR 링크를 남긴다.

---

## 3. DB 정합성 확인 포인트

- `companies`, `departments`, `company_members` 관계가 `docs/03-design/ERD.md`와 충돌하지 않는지 확인한다.
- 부서 삭제 시 `company_members.department_id`가 실제로 `NULL`이 되는지 쿼리로 직접 확인한다 (§2.2 #13).
- secret, DB password, token은 evidence에 기록하지 않는다 (시트 3.1.5 row 비고 기준).

---

## 4. 검증 방법 (시트 원문: test 결과, curl/Postman evidence, DB 확인)

> **팀 방향(2026-07-07 확인, canonical 문서 미반영)**: Backend 단위 테스트/통합 테스트는 `./gradlew test`가 아니라 **Apidog collection 실행**으로 진행한다. 이 방향은 아직 `docs/00-project/Definition-of-Done.md`, `docs/00-project/검증-방법.md`에는 반영되어 있지 않으므로, 이 문서 §2의 개별 케이스는 Apidog request로 만들어 실행하는 것을 기본으로 하고 결과를 이 표에 기록한다.

1. 3.1.1~3.1.4가 merge된 시점에 §1 표를 갱신한다.
2. §2의 22개 케이스(19-A, 19-B 포함)를 Apidog collection의 request로 등록하고 순서대로 실행해 결과를 기록한다.
3. §3 DB 확인 포인트를 실제 쿼리로 검증한다.
4. 결과를 종합해 Reviewer(황스일)에게 재확인을 요청한다.
5. 실패 케이스가 있으면 완료 처리를 보류하고 해당 owner(여도현/김민경)에게 후속 조치를 요청한다.
