> **작성자**: Threadia 팀 / Sisyphus 지원
> **작성일**: 2026-07-01
> **최종 수정**: 2026-07-01
> **버전**: v0.3
> **상태**: 검토 중
> **태그**: erd, db, schema, traceability

# ERD 상세화 근거표

## 1. 목적

이 문서는 `docs/03-design/ERD.md`를 상세화하고 `db/schema.sql`을 작성하기 전에, 각 table의 근거 문서와 상세화 범위를 고정하기 위한 작업용 근거표다.

현재 `ERD.md`는 table 목록과 핵심 관계는 있으나, Auth/Invitation/조건부 일부를 제외한 주요 MVP table의 column, constraint, index, enum 기준이 부족하다. 따라서 `schema.sql` 작성 전에 table별 근거를 확인하고 상세화 우선순위를 나눈다.

## 2. 적용 원칙

| 원칙 | 내용 |
|---|---|
| Sprint 1 구현 범위 | Auth-only. 구현·검증 중심 table은 `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens`다. |
| `schema.sql` 범위 | `docs/03-design/ERD.md` 기준 전체 MVP DDL을 둘 수 있으나, Sprint 1 검증은 Auth 4개 table 중심이다. |
| schema 관리 방식 | Flyway/Liquibase를 사용하지 않고, `ERD.md`와 `db/schema.sql`을 기준으로 관리한다. |
| 상세화 순서 | 요구사항/추적표/API/화면/보류표 근거 확인 → ERD 상세화 → `schema.sql` 작성 → 변경이력 기록 순서다. |
| 권한 설계 | `users.role`을 만들지 않고 `company_members.role`, `project_members.role`로 판정한다. |
| 시간 기준 | 서버 생성/만료 시각은 UTC 기준으로 저장·비교하고 API 응답은 ISO-8601 UTC `Z` 형식이다. |
| 이메일 기준 | signup, login, password reset, OAuth, invitation lookup은 `trim + lowercase` 정규화 기준을 공유한다. |

## 3. 기준 문서

| 문서 | 사용 목적 |
|---|---|
| `docs/03-design/ERD.md` | table 목록, 현재 상세 정의, Mermaid 관계 기준 |
| `docs/02-requirements/요구사항-정의서.md` | Sprint 범위, 수용 기준, 핵심 불변식 확인 |
| `docs/02-requirements/기능-분해표.md` | 기능 ID와 table/API 연결 확인 |
| `docs/02-requirements/기능-시나리오-API-ERD-추적표.md` | 시나리오별 API/ERD 추적성 확인 |
| `docs/04-api/API-명세서.md` | endpoint, enum, request/response, error case 기준 |
| `docs/03-design/화면정의서.md` | 화면에서 필요한 조회/필터/상태 정보 확인 |
| `docs/07-decisions/DB-schema-관리-정책.md` | ERD → schema.sql → 변경이력 작업 흐름 확인 |
| `docs/07-decisions/보류-기능-최종표.md` | 보류/제외 기능이 schema에 섞이지 않도록 확인 |
| `docs/00-project/sprint-운영/` | Sprint 1 Auth-only, Day 1 schema.sql 생성, DoD 기준 확인 |

## 4. table 상세화 근거표

