> **작성자**: Threadia 팀
> **작성일**: 2026-07-07
> **최종 수정**: 2026-07-17
> **버전**: v0.9 WBS 4.8.1 Notification Backend / 4.8.2 Frontend 실행 분리 반영
> **상태**: 검토 중
> **태그**: sprint-3, backlog, wbs, platform-admin, extension

# Sprint 3 Backlog 초안

## 1. 문서 목적

이 문서는 Sprint 3에서 기존 `WBS 4.x Backlog Development`를 실행 가능한 Sprint 3 backlog로 재편하기 위한 초안이다.

현재 문서는 Gate 1 요구사항추적표 scope/status/boundary, Gate 2 canonical 문서 반영, Gate 3 Google Sheets WBS 4.x rewrite, Gate 4 요구사항추적표 WBS ID backfill, 그리고 선택 parent의 Level 3 실행 row 추가가 완료된 상태를 기준으로 한다.

## 2. 작성 원칙

1. 기간, capacity, Owner, Reviewer, 지원 담당은 일단 공란으로 둔다.
2. 기존 `4.x`를 Sprint 3 root와 병목 방지형 Level 2 parent row로 재작성한다.
3. 너무 작은 작업 row로 과분할하지 않는다. WBS row는 PR, 문서 변경 묶음, API/화면 vertical slice, QA gate처럼 검증 가능한 단위로 둔다.
4. Level 3 row는 `4.2`, `4.7`, `4.8`, `4.9`, `4.10`, `4.11`, `4.12`, `4.13`처럼 decision, 정책, 구현, 검증 순서가 실제로 갈리는 parent에만 둔다.
5. 쉬운 작업은 Owner 1명만 배정하고 지원 담당은 공란으로 둔다. 어려운 작업이나 cross-domain 작업에만 지원 담당 1명을 둔다.
6. `decision-needed` 항목은 구현 row와 분리한다.
7. `선행 WBS ID`, `의존 유형`, `착수 가능 조건`, `병렬 그룹`, `의존성`을 채워 팀원이 자율적으로 착수 여부를 판단할 수 있게 한다.
8. 같은 병렬 그룹의 row는 동시에 착수 가능해야 한다. Gate row, backend/API row, frontend row, QA row를 같은 그룹으로 섞지 않는다.
9. S15는 `3.7~3.7.5` Sprint 2 planning gate와 Go/No-Go handoff를 선행 기준으로 두고, Sprint 3 `4.10`에서 primary 구현/결정 row로 추적한다.
10. 동시 문서 편집은 optimistic locking을 기본 권장안으로 두되, 후속 변경 가능성을 명시한다.
11. 다자간 DM과 상대 read receipt는 이번 개발 기간에 보류하고, 향후 재검토 전까지 1:1 DM 범위만 유지한다.
12. 알림 primary WBS 매핑은 `NOTI-01`을 `4.8`에 둔다. `NOTI-02/03`은 현재 계약/reference row로 유지하고, `PROJECT_NOTIFICATION_SCOPE_DEFERRED`는 API boundary code로 관리한다.
13. 검색과 첨부는 기술 정책 결정 row를 선행한다.
14. project restore와 issue/document permanent delete는 포함하되, 정책 결정 row를 선행한다.

## 3. WBS 4.x parent 재구성 요약

아래 표는 Gate 3 완료 기준 Google Sheets `WBS`의 `A:Z` 컬럼 중 Sprint 3 parent row에서 채운 핵심 metadata를 요약한다. Level 3 실행 row는 4장에서 별도로 관리한다.

