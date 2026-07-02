> **작성자**: Threadia 팀  
> **작성일**: 2026-07-02  
> **최종 수정**: 2026-07-02  
> **버전**: v0.1  
> **상태**: 작성 중  
> **태그**: sprint-2, backlog, company, project, rbac

# Sprint 2 Backlog

## 1. 문서 목적

이 문서는 Sprint 2 review 전 backlog 초안이다. WBS row concept 3.1, 3.2, 3.3, 3.4, 3.6은 confirmed backlog로 정리하고, WBS row concept 3.5는 candidate backlog로 분리한다.

이 단계에서는 WBS와 Google Sheets를 수정하지 않는다. canonical 확정 문서인 요구사항, ERD, 화면정의서, API 명세서, DoD, 검증 방법, 보류 기능 최종표는 reference only로 사용하며 수정하지 않는다.

## 2. Backlog 작성 원칙

1. Sprint 2 confirmed scope는 company, department, company-members, RBAC and project foundation이다.
2. Minimum Issue/Kanban은 capacity candidate only이며 committed Sprint 2 scope가 아니다.
3. 현재 backend는 skeleton, JDBC, MySQL connection test only 상태이고, Sprint 2 domain API와 DB feature behavior는 구현되어 있지 않다.
4. 현재 frontend는 후속 화면을 위한 route skeleton을 가지고 있지만, Sprint 2 domain API behavior는 구현되어 있지 않다.
5. 완료 조건은 새 DoD 문서를 만들지 않고 `docs/00-project/Definition-of-Done.md`를 따른다.
6. 검증 방법은 새 검증 문서를 만들지 않고 `docs/00-project/검증-방법.md`를 따른다.
7. secret, credential, password, token, private data는 backlog와 evidence에 기록하지 않는다.

## 3. Confirmed backlog

### 3.1 Company/RBAC Backend

| 항목 | 내용 |
|---|---|
| 상태 | confirmed |
| Owner | 여도현 |
| Reviewer | 황스일 |
| Support | 여도현, 김민정, 김민경 |
| 기준 WBS 개념 | 3.1 Company/RBAC Backend |
| 기준 문서 | `기능-분해표.md`, `기능-시나리오-API-ERD-추적표.md`, `ERD.md`, `API-명세서.md`, `Definition-of-Done.md`, `검증-방법.md` |

**작업 범위**

| 세부 항목 | 설명 |
|---|---|
| 회사 API | `POST /api/v1/companies`, `GET/PATCH /api/v1/companies/{companyId}`, 회사 통계 조회의 구현 가능 범위를 정리하고 local 검증한다 |
| 부서 API | 부서 목록, 생성, 수정, 삭제와 멤버 `department_id = NULL` 처리 기준을 구현 또는 구현 계획과 blocker로 분리한다 |
| 회사 구성원 API | company members 목록, 역할/부서/상태 변경 기준을 구현한다 |
| RBAC | `users.role`을 만들지 않고 `company_members.role` 기준으로 C와 일반 구성원 접근을 구분한다 |
| 공통 API 기반 | skeleton 상태에서 request validation, error response, auth context 접근 방식의 최소 기준을 잡는다 |

**완료 조건**

- ERD의 `companies`, `departments`, `company_members`, `users` 관계와 충돌하지 않는다.
- 회사와 부서, 구성원 API의 정상 케이스와 주요 실패 케이스를 curl, Postman, test 중 하나로 확인한다.
- `users.role` 필드를 새로 만들지 않고 company membership 기반 권한을 사용한다.
- DB password, token, connection string은 evidence에 기록하지 않는다.

**검증 방법**

- Backend 변경 시 `./gradlew test`를 우선 실행한다.
- API 추가 시 정상, validation 실패, 권한 부족, 리소스 없음 케이스를 확인한다.
- DB 변경 시 ERD, `db/schema.sql`, 실제 query 또는 조회 API 기준으로 정합성을 확인한다.

**의존성**

- Auth context와 현재 사용자 식별 방식이 필요하다.
- local MySQL 8.4 또는 test DB 접근 방식이 필요하다.
- 현재 backend skeleton에 domain package 구조를 추가해야 한다.

### 3.2 Company Frontend/Entry

