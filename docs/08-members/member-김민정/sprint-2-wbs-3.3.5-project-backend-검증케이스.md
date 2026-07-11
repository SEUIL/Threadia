> **작성자**: 김민정 (WBS 3.3.5 Owner)
> **작성일**: 2026-07-07
> **최종 수정**: 2026-07-10
> **버전**: v1.6
> **상태**: 검토 요청
> **태그**: sprint-2, wbs-3.3.5, project, qa

# Sprint 2 WBS 3.3.5 Project Backend 검증 케이스

> 기준: 구글 시트 WBS 탭 3.3.5 row, `docs/02-requirements/기능-시나리오-API-ERD-추적표.md`(PROJECT-01~04, PROJECT-MEMBER-01~02), `docs/04-api/API-명세서.md` 13.5절
> WBS 3.3.5(Project Backend 검증 evidence 정리, Owner 김민정)의 착수 가능 조건은 "검증 케이스 선작성 가능; 최종 evidence는 구현 결과 후"다. 선행 WBS(3.3.1~3.3.4, Owner 김태준)가 끝나기 전까지는 이 문서의 케이스 목록만 준비하고, 실제 실행 결과는 구현 완료 후 채운다.
> 완료 조건(시트 원문): "생성/목록/상세/수정/archive와 권한 실패 케이스 검증 결과 정리".

---

## 0. WBS 3.3.5 진행 현황 (본인 self-report)

