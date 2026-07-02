> **작성자**: Threadia 팀  
> **작성일**: 2026-07-02  
> **최종 수정**: 2026-07-02  
> **버전**: v0.1  
> **상태**: 작성 중  
> **태그**: sprint-2, plan, company, project, rbac

# Sprint 2 상세계획

## 1. 문서 목적

이 문서는 Sprint 2 착수 전 review를 위한 상세계획 초안이다. WBS 편집 전에 팀이 범위, 일정, 의존성, 제외 범위를 확인할 수 있도록 작성한다.

이 단계에서는 WBS와 Google Sheets를 수정하지 않는다. `docs/02-requirements/`, `docs/03-design/`, `docs/04-api/`, `docs/00-project/`, `docs/07-decisions/`의 canonical 확정 문서는 참조만 하며 수정하지 않는다.

## 2. Sprint 2 목표

Sprint 2의 목표는 Sprint 1에서 만들어진 Spring Boot, MySQL, Frontend route skeleton을 바탕으로 회사 workspace와 프로젝트 기반 기능을 실제 API, DB, 화면 흐름으로 연결 가능한 상태까지 끌어올리는 것이다.

확정 범위는 회사, 부서, 회사 구성원, RBAC, 프로젝트 기반구축이다. Minimum Issue/Kanban은 capacity candidate일 뿐이며, Sprint 2 committed scope로 보지 않는다.

## 3. 현재 코드 상태 제약

| 영역 | 현재 상태 | Sprint 2 계획에 주는 제약 |
|---|---|---|
| Backend | `backend/build.gradle`은 Spring Boot 3.5.16, Java 17, JDBC, web, validation, actuator, MySQL connector 기반이다 | JPA 기반 완성 구현을 전제로 쓰지 않고, 현재 skeleton에서 API, service, repository, JDBC 또는 이후 합의된 persistence 방식을 명확히 정해야 한다 |
| Backend application | `BackendApplication.java`는 skeleton only 상태다 | 도메인 package, error handling, 인증 context, 권한 검사 구조를 Sprint 2 초반에 먼저 잡아야 한다 |
| Backend test | `MysqlConnectionTests`는 `THREADIA_DB_PASSWORD`가 있을 때만 MySQL 8.4 연결을 확인한다 | DB 검증은 secret을 문서에 남기지 않고, 환경 변수 존재 여부에 따라 실행 가능성을 분리해 기록해야 한다 |
| Frontend | hub, projects, members, settings, backlog, board, docs, messages, notifications, threads, dm, Auth 관련 route skeleton이 있다 | route 존재를 기능 완료로 보지 않고, API 연동, 상태 표시, 권한별 화면 노출까지 별도 완료 조건으로 둔다 |
| Sprint 2 domain API | 현재 직접 확인 기준으로 Auth service API reference 외 Sprint 2 domain API behavior는 구현되어 있지 않다 | 회사, 부서, 구성원, 프로젝트, RBAC API를 새 구현 대상으로 계획해야 한다 |

현재 code state constraint는 backend가 skeleton, JDBC, MySQL connection test only 상태이고, frontend가 후속 화면 route skeleton을 가진 상태라는 점이다. Sprint 2 domain API와 DB feature behavior는 아직 구현되어 있지 않다.

## 4. 확정 범위

| WBS 개념 | 확정 범위 | 목표 결과 |
|---|---|---|
| 3.1 Company/RBAC Backend | 회사, 부서, 회사 구성원, company membership role, 기본 RBAC 검사 | ERD와 API 명세 기준의 company domain API가 local에서 검증 가능하다 |
| 3.2 Company Frontend/Entry | 회사 관리, 구성원 관리, workspace 진입 흐름 | C 역할 사용자가 회사 관리 화면과 구성원 화면에 진입하고 기본 상태를 확인할 수 있다 |
| 3.3 Project Backend | 프로젝트 생성, 목록, 상세, 수정, archive, project membership 기반 | 회사 하위 프로젝트를 API로 생성, 조회, 수정, archive할 수 있다 |
| 3.4 Project Frontend | 프로젝트 목록, 프로젝트 상세 또는 설정 진입, 프로젝트 생성 entry | 화면정의서 S04, S12 기준으로 프로젝트 기반 화면 흐름을 확인할 수 있다 |
| 3.6 CI/Deploy/QA | Sprint 2 변경 범위의 test, build, CI, QA evidence 정리 | Backend, Frontend, 문서 변경이 Definition of Done과 검증 방법 기준을 따른다 |

Sprint 2 confirmed scope는 company, department, company-members, RBAC and project foundation이다. 이 범위는 Foundation + candidate 옵션의 Foundation에 해당한다.

## 5. Candidate 범위

| WBS 개념 | Candidate 항목 | 착수 조건 | Sprint 2 commitment 여부 |
|---|---|---|---|
| 3.5 Issue/Kanban Candidate | Minimum Issue/Kanban, 5컬럼 상태, 이슈 목록 또는 카드 상태 변경의 최소 vertical slice | 3.1부터 3.4까지의 company/project foundation이 검증되고 team capacity가 남는 경우 | committed scope가 아니다 |

Minimum Issue/Kanban은 capacity candidate only다. 일정 여유가 생겨도 WBS 또는 팀 합의 없이 Sprint 2 확정 범위로 승격하지 않는다.

## 6. 명시적 제외 범위