| 항목 | 내용 |
|---|---|
| 상태 | confirmed |
| Owner | 정철진 |
| Reviewer | 김민정 |
| Support | TBD |
| 기준 WBS 개념 | 3.2 Company Frontend/Entry |
| 기준 문서 | `화면정의서.md`, `API-명세서.md`, `Definition-of-Done.md`, `검증-방법.md` |

**작업 범위**

| 세부 항목 | 설명 |
|---|---|
| 회사 entry | S03에서 회사 관리 또는 workspace entry로 진입하는 흐름을 정리한다 |
| S13 회사 관리 | 회사 정보 생성 또는 관리 모드의 Sprint 2 적용 범위를 구현한다 |
| S14 구성원 관리 | 회사 구성원 목록, 역할, 부서, 초대 상태 표시의 Sprint 2 적용 범위를 구현한다 |
| 권한별 UI | A, P, C 역할별 메뉴 노출과 접근 제한을 API 기준과 맞춘다 |
| 상태 처리 | loading, empty, error, forbidden 상태를 최소한으로 구분한다 |

**완료 조건**

- route skeleton 존재만으로 완료 처리하지 않는다.
- C 역할 사용자가 회사 관리와 구성원 관리 화면에 진입할 수 있다.
- 권한이 없는 사용자의 접근 또는 액션 제한이 화면에서 확인된다.
- API 미완료로 인해 mock을 쓰는 경우, mock 범위와 실제 API 전환 조건을 기록한다.

**검증 방법**

- Frontend 변경 시 `npm run dev` 또는 프로젝트에서 합의된 script로 route와 화면을 확인한다.
- 가능하면 `npm run lint`, `npm run typecheck`, `npm run build`를 실행하고, script가 없거나 실패하면 사유를 남긴다.
- 브라우저 기준 route, 사용자 역할, 확인 결과를 evidence로 남긴다.

**의존성**

- Company/RBAC Backend API 응답 형식이 필요하다.
- 화면정의서 S03, S13, S14 기준과 API 명세서의 endpoint가 필요하다.

### 3.3 Project Backend

| 항목 | 내용 |
|---|---|
| 상태 | confirmed |
| Owner | 김태준 |
| Reviewer | 여도현 |
| Support | 김태준, 김민경 |
| 기준 WBS 개념 | 3.3 Project Backend |
| 기준 문서 | `기능-분해표.md`, `기능-시나리오-API-ERD-추적표.md`, `ERD.md`, `API-명세서.md`, `Definition-of-Done.md`, `검증-방법.md` |

**작업 범위**

| 세부 항목 | 설명 |
|---|---|
| 프로젝트 목록 | `GET /api/v1/companies/{companyId}/projects` 기준으로 C는 회사 전체, A/P는 소속 프로젝트만 조회한다 |
| 프로젝트 생성 | `POST /api/v1/companies/{companyId}/projects`는 C 권한 기준으로 구현한다 |
| 프로젝트 상세와 수정 | `GET/PATCH /api/v1/projects/{projectId}` 기준으로 프로젝트 기본 정보를 처리한다 |
| 프로젝트 archive | 물리 삭제가 아니라 `ARCHIVED` 상태로 처리한다 |
| project membership 기반 | `project_members.role` 기준의 P/C 권한 분리를 준비한다 |

**완료 조건**

- 프로젝트는 반드시 회사에 속한다.
- 프로젝트 삭제는 hard delete가 아니라 archive 기준을 따른다.
- project member와 company member 관계가 ERD 기준과 충돌하지 않는다.
- project invitation은 필요한 경우 Sprint 2 reference로만 다루며, 완료 조건에 넣을지는 별도 review에서 결정한다.

**검증 방법**

- Backend 변경 시 `./gradlew test`를 실행한다.
- 프로젝트 생성, 목록, 상세, 수정, archive 정상 케이스를 API로 확인한다.
- 권한 부족과 존재하지 않는 companyId, projectId 실패 케이스를 확인한다.

**의존성**

- Company/RBAC Backend가 먼저 동작해야 한다.
- 회사 구성원과 프로젝트 멤버 기준 데이터가 필요하다.
- Auth context에서 현재 사용자의 company membership을 확인할 수 있어야 한다.

### 3.4 Project Frontend

