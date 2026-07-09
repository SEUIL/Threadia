> **작성자**: 김민정 (WBS 3.1.5 Owner)
> **작성일**: 2026-07-07
> **최종 수정**: 2026-07-08
> **버전**: v2.5
> **상태**: 작성 중
> **태그**: sprint-2, wbs-3.1.5, company, rbac, qa

# Sprint 2 WBS 3.1.5 Company/RBAC Backend 검증 케이스

> 기준: 구글 시트 WBS 탭 3.1.5 row, `docs/02-requirements/기능-시나리오-API-ERD-추적표.md`(COMPANY-01/02/04, DEPT-01~04, MEMBER-01/03/04, RBAC-01), `docs/04-api/API-명세서.md` 13.4절
> WBS 3.1.5(Company/RBAC Backend 검증 evidence 정리, Owner 김민정)의 착수 가능 조건은 "검증 케이스 선작성 가능; 최종 evidence는 구현 결과 후"다. 선행 WBS(3.1.1~3.1.4, Owner 여도현/김민경)가 끝나기 전까지는 이 문서의 케이스 목록만 준비하고, 실제 실행 결과는 구현 완료 후 채운다.

---

## 0. WBS 3.1.5 진행 현황 (본인 self-report)

| WBS | 진척률 | 비고 |
|---|---|---|
| 3.1.5 | 90% | §2 검증 케이스 23개(19-A/19-B 포함) 중 22개 실행 완료(21개 정상 통과, #4/#7 버그 2건 발견), #19는 3.3.x(Project API) 미구현으로 실행 불가. §3 DB 정합성 포인트도 실행 중 확인 완료. 남은 건 #4/#7 버그를 owner(김민경)·Reviewer(여도현)에게 전달하고 후속 조치 확인하는 것 |

---

## 1. 착수 조건 확인

| WBS | 작업 | Owner | 진척률(시트 기준) | 비고 |
|---|---|---|---|---|
| 3.1.1 | Backend 공통 구조와 auth context 기준 확정 | **김민경** (시트상 여도현→김민경으로 재배정 확인, 2026-07-08) | **100% (완료)** | dev(`dfb248f`)의 `AuthContext`/`AuthContextService`/`CompanyMembershipService` merge와 일치. PR #49 Reviewer review + merge로 확정, 시트에도 반영 완료 |
| 3.1.2 | 회사 조회/생성/수정/통계 API 구현 | 김민경 | **100% (완료, 단 §2.1 실행 중 버그 2건 발견 — 2026-07-08)** | dev(`eb8bb4c`)에 `CompanyController`, `CompanyService`, `CompanyControllerTest`, `CompanyServiceTest` merge 확인(2026-07-07)과 일치. **다만 §2.1 Apidog 실행 결과 #4(존재 확인보다 권한 확인이 먼저 실행되는 순서 버그), #7(로고 저장 endpoint 자체가 미구현)이 확인됨 — 상세는 §2.1 표와 비고 참고. Reviewer review+merge 요건은 충족했으나 review 범위에서 이 2건은 발견되지 못했음** |
| 3.1.3 | 부서 목록/생성/수정/삭제 API 구현 | 김민경 | 진행중(코드 완성, review 기록 없음) | PR #54 merge(2026-07-08). `DepartmentController`(조회/생성/수정/삭제 전부), `DepartmentService`, `DepartmentRepository`, exception 클래스까지 전부 존재. **다만 GitHub review 기록이 0건**이고 부팀장(cjfwls39) bypass로 merge됨 — 새 규칙(Reviewer review+merge) 기준으로는 아직 100% 확정 조건을 충족하지 못함. §2.2 실행은 가능 |
| 3.1.4 | 회사 구성원 목록과 역할/부서/상태 변경 API 구현 | 여도현 | **100% (완료)** | PR #56 merge(2026-07-08). **Reviewer(황스일/SEUIL)가 review(APPROVED) 후 본인이 직접 merge까지 완료** — 새 규칙에 정확히 부합하는 100% 확정 사례. 타 회사 department 지정 차단, 마지막 활성 COMPANY_ADMIN 변경 차단, LEFT 직접 재활성화 차단까지 구현됨(canonical 문서 경로 동기화 이슈 1건은 review 중 발견되어 반영 완료) |

진척률 갱신 규칙(2026-07-08 변경): 진행중~99%는 각 Owner 본인, **100% 확정은 Reviewer가 해당 PR을 review + merge 완료한 시점**. **3.1.1, 3.1.2, 3.1.4가 100% 확정됐으므로 §2.1(회사 API)·§2.3/§2.3-A(회사 구성원 API)·§2.4(RBAC 공통)는 지금부터 실제 실행이 가능하다.** **3.1.3(부서 API)은 코드상 완성이지만 review 기록이 없어 §2.2를 시험 실행은 할 수 있으나 최종 완료 판정은 보류한다.**

---

## 2. 검증 케이스

### 2.1 회사 API (3.1.2 대상, 요구사항 COMPANY-01/02/04)

| # | endpoint | 케이스 | 종류 | 기대 결과 | 확인 결과 | 비고 |
|---|---|---|---|---|---|---|
| 1 | `POST /api/v1/companies` | 정상 생성 (name/representativeName/description/industryCategory) | 정상 | 201, `company` 생성 + 생성자 `companyMember.role = COMPANY_ADMIN` | **정상 통과** | Apidog 실행(2026-07-08) 201 확인, `companyId=6` 생성 |
| 2 | `POST /api/v1/companies` | 필수 필드 누락(name 없음) | 실패 | 400 `VALIDATION_ERROR` | **정상 통과** | Apidog 실행(2026-07-08) 400 확인 |
| 3 | `GET /api/v1/companies/{companyId}` | 소속 사용자(A/P/C) 조회 | 정상 | 200, 회사 정보 반환 | **정상 통과** | Apidog 실행(2026-07-08) 200 확인 (`companyId=6`) |
| 4 | `GET /api/v1/companies/{companyId}` | 존재하지 않는 companyId | 실패 | 404 `NOT_FOUND` | **실패(버그)** | Apidog 실행(2026-07-08) 결과 403 `FORBIDDEN` 반환. 원인: `CompanyService.get()`이 `companyAccessService.requireActiveMember(companyId, authContext)`를 존재 확인(`company(companyId)`의 `orElseThrow(CompanyNotFoundException)`)보다 먼저 실행 — 존재하지 않는 companyId는 `company_members` row도 없으므로 "권한 없음"과 동일 취급되어 403이 먼저 던져짐. `CompanyService.update()`, `stats()`도 동일 순서라 같은 증상 예상. **동일 패턴이 `DepartmentService`(3곳), `CompanyMemberService`(2곳)에도 존재** — Company/Department/CompanyMember API 전반에 걸친 구조적 이슈. Apidog 요청 자체는 정상 토큰으로 정상 도달함(401이 아닌 403이 이를 뒷받침) — 설정 오류 아님 |
| 5 | `PATCH /api/v1/companies/{companyId}` | C 역할 사용자가 name/description 등 수정 | 정상 | 200, 변경 반영 | **정상 통과** | Apidog 실행(2026-07-08) 200 확인, description 변경 반영 |
| 6 | `PATCH /api/v1/companies/{companyId}` | A/P 역할 사용자가 수정 시도 | 실패 | 403 `FORBIDDEN` | **정상 통과** | Apidog 실행(2026-07-08) 403 확인. 최초 확인 시점엔 member1이 아직 회사 6에 실제로 가입되지 않은 상태(비멤버)였음이 나중에 DB 확인으로 드러나, invitation accept 재실행(`membershipCreated: true` 확인) 후 정식 MEMBER 신분으로 재확인함. RBAC-01과 겹치는 케이스 |
| 7 | `POST /api/v1/companies/{companyId}/logo` | logoUrl 저장 | 정상 | 200, `logoUrl` 갱신 | **실패(버그)** | Apidog 실행(2026-07-08) 결과 500, body `{"error":{"code":"INTERNAL_ERROR","message":"서버 오류가 발생했습니다.","details":{}},"meta":{...}}`. 원인은 2가지가 겹쳐있음: **(1) 기능 미구현** — `CompanyController`에 `/logo` 매핑 자체가 존재하지 않음(생성/조회/수정/통계 4개 endpoint만 구현됨). API 명세서(COMPANY-02)에는 명시되어 있어 3.1.2 완료 판정과 충돌. **(2) 별개의 시스템적 버그** — 매핑되지 않은 URL은 Spring이 `NoResourceFoundException`으로 처리하는데 `GlobalExceptionHandler`가 이 예외를 별도로 처리하지 않아 catch-all(`handleUnexpected`)로 떨어져 500이 됨. 이는 백엔드 서버 콘솔 로그로 재확인됨(2026-07-08, 별도 재현: 잘못된 URL `/api/v1/companies/7/accept` 호출 시 동일하게 `NoResourceFoundException` → 500). 즉 `/logo`뿐 아니라 **존재하지 않는/오타난 URL 전체가 404 대신 500을 반환**하는 구조적 문제이며, `GlobalExceptionHandler`에 `NoResourceFoundException → 404` 매핑 추가가 필요 |
| 8 | `GET /api/v1/companies/{companyId}/stats` | C 역할 사용자 통계 조회 | 정상 | 200, 구성원/부서/프로젝트 count 반환 | **정상 통과** | Apidog 실행(2026-07-08) `accessToken`(C) 200, `memberAccessToken`(정식 MEMBER, invitation accept 완료 확인 후 재실행) 403 모두 기대값 확인 — PR #50 review에서 지적됐던 "stats는 C 권한" 이슈가 실제로 반영됐음을 재확인. COMPANY-04 |

### 2.2 부서 API (3.1.3 대상, 요구사항 DEPT-01~04)

| # | endpoint | 케이스 | 종류 | 기대 결과 | 확인 결과 | 비고 |
|---|---|---|---|---|---|---|
| 9 | `GET /api/v1/companies/{companyId}/departments` | C 역할 목록 조회 | 정상 | 200, 부서 목록 반환 | **정상 통과** | Apidog 실행(2026-07-08) 200 확인. DEPT-01 |
| 10 | `POST /api/v1/companies/{companyId}/departments` | 신규 부서명으로 생성 | 정상 | 201, 부서 생성 | **정상 통과** | Apidog 실행(2026-07-08) 201 확인, `departmentId=1` 생성. DEPT-02 |
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
| 19 | `PATCH /api/v1/project-members/{projectMemberId}` (프로젝트 관리자 지정) | PROJECT_ADMIN 최소 1명 유지 위반 시도 | 실패 | 409 `CONFLICT` 계열 (정확한 에러코드는 구현 후 확인) | **확인 불가(차단)** | 2026-07-08 기준 `com.threadia.backend.project` 패키지에 Controller 자체가 없음(3.3.2~3.3.4 미착수) — 실행 자체가 불가능. 3.3.x 구현 후 재시도 필요. MEMBER-04, 3.3.4와 경계 겹침 |

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
2. §2의 23개 케이스(19-A, 19-B 포함)를 Apidog collection의 request로 등록하고 순서대로 실행해 결과를 기록한다.
3. §3 DB 확인 포인트를 실제 쿼리로 검증한다.
4. 결과를 종합해 Reviewer(황스일)에게 재확인을 요청한다.
5. 실패 케이스가 있으면 완료 처리를 보류하고 해당 owner(여도현/김민경)에게 후속 조치를 요청한다.

---

## 5. 지금 미리 준비할 수 있는 것 (선행 작업 불필요)

- §2 검증 케이스 23개 목록(19-A, 19-B 포함)과 기대 결과
- §3 DB 확인 포인트
- Apidog collection에 §2 케이스별 request 틀 미리 만들어두기 (endpoint, method, request body 예시까지는 API 명세서 기준으로 채울 수 있음, 실제 실행은 API 완성 후)

## 6. 착수 후에만 가능한 것 (3.1.1~3.1.4 완료 필요)

- §2 케이스의 실제 실행과 결과 기록
- §3 DB 쿼리 실행 확인
- 최종 evidence 정리와 Reviewer 요청
