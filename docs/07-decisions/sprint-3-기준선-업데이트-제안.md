> **작성자**: Threadia 팀
> **작성일**: 2026-07-07
> **최종 수정**: 2026-07-07
> **버전**: v0.3 Gate 3/4 완료 정합화
> **상태**: 검토 중
> **태그**: decision, sprint-3, baseline, proposal, changelog

# Sprint 3 기준선 업데이트 제안

## 1. 문서 목적

이 문서는 `Sprint 3 범위 결정 초안`과 `Sprint 3 Backlog 초안`을 기준으로 canonical 문서, Google Sheets `요구사항추적표`, Google Sheets `WBS`, `기획문서-변경이력.md`에 대한 반영 상태와 남은 변경 묶음을 검토하기 위한 제안 문서다.

이 문서는 Gate 1 요구사항추적표 scope/status/boundary, Gate 2 canonical 문서 반영, Gate 3 Google Sheets WBS 4.x rewrite, Gate 4 요구사항추적표 WBS ID backfill이 완료된 현재 workflow 상태를 기록한다.

| 대상 | 처리 |
|---|---|
| canonical 확정 문서 | Gate 2 승인에 따라 Sprint 3 기준선 반영을 완료했다. |
| Google Sheets `요구사항추적표` | Gate 1 승인에 따라 scope/status/boundary를 정리했다. |
| Google Sheets `WBS` | Gate 3에 따라 Sprint 3 4.x 구조로 rewrite를 완료했다. |
| `docs/07-decisions/기획문서-변경이력.md` | Gate 3/4 완료와 요구사항추적표 primary WBS backfill 정책을 변경이력에 반영한다. |
| `docs/01-planning/` | raw archive이므로 수정하지 않는다. |

## 2. 기준 문서와 현재 상태

| 구분 | 현재 기준 |
|---|---|
| Sprint 3 scope draft | `docs/07-decisions/sprint-3-범위-결정.md` |
| Sprint 3 backlog draft | `docs/00-project/sprint-운영/sprint-3-backlog.md` |
| 요구사항 기준 | `docs/02-requirements/기능-분해표.md`, `요구사항-정의서.md`, `사용자-시나리오.md`, `기능-시나리오-API-ERD-추적표.md` |
| 설계 기준 | `docs/03-design/화면정의서.md`, `docs/03-design/ERD.md` |
| API 기준 | `docs/04-api/API-명세서.md` |
| 보류 기준 | `docs/07-decisions/보류-기능-최종표.md` |
| 변경이력 기준 | `docs/07-decisions/기획문서-변경이력.md` |
| Google Sheets `WBS` 현재 4.x | Gate 3 완료 기준 `4` root와 `4.1~4.14` Sprint 3 구조 |
| Google Sheets `요구사항추적표` 현재 상태 | Gate 1 scope/status/boundary 정리와 Gate 4 WBS ID backfill 완료 |

## 3. 기준선 업데이트 원칙

1. Gate 1 요구사항추적표 scope/status/boundary와 Gate 2 canonical 문서 반영은 승인된 기준선으로 둔다.
2. `요구사항추적표`의 scope/status/boundary는 WBS 4.x rewrite보다 먼저 정리된 상태다.
3. WBS ID 참조는 Gate 3 WBS 4.x row 반영 후 Gate 4에서 `요구사항추적표`에 backfill했다.
4. canonical 문서 의미 변경은 `기획문서-변경이력.md`의 단일 변경 묶음으로 기록한다.
5. 기간, capacity, Owner, Reviewer, 지원 담당은 사용자 지시에 따라 공란으로 둔다.
6. 새 요구사항 ID는 이 제안에서 임의 생성하지 않는다.
7. Project invitation은 Sprint 3 `PROJECT-MEMBER-01~02` 후보로 다루고, 기존 `INVITE-01~04` 회사 invitation row를 덮어쓰지 않는다.
8. WBS는 작은 작업으로 과분할하지 않고 PR/검증 가능한 굵은 실행 단위로 둔다.
9. 쉬운 작업은 Owner 1명과 지원 담당 공란을 기본으로 한다. 어려운 작업, cross-domain 작업, 보안/감사/인프라 위험이 있는 작업에만 지원 담당 1명을 둔다.
10. `선행 WBS ID`, `의존 유형`, `착수 가능 조건`, `병렬 그룹`, `의존성`을 채워 병목과 대기 상태를 줄인다.

