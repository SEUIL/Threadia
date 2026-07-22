> **작성자**: 황스일
> **작성일**: 2026-07-20
> **최종 수정**: 2026-07-22
> **버전**: v0.2
> **상태**: 검토 중
> **태그**: sprint-3, wbs-4.5, link-01, link-03, backend, handoff

# Sprint 3 WBS 4.5 LINK-01, LINK-03 Backend planning handoff

## 1. 목적과 상태 경계

이 문서는 여도현이 WBS `4.5` Backend 계획을 시작할 수 있도록 현재 코드, canonical 범위, 구현 전 결정 사항과 검증 게이트를 전달한다. Google Sheets의 `4.5` 관련 row는 계획 `0%`이며, 이 문서는 구현 완료, source/message linkage 완료, notification producer 완료 또는 배포 가능 상태를 선언하지 않는다.

WBS planning assignment상 Owner는 **여도현**, Reviewer는 **황스일**이다. 이는 구현 담당과 검토 예정 배정일 뿐, 구현 착수 승인, 구현 결과, 코드 review 완료를 뜻하지 않는다.

## 2. WBS 계층과 범위

| WBS | 구분 | 계획 범위 | 상태 |
|---|---|---|---|
| `4.5` | parent | `LINK-01`, `LINK-03` cross-domain 연결 | 계획 `0%` |
| `4.5.1` | LINK-01 parent | 프로젝트 채팅 메시지에서 이슈 생성 | 계획 `0%` |
| `4.5.1.1` | Backend | 프로젝트 메시지 원본 검증과 from-message 이슈 생성 | 계획 `0%` |
| `4.5.1.2` | Frontend contract-gated | Backend API 계약 확정 후 S09/S05 연동 | 계획 `0%` |
| `4.5.2` | LINK-03 parent | 문서 수정 또는 문서 쓰레드 이벤트의 알림 연결 | 계획 `0%` |
| `4.5.2.1` | Backend | 문서 변경 event와 알림 producer 정책을 확정한 뒤 구현 | 계획 `0%` |
| `4.5.2.2` | Frontend contract-gated | producer와 inbox API 계약 검증 후 S08/S10/S11 연동 | 계획 `0%` |

확정된 포함 범위는 `LINK-01`의 프로젝트 채팅 메시지에서 이슈 생성과 `LINK-03`의 문서 이벤트 알림 연결이다. [`Sprint 3 범위 결정`](../../07-decisions/sprint-3-범위-결정.md)은 `4.5`를 두 기능으로 한정하고, [`Sprint 3 backlog`](../../00-project/sprint-운영/sprint-3-backlog.md)은 project message만 이슈 전환 가능해야 한다고 규정한다.

다음 항목은 이번 WBS에서 제외한다.

- `LINK-02` 문서와 이슈의 양방향 연결 및 graph edge, primary WBS는 `4.6`이다.
- DM 메시지 이슈 전환은 제공하지 않는다.
- 이슈 또는 문서 생성 시 thread 자동 생성은 제공하지 않는다. 대표 thread는 명시적 get-or-create만 허용한다.
- Push, WebSocket, Redis, notification preference, notification event-recipient matrix의 전면 구현은 범위 밖이다.

## 3. 코드 수준 현재 상태

| 영역 | 확인한 구현 | `4.5` 관점의 미구현 경계 |
|---|---|---|
| 프로젝트 채팅 | [`ProjectChatService`](../../../backend/src/main/java/com/threadia/backend/projectchat/service/ProjectChatService.java)는 active user와 프로젝트 접근을 확인한 뒤 메시지를 저장·목록·상세 조회하고 read pointer를 관리한다. | `issues`를 생성하지 않으며 `sourceType= MESSAGE` 또는 `sourceMessageId`를 기록하는 경로가 없다. |
| 문서 | [`DocumentService`](../../../backend/src/main/java/com/threadia/backend/document/service/DocumentService.java)는 문서 CRUD, trash/restore, `document_issue_links`, graph를 처리한다. PR #229에서 update의 `DOCUMENT_UPDATED` producer 연결을 추가한다. | create, trash/restore, thread comment producer는 포함하지 않는다. `document_issue_links` 자체는 `LINK-02`이므로 `4.5`에서 확장하지 않는다. |
| 알림 | [`NotificationService`](../../../backend/src/main/java/com/threadia/backend/notification/service/NotificationService.java)는 이미 존재하는 `notification_recipients`를 본인 범위에서 목록·unread·read·read-all 처리한다. PR #229에서 `DOCUMENT_UPDATED` event/recipient 저장 producer를 별도 추가한다. | event-recipient matrix 전면 구현, preference, Push/WebSocket은 구현하지 않는다. |

