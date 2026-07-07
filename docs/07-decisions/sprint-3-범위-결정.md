> **작성자**: Threadia 팀
> **작성일**: 2026-07-07
> **최종 수정**: 2026-07-07
> **버전**: v0.2 Gate 3/4 완료 정합화
> **상태**: 검토 중
> **태그**: decision, sprint-3, scope, backlog, platform-admin

# Sprint 3 범위 결정 초안

## 1. 문서 목적

이 문서는 Sprint 3에서 Backlog 전체 개발, S15 platform admin, 조건부 확장, 고급 DM/문서/알림 기능을 어떤 기준으로 Sprint 3 실행 범위에 올릴지 정리하는 scope decision 초안이다.

이 문서는 Sprint 3 Gate 1 요구사항추적표 scope/status/boundary, Gate 2 canonical 문서 반영, Gate 3 Google Sheets WBS 4.x rewrite, Gate 4 요구사항추적표 WBS ID backfill이 완료된 상태를 기준으로 한다.

## 2. 현재 기준

| 구분 | 현재 상태 |
|---|---|
| Sprint 1 | Auth-only 완료 기준 |
| Sprint 1.5 | Company invitation 진행/계획 기준 |
| Sprint 2 | Company/RBAC/Project foundation 계획, Issue/Kanban은 capacity candidate |
| Backlog 4.x | Gate 3 완료 기준 Sprint 3 `4.1~4.14` 구조로 재작성 완료 |
| S15 | `3.7~3.7.5` Sprint 2 planning gate와 Go/No-Go handoff 기준 존재, Sprint 3 구현/결정 primary row는 `4.10` |

## 3. Sprint 3 범위 모델

Sprint 3는 기존 `WBS 4.x Backlog Development`를 실행 가능한 Sprint 3 범위로 재편하는 것을 목표로 한다.

| 범위 | Sprint 3 처리 |
|---|---|
| Backlog core | Sprint 3 committed 후보로 승격한다. |
| S15 platform admin | 선행 WBS 해결 후 상세 확정과 구현을 Sprint 3 범위에 포함한다. |
| 조건부 확장 | activity feed, search, dashboard를 Sprint 3 범위로 올리되, 검색 방식 결정 row를 선행한다. |
| 고급 DM | 다자간 DM과 read receipt를 Sprint 3 범위로 올리되, DM room model 전환을 선행한다. |
| 고급 문서 | 실시간 공동 편집 대신 optimistic locking 기반 충돌 감지를 우선한다. 변경 가능성을 명시한다. |
| 고급 알림 | `NOTI-01` 알림 목록/읽음 처리를 Sprint 3 primary로 둔다. `NOTI-02/03`은 현재 계약/reference, `PROJECT_NOTIFICATION_SCOPE_DEFERRED`는 API boundary로 유지한다. |
| 첨부 | S3 presigned URL 정책 결정을 선행하고 구현 범위를 확정한다. |
| 복원/영구 삭제 | project restore와 issue/document permanent delete를 포함하되, 참조 무결성과 audit 정책 결정을 선행한다. |

## 4. Blocker 처리 결정

| 항목 | 결정 |
|---|---|
| 기간, capacity, Owner, Reviewer | Sprint 3 초안에서는 공란으로 둔다. |
| S15 상세 확정 | `3.7~3.7.5` planning gate와 Go/No-Go handoff를 전제로 `4.10`에서 상세 확정과 구현을 추적한다. |
| 동시 문서 편집 | Sprint 3 기본안은 version/revision 기반 optimistic locking이다. CRDT 또는 실시간 공동 편집은 후속 변경 가능성을 남긴다. |
| 고급 DM 모델 | 기존 1:1 전용 모델을 room + participants 구조로 일반화하는 방향을 권장한다. 1:1과 group DM을 같은 room model로 처리한다. |
| 알림 scope | `GLOBAL` preference 계약을 유지한다. `PROJECT` scope는 `PROJECT_NOTIFICATION_SCOPE_DEFERRED` API boundary이며 요구사항추적표 primary row로 backfill하지 않는다. |
| 검색 방식 | MySQL `LIKE/FULLTEXT` 우선과 검색 엔진 도입 여부를 WBS 결정 row로 둔다. 기본 권장안은 MySQL 우선이다. |
| 첨부 방식 | S3 presigned upload/download, metadata table, 권한, 크기, MIME 정책을 WBS 결정 row로 둔다. |
| project restore / permanent delete | 포함한다. 다만 permanent delete는 참조 무결성, audit, soft delete 보존 기간 정책을 먼저 결정한다. |

## 5. Sprint 3 committed 후보

| 영역 | 기능 ID | 포함 내용 |
|---|---|---|
| Profile / Full Hub | `USER-01`, `HUB-01`, `NAV-01` | 프로필 수정, `/auth/me` 확장 context, S03/S04 진입 완성 |
| Project invitation | `PROJECT-MEMBER-01~02`, project invitation 정책 | 프로젝트 초대, 수락, project membership 생성 |
| Sprint / Issue / Kanban | `SPRINT-01~02`, `ISSUE-01~04`, `KANBAN-01~02` | Sprint 운영, 이슈 CRUD, 5컬럼 칸반, 휴지통/복원 |
| Message / Issue / Document link | `LINK-01`, `LINK-03` | 메시지에서 이슈 생성, 문서 이벤트 알림 후보. `LINK-02` 문서-이슈 연결 primary WBS는 `4.6` |
| Documents / Graph | `DOC-01~04`, `GRAPH-01~02`, `LINK-02` | Markdown 문서, 회의록, graph 조회, 문서-이슈 edge |
| Thread / Chat / DM | `THREAD-01~04`, `CHAT-01~03`, `DEFER-05~06` | 쓰레드, 프로젝트 채팅, 1:1 DM, 다자간 DM, read receipt |
| Notification | `NOTI-01` | 알림 목록, 읽음 처리 core. `NOTI-02/03`은 현재 계약/reference row로 유지 |
| Conditional extensions | `FEED-01`, `SEARCH-01`, `DASH-01~04` | activity feed, 검색, 프로젝트 dashboard |
| S15 Platform Admin | `DEFER-01` 재분해 | 문의/신고, 사용자 정지, 회사 비활성화, 시스템 설정, 운영 로그 |
| Attachment | `ATTACH-01` | S3 presigned URL 기반 첨부 |
| Restore / permanent delete | 보류 API 코드 승격 | project restore, issue/document permanent delete 정책 및 구현 |