| WBS ID | Level | 대분류 | 작업명 | 선행 WBS ID | 의존 유형 | 착수 가능 조건 | 병렬 그룹 | 상태 | 우선순위 | 확정성 |
|---|---:|---|---|---|---|---|---|---|---|---|
| 4 | 1 | Full Backlog / Platform / Extension | Sprint 3 전체 Backlog 구축 | 3.6.4, 3.7.5 | Gate | Sprint 2 review와 S15 planning gate 결과 확인 | S3-Root | 계획 | P0 | Sprint 3 scope 후보 |
| 4.1 | 2 | Scope / Docs / WBS | Sprint 3 범위 결정과 기준선 동기화 | 3.6.3, 3.7.4 | Gate | Sprint 3 scope proposal 합의 | S3-Gate | 계획 | P0 | decision-needed |
| 4.2 | 2 | Profile / Hub | 프로필 수정과 완성형 S03/S04 context | 3.1, 3.2, 3.3, 3.4, 4.1 | API+Frontend | workspace context와 project foundation 기준 확인 | S3-Foundation-UX | 계획 | P1 | Sprint 3 후보 |
| 4.3 | 2 | Project Invitation | Project invitation과 project member accept 흐름 | 3.1, 3.3, 3.4, 4.1 | API+Frontend | company/project membership 기준과 invitation 정책 확인 | S3-Collaboration-A | 계획 | P0 | Sprint 3 후보 |
| 4.4 | 2 | Sprint / Issue / Kanban | Sprint 운영과 이슈/칸반 전체 흐름 | 3.3, 3.4, 3.5.1, 4.1 | API+Frontend | Project foundation과 Issue/Kanban candidate 결과 확인 | S3-Core-Board | 계획 | P0 | Sprint 3 후보 |
| 4.5 | 2 | Message / Link | 메시지→이슈와 문서 이벤트 알림 연결 | 4.4, 4.6, 4.7 | Cross-domain | Issue, Document, Chat 최소 저장 흐름 확인 | S3-Linking | 계획 | P1 | Sprint 3 후보 |
| 4.6 | 2 | Documents / Graph | 문서·회의록, Markdown editor, 그래프 | 3.3, 4.1 | API+Frontend | Project foundation과 문서/그래프 canonical 반영 기준 확인 | S3-Docs | 계획 | P0 | Sprint 3 후보 |
| 4.7 | 2 | Thread / Chat / DM | 쓰레드, 프로젝트 채팅, 1:1 DM | 3.1, 3.3, 4.1 | API+Frontend | project message와 1:1 DM 기준 확인 | S3-Communication | 계획 | P0 | Sprint 3 후보 |
| 4.8 | 2 | Notification | 알림 core | 4.3, 4.4, 4.6, 4.7 | Backend+Frontend | 기존 알림 DDL을 사용하는 inbox/read-state core를 먼저 구현하고 producer 연계는 별도 정책 승인 후 진행 | S3-Events | 진행 중 | P0 | Sprint 3 후보 |
| 4.9 | 2 | Conditional Extensions | activity feed, search, dashboard | 4.4, 4.6, 4.8 | Decision+API | MySQL 우선 검색 방식과 dashboard/feed source 확인 | S3-Extensions | 계획 | P1 | decision-needed |
| 4.10 | 2 | S15 Platform Admin | S15 상세 확정과 구현 | 3.7.1~3.7.5, 3.1~3.4, 4.1 | Implementation+Decision | `3.7~3.7.5` planning gate와 Go/No-Go handoff 확인 | S3-Platform | 계획 | P1 | Sprint 3 primary row |
| 4.11 | 2 | Attachment | S3 presigned attachment 정책 결정과 구현 | 4.4, 4.6, 4.7 | Decision+Implementation | 대상 범위, 권한, 크기, MIME, 만료 기준 결정 | S3-Attachment | 계획 | P1 | decision-needed |
| 4.12 | 2 | Advanced Document Editing | optimistic locking 기반 문서 충돌 처리 | 4.6 | Decision+Implementation | document version/revision 정책 결정 | S3-Docs | 계획 | P1 | decision-needed |
| 4.13 | 2 | Restore / Permanent Delete | project restore와 issue/document permanent delete | 4.4, 4.6, 4.10 | Policy+Implementation | restore/delete 권한, 보존 기간, 참조 처리, audit 기준 결정 | S3-Retention | 계획 | P2 | decision-needed |
| 4.14 | 2 | Integration QA | Sprint 3 통합 QA와 문서 정합성 gate | 4.2~4.13 | QA Gate | feature evidence와 문서/WBS/추적표 변경 반영 완료 | S3-QA | 계획 | P0 | QA gate |