현재 서비스 test는 각 서비스의 접근 제어와 현재 기능을 다룬다. 예를 들어 프로젝트 채팅 test는 메시지 생성과 project membership을, 문서 test는 문서 권한과 `LINK-02` 정합성을, 알림 test는 recipient 소유권과 scope filter를 검증한다. 이 test들은 LINK-01 source linkage나 LINK-03 notification producer의 구현 evidence가 아니다.

## 4. Backend planning handoff

### 4.1 `4.5.1.1` LINK-01 Backend

1. [`기능-시나리오-API-ERD 추적표`](../../02-requirements/기능-시나리오-API-ERD-추적표.md)의 `POST /api/v1/projects/{projectId}/issues/from-message` 후보를 기준으로 request/response, 오류 코드, 권한을 설계한다. 아직 이 문서는 구현 완료 API가 아니라 reference contract다.
2. 요청의 `projectId`와 source message의 `project_id` 일치를 검증한다. 대상 message가 없거나 다른 프로젝트이면 생성하지 않는다.
3. 요청자는 active company membership과 active project membership 또는 허용된 company admin 권한을 통과해야 한다. 이슈 생성 권한과 message 열람 권한을 모두 재확인한다.
4. 생성 이슈는 canonical ERD의 `source_type = MESSAGE`, `source_message_id = messages.id` 제약을 만족하고, 같은 project의 reporter, assignee, sprint 등 기존 Issue service policy를 우회하지 않는다.
5. DM entity 또는 DM endpoint를 사용하지 않는다. DM 메시지 이슈 전환은 `DM_MESSAGE_CANNOT_CREATE_ISSUE` 경계를 유지한다.
6. transaction 경계, 중복 변환 허용 여부, source message를 response에 어느 수준까지 노출할지는 §7의 결정이 확정된 뒤 구현한다.

### 4.2 `4.5.2.1` LINK-03 Backend

1. PR #229에서는 문서 update만 `DOCUMENT_UPDATED` 후보로 확정한다. 문서 create, trash/restore, thread comment event는 이 row의 producer 범위에 포함하지 않는다.
2. event를 생성하기로 승인되면 문서 변경 transaction과 `notification_events`, `notification_recipients`의 원자성, 실패 시 rollback 규칙을 설계한다. `activity_events`와 `notification_events`는 통합하지 않는다.
3. event에는 canonical catalog의 `DOCUMENT_UPDATED` 또는 승인된 type, `DOCUMENT` target, project/company/actor 범위를 사용한다. title/body와 metadata는 민감한 문서 원문을 복제하지 않는 최소 정보로 정한다.
4. PR #229의 수신자는 연결된 issue의 reporter/assignee 중 actor를 제외한 active user, active company member, active project member로 한정한다. event-recipient matrix 전면 구현, thread comment, mention, preference 정책은 후속 결정 전까지 확장하지 않는다.
5. `NotificationService`의 기존 inbox/read-state API는 consumer로만 재사용한다. preference, Push/WebSocket, Redis, producer 범용화는 이 row의 완료 조건으로 넣지 않는다.

PR #229의 `DOCUMENT_UPDATED` producer는 문서 수정과 notification 원장 저장의 strong consistency 정책을 따른다. `DocumentService.update()`와 `DocumentNotificationProducerService.documentUpdated()`는 같은 transaction에서 실행하며 producer는 `Propagation.MANDATORY`를 유지한다. notification event/recipient persistence 또는 eligible recipient lookup 실패는 문서 수정 rollback 조건이다. 단, eligible recipient count가 0인 상태는 실패가 아니므로 문서 수정과 notification event 저장은 commit하고 recipient row는 생성하지 않는다. 이번 PR 범위에는 best-effort 알림, outbox, async retry, Push/WebSocket, Redis, notification preference를 포함하지 않는다.

## 5. Frontend contract gates