| 번호 | table | 구현 단계 | 현재 ERD 상세도 | 핵심 근거 | 상세화 때 확정할 내용 | `schema.sql` 반영 판단 |
|---:|---|---|---|---|---|---|
| 1 | `users` | Sprint 1 확정 | 상세 있음 | AUTH-01/02, USER-01, `/auth/signup`, `/auth/login`, `/auth/me`, `/users/me/profile` | `email` unique, `password_hash` NULL 허용, `status`, `avatar_url`, `created_at`, `updated_at`, `users.role` 금지 | 필수 |
| 2 | `oauth_accounts` | Sprint 1 확정 | 상세 있음 | AUTH-03, OAuth callback, provider enum | `provider`, `provider_user_id`, `provider_email`, `UNIQUE(provider, provider_user_id)`, `UNIQUE(user_id, provider)`, blind auto-link 금지 | 필수 |
| 3 | `password_reset_tokens` | Sprint 1 확정 | 상세 있음 | AUTH-04, password reset request/confirm | `token_hash` unique, raw token 저장 금지, `expires_at`, `used_at`, confirm 후 refresh token revoke | 필수 |
| 4 | `refresh_tokens` | Sprint 1 확정 | 상세 있음 | AUTH-02, token refresh, logout, password reset confirm | `token_hash` unique, `expires_at`, `rotated_at`, `revoked_at`, 14일 만료, rotation/invalidation | 필수 |
| 5 | `companies` | MVP reference, Sprint 1 제외 | 요약만 있음 | COMPANY-01/02/04, 회사 생성/조회/수정/로고/통계, Sprint 1.5 seed 회사 | 회사명, 설명, 대표자, 업종, 로고 URL, 상태, 통계 API에 필요한 기준, 생성자가 COMPANY_ADMIN이 되는 정책 확인 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 6 | `departments` | MVP reference, Sprint 1 제외 | 요약만 있음 | DEPT-01~04, 부서 CRUD, 삭제 시 구성원 department NULL | `company_id`, 부서명, 단일 depth, 회사별 부서명 unique, 삭제 정책 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 7 | `company_members` | Sprint 1.5 core, Sprint 1 제외 | 요약만 있음 | RBAC-01, MEMBER-01~04, INVITE accept 시 membership 생성 | `company_id`, `user_id`, `department_id`, `role`, `status`, unique membership, 부서 삭제 시 NULL | 전체 DDL 포함 가능, Sprint 1.5 선행 근거 |
| 8 | `invitations` | Sprint 1.5 company, Sprint 2 project reference | 상세 있음 | INVITE-01~04, MEMBER-02, 회사 invitation, project invitation 분리 | company/project scope, inviter/invitee, role, status, 7일 만료, lazy expiration, 중복 pending 제한 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 9 | `projects` | MVP reference, Sprint 1 제외 | 요약만 있음 | PROJECT-01~04, 회사 하위 프로젝트, archive 정책 | `company_id`, 이름, 설명, 상태, 기간, archive 처리, 생성자 column 포함 여부 TBD, 수정 시각 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 10 | `project_members` | MVP/Sprint 2 reference | 요약만 있음 | PROJECT-MEMBER-01~02, RBAC-01, project invitation accept | `project_id`, `user_id`, role, status, PROJECT_ADMIN 최소 1명 정책, same company active member 제한 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 11 | `sprints` | MVP reference, Sprint 1 제외 | 요약만 있음 | SPRINT-01~02, Sprint 목록/생성/시작/종료/이슈 배정 | `project_id`, 이름, 목표, 기간, 상태, start/complete 시각, 미완료 이슈 처리 기준 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 12 | `issues` | MVP reference, Sprint 1 제외 | 요약만 있음 | ISSUE-01~04, KANBAN-01~02, LINK-01, Sprint 배정, schema 수정필요사항 | `project_id`, `sprint_id`, 작성자, 담당자, title, description, status 5컬럼, priority, due date, `issue_key VARCHAR(40)`, source message, trash/restore pair CHECK | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 13 | `issue_status_history` | MVP reference, Sprint 1 제외 | 요약만 있음 | KANBAN-02, 이슈 상태 변경 이력 | `issue_id`, 이전 status, 변경 status, 변경자, 변경 시각. A/P/C 권한 차이는 service policy로 분리 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 14 | `documents` | MVP reference, Sprint 1 제외 | 요약만 있음 | DOC-01~04, Markdown 문서/회의록, 휴지통/복원 | `project_id`, 작성자, title, content, type/category, trash/restore. 문서 수정 event는 `notification_events`에서 판단 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 15 | `document_issue_links` | MVP reference, Sprint 1 제외 | 요약만 있음 | LINK-02, GRAPH-01/02, 문서-이슈 양방향 연결, schema 수정필요사항 | `project_id`, `document_id`, `issue_id`, 생성자, unique link, 그래프 edge 기준, document/issue/project 정합 service policy | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 16 | `messages` | MVP reference, Sprint 1 제외 | 요약만 있음 | CHAT-01/03, LINK-01, 프로젝트 메시지에서 이슈 생성, API request `threadId`, schema 수정필요사항 | `project_id`, 작성자, `thread_id`, content, source issue 연결 여부, 수정/삭제 API 미제공, `updated_at` 제외, `(project_id, created_at, id)` 조회 index | 전체 DDL 포함 가능. `thread_id` 누락 DDL은 API와 충돌하므로 수정 필요 |
| 17 | `project_chat_member_states` | MVP reference, Sprint 1 제외 | 요약만 있음 | CHAT-03, 프로젝트 채팅 mark-read/unread count | `project_id`, `user_id`, last read message/시각, unread count 계산 기준 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 18 | `direct_message_rooms` | MVP reference, Sprint 1 제외 | 요약만 있음 | CHAT-02/03, 1:1 DM, 다자간 DM 보류 | `company_id`, `user1_id`, `user2_id`, `user1_id < user2_id`, unique room, 같은 회사 구성원 제한 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 19 | `direct_messages` | MVP reference, Sprint 1 제외 | 요약만 있음 | CHAT-02/03, DM 메시지 목록/전송, schema 수정필요사항 | `room_id`, sender, content, 생성 시각, DM 메시지 from-issue 금지, `(room_id, created_at, id)` 조회 index | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 20 | `dm_room_user_states` | MVP reference, Sprint 1 제외 | 요약만 있음 | CHAT-03, DM mark-read/unread count, 상대 read receipt 보류 | `room_id`, `user_id`, last read message/시각, 방 단위 unread position | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 21 | `threads` | MVP reference, Sprint 1 제외 | 요약만 있음 | THREAD-01~03, Issue/Document/Message 대표 쓰레드 | `project_id`, `target_type`, `target_id`, topic, created_by, `UNIQUE(target_type, target_id)` 범위 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 22 | `thread_comments` | MVP reference, Sprint 1 제외 | 요약만 있음 | THREAD-04, 댓글 작성, @멘션 알림 | `thread_id`, 작성자, content, mention notification 생성 기준, 결정 표시 column은 TBD | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 23 | `activity_events` | 조건부 확장 | 상세 있음 | FEED-01, 활동 원장, notification과 분리 | event type, target type/id, metadata JSON, actor nullable, notification event 생성 조건 제한 | 전체 MVP DDL 스켈레톤에는 포함 가능. 실제 활성화는 조건부 확장 |
| 24 | `notification_events` | MVP reference, Sprint 1 제외 | 요약만 있음 | NOTI-01, LINK-03, THREAD-04, invitation event optional, API notification 응답 예시 | event source, type, actor, target, `title`, `body`, 선택적 `activity_event_id`, activity_events와 역할 분리. `route`/`payload_json`은 보류 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 25 | `notification_recipients` | MVP reference, Sprint 1 제외 | 요약만 있음 | NOTI-01, 알림 목록/읽음/read-all, schema 수정필요사항 | `notification_event_id`, recipient user, read_at, ownership check, unread count 기준, `(recipient_user_id, read_at, created_at, id)` 조회 index | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 26 | `notification_preferences` | MVP reference, Sprint 1 제외 | 요약만 있음 | NOTI-02/03, GLOBAL scope, invitation 별도 preference 제외 | `user_id`, scope GLOBAL, event category, enabled, project scope 보류 | 전체 DDL 포함 가능, Sprint 1 구현 제외 |
| 27 | `project_dashboard_summaries` | 조건부 확장 | 상세 있음 | DASH-01~04, project dashboard summary, read model | 원본 아님, aggregate counts, sprint summary, refreshed_at, Redis cache 조건 | 전체 MVP DDL 스켈레톤에는 포함 가능. 실제 활성화는 조건부 확장 |