## 4. 승인 전 작성 가능한 산출물

| 산출물 | 위치 | 상태 |
|---|---|---|
| Sprint 3 기준선 업데이트 제안 | 이 문서 | 검토 중 |
| 기획문서 변경이력 추가 예정 entry | 이 문서 8장 | 초안 |
| canonical 문서별 수정 영향도 | 이 문서 5장 | Gate 2 반영 완료 |
| 요구사항추적표 1차 변경 draft | 이 문서 6장 | Gate 1 반영 완료 |
| WBS 4.x rewrite 기준 | `sprint-3-backlog.md`와 Google Sheets `WBS` 4.x | Gate 3 반영 완료 |

## 5. canonical 문서 영향도

| 문서 | 수정 대상 섹션/키워드 | 반영 방향 |
|---|---|---|
| `docs/02-requirements/기능-분해표.md` | `최종 범위 결정`, `기능 분해표`, `보류 기능과의 정합성`; `FEED-01`, `SEARCH-01`, `DASH-01~04`, `ATTACH-01`, `DEFER-01`, `DEFER-04~06`, `PROJECT_NOTIFICATION_SCOPE_DEFERRED`, `PROJECT_RESTORE_DEFERRED`, `PERMANENT_DELETE_DEFERRED` | Sprint 3 승격, decision-needed, 유지 보류 항목을 재분류한다. |
| `docs/02-requirements/요구사항-정의서.md` | `범위 기준`, `MVP reference 기능 요구사항`, `조건부 확장 요구사항`, `보류/제외 요구사항` | Sprint 3 범위 기준과 수용 기준을 추가하고 기존 Sprint 2 이후/보류 표현을 갱신한다. |
| `docs/02-requirements/사용자-시나리오.md` | `최종 결정사항`, `SC-A-01`, `SC-LINK-01`, `조건부 확장 흐름`, `MVP 이후/보류 흐름` | S15, 고급 DM, 문서 충돌, PROJECT 알림 scope, 조건부 확장 승격 흐름을 반영한다. |
| `docs/02-requirements/기능-시나리오-API-ERD-추적표.md` | `시나리오별 추적표`, `조건부 확장 추적표`, `보류/Stub 역추적표`, `API-ERD 핵심 불변식`, `검증 체크리스트` | Sprint 3 승격 기능의 API/ERD/화면 추적성과 보류 경계를 갱신한다. |
| `docs/03-design/화면정의서.md` | `전체 화면 흐름`, `화면 목록`, `S03`, `S05`, `S08`, `S09`, `S11`, `S12`, `S13`, `S14`, `S15`, `MVP 보류/선택/조건부 확장 기능` | Sprint 3 화면 상태, S15, DM, 알림, 문서 충돌, 복원/삭제 action을 반영한다. |
| `docs/03-design/ERD.md` | entity summary, Mermaid ERD, `documents`, `direct_message_rooms`, `notification_preferences`, service policy; 신규 후보 `platform_admins`, `support_cases`, audit log, `attachments`, document versioning | S15, DM room + participants, PROJECT notification scope, optimistic locking, attachment data 영향을 정리한다. |
| `docs/04-api/API-명세서.md` | 이슈/문서/그래프, 쓰레드/채팅/DM, 알림, 조건부 확장 API, 보류/Stub 목록; `S15`, `DOCUMENT_VERSION_CONFLICT`, `PROJECT scope`, `attachment`, restore/permanent delete | Sprint 3 API 계약으로 승격할 endpoint와 여전히 decision-needed인 stub을 분리한다. |
| `docs/07-decisions/보류-기능-최종표.md` | 보류/선택/조건부 확장 최종표, 조건부 확장 상세 기준, 이월 기술 검토, 정합성 메모 | Sprint 3로 승격한 항목은 보류 상태에서 제거하거나 Sprint 3 포함/decision-needed로 상태를 바꾼다. |
| `docs/07-decisions/기획문서-변경이력.md` | 목차, 신규 change bundle 본문 | Sprint 3 scope/backlog baseline 변경 묶음을 추가한다. |

