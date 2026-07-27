> **작성자**: Threadia 팀  
> **작성일**: 2026-07-13  
> **최종 수정**: 2026-07-13  
> **버전**: v1.0  
> **상태**: 검토 중  
> **태그**: sprint-3, wbs, scope, baseline, collaboration

# Sprint 3 WBS 4.1 committed scope baseline

## 1. 목적과 기준

이 문서는 사용자 승인 `Core 협업 우선` 결정을 Sprint 3 WBS `4.1`의 검토용 scope baseline으로 기록한다. 실행 후보는 의존성 순서에 따라 `4.2`부터 `4.8`까지와 `4.13`이다. 다만 `4.13`은 retention/audit 정책 결정 전에는 시작할 수 없으므로 후보 상태를 유지하되 착수 gate는 닫아 둔다.

이 문서는 scope baseline일 뿐이다. 코드, API, ERD 구현의 authority가 아니며 reviewer 승인, AWS 배포 승인, OIDC 또는 CodeDeploy 승인도 아니다. Google Sheets WBS를 수정하라는 지시도 아니므로, 이 문서 작성으로 WBS Sheet나 canonical 문서의 상태는 바뀌지 않는다.

`3.7.4`의 S15 planning Go와 implementation No-Go를 그대로 적용한다. 따라서 `4.10`은 이 기준선의 committed 범위에 넣을 수 없으며, `4.10`을 전제로 하는 `4.13`도 retention/audit 정책과 별도 implementation Go가 확인되기 전에는 구현에 착수할 수 없다.

## 2. Core 협업 실행 후보와 의존 순서

선행 WBS 완료 evidence와 각 행의 착수 조건을 먼저 확인한다. 같은 단계의 항목도 실제 선행 조건이 닫히지 않으면 병렬 착수를 선언하지 않는다.

| 순서 | WBS ID | 분류 | 선행 WBS | 핵심 결과 | 검증 경계 |
|---:|---|---|---|---|---|
| 1 | `4.2` | committed execution candidate | `3.1`, `3.2`, `3.3`, `3.4`, `4.1` | 프로필 수정과 S03/S04 workspace context의 최소 vertical slice를 제공한다. | mock company/project role을 제거하고 프로필 수정과 workspace context를 test, curl, 브라우저에서 확인한다. |
| 2 | `4.3` | committed execution candidate | `3.1`, `3.3`, `3.4`, `4.1` | 같은 회사 ACTIVE member의 project invitation 생성, 조회, 수락, 취소 흐름을 제공한다. | invitation 정책과 company/project membership 경계를 test, curl, DB, 브라우저에서 확인한다. 기존 company invitation `INVITE-01~04`의 의미는 변경하지 않는다. |
| 3 | `4.4` | committed execution candidate | `3.3`, `3.4`, `3.5.1`, `4.1` | Sprint 운영, 이슈 CRUD, 고정 5컬럼 Kanban, 휴지통과 복원 흐름을 제공한다. | Sprint, issue CRUD, 상태 변경, 휴지통과 복원을 test, curl, 브라우저에서 확인한다. 칸반 컬럼 커스터마이징은 포함하지 않는다. |
| 4 | `4.6` | committed execution candidate | `3.3`, `4.1` | Markdown 문서와 회의록 CRUD, 문서-이슈 edge, graph 조회의 최소 vertical slice를 제공한다. | 문서 CRUD, 휴지통과 복원, graph node와 edge 조회를 test, curl, 브라우저에서 확인한다. optimistic locking은 `4.12` 결정 전 이 행의 검증 범위에 넣지 않는다. |
| 5 | `4.7` | committed execution candidate | `3.1`, `3.3`, `4.1` | thread, project chat, 1:1 DM과 unread/read의 core communication 흐름을 제공한다. | project message 기준과 1:1 DM 경계를 test, curl, 브라우저에서 확인한다. group DM과 read receipt는 `4.7.4` decision-needed로 남긴다. |
| 6 | `4.5` | committed execution candidate | `4.4`, `4.6`, `4.7` | project message에서 이슈를 만들고 문서 이벤트 알림 연결 경계를 제공한다. | project message만 이슈 전환 대상인지와 문서 이벤트 알림 경계를 test, curl, DB에서 확인한다. DM 메시지의 이슈 전환은 포함하지 않는다. |
| 7 | `4.8` | committed execution candidate | `4.3`, `4.4`, `4.6`, `4.7` | invitation과 core event source에 대한 notification 목록, 읽음, unread 흐름을 제공한다. | recipient와 event source 경계, 목록과 읽음 처리를 test, curl, 브라우저에서 확인한다. `NOTI-02/03`과 `PROJECT_NOTIFICATION_SCOPE_DEFERRED`는 포함하지 않는다. |
| 8 | `4.13` | committed candidate, gate closed | `4.4`, `4.6`, `4.10`, `4.13.1` | retention/audit 정책이 결정된 경우에만 project restore와 issue/document permanent delete의 제한 범위를 검토한다. | retention 기간, audit 보존과 조회 권한, restore/delete 권한, 참조 처리, `4.10`의 별도 implementation Go를 decision record로 먼저 확인한다. 이 조건 전에는 구현 검증을 시작하지 않는다. |