## 6. 선행 설계가 필요한 항목

| 항목 | 이유 | Sprint 3 처리 |
|---|---|---|
| S15 ERD/API/화면 상세 | 현재는 1차 범위와 인가 모델만 있음 | 상세 확정 row 후 구현 row로 분리 |
| 동시 문서 편집 | 실시간 공동 편집은 구현 부담이 큼 | optimistic locking 우선, 실시간은 후속 변경 가능 |
| 다자간 DM | 현재 ERD는 1:1 unique 구조에 가까움 | room + participants 모델 전환 결정 후 구현 |
| PROJECT 알림 설정 | 기존 API는 PROJECT scope를 deferred 처리 | API boundary code로 유지하고 요구사항추적표 primary WBS backfill 대상에서 제외 |
| 검색 | 검색 엔진 도입 여부가 미정 | 결정 row 후 MySQL 우선 구현 |
| 첨부 | S3, metadata, 권한, 파일 제한 정책 필요 | 정책 결정 row 후 구현 |
| permanent delete | 참조 보존과 audit 위험이 큼 | 정책 결정 row 후 제한 구현 |

## 7. 문서 영향 범위

| 문서 | 변경 방향 |
|---|---|
| `docs/02-requirements/기능-분해표.md` | Sprint 3 승격 항목의 우선순위와 보류 상태를 조정한다. |
| `docs/02-requirements/요구사항-정의서.md` | Sprint 3 범위 기준과 수용 기준을 추가한다. |
| `docs/02-requirements/사용자-시나리오.md` | S15, 고급 DM, 고급 문서, 고급 알림, 조건부 확장 사용자 흐름을 추가한다. |
| `docs/02-requirements/기능-시나리오-API-ERD-추적표.md` | Sprint 3 승격 기능의 API/ERD/화면 추적성을 갱신한다. |
| `docs/03-design/화면정의서.md` | S15 상세, project dashboard, 고급 DM/알림/문서 editing 화면 상태를 반영한다. |
| `docs/03-design/ERD.md` | `platform_admins`, `support_cases`, audit log, attachments, DM participants, notification project scope, document versioning을 반영한다. |
| `docs/04-api/API-명세서.md` | S15, feed/search/dashboard, attachment, project notification scope, group DM/read receipt, document version conflict API를 추가한다. |
| `docs/07-decisions/보류-기능-최종표.md` | Sprint 3로 승격한 항목을 보류에서 제거하거나 Sprint 3 포함으로 상태를 바꾼다. |
| `docs/07-decisions/기획문서-변경이력.md` | Sprint 3 scope 승격 변경 묶음을 추가한다. |

## 8. WBS 반영 원칙

1. 기존 `WBS 4.x`를 Sprint 3 root와 세부 실행 row로 재작성한다.
2. 기간, capacity, Owner, Reviewer, 지원 담당은 공란으로 둔다.
3. `decision-needed` 항목은 구현 row와 분리한다.
4. S15는 `3.7~3.7.5` Sprint 2 planning gate와 Go/No-Go handoff를 거쳐 `4.10` Sprint 3 primary 구현/결정 row로 진행한다.
5. 검색, 첨부, permanent delete처럼 정책 결정이 필요한 항목은 decision row를 선행한다.
6. Gate 1 요구사항추적표 scope/status/boundary, Gate 2 canonical 문서 반영, Gate 3 WBS rewrite, Gate 4 요구사항추적표 WBS ID backfill은 완료된 기준선으로 둔다.

## 9. 검증 기준

| 검증 항목 | 기준 |
|---|---|
| 범위 분류 | 모든 Sprint 3 항목이 committed, decision-needed, excluded 중 하나로 분류된다. |
| 보류표 정합성 | Sprint 3로 승격한 항목이 `보류-기능-최종표.md`에 보류로 남지 않는다. |
| API/ERD 정합성 | 신규 API는 ERD table 또는 비저장 정책과 연결된다. |
| S15 정합성 | `users.role` 없이 `platform_admins` 기준을 유지한다. |
| 알림 정합성 | `activity_events`, `notification_events`, audit log를 서로 합치지 않는다. |
| 문서 변경이력 | 확정 문서 의미 변경 시 `기획문서-변경이력.md`에 변경 묶음을 남긴다. |

## 10. 미확정 사항

| 항목 | 현재 처리 |
|---|---|
| Sprint 3 일정 | 공란 유지 |
| capacity | 공란 유지 |
| Owner / Reviewer / 지원 담당 | 공란 유지 |
| 검색 엔진 도입 여부 | WBS decision row에서 결정 |
| 첨부 상세 정책 | WBS decision row에서 결정 |
| permanent delete 상세 정책 | WBS decision row에서 결정 |