## 6. 요구사항추적표 1차 변경 draft

### 6.1 Sprint 3 committed 후보

아래 row는 Gate 1 승인에 따라 `구현 단계`, `추적 상태`, `보류/제외 경계`, `기준 문서`를 Sprint 3 기준으로 갱신했고, Gate 4에서 primary WBS ID를 backfill한 범위다.

| 그룹 | 요구사항 ID | 1차 변경 방향 |
|---|---|---|
| Profile / Hub | `USER-01`, `HUB-01`, `NAV-01` | Sprint 3 후보 또는 Sprint 3 committed로 승격하고 S03/S04 완성 context 기준을 추가한다. |
| Project invitation | `PROJECT-MEMBER-01~02` | project invitation과 accept 흐름을 Sprint 3 후보로 올린다. `INVITE-01~04`는 company invitation 전용으로 유지한다. |
| Sprint / Issue / Kanban | `SPRINT-01~02`, `ISSUE-01~04`, `KANBAN-01~02` | Sprint 2 candidate/MVP reference 표현을 Sprint 3 전체 흐름 후보로 갱신한다. |
| Message / Link | `LINK-01`, `LINK-03` | 메시지→이슈와 문서 이벤트 알림 후보를 Sprint 3 범위로 올린다. `LINK-02` 문서-이슈 연결 primary WBS는 `4.6`으로 둔다. |
| Documents / Graph | `DOC-01~04`, `GRAPH-01~02` | 문서/회의록, graph, 문서-이슈 edge를 Sprint 3 후보로 갱신한다. |
| Thread / Chat / DM core | `THREAD-01~04`, `CHAT-01~03` | 쓰레드, 프로젝트 채팅, 1:1 DM, unread 기준을 Sprint 3 후보로 갱신한다. |
| Notification core | `NOTI-01` | 알림 목록, 읽음 처리, unread 흐름을 Sprint 3 후보로 갱신한다. |
| Conditional extensions | `FEED-01`, `DASH-01~04` | 조건부 확장에서 Sprint 3 후보로 승격하되 Redis/cache/summary table은 성능 조건부로 유지한다. |

### 6.2 decision-needed 후보

아래 row는 Sprint 3에 포함하되 구현 row보다 정책/설계 결정 row를 먼저 둔다.

| 요구사항 ID 또는 API 코드 | 1차 변경 방향 | 선행 결정 |
|---|---|---|
| `SEARCH-01` | Sprint 3 decision-needed | MySQL `LIKE/FULLTEXT` 우선인지 검색 엔진을 도입할지 결정한다. |
| `ATTACH-01` | Sprint 3 decision-needed | S3 presigned URL, metadata, 권한, 크기, MIME 정책을 결정한다. |
| `DEFER-01` | Sprint 3 decision-needed | `3.7~3.7.5` planning gate와 Go/No-Go handoff 이후 `4.10`에서 S15 구현/결정 row로 추적한다. |
| `DEFER-04` | Sprint 3 decision-needed | optimistic locking 우선안과 실시간 공동 편집 후속 가능성을 결정한다. |
| `DEFER-05`, `DEFER-06` | Sprint 3 decision-needed | DM room + participants 모델과 read receipt 정책을 결정한다. |
| `PROJECT_NOTIFICATION_SCOPE_DEFERRED` | Sprint 3 decision-needed | API boundary code로 유지한다. 요구사항추적표 primary row로 backfill하지 않는다. |
| `PROJECT_RESTORE_DEFERRED` | Sprint 3 decision-needed | archive 프로젝트 복원 권한과 UI/API 기준을 결정한다. |
| `PERMANENT_DELETE_DEFERRED` | Sprint 3 decision-needed | 참조 무결성, audit, 보존 기간, 권한 기준을 결정한다. |