`4.13`은 Core 협업 우선순위의 후보이나 현재 착수 가능한 committed implementation이 아니다. `4.13.1` retention/audit 정책 결정과 `4.10`의 implementation No-Go 해제가 모두 확인되기 전까지는 대기 상태를 유지한다.

## 3. decision-needed와 deferred 경계

아래 항목은 이번 baseline에서 committed execution candidate로 승격하지 않는다. 각 항목의 기존 결정과 보류 경계를 유지한다.

| WBS ID | 분류 | 현재 처리 | committed로 두지 않는 이유 |
|---|---|---|---|
| `4.9` | decision-needed/deferred | activity feed, search, dashboard의 범위와 search 방식 결정을 유지한다. | feed/dashboard source와 MySQL `LIKE/FULLTEXT` 우선 여부가 닫히지 않았다. |
| `4.10` | deferred, implementation No-Go | S15의 `3.7.4` planning Go와 implementation No-Go를 유지한다. | platform foundation, security 방향, ERD/API/화면 상세, system settings, audit 보존 정책 evidence가 없다. 이 행은 baseline committed 범위에 포함하지 않는다. |
| `4.11` | decision-needed/deferred | attachment 대상, 권한, 크기, MIME, 만료와 S3 presigned 정책 결정을 유지한다. | 정책과 계약이 결정되지 않았으며 AWS foundation은 별도 hold track이다. |
| `4.12` | decision-needed/deferred | document version/revision과 conflict 처리 정책 결정을 유지한다. | optimistic locking의 API, backend, UI 검증 경계가 아직 확정되지 않았다. |
| `4.13` | committed candidate, gate closed | Core 협업 우선 후보로 기록하되 `4.13.1` 정책 결정과 `4.10` 별도 Go 전까지 대기한다. | retention/audit와 참조 무결성 정책 없이 restore 또는 permanent delete를 구현할 수 없다. |

`4.10`의 `4.10.1`, `4.10.2`, `4.10.3`, `4.10.4`도 implementation No-Go 범위에 포함한다. 특히 `4.10.3` system settings는 decision-needed이며, `4.10`의 어떤 child row도 이 baseline으로 구현 착수를 승인받지 않는다.

## 4. 검토 방법

reviewer는 아래 항목을 확인해 이 baseline의 분류와 경계를 검토한다.

1. `4.2`부터 `4.8`까지가 선행 WBS와 검증 경계를 충족하는 순서로만 실행 후보에 남아 있는지 확인한다.
2. `4.5`와 `4.8`이 선행 기능의 완료 evidence 없이 조기 착수 대상으로 바뀌지 않았는지 확인한다.
3. `4.9`부터 `4.12`까지, 특히 `4.10`이 committed로 승격되지 않았는지 확인한다.
4. `4.13`이 후보로만 기록되고 retention/audit 정책, `4.10` implementation Go, 참조 처리 기준이 없이는 시작하지 않도록 gate가 유지되는지 확인한다.
5. 이 문서가 WBS Sheet, canonical 문서, 코드, API, ERD, AWS, OIDC, CodeDeploy, GitHub workflow를 변경하거나 승인하는 문서로 해석되지 않는지 확인한다.

## 5. 기준 문서

- `docs/00-project/sprint-운영/sprint-2-wbs-3.7.4-S15-Go-No-Go-handoff.md`
- `docs/00-project/sprint-운영/sprint-3-backlog.md`
- `docs/07-decisions/sprint-3-범위-결정.md`
- `docs/07-decisions/sprint-3-기준선-업데이트-제안.md`