| WBS | 진척률 | 비고 |
|---|---|---|
| 3.3.5 | 95% (검증 완료·F-1/#20 재검증 통과, Reviewer 머지만 대기) | Apidog 실행 완료(2026-07-10~11). §2 검증 케이스 20개 **전부 정상 통과**(#20은 PR #74 리뷰(황스일) 반영해 "비활성 회사 구성원 승격 시도 → 409"로 재정의·재검증). §3 DB 정합성 포인트 4개 전부 쿼리로 확인 완료. 코드 리뷰 Finding F-1(§2.1 하단, list/create 존재확인/권한확인 순서)은 이슈 [#73](https://github.com/SEUIL/Threadia/issues/73) → PR [#76](https://github.com/SEUIL/Threadia/pull/76)(김민경 수정, 여도현 review+merge)으로 dev 반영, **재검증(없는 companyId → 404) 통과(2026-07-11)로 해결 완료**. **100% 확정 조건: Reviewer(정철진)가 PR [#74](https://github.com/SEUIL/Threadia/pull/74) review+merge.** |

---

## 1. 착수 조건 확인

| WBS | 작업 | Owner | Reviewer | 선행 | 진척률(2026-07-08 기준) | 비고 |
|---|---|---|---|---|---|---|
| 3.3.1 | Project DB/model/repository 기준 구현 | 김태준 | 여도현 | 3.1 | **100% (완료)** | PR #57 merge(2026-07-08). **Reviewer(여도현/dhy257)가 review(APPROVED) 후 본인이 직접 merge** — 새 규칙 기준 100% 확정. `Project`/`ProjectMember` 도메인, repository, entity mapping 테스트 포함. API/서비스 계층(Controller/Service)은 3.3.2 이후 범위라 아직 없음(정상) |
| 3.3.2 | 프로젝트 목록/생성 API 구현 | 김태준 | 정철진 | 3.3.1, 3.1 | **100% (완료, 부팀장 merge 인정)** | PR #61 merge(2026-07-08, `feature/wbs-3.3.2-project-api`). GitHub review 코멘트는 없으나 **지정 Reviewer(정철진/cjfwls39)가 직접 merge** — 3.1.3에서 확립된 "부팀장이자 Reviewer의 직접 merge = Reviewer merge 인정" 기준 적용. `ProjectController`(`GET/POST /companies/{companyId}/projects`) dev에 구현 확인 |
| 3.3.3 | 프로젝트 상세/수정/archive API 구현 | 김태준 | 여도현 | 3.3.2 | **완료(단, Reviewer 승인 불일치 — 2026-07-09)** | PR #65 merge(2026-07-09, `feature/wbs-3.3.3-project-detail-update-archive`). **지정 Reviewer(여도현/dhy257)는 COMMENTED만 남기고 APPROVE하지 않았고, 실제 merge는 정철진(cjfwls39)이 수행** — 지정 Reviewer 승인도, 지정 Reviewer의 직접 merge도 아니라 3.3.1/3.3.2/3.3.4와 다른 패턴. `ProjectController`(`GET/PATCH /projects/{projectId}`, `POST /projects/{projectId}/archive`)는 dev에 구현되어 §2.2 실행 자체는 가능하나, 진척률 100% 확정 여부는 Reviewer(여도현) 재확인 필요 |
| 3.3.4 | project_members 조회/역할 기준 skeleton 구현 | 김태준 | 황스일 | 3.3.2, 3.1.4 | **100% (완료)** | PR #70 merge(2026-07-09, `feature/wbs-3.3.4-project-members-skeleton`). **Reviewer(황스일/SEUIL)가 review(APPROVED) 후 본인이 직접 merge** — 새 규칙에 정확히 부합. `ProjectMemberController`(`GET /projects/{projectId}/project-members`, `PATCH`/`DELETE /project-members/{projectMemberId}`) dev에 구현 확인 |

진척률 갱신 규칙(2026-07-08 변경): 진행중~99%는 Owner(김태준) 본인, **100% 확정은 Reviewer가 해당 PR을 review + merge 완료한 시점**(부팀장이자 Reviewer의 직접 merge도 인정). 3.3.1은 3.1(Company/RBAC Backend)에도 의존하므로, 3.1.1~3.1.4 진행 상황도 함께 참고한다([`sprint-2-wbs-3.1.5-company-rbac-검증케이스.md`](./sprint-2-wbs-3.1.5-company-rbac-검증케이스.md) §1). **3.3.1~3.3.4가 모두 dev에 merge되어 §2 검증 케이스 20개 전체 실행 착수가 가능하다** (3.3.3의 Reviewer 승인 불일치는 별도 확인 필요 항목으로 남기되, 실행 자체를 막지는 않는다).

---

## 2. 검증 케이스

### 2.1 프로젝트 목록/생성 (3.3.2 대상, 요구사항 PROJECT-01/02)

| # | endpoint | 케이스 | 종류 | 기대 결과 | 확인 결과 | 비고 |
|---|---|---|---|---|---|---|
| 1 | `GET /api/v1/companies/{companyId}/projects` | C 역할 조회 | 정상 | 200, 회사 전체 프로젝트 목록 반환 | **정상 통과** | Apidog 실행(2026-07-10) 200 확인. PROJECT-01 |
| 2 | `GET /api/v1/companies/{companyId}/projects` | A/P 역할 조회 | 정상 | 200, 본인 소속 프로젝트만 반환 | **정상 통과** | Apidog 실행(2026-07-10) 최초 403 발생 → 원인 파악 결과 API 버그 아님. `memberAccessToken`(member1, companyMemberId=7)이 3.1.5 §2.3 #17 테스트(SUSPENDED 전환) 이후 ACTIVE로 되돌려지지 않은 채 남아있던 **테스트 데이터 오염**이 원인. `PATCH /api/v1/companies/6/company-members/7`로 status를 ACTIVE로 되돌린 뒤 재실행하여 200 확인. "A/P는 소속 프로젝트만 조회" 완료 조건 검증됨 |
| 3 | `POST /api/v1/companies/{companyId}/projects` | C 역할이 name/projectKey/status/기간으로 생성 | 정상 | 201, `project` 생성 + 생성자 `projectMember.role = PROJECT_ADMIN` | **정상 통과** | Apidog 실행(2026-07-10) 201 확인, `projectId=1`(companyId=6, projectKey=QA-1) 생성. §2.3 #15로 생성자가 PROJECT_ADMIN 1명으로 등록됨을 재확인 |
| 4 | `POST /api/v1/companies/{companyId}/projects` | A/P 역할이 생성 시도 | 실패 | 403 `FORBIDDEN` | **정상 통과** | Apidog 실행(2026-07-10) 403 확인. "생성 권한은 C" 완료 조건 검증됨 |
| 5 | `POST /api/v1/companies/{companyId}/projects` | 필수 필드 누락(name 없음) | 실패 | 400 `VALIDATION_ERROR` | **정상 통과** | Apidog 실행(2026-07-10) 400 확인 |
| 6 | `POST /api/v1/companies/{companyId}/projects` | 회사 내 중복 `projectKey`로 생성 | 실패 | 409 `CONFLICT` (정확한 코드는 구현 후 확인) | **정상 통과** | Apidog 실행(2026-07-10) — #3과 동일한 `projectKey=QA-1`로 재요청 시 409 확인(의도치 않게 #3을 재실행하면서 함께 확인됨). ERD 기준 unique 제약 정상 동작 |

> **Finding F-1 (코드 리뷰로 발견, 2026-07-10)**: `ProjectService.list()`/`create()`가 **회사 존재 확인(`company(companyId)`)을 권한 확인보다 나중에 실행**한다. `requireActiveMember`/`requireCompanyAdmin`(`CompanyMembershipService`)은 회사 존재 여부를 보지 않고 멤버십만 확인해 403을 던지므로, **존재하지 않는 companyId로 `GET/POST /companies/{companyId}/projects`를 호출하면 404가 아니라 403이 반환**된다(존재확인용 `company(companyId)` 호출이 권한 체크 뒤에 있어 실패 경로에서 도달 불가). Company API #4(PR #66)·Department 이슈([#67](https://github.com/SEUIL/Threadia/issues/67), PR #68)에서 이미 `인증→존재확인→권한확인` 순서로 통일하며 고친 것과 **동일한 패턴이 3.3.2(Project 목록/생성)에만 남아있음**. §2 케이스(#1~#6)는 존재하는 companyId 기준이라 정상 통과했고, 이 순서 문제는 Apidog 블랙박스가 아니라 코드 리뷰에서 발견됨. **참고: `detail()`/`update()`/`archive()`(3.3.3)는 `project(projectId)` 존재확인을 권한확인보다 먼저 실행해 순서가 정상이므로 이 finding 대상이 아님.** 별도 이슈로 분리해 3.3.2 owner(김태준)에게 전달함: [#73](https://github.com/SEUIL/Threadia/issues/73).
>
> **해결 완료(2026-07-11)**: 이슈 #73은 3.3.2 owner(김태준) 대상이었으나 **김민경(xaphxe)이 처리**하여 PR [#76](https://github.com/SEUIL/Threadia/pull/76)으로 `list()`/`create()`를 `인증→존재확인→권한확인` 순서로 수정(`ProjectService` `company(companyId)`를 권한확인 앞으로 이동, `ProjectServiceTest`에 순서 검증 추가), **여도현(dhy257)이 review(APPROVED)+merge**하여 dev 반영·이슈 #73 CLOSED. **재검증**: dev 최신 백엔드 재기동 후 Apidog로 `GET`/`POST /api/v1/companies/9999/projects` 실행 → 둘 다 **404 `NOT_FOUND`** 확인(수정 전 403 → 수정 후 404). Company #4(PR #66)·Department #67(PR #68)과 동일 패턴이 Project까지 정리 완료.

### 2.2 프로젝트 상세/수정/archive (3.3.3 대상, 요구사항 PROJECT-03/04)

| # | endpoint | 케이스 | 종류 | 기대 결과 | 확인 결과 | 비고 |
|---|---|---|---|---|---|---|
| 7 | `GET /api/v1/projects/{projectId}` | A/P/C 소속 사용자 조회 | 정상 | 200, 프로젝트 상세 반환 | **정상 통과** | Apidog 실행(2026-07-10) 200 확인, `projectId=1` |
| 8 | `GET /api/v1/projects/{projectId}` | 존재하지 않는 projectId | 실패 | 404 `NOT_FOUND` | **정상 통과** | Apidog 실행(2026-07-10) 404 확인, `projectId=9999` |
| 9 | `GET /api/v1/projects/{projectId}` | 소속되지 않은 프로젝트 조회 시도 | 실패 | 403 `FORBIDDEN` 또는 404 (구현 정책 확인 필요) | **정상 통과** | Apidog 실행(2026-07-10) 403 확인. `projectId=2`(QA-2, 사전 준비로 별도 생성)에 소속되지 않은 `memberAccessToken`으로 조회 시도. **구현 정책 확정: 403(존재 자체는 노출하되 접근 거부)** — 존재하지 않는 프로젝트(#8)는 404, 소속 안 된 프로젝트는 403으로 구분됨 |
| 10 | `PATCH /api/v1/projects/{projectId}` | P/C 역할이 정보 수정 | 정상 | 200, 변경 반영 | **정상 통과** | Apidog 실행(2026-07-10) 200 확인, `projectId=1` |
| 11 | `PATCH /api/v1/projects/{projectId}` | A 역할이 수정 시도 | 실패 | 403 `FORBIDDEN` | **정상 통과** | Apidog 실행(2026-07-10) 403 확인. "수정은 P/C 권한" 완료 조건 검증됨 |
| 12 | `POST /api/v1/projects/{projectId}/archive` | P/C 역할이 archive 요청 | 정상 | 200, `status = ARCHIVED` | **정상 통과** | Apidog 실행(2026-07-10) 200 확인, `projectId=1`. DB 직접 조회로 hard delete가 아님을 재확인 — row 존재, `status=ARCHIVED`, `archived_at`/`archived_by_user_id`/`archive_reason` 모두 정상 기록됨 |
| 13 | `POST /api/v1/projects/{projectId}/archive` | A 역할이 archive 시도 | 실패 | 403 `FORBIDDEN` | **정상 통과** | Apidog 실행(2026-07-10) — 최초 시도는 URL의 `-forbidden` 접미사를 지우지 않아 404(매핑 안 된 URL) 발생, 접미사 제거 후 재요청하여 403 확인. `#12`(archive)보다 먼저 실행해 상태 충돌 방지 |
| 14 | `POST /api/v1/projects/{projectId}/archive` | 이미 ARCHIVED 상태에서 재요청 | 실패 또는 idempotent (구현 정책 확인 필요) | 명세서에 명시 없음 — 구현 결과로 정책 확정 | **정상 통과(정책 확정)** | Apidog 실행(2026-07-10) 409 확인. `ProjectService.archive()`가 `status == ARCHIVED`면 `ProjectConflictException`을 던지도록 구현되어 있어, idempotent(200)가 아니라 **409 CONFLICT로 정책 확정**. 완료 조건에 없던 경계 케이스였으나 실행으로 명확히 정리됨 |

### 2.3 프로젝트 멤버 (3.3.4 대상, 요구사항 PROJECT-MEMBER-01/02)

| # | endpoint | 케이스 | 종류 | 기대 결과 | 확인 결과 | 비고 |
|---|---|---|---|---|---|---|
| 15 | `GET /api/v1/projects/{projectId}/project-members` | A/P/C 소속 사용자 조회 | 정상 | 200, 멤버 목록 반환 | **정상 통과** | Apidog 실행(2026-07-10) 200 확인, `projectId=1`에 생성자(PROJECT_ADMIN) 1명만 있는 상태 확인. **이 확인으로 프로젝트 멤버 "추가" 전용 API가 없다는 사실이 드러남** — `ProjectMemberService`에는 `list`/`update`/`remove`만 있고 `create`가 없음. `docs/04-api/API-명세서.md:886-890`에 이미 명시된 의도적 설계로 확인됨: 프로젝트 멤버 추가는 Sprint 1.5에서 user-facing API로 제공하지 않고, Sprint 3에서 `POST /projects/{projectId}/invitations` 기반 invitation 흐름으로 도입 예정. §2.3 #16/#18/#19는 이 제약 때문에 DB에 `project_members` row를 직접 seed해서 검증함(아래 비고 참고) |
| 16 | `PATCH /api/v1/project-members/{projectMemberId}` | P/C가 role/status 변경 | 정상 | 200, 변경 반영 | **정상 통과** | Apidog 실행(2026-07-10) 200 확인. **projectMemberId=2는 API가 아니라 DB에 직접 INSERT로 seed함**(companyId=6의 ACTIVE 구성원 중 프로젝트 미소속 사용자를 MEMBER/ACTIVE로 추가) — 멤버 추가 API가 없어서(#15 비고 참고) 이 케이스는 `update()` 로직 자체만 검증하는 것이며, "멤버가 어떻게 들어오는가"는 검증 범위에 포함하지 않음 |
| 17 | `PATCH /api/v1/project-members/{projectMemberId}` | 마지막 `PROJECT_ADMIN`을 `MEMBER`로 강등 시도 | 실패 | 409 `CONFLICT` 계열 (정확한 코드는 구현 후 확인) | **정상 통과** | Apidog 실행(2026-07-10) 409 확인. `projectMemberId=1`(생성자, 유일한 PROJECT_ADMIN) 대상, 별도 seed 불필요. "PROJECT_ADMIN 최소 1명 유지" 완료 조건이 수정(PATCH) 경로에서도 정상 동작함을 확인 — #19(DELETE 경로)와 함께 두 경로 모두 검증 완료 |
| 18 | `DELETE /api/v1/project-members/{projectMemberId}` | P/C가 일반 멤버 제거 | 정상 | 200, `removed: true` | **정상 통과** | Apidog 실행(2026-07-10) 200, `removed: true` 확인. #16과 동일한 seed row(`projectMemberId=2`)를 그대로 재사용해 삭제 |
| 19 | `DELETE /api/v1/project-members/{projectMemberId}` | 마지막 `PROJECT_ADMIN` 제거 시도 | 실패 | 409 `CONFLICT` 계열 (정확한 코드는 구현 후 확인) | **정상 통과** | Apidog 실행(2026-07-10) 409 확인. `projectMemberId=1`(생성자, 유일한 PROJECT_ADMIN)은 별도 seed 없이 바로 테스트 가능 — 삭제 경로에서도 "PROJECT_ADMIN 최소 1명 유지" 규칙이 정상 동작 |
| 20 | `PATCH /api/v1/project-members/{projectMemberId}` | 비활성 회사 구성원을 프로젝트에서 ACTIVE/PROJECT_ADMIN으로 지정 시도 | 실패 | 409 `CONFLICT` | **정상 통과(재검증, 2026-07-11)** | **케이스 재정의**: 신규 멤버 "추가" API는 Sprint 3 범위라 없지만, 기존 프로젝트 멤버의 회사 membership을 비활성으로 만든 뒤 `PATCH /project-members/{id}`로 ACTIVE/PROJECT_ADMIN 승격을 시도하면 `ProjectMemberService.validateTargetActiveCompanyMember()`(`isActiveMember` 검사)의 실패 경로를 검증할 수 있음(PR #74 리뷰 반영). **재검증**: user 20(projectMemberId=6)의 `company_members.status`를 SUSPENDED로 만든 뒤 `PATCH /api/v1/project-members/6` {role:PROJECT_ADMIN,status:ACTIVE} 실행 → **409 `CONFLICT`**, message "대상 사용자가 활성 회사 구성원이 아닙니다." 확인. 검증 후 회사 membership은 ACTIVE로 원상복구. "회사 구성원만 프로젝트 멤버" 완료 조건 검증됨 |

`확인 결과`는 `정상 통과 / 실패(버그) / 확인 불가` 중 하나로 표기한다. `실패(버그)`면 비고에 재현 방법과 관련 PR 링크를 남긴다.

---

## 3. DB 정합성 확인 포인트

- `projects`, `project_members` 관계가 `docs/03-design/ERD.md`와 충돌하지 않는지 확인한다. — **확인 완료(2026-07-10)**: 스키마/실행 결과 모두 ERD와 일치, 충돌 없음
- 모든 프로젝트가 `company_id`를 가지고 있어 회사에 반드시 속하는지 확인한다. — **확인 완료(2026-07-10)**: `SELECT COUNT(*) FROM projects WHERE company_id IS NULL` → 0
- archive된 프로젝트가 실제로 물리 삭제되지 않고 `status = ARCHIVED`로만 남는지 DB로 직접 확인한다 (§2.2 #12). — **확인 완료(2026-07-10)**: `projectId=1` row 존재, `status=ARCHIVED`, `archived_at`/`archived_by_user_id`/`archive_reason` 정상 기록
- `project_members`에 `PROJECT_ADMIN`이 0명인 row가 없는지 확인한다 (§2.3 #17, #19). — **확인 완료(2026-07-10)**: LEFT JOIN 쿼리로 ACTIVE PROJECT_ADMIN 없는 프로젝트 0건 확인
- secret, DB password, token은 evidence에 기록하지 않는다.

---

## 4. 검증 방법 (시트 원문: test 결과, curl/Postman evidence, DB 확인)

> **팀 방향(2026-07-07 확인, canonical 문서 미반영)**: Backend 단위 테스트/통합 테스트는 `./gradlew test`가 아니라 **Apidog collection 실행**으로 진행한다. 이 문서 §2의 개별 케이스는 Apidog request로 등록해 실행하는 것을 기본으로 한다.

1. 3.3.1~3.3.4가 merge된 시점에 §1 표를 갱신한다.
2. §2의 20개 케이스를 Apidog collection의 request로 등록하고 순서대로 실행해 결과를 기록한다.
3. §3 DB 확인 포인트를 실제 쿼리로 검증한다.
4. 결과를 종합해 Reviewer(정철진)에게 재확인을 요청한다.
5. 실패 케이스가 있으면 완료 처리를 보류하고 owner(김태준)에게 후속 조치를 요청한다.