### 6.3 유지 보류 또는 제외

| 요구사항 ID 또는 API 코드 | 유지 기준 |
|---|---|
| `DEFER-02` | NoSQL feed store와 검색 엔진은 Sprint 3 기본안에서 제외한다. MySQL 원장과 MySQL 검색 우선을 유지한다. |
| `DEFER-03` | 칸반 컬럼 커스터마이징은 5컬럼 고정 원칙을 유지하고 Sprint 3 기본 범위에 넣지 않는다. |
| `DM_MESSAGE_CANNOT_CREATE_ISSUE` | DM은 사적 소통으로 유지하고 메시지→이슈 전환 대상에서 제외한다. |
| `INVITE-01~04` | company invitation 전용 row로 유지한다. project invitation으로 의미를 덮어쓰지 않는다. |

## 7. WBS 4.x rewrite draft 기준

WBS rewrite는 `docs/00-project/sprint-운영/sprint-3-backlog.md`의 4.x 구조를 기준으로 Gate 3에서 Google Sheets에 반영했다. 기존 `A:Z` 컬럼 중 `선행 WBS ID`, `의존 유형`, `착수 가능 조건`, `병렬 그룹`, `의존성`, `완료 조건`, `검증 방법`을 함께 채운다.

| WBS ID | 변경 방향 | 요구사항 ID | 의존 유형 | 병렬 그룹 |
|---|---|---|---|---|
| `4` | Sprint 3 전체 Backlog 구축 root | 전체 Sprint 3 | Gate | S3-Root |
| `4.1` | Sprint 3 범위 결정과 승격 문서 동기화 | 공통/문서 | Gate | S3-Gate |
| `4.2` | 프로필 수정과 완성형 S03/S04 context | `USER-01`, `HUB-01`, `NAV-01`, `RBAC-01` | API+Frontend | S3-Foundation-UX |
| `4.3` | Project invitation과 project member accept 흐름 | `PROJECT-MEMBER-01~02`, project invitation 정책 | API+Frontend | S3-Collaboration-A |
| `4.4` | Sprint / Issue / Kanban 전체 흐름 | `SPRINT-01~02`, `ISSUE-01~04`, `KANBAN-01~02` | API+Frontend | S3-Core-Board |
| `4.5` | 메시지→이슈와 문서 이벤트 알림 연결 | `LINK-01`, `LINK-03` | Cross-domain | S3-Linking |
| `4.6` | Documents / Graph / Markdown editor | `DOC-01~04`, `GRAPH-01~02`, `LINK-02` | API+Frontend | S3-Docs |
| `4.7` | Thread / Chat / DM | `THREAD-01~04`, `CHAT-01~03`, `DEFER-05~06` | API+Frontend+Decision | S3-Communication |
| `4.8` | Notification core | `NOTI-01` | Event integration | S3-Events |
| `4.9` | Conditional extensions | `FEED-01`, `SEARCH-01`, `DASH-01~04` | Decision+API | S3-Extensions |
| `4.10` | S15 Platform Admin | `DEFER-01` | Implementation+Decision | S3-Platform |
| `4.11` | Attachment / S3 presigned URL | `ATTACH-01` | Decision+Implementation | S3-Attachment |
| `4.12` | Advanced document editing | `DEFER-04` | Decision+Implementation | S3-Docs |
| `4.13` | Restore / permanent delete | `PROJECT_RESTORE_DEFERRED`, `PERMANENT_DELETE_DEFERRED` | Policy+Implementation | S3-Retention |
| `4.14` | Sprint 3 통합 QA와 문서 정합성 gate | 공통/QA | QA Gate | S3-QA |

WBS row는 Level 2 중심으로 입력한다. 세부 구현 task는 PR, issue, 담당자 작업 기록에서 관리하고, WBS에는 병렬 착수와 검증 단위가 보이는 수준까지만 둔다. 요구사항추적표 primary WBS ID는 기능 ID별 단일 값을 원칙으로 하며, `DOC-02`와 `LINK-02`의 primary는 `4.6`, document optimistic locking boundary는 `4.12`, `LINK-01/LINK-03` primary는 `4.5`, `NOTI-01` primary는 `4.8`로 둔다. `NOTI-02/03`과 `PROJECT_NOTIFICATION_SCOPE_DEFERRED`는 현재 계약/reference 또는 API boundary로 유지하고 primary WBS backfill 대상이 아니다.