| 항목 | 내용 |
|---|---|
| 상태 | confirmed |
| Owner | 정철진 |
| Reviewer | 김민정 |
| Support | TBD |
| 기준 WBS 개념 | 3.4 Project Frontend |
| 기준 문서 | `화면정의서.md`, `API-명세서.md`, `Definition-of-Done.md`, `검증-방법.md` |

**작업 범위**

| 세부 항목 | 설명 |
|---|---|
| S04 프로젝트 목록 | 프로젝트 카드, 상태, 멤버 수, PM 등 표시 가능한 필드를 API 기준으로 연결한다 |
| 프로젝트 생성 entry | C 권한 사용자에게만 새 프로젝트 생성 entry를 노출한다 |
| S12 프로젝트 설정 | 프로젝트 상세, 수정, archive, project member entry의 최소 흐름을 구현한다 |
| 권한별 UI | A는 새 프로젝트 생성 버튼을 보지 않고, P/C 액션과 조회 액션을 구분한다 |
| API 상태 | loading, empty, error, forbidden 상태를 화면에서 구분한다 |

**완료 조건**

- S04와 S12 기준 흐름이 브라우저에서 확인된다.
- route skeleton만 있는 상태를 완료로 보지 않는다.
- 프로젝트 API가 미완료이면 mock 범위와 실제 API 전환 조건을 기록한다.
- 프로젝트 홈 또는 대시보드는 조건부 확장 reference이며 이번 confirmed scope의 필수 완료 조건이 아니다.

**검증 방법**

- Frontend route와 화면을 브라우저에서 확인한다.
- API 성공, empty, forbidden 상태를 가능한 범위에서 확인한다.
- script가 준비되어 있으면 lint, typecheck, build를 실행한다.

**의존성**

- Project Backend API 응답 형식이 필요하다.
- Company Frontend entry와 회사 선택 context가 필요하다.

### 3.6 CI/Deploy/QA

| 항목 | 내용 |
|---|---|
| 상태 | confirmed |
| Owner | 황스일 |
| Reviewer | 김민정 |
| Support | TBD |
| 기준 WBS 개념 | 3.6 CI/Deploy/QA |
| 기준 문서 | `Definition-of-Done.md`, `검증-방법.md`, `CONTRIBUTING.md` |

**작업 범위**

| 세부 항목 | 설명 |
|---|---|
| Backend 검증 | Sprint 2 backend 변경에 대해 `./gradlew test`와 필요한 경우 build를 확인한다 |
| Frontend 검증 | route, 화면, lint, typecheck, build 중 현재 script와 변경 범위에 맞는 검증을 정리한다 |
| API QA | company, department, company member, project API 정상과 실패 케이스 evidence를 모은다 |
| 문서 QA | Sprint 2 문서가 canonical docs와 충돌하지 않는지 확인한다 |
| CI/Deploy | CI는 변경 범위 검증 중심으로 두고, 실제 cloud deploy나 shared infra 변경은 별도 승인 전까지 수행하지 않는다 |

**완료 조건**

- 각 confirmed backlog item별 owner가 실행한 검증 결과를 reviewer가 재확인할 수 있다.
- test 또는 build 미실행 항목은 사유와 후속 조치를 기록한다.
- DB, cloud, secret, shared infra 변경은 승인 필요 작업으로 분리한다.
- WBS와 Google Sheets를 수정하지 않았음을 확인한다.

**검증 방법**

- 문서 변경은 메타정보, 링크, 문체, 충돌 여부를 확인한다.
- 코드 변경은 해당 영역의 test, build, API, 화면 검증을 수행한다.
- secret 노출 여부를 확인한다.

**의존성**

- 각 feature owner의 evidence 제출이 필요하다.
- 현재 프로젝트 script와 CI workflow 상태 확인이 필요하다.

## 4. Candidate backlog

### 3.5 Issue/Kanban Candidate

| 항목 | 내용 |
|---|---|
| 상태 | candidate |
| Owner | 김민경 |
| Reviewer | 여도현 |
| Support | 김민경, 김태준 |
| 기준 WBS 개념 | 3.5 Issue/Kanban Candidate |
| 기준 문서 | `기능-분해표.md`, `기능-시나리오-API-ERD-추적표.md`, `ERD.md`, `화면정의서.md`, `API-명세서.md`, `보류-기능-최종표.md` |

**작업 후보 범위**