## 4. 선택적 Level 3 실행 row

Level 3 row는 전체 `4.x`를 기계적으로 쪼갠 것이 아니라, 실제 착수 순서와 검증 기준이 달라지는 parent에만 추가한다. `4.2.1`과 `4.2.2`는 각각 Backend API와 그 API에 의존하는 Frontend 계획 범위를 분리한다. 요구사항추적표 primary WBS ID는 child row가 아니라 parent `4.2`를 유지한다.

| Parent | Level 3 row | 작업명 | 성격 |
|---|---|---|---|
| `4.2` | `4.2.1` | Backend: `PATCH /api/v1/users/me/profile` partial nickname/avatarUrl 수정 지원, 기존 `GET /api/v1/auth/me`와 `GET /api/v1/users/me/company-memberships`는 분리된 verified context contract로 유지 | Backend-only, `4.1` 선행, Sprint 3 후보 |
| `4.2` | `4.2.2` | Frontend: mock role 제거, 프로필 UI, S03/S04 API 연동, 브라우저 검증 | Frontend-only, `4.2.1` 의존, planned only, 0% |
| `4.7` | `4.7.1` | thread core 구현 | Sprint 3 후보 |
| `4.7` | `4.7.2` | project chat 구현 | Sprint 3 후보 |
| `4.7` | `4.7.3` | 1:1 DM 구현 | Sprint 3 후보 |
| `4.7` | `4.7.4` | group DM/read receipt 이번 개발 기간 보류 기록 | 보류, 향후 재검토 |
| `4.8` | `4.8.1` | Backend: 기존 `notification_events`, `notification_recipients` 기반 inbox 목록, unread count, 개별 read, 조건부 read-all 구현 | Backend-only, 진행 중, producer/preferences/push 미포함 |
| `4.8` | `4.8.2` | Frontend: S11 알림 inbox UI와 WBS 4.8.1 API 연동 | Frontend-only, `4.8.1` 의존, planned only, 0%, Owner 정철진 |
| `4.9` | `4.9.1` | activity feed 최소 구현 | Sprint 3 후보 |
| `4.9` | `4.9.2` | search 방식 결정 | decision-needed |
| `4.9` | `4.9.3` | dashboard aggregate 구현 | Sprint 3 후보 |
| `4.10` | `4.10.1` | support cases 구현 범위 확정/구현 | Sprint 3 후보 |
| `4.10` | `4.10.2` | user/company enforcement 구현 | 정책 확정, 구현 미착수 |
| `4.10` | `4.10.3` | system settings 구현 범위 확정 | 정책 결정 확정, 구현 미착수 |
| `4.10` | `4.10.4` | audit/operation logs 및 platform admin lifecycle/grant/revoke 구현 | 정책 확정, 구현 미착수 |
| `4.11` | `4.11.1` | attachment policy/contract 결정 | decision-needed |
| `4.11` | `4.11.2` | presigned upload/download 구현 | Sprint 3 후보 |
| `4.11` | `4.11.3` | metadata/validation 검증 | Sprint 3 후보 |
| `4.12` | `4.12.1` | version/revision 정책 결정 | decision-needed |
| `4.12` | `4.12.2` | conflict API/backend 구현 | Sprint 3 후보 |
| `4.12` | `4.12.3` | conflict UI와 검증 | Sprint 3 후보 |
| `4.13` | `4.13.1` | retention/audit 정책 결정 | decision-needed |
| `4.13` | `4.13.2` | project restore 구현 | Sprint 3 후보 |
| `4.13` | `4.13.3` | issue/document permanent delete 구현 | Sprint 3 후보 |

## 5. 병목 방지형 parent WBS 입력 초안

아래 표는 parent row의 Owner/지원 배정 원칙이다. 실제 이름은 WBS baseline 승인 후 입력하고, Level 3 row가 있는 parent는 실제 착수 담당을 child row에 우선 배정한다.