## 5. 상세화 우선순위

| 우선순위 | 대상 | 이유 |
|---:|---|---|
| 1 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` | Sprint 1 구현·검증 대상이며 Day 2 Auth JPA Entity의 직접 기준이다. |
| 2 | `companies`, `company_members`, `invitations` | Sprint 1.5 invitation과 seed 회사/COMPANY_ADMIN 검증의 기반이다. |
| 3 | `projects`, `project_members` | Sprint 2 project foundation과 RBAC 확장의 기반이다. |
| 4 | `sprints`, `issues`, `issue_status_history` | 이슈/칸반과 Sprint 운영의 core path다. |
| 5 | `documents`, `document_issue_links`, `threads`, `thread_comments` | Threadia의 문서-이슈-논의 연결 핵심 흐름이다. |
| 6 | `messages`, `project_chat_member_states`, `direct_message_rooms`, `direct_messages`, `dm_room_user_states` | 채팅/DM 흐름과 unread count 기준이 필요하다. |
| 7 | `notification_events`, `notification_recipients`, `notification_preferences`, `activity_events` | 알림/활동 원장 분리 기준을 schema로 명확히 해야 한다. |
| 8 | `project_dashboard_summaries` | 조건부 read model이므로 core schema 후 확정한다. |

## 6. ERD 상세화 전 확인할 미결 사항

| 항목 | 현재 근거 | 결정 필요 |
|---|---|---|
| 전체 DDL 포함 범위 | Sprint 운영 문서는 전체 MVP DDL을 둘 수 있다고 하되 Sprint 1 검증은 Auth 4개 table 중심이라고 한다. | 전체 MVP 27개 table 스켈레톤은 둘 수 있다. 단 `activity_events`, `project_dashboard_summaries`의 실제 활성화는 조건부 확장으로 분리한다. |
| `issues` 정렬 | ERD의 6일차 이월 검토 사항에 position 관리 방식이 남아 있다. | Kanban column 내 정렬용 `position` column 포함 여부 |
| `threads` 생성 정책 | 자동 생성 vs 수동 생성 정책이 이월 사항이다. | `UNIQUE(target_type, target_id)`와 get-or-create 기준 확정 |
| `activity_events` → `notification_events` 연계 | 두 원장은 분리하되 필요 시 연계만 허용한다. | 선택적 `activity_event_id`는 수용 가능. 어떤 event가 notification을 생성하는지 enum/metadata 수준은 구현 전 결정 |
| attachments | S3 사용은 확정이나 범용 attachments table은 Sprint 1 이후 확정이다. | 이번 ERD/schema에 attachments를 포함하지 않을지 확인 |
| soft delete 공통화 | project archive, issue/document trash는 확정되어 있다. | `archived_at`, `trashed_at`, `restored_at` 등 column 명명 통일 |
| 조회 성능 index | 채팅/DM/알림 목록은 cursor pagination과 unread filter를 사용한다. | `messages`, `direct_messages`, `notification_recipients` 복합 index를 최종 schema에 반영 |
| 날짜 범위 CHECK | 프로젝트/Sprint 기간, invitation 만료는 역전되면 안 된다. | DB CHECK로 최소 범위 검증을 하고, 세부 정책은 service policy로 보완 |

## 6.1 검증 결과 메모

| 검증 항목 | 결과 | 후속 조치 |
|---|---|---|
| ERD table coverage | 통과. `ERD.md` 요약의 27개 table과 이 근거표의 27개 table이 일치한다. | 이 순서를 기준으로 ERD 상세화 진행 |
| canonical 근거 존재 | 조건부 통과. 모든 table과 기능 ID는 canonical 문서에 존재한다. | 약한 column 추정은 TBD 또는 service policy로 낮춤 |
| `schema.sql` 바로 작성 가능성 | 부분 통과. 전체 MVP 27개 table 스켈레톤은 가능하나, 오후 schema 초안에는 API/ERD와 충돌하는 항목이 있다. | `messages.thread_id`, notification 표시 컬럼 판단, stale 변경이력 문구를 수정 후 canonical 반영 |
| 조건부 확장 table | 통과. `activity_events`, `project_dashboard_summaries`는 전체 MVP DDL 스켈레톤 포함 가능하되 실제 활성화는 조건부 확장으로 분리한다. | ERD와 schema 주석에 구현·검증 범위 분리를 명시 |

## 6.2 오후 schema 초안 리뷰 결과

| 항목 | 판단 | 근거 | 조치 |
|---|---|---|---|
| 27개 table coverage | 수용 | 오후 `schema.sql`에서 `CREATE TABLE` 27개를 확인했다. | 전체 MVP DDL 스켈레톤 방향은 유지한다. |
| API enum 정합화 | 수용 | API 명세서의 `projectStatus`, `sprintStatus`, `documentType` 값과 오후 schema가 대체로 일치한다. | ERD의 `projects.status`, `sprints.status`, `documents.document_type`을 API 기준으로 맞춘다. |
| `messages.thread_id` | 수정 전 blocker | `POST /api/v1/projects/{projectId}/messages` request에 `threadId`가 있으나 오후 schema `messages` table에는 FK가 없다. | 최종 DDL에 `thread_id BIGINT NULL`, FK -> `threads.id`, index를 추가해야 한다. |
| notification 표시 컬럼 | 수정 전 blocker | API 명세서의 `GET /api/v1/notifications` 응답 예시는 `title`, `body`, `target`, `readAt`을 이미 포함한다. | `title`/`body`를 “응답 스키마 없음” 이유로 제거하지 않는다. `route`/`payload_json`만 추가 결정 전 보류한다. |
| C1~C8 상세 제약 | 부분 수용 | `archive_reason`, `chk_inv_scope`, `chk_inv_self`, `chk_inv_role`, `chk_issue_source`, `document_issue_links.link_type=RELATED`는 API/정책 근거가 있다. | ERD에 수용하되 service policy 경계는 유지한다. |
| 오후 변경이력 문서 | 수정 필요 | 오후 `기획문서-변경이력.md`는 15개 table 기준 문구가 남아 있어 27개 table 완료 상태와 맞지 않는다. | canonical 변경이력은 27개 table 상세화 기준으로 기록한다. |

## 6.3 추가 수정필요사항 반영 결과

| 항목 | 판단 | 근거 | 조치 |
|---|---|---|---|
| `issues.issue_key` 길이 | 수용 | `project_key VARCHAR(20)` + `-` + 순번 조합은 20자를 넘을 수 있다. | `issue_key VARCHAR(40)`로 확대한다. |
| 메시지/DM 조회 index | 수용 | 목록 조회는 `project_id`/`room_id` 필터 후 `created_at` 기반 cursor pagination을 수행한다. | `messages(project_id, created_at, id)`, `direct_messages(room_id, created_at, id)`를 추가한다. |
| 알림 조회 index | 수용 | 알림 목록과 unread filter는 사용자, read 상태, 생성 시각을 함께 사용한다. | `notification_recipients(recipient_user_id, read_at, created_at, id)`를 추가한다. |
| `messages.updated_at` | 수용 | MVP 메시지는 수정/삭제 API를 제공하지 않는다. | `messages`에는 `updated_at`을 두지 않는다. |
| soft-delete pair CHECK | 수용 | `trashed_at`만 있고 `trashed_by_user_id`가 없는 불완전 상태를 막아야 한다. | `issues`, `documents`에 pair CHECK를 둔다. |
| archive 상태 CHECK | 부분 수용 | archive 상태와 `archived_at`/`archived_by_user_id` 정합성이 필요하다. | `projects`에는 최소 archive pair CHECK를 둔다. `archive_reason` 필수 여부는 API validation에 둔다. |
| 날짜 range CHECK | 수용 | 기간 역전과 invitation 만료 역전은 DB에서 최소 방어 가능하다. | `projects`, `sprints`, `invitations`에 CHECK를 둔다. |
| service policy 검증 목록 | 수용 | tenant/project/다형성 정합성은 FK로 직접 표현할 수 없다. | ERD `5.21 service policy 검증 항목`과 향후 service test/DoD에 반영한다. |

## 7. 다음 작업

1. 이 근거표를 기준으로 `ERD.md`의 27개 table 상세 정의를 유지한다.
2. 오후 schema 초안의 blocker와 추가 수정필요사항을 반영해 `db/schema.sql`을 생성한다.
3. 실제 repo `db/schema.sql` 작성 내용을 `docs/07-decisions/DB-schema-변경이력.md`에 별도 DB schema 변경 이력으로 기록한다.
4. `docs/07-decisions/기획문서-변경이력.md`에는 canonical ERD 상세화와 schema 생성 변경 묶음을 유지한다.
5. MySQL 8.4 실행 검증은 팀 환경에서 수행한다.