| 제외 항목 | 제외 이유 |
|---|---|
| WBS 또는 Google Sheets 수정 | 사용자가 현 단계에서 WBS를 수정하지 말라고 명시했다 |
| canonical 확정 문서 수정 | 이 문서는 Sprint 2 planning draft이며, 확정 문서는 reference only다 |
| `sprint-2-해야할일.md` | Sprint 2 문서 수를 늘리지 않기 위해 생성하지 않는다 |
| `sprint-2-프로젝트-기반구축.md` | 프로젝트 기반구축은 이 상세계획과 backlog 안에서 다룬다 |
| 별도 FE/BE DoD 문서 | canonical `Definition-of-Done.md`와 `검증-방법.md`를 기준으로 사용한다 |
| Minimum Issue/Kanban 확정 구현 | WBS 3.5는 candidate이며 Sprint 2 committed scope가 아니다 |
| 문서, 채팅, DM, 쓰레드, 알림 본구현 | Sprint 2 foundation 범위를 넘어선다 |
| 플랫폼 관리자 F/S15 | 보류 기능이며 이번 Foundation + candidate 범위에 포함하지 않는다 |
| 미가입 사용자 초대, invitation email 발송, token link, resend | Sprint 1.5 및 보류 기능 기준에서 제외 또는 후보로 관리된다 |
| implementation code change | 이 문서 작업은 planning 문서 생성만 수행한다 |

## 7. 일정과 운영 원칙, 2026-07-10부터 2026-07-21까지

| 기간 | 운영 초점 | 산출물 기준 |
|---|---|---|
| 2026-07-10 | Sprint 2 kickoff, scope freeze, current code baseline 확인 | 확정 범위와 candidate 경계 재확인, WBS는 read only |
| 2026-07-13부터 2026-07-14까지 | Company/RBAC Backend와 Company Frontend entry 우선 진행 | 회사, 부서, 구성원, 권한 검사 skeleton과 화면 entry 검증 |
| 2026-07-15부터 2026-07-16까지 | Project Backend와 Project Frontend foundation 진행 | 프로젝트 생성, 목록, 상세, 수정, archive 흐름의 최소 검증 |
| 2026-07-17 | 통합, 권한별 접근, API와 화면 연결 확인 | C, P, A 역할별 접근 기준과 실패 케이스 점검 |
| 2026-07-20 | QA, CI, 문서 evidence 정리, candidate 착수 여부 판단 | committed scope 검증이 먼저 통과해야 candidate 검토 가능 |
| 2026-07-21 | Sprint 2 review 준비, 미완료와 blocker 정리 | 완료, 미완료, candidate 미착수 사유를 분리해 기록 |

운영 원칙은 다음과 같다.

1. Foundation 먼저 진행한다.
2. company와 project domain의 API, DB, 화면 기준을 먼저 맞춘 뒤 candidate를 판단한다.
3. 각 backlog item은 owner, reviewer, support 기준으로 review 가능해야 한다.
4. secret, DB password, token, private data는 문서와 evidence에 기록하지 않는다.
5. 변경 검증은 canonical DoD와 검증 방법 문서를 따른다.

## 8. 주요 의존성과 blocker

| 항목 | 의존성 | blocker | 대응 |
|---|---|---|---|
| Company/RBAC Backend | ERD의 `companies`, `departments`, `company_members`, API 명세의 회사/부서/구성원 endpoint | 현재 backend가 skeleton이라 공통 error, auth context, persistence 구조가 비어 있다 | Sprint 2 초반에 package 구조와 최소 공통 응답, 권한 검사 기준을 먼저 정한다 |
| Company Frontend | 화면정의서 S13, S14와 API 응답 형식 | route skeleton은 있지만 API behavior가 없다 | mock 완료로 처리하지 않고 실제 API 연동 또는 명확한 미구현 사유를 기록한다 |
| Project Backend | 회사와 company membership, project role | 프로젝트는 회사 소속과 RBAC에 의존한다 | 회사 기반 API와 seed 또는 테스트 데이터 준비 후 진행한다 |
| Project Frontend | S04, S12 화면과 project API | 권한별 버튼 노출과 진입 조건이 API와 맞아야 한다 | C, P, A 역할별 route와 action 노출 기준을 review한다 |
| Candidate Issue/Kanban | 프로젝트와 project membership | Foundation 미완료 시 candidate 착수 불가 | 2026-07-20에 capacity를 보고 착수 여부를 판단한다 |
| CI/Deploy/QA | Backend, Frontend script와 환경 변수 | DB password가 없으면 MySQL 연결 test가 skip될 수 있다 | skip과 pass를 구분해 evidence에 남긴다 |

## 9. 참조 문서

| 문서 | 사용 목적 |
|---|---|
| `docs/02-requirements/기능-분해표.md` | 기능 ID와 MVP, 후속 sprint 구현 범위 확인 |
| `docs/02-requirements/기능-시나리오-API-ERD-추적표.md` | company, project, issue, invitation의 API와 ERD 추적성 확인 |
| `docs/03-design/ERD.md` | company, department, company_members, projects, project_members, invitations 관계 확인 |
| `docs/03-design/화면정의서.md` | S03, S04, S12, S13, S14 화면 기준 확인 |
| `docs/04-api/API-명세서.md` | API method, endpoint, enum, error 기준 확인 |
| `docs/00-project/Definition-of-Done.md` | 완료 조건 기준 확인 |
| `docs/00-project/검증-방법.md` | test, build, API, 화면, DB, 문서 검증 방식 확인 |
| `docs/07-decisions/보류-기능-최종표.md` | 보류, 조건부 확장, 제외 범위 확인 |