| 세부 항목 | 설명 |
|---|---|
| Minimum Issue | 프로젝트 하위 이슈 목록과 생성의 최소 vertical slice를 검토한다 |
| Minimum Kanban | BACKLOG, TODO, IN_PROGRESS, IN_REVIEW, DONE 5컬럼 상태를 기준으로 최소 조회 또는 상태 변경을 검토한다 |
| 권한 | A는 DONE 이동 불가, P/C는 전체 상태 이동 가능 기준을 확인한다 |
| 제외 유지 | 칸반 컬럼 커스터마이징은 보류로 유지한다 |

**착수 조건**

- 3.1부터 3.4까지의 Foundation confirmed backlog가 review 가능한 수준으로 통과해야 한다.
- Project Backend와 Project Frontend의 최소 flow가 검증되어야 한다.
- 2026-07-20 기준 capacity가 남아야 한다.
- WBS 또는 팀 합의 없이 committed scope로 승격하지 않는다.

**완료 조건 후보**

- 이 항목은 Sprint 2 committed scope가 아니므로 완료 조건도 후보 기준이다.
- 착수한다면 최소 이슈 목록 또는 상태 변경 중 하나를 API와 화면에서 확인한다.
- 이슈 휴지통, 문서 연결, from-message, thread, notification은 candidate 최소 범위에서 제외한다.

**검증 방법 후보**

- Backend는 `./gradlew test`와 이슈 API 정상, 실패 케이스를 확인한다.
- Frontend는 S05 또는 관련 route에서 최소 칸반 표시와 상태를 브라우저로 확인한다.
- 미착수 시에는 미착수 사유를 Sprint review에 기록한다.

## 5. DoD와 검증 기준

Sprint 2는 별도 DoD 문서를 만들지 않는다. 완료 판정은 `docs/00-project/Definition-of-Done.md`를 기준으로 한다.

검증 방법도 별도 문서를 만들지 않는다. Backend test, build, API 확인, DB 확인, Frontend route와 화면 확인, 문서 검증은 `docs/00-project/검증-방법.md`를 기준으로 한다.

| 작업 유형 | Sprint 2 적용 기준 |
|---|---|
| Backend | 변경 범위에 맞는 test, API 정상과 실패 케이스, DB 정합성을 확인한다 |
| Frontend | route skeleton이 아니라 사용자 흐름, API 상태, 권한별 UI를 확인한다 |
| DB | ERD와 `db/schema.sql`, 실제 저장과 조회 기준을 비교한다 |
| 문서 | 메타정보, 링크, `~다` 체, canonical docs와의 충돌 여부를 확인한다 |
| PR/QA | owner, reviewer, support, 검증 결과, 미확인 항목을 남긴다 |

## 6. 제외하거나 만들지 않는 문서

| 문서 | 처리 | 이유 |
|---|---|---|
| `sprint-2-해야할일.md` | 생성하지 않는다 | Sprint 2 문서 수를 최소화하고 backlog 문서에서 작업 항목을 관리한다 |
| `sprint-2-프로젝트-기반구축.md` | 생성하지 않는다 | 프로젝트 기반구축은 `sprint-2-상세계획.md`와 이 backlog 안에 포함한다 |
| `sprint-2-백엔드-dod.md` | 생성하지 않는다 | backend 완료 기준은 canonical `Definition-of-Done.md`를 따른다 |
| `sprint-2-프론트엔드-dod.md` | 생성하지 않는다 | frontend 완료 기준은 canonical `Definition-of-Done.md`를 따른다 |

## 7. 참조 문서

| 문서 | 사용 목적 |
|---|---|
| `docs/02-requirements/기능-분해표.md` | 기능 ID와 Sprint 2 후보, 보류 경계 확인 |
| `docs/02-requirements/기능-시나리오-API-ERD-추적표.md` | 기능, API, ERD, 화면 추적성 확인 |
| `docs/03-design/ERD.md` | DB table과 관계 기준 확인 |
| `docs/03-design/화면정의서.md` | S03, S04, S05, S12, S13, S14 화면 기준 확인 |
| `docs/04-api/API-명세서.md` | endpoint, enum, error, request와 response 기준 확인 |
| `docs/00-project/Definition-of-Done.md` | 완료 판정 기준 확인 |
| `docs/00-project/검증-방법.md` | 실행 검증 방법 확인 |
| `docs/07-decisions/보류-기능-최종표.md` | candidate와 제외 범위 확인 |