## 8. 기획문서 변경이력 추가 예정 entry

아래 내용은 approval 이후 `docs/07-decisions/기획문서-변경이력.md`에 추가할 변경 묶음 초안이다.

```md
| 2026-07-07 Sprint 3 scope/backlog 기준선 업데이트 | Sprint 3 범위 결정, 요구사항추적표, canonical 문서, WBS 4.x를 Sprint 3 실행 기준으로 동기화한다. | [블록 링크](#change-2026-07-07-sprint-3-baseline-update) |

<a id="change-2026-07-07-sprint-3-baseline-update"></a>

## 2026-07-07 Sprint 3 scope/backlog 기준선 업데이트

> 변경이력 묶음 ID: Sprint 3 scope/backlog 기준선 업데이트 ^change-2026-07-07-sprint-3-baseline-update

### 1. 목적

Sprint 3에서 Backlog 전체 개발, S15 platform admin, 조건부 확장, 고급 DM/문서/알림 기능을 실행 가능한 Sprint 3 범위와 WBS 4.x 기준으로 정렬한다.

### 2. 변경 원칙

- Sprint 3 committed, decision-needed, 유지 보류 항목을 분리한다.
- 요구사항추적표의 scope/status/boundary를 WBS 4.x rewrite보다 먼저 정리한다.
- WBS 4.x 반영 후 요구사항추적표에 WBS reference를 backfill한다.
- 기간, capacity, Owner, Reviewer, 지원 담당은 공란으로 둔다.
- WBS row는 과분할하지 않고 PR/검증 가능한 굵은 실행 단위로 둔다.
- 쉬운 작업은 Owner 1명과 지원 담당 공란을 기본으로 하고, 어려운 작업에만 지원 담당 1명을 둔다.
- 선행 WBS ID, 의존 유형, 착수 가능 조건, 병렬 그룹을 함께 채운다.
- project invitation은 기존 company invitation row를 덮어쓰지 않는다.
- `docs/01-planning/` archive는 수정하지 않는다.

### 3. 변경 문서

| 문서 | 변경 내용 |
|---|---|
| Google Sheets `요구사항추적표` | Sprint 3 후보, decision-needed, 유지 보류 상태와 WBS reference를 동기화 |
| Google Sheets `WBS` | 기존 4.x Backlog Development를 Sprint 3 4.1~4.14 구조로 재작성하고 dependency metadata를 채운다. |
| `docs/02-requirements/기능-분해표.md` | Sprint 3 승격 항목과 보류 경계 갱신 |
| `docs/02-requirements/요구사항-정의서.md` | Sprint 3 범위와 수용 기준 반영 |
| `docs/02-requirements/사용자-시나리오.md` | Sprint 3 사용자 흐름과 보류 해제 흐름 반영 |
| `docs/02-requirements/기능-시나리오-API-ERD-추적표.md` | API/ERD/화면 추적성과 stub 경계 갱신 |
| `docs/03-design/화면정의서.md` | Sprint 3 화면 상태, S15, DM, 알림, 문서 충돌, 복원/삭제 action 반영 |
| `docs/03-design/ERD.md` | S15, DM room model, PROJECT notification scope, attachment, document versioning 영향 반영 |
| `docs/04-api/API-명세서.md` | Sprint 3 API 계약과 보류/Stub 목록 갱신 |
| `docs/07-decisions/보류-기능-최종표.md` | Sprint 3 승격 항목을 보류표에서 재분류 |
| `docs/07-decisions/기획문서-변경이력.md` | 이번 변경 묶음 추가 |

### 4. 영향 범위

| 구분 | 영향 |
|---|---|
| 요구사항 | Backlog core, 조건부 확장, S15, 고급 DM/문서/알림, 첨부, restore/delete의 Sprint 3 상태가 바뀐다. |
| API | Sprint/Issue/Kanban, 문서/그래프, 채팅/DM, 알림, 조건부 확장, S15, attachment, document conflict, restore/delete API 계약이 갱신된다. |
| ERD/DB | S15, support/audit, attachment, DM participants, document versioning, PROJECT notification scope 영향이 정리된다. |
| 화면 | S03~S15 화면 상태와 action 경계가 Sprint 3 기준으로 정렬된다. |
| Sprint/DoD/검증 | WBS 4.x와 요구사항추적표, canonical docs의 정합성 QA가 Sprint 3 gate로 추가된다. |

### 5. AI 사용 및 사람 검토

| 항목 | 내용 |
|---|---|
| AI 사용 여부 | 사용 |
| AI 사용 범위 | Sprint 3 초안, canonical 문서, Google Sheets WBS/요구사항추적표 read-only 대조와 변경 묶음 초안 작성 |
| 최종 책임자 | 미정 |
| 검토자 | 미정 |
| 관련 PR/작업 기록 | 미정 |
```