| Row | Backend 계약 게이트 | Frontend 착수 전 확인 | 완료 판정 경계 |
|---|---|---|---|
| `4.5.1.2` | from-message endpoint의 path, request body, 성공 response, 400/403/404/409 오류와 source message 조회 권한이 test와 curl로 확인된다. | S09 메시지 action과 S05 이슈 생성 화면이 같은 `projectId`와 오류 상태를 처리한다. DM 화면에는 action을 노출하지 않는다. | Frontend 연동은 `4.5.1.1` 구현 evidence 후 별도 확인한다. 이 문서는 UI 구현 완료를 뜻하지 않는다. |
| `4.5.2.2` | event type, target, recipient 정책, inbox 조회 시 표시할 title/body/target이 승인되고 producer test와 curl evidence가 있다. | S08 문서 수정, 필요 시 S10 thread, S11 inbox가 어떤 event를 표시하는지와 클릭 target을 합의한다. | producer와 consumer의 end-to-end evidence 후 별도 확인한다. Push와 preference UI는 포함하지 않는다. |

## 6. 구현 및 검증 게이트

| 게이트 | 최소 evidence | 통과 기준 |
|---|---|---|
| 단위·서비스 test | `ProjectChatService`, Issue service, `DocumentService`, notification producer 대상의 정상·실패 test | 다른 project source, 비활성 또는 비구성원, DM source, recipient 소유권을 거절한다. |
| HTTP/curl | 인증된 정상 요청과 400/403/404/409 사례를 base URL과 secret 없이 재현 | LINK-01은 project message만 이슈화한다. LINK-03은 승인된 event만 inbox에서 조회된다. |
| DB | `issues`, `messages`, `notification_events`, `notification_recipients`를 transaction 후 직접 대조 | issue source FK와 project 범위가 일치한다. 승인된 event와 recipient만 저장되며 실패 요청은 부분 row를 남기지 않는다. |
| 보안 | authorization negative case와 tenant isolation 확인 | 다른 프로젝트 message를 source로 쓰지 못하고, 다른 사용자 notification recipient를 읽거나 변경하지 못한다. |
| 회귀 | 기존 chat, document, notification consumer test 실행 | 메시지 읽음 위치, 문서 CRUD 및 `LINK-02`, notification inbox/read-state 동작을 훼손하지 않는다. |

테스트, curl, DB, security evidence가 모두 기록되기 전에는 WBS 상태를 완료로 올리지 않는다. 구현 결과와 실행 일시, 사용한 테스트 데이터의 식별자는 secret, token, password, 개인정보 없이 남긴다.

## 7. 미결 결정과 확인 책임

| 미결 사항 | 구현 전 확인 책임 | 확인 결과가 없을 때 처리 |
|---|---|---|
| `4.5`가 LINK-01/LINK-03만 포함하고 LINK-02, DM conversion, auto-create, push/preferences를 제외하는지 | 황스일, WBS Reviewer | 범위를 넓히지 않고 계획 상태를 유지한다. |
| LINK-01 from-message request 필드, Issue 기본값, 중복 변환 정책, 오류 코드 | 여도현, Backend Owner가 초안을 작성하고 황스일이 review | API 구현과 Frontend gate를 열지 않는다. |
| LINK-03 후속 event source, thread, mention, preference, 전체 recipient matrix 확장 여부 | 요구사항 또는 제품 결정 담당자가 정책을 확정하고 황스일이 WBS 경계를 review | PR #229의 `DOCUMENT_UPDATED` producer 범위를 넘겨 구현하지 않는다. |
| `4.5.1.2`, `4.5.2.2`의 화면 action, 오류 표현, target 이동 | 해당 WBS Frontend 담당자가 Backend Owner와 계약을 확인 | Frontend는 mock 의존 또는 완료 선언을 하지 않는다. |
| schema 변경 필요성, migration 방식, ERD/API canonical 변경 | 여도현이 영향도를 제시하고 황스일과 문서 책임자가 확인 | 기존 schema 계약 밖의 column/table을 추가하지 않는다. |

## 8. 기준 문서

- [`Sprint 3 범위 결정`](../../07-decisions/sprint-3-범위-결정.md)
- [`Sprint 3 backlog`](../../00-project/sprint-운영/sprint-3-backlog.md)
- [`기능 분해표`](../../02-requirements/기능-분해표.md)
- [`기능-시나리오-API-ERD 추적표`](../../02-requirements/기능-시나리오-API-ERD-추적표.md)
- [`ERD`](../../03-design/ERD.md)
- [`API 명세서`](../../04-api/API-명세서.md)