| WBS ID | 요구사항 ID | 관련 API/화면/DB | 산출물 | Owner/지원 원칙 | 완료 조건 | 검증 방법 |
|---|---|---|---|---|---|---|
| 4.1 | 공통/문서 | scope decision, backlog, canonical docs, 요구사항추적표, WBS | Sprint 3 scope와 기준선 변경 묶음 | Owner 1명, 지원 공란 | committed, decision-needed, 유지 보류 항목과 수정 대상이 분리된다 | 문서 review, WBS/추적표 대조 |
| 4.2 | `USER-01`, `HUB-01`, `NAV-01`, `RBAC-01` | `/auth/me`, `/users/me/profile`, S03, S04 | 프로필 수정과 workspace context vertical slice | Owner 1명, API/FE 동시 변경 시 지원 1명 | mock company/project role 제거와 프로필 수정 가능 | test, curl, 브라우저 확인 |
| 4.3 | `PROJECT-MEMBER-01~02` | project invitations, `project_members`, S12 | project invitation create/list/accept/cancel vertical slice | Owner 1명, 지원 1명 가능 | 같은 회사 ACTIVE member 대상 project invitation core path 확인 | test, curl, DB 확인, 브라우저 확인 |
| 4.4 | `SPRINT-01~02`, `ISSUE-01~04`, `KANBAN-01~02` | sprints, issues, issue status, S05, S06 | Sprint/Issue/Kanban board vertical slice | Owner 1명, 지원 1명 가능 | Sprint, issue CRUD, 5컬럼 상태 변경, 휴지통/복원 확인 | test, curl, 브라우저 확인 |
| 4.5 | `LINK-01`, `LINK-03` | messages, issues, documents, threads | 메시지→이슈와 문서 이벤트 알림 연결 흐름 | Owner 1명, cross-domain이면 지원 1명 | project message만 이슈 전환 가능하고 문서 이벤트 알림 경계가 확인된다 | test, curl, DB 확인 |
| 4.6 | `DOC-01~04`, `GRAPH-01~02`, `LINK-02` | documents, document_issue_links, graph, S07, S08 | 문서/회의록과 graph vertical slice | Owner 1명, 지원 1명 가능 | 문서 CRUD, 휴지통/복원, graph node/edge 조회 가능 | test, curl, 브라우저 확인 |
| 4.7 | `THREAD-01~04`, `CHAT-01~03` | threads, messages, DM rooms, S09, S10 | thread/chat/1:1 DM communication vertical slice | Owner 1명 | thread, project chat, 1:1 DM, unread/read 위치 기준 확인. `4.7.4`는 완료가 아닌 이번 개발 기간 보류 기록으로 유지 | test, curl, 브라우저 확인 |
| 4.8 | `NOTI-01` | notification events, recipients, S11 | notification inbox/read-state core와 후속 Frontend 연동 | `4.8.1` Backend Owner 1명, `4.8.2` Frontend Owner 정철진 | Backend 목록/unread/read/read-all 검증 후 Frontend 연동. producer matrix와 preferences는 별도 범위 | Backend test/build/curl, 후속 브라우저 확인 |
| 4.9 | `FEED-01`, `SEARCH-01`, `DASH-01~04` | activity_events, `/search`, `/dashboard` | 조건부 확장 decision과 MySQL 우선 구현 | Owner 1명, 검색 결정 시 지원 공란 | feed/search/dashboard 범위와 검색 방식 결정이 남는다 | decision record, test, curl |
| 4.10 | `DEFER-01` | S15, platform_admins, support_cases, audit log | S15 planning gate 이후 구현/결정 vertical slice | Owner 1명, 보안/감사 위험으로 지원 1명 가능 | `3.7~3.7.5` Go/No-Go handoff를 확인하고 `users.role` 없이 platform admin 분리와 audit 기준 확인 | 문서 review, test, curl, 보안 review |
| 4.11 | `ATTACH-01` | attachments, S3 presigned URL, S05/S08/S09 | attachment 정책 결정과 구현 | Owner 1명, 인프라 연동 시 지원 1명 가능 | presigned upload/download, metadata, 권한 확인 | decision record, test, curl |
| 4.12 | `DEFER-04` | documents.version, `DOCUMENT_VERSION_CONFLICT`, S08 | optimistic locking 기반 문서 충돌 처리 boundary | Owner 1명, 지원 공란 | stale version 저장 거절과 conflict UI 기준 확인 | test, curl, 브라우저 확인 |
| 4.13 | `PROJECT_RESTORE_DEFERRED`, `PERMANENT_DELETE_DEFERRED` | project restore, issue/document permanent delete, audit | restore/delete 정책 결정과 제한 구현 | Owner 1명, 정책/감사 위험으로 지원 1명 가능 | restore와 permanent delete 권한, 참조, audit 기준 충족 | decision record, test, curl, DB 확인 |
| 4.14 | 공통/QA | backend, frontend, canonical docs, WBS, 요구사항추적표 | Sprint 3 통합 QA와 release gate | Owner 1명, 각 domain evidence owner 협조 | 완료/미완료/이월, 문서/WBS/추적표 충돌 여부 분리 | test/build/browser, diff, grep |