## 9. 승인 gate

아래 항목은 baseline 변경 gate별 현재 상태다.

| Gate | 승인 대상 | 현재 상태 |
|---|---|---|
| Gate 1 | Sprint 3 scope와 baseline update proposal | 승인 완료, 요구사항추적표 1차 scope/status/boundary 정리 완료 |
| Gate 2 | canonical 문서 의미 변경 | 승인 완료, 5장 대상 문서 반영 완료 |
| Gate 3 | WBS 4.x rewrite | 완료, Google Sheets `WBS` 4.x 재작성 반영 |
| Gate 4 | WBS reference backfill | 완료, 요구사항추적표 primary WBS ID backfill 반영 |
| Gate 5 | 변경이력 확정 | 별도 확인 필요, `기획문서-변경이력.md` 실제 반영 전 |

## 10. 검증 기준

| 검증 항목 | 기준 |
|---|---|
| scope 분류 | Sprint 3 항목이 committed 후보, decision-needed, 유지 보류/제외 중 하나로 분류된다. |
| 요구사항추적표 | Sprint 3 scope/status/boundary가 canonical 문서와 충돌하지 않는다. |
| WBS | Gate 3 완료 기준 WBS 4.x row가 Sprint 3 backlog의 4.1~4.14 구조와 일치하고 선행 WBS ID, 의존 유형, 착수 가능 조건, 병렬 그룹이 채워진다. |
| WBS backfill | Gate 4 완료 기준 요구사항추적표 primary WBS ID가 실제 WBS row와 상호 참조된다. |
| 보류표 | Sprint 3로 승격한 항목이 보류로만 남지 않는다. |
| S15 | `users.role` 단일 권한 모델을 만들지 않고 `platform_admins` 분리 기준을 유지한다. |
| 알림/활동 | `activity_events`, `notification_events`, audit log를 서로 합치지 않는다. |
| archive | `docs/01-planning/` 변경이 없다. |
| secret | secret, token, password, 개인정보가 문서와 evidence에 기록되지 않는다. |

## 11. Open questions

| 항목 | 결정 필요 |
|---|---|
| Project invitation 요구사항 ID | 현재 기준선은 `PROJECT-MEMBER-01~02` Sprint 3 후보이며 `INVITE-01~04`는 company invitation 전용으로 유지 |
| Search | MySQL `LIKE/FULLTEXT` 우선안 확정 또는 검색 엔진 도입 여부 결정 필요 |
| Attachment | S3 presigned URL, metadata, 권한, 크기, MIME 정책 결정 필요 |
| S15 | `3.7~3.7.5` planning gate와 Go/No-Go handoff 결과를 `4.10` Sprint 3 구현/결정 row에서 확인 |
| Permanent delete | 참조 무결성, audit, 보존 기간, 권한 기준 결정 필요 |
| Document conflict | optimistic locking 우선안과 실시간 공동 편집 후속 가능성 결정 필요 |