## 6. WBS 입력 시 공란 유지 컬럼

| 컬럼 | 처리 |
|---|---|
| 계획 구간 | 공란 또는 Sprint 3 합의 후 입력 |
| 계획 시작일 | 공란 |
| 계획 종료일 | 공란 |
| Owner | 공란 |
| Reviewer | 공란 |
| 지원 담당 | 공란 |
| capacity | 별도 column이 없으므로 입력하지 않음 |

## 7. 기존 WBS 4.x와의 변경 요약

| 기존 WBS | 기존 의미 | Sprint 3 변경 방향 |
|---|---|---|
| 4 | Backlog 5일 개발 | Sprint 3 전체 Backlog 구축 root로 변경 |
| 4.1 | Profile/Full Hub 미배정 reference | Sprint 3 범위 결정 문서와 WBS 동기화로 이동, Profile/Hub는 4.2로 배치 |
| 4.2 | Project invitation | 4.3으로 확장 유지 |
| 4.3 | Sprint/Issue/Kanban 확장 | 4.4로 확장 유지 |
| 4.4 | Message-Issue Link | 4.5로 확장 유지 |
| 4.5 | Documents/Graph | 4.6으로 확장 유지 |
| 4.6 | Thread/Chat/DM skeleton | 4.7로 고급 DM까지 확장 |
| 4.7 | Notification 최소 흐름 | 4.8로 PROJECT scope와 invitation notification까지 확장 |
| 4.8 | Conditional Extensions | 4.9로 유지하되 search 결정 row 추가 |
| 4.9 | Optional/Deferred | 4.10 S15로 재배치, optional/deferred는 세부 row로 분해 |
| 4.10 | Deferred Invitation/Tech Details | 검색/첨부/고급 기능 decision row로 분산 |

## 8. 확정 전 주의사항

1. 이 문서는 Gate 3 Google Sheets WBS rewrite와 Gate 4 요구사항추적표 WBS ID backfill 완료 결과를 반영한 Sprint 3 backlog 기준이다.
2. S15는 `3.7~3.7.5` Sprint 2 planning gate와 Go/No-Go handoff를 전제로 `4.10`에서 상세 확정과 구현을 추적한다.
3. optimistic locking, DM room model, MySQL 우선 검색, S3 presigned URL은 현재 권장안이며 구현 중 변경될 수 있다.
4. permanent delete는 참조 무결성과 audit 정책이 확정되지 않으면 구현 row로 넘어가면 안 된다.
5. 요구사항추적표 primary WBS ID는 기능 ID별 단일 값을 원칙으로 한다. Level 3 row를 추가해도 Profile/Hub 관련 primary는 `4.2`, `LINK-02`와 `DOC-02` primary는 `4.6`, `DEFER-04` primary는 `4.12`, `NOTI-01` primary는 `4.8`이다. `4.2.1`/`4.2.2`와 `4.8.1`/`4.8.2`는 각 parent의 실행 분리 row이며 parent 완료를 의미하지 않는다.
