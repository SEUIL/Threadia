# 기능-시나리오-API-ERD 추적표 v1.0

> **작성자**: Threadia 팀  
> **작성일**: 2026-06-15  
> **최종 수정**: 2026-06-17  
> **버전**: 6일차 수정본 v1
> **상태**: 6일차 수정본
> **수정 근거**: Threadia-기획문서-검수리포트.md
> **기준 문서**: 기능 분해표 v3.0, 사용자 시나리오 v3.0, API 명세서 v2.0, ERD v2.0, 화면정의서_v3, 보류 기능 최종표 v1.0

---

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows이며, Sprint 1 DB migration은 `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens`만 포함한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.


## 1. 문서 목적

이 문서는 Threadia 4일차 최종 산출물 사이의 MVP 추적성을 확인하기 위한 reference다. Phase A Sprint 1 구현 target은 Auth only로 제한한다. 기능 ID가 어떤 사용자 시나리오에서 사용되고, 어떤 API와 ERD 테이블로 구현되는지 한눈에 검증하는 것을 목적으로 한다.

검증 원칙은 다음과 같다.

- MVP와 Sprint 1 확정 기능은 사용자 시나리오, API, ERD 중 하나 이상과 연결되어야 한다.
- 조건부 확장 기능은 Sprint 1 기본 범위와 분리하되, 도입 시 사용할 API와 데이터 원장을 명시한다.
- 보류 기능은 필수 시나리오와 필수 API에 남기지 않고, 보류표 또는 stub 정책으로만 관리한다.

---

## 2. 추적 상태 기준

| 상태 | 의미 |
|---|---|
| 연결 완료 | 기능, 시나리오, API, ERD가 모두 연결됨 |
| API 중심 | 별도 사용자 시나리오보다 API/화면 동작으로 검증하는 기능 |
| 데이터 중심 | 기능은 명확하나 ERD/권한 정책이 핵심인 기능 |
| 조건부 확장 | Sprint 1 기본 범위 밖이지만 기술 계획과 도입 조건이 확정됨 |
| 보류/제외 | Sprint 1 필수 흐름에서 제거하고 보류표 또는 stub으로만 관리 |

---

## 3. 시나리오별 추적표

| 시나리오 | Actor | 연결 기능 | 주요 API | 주요 ERD | 화면 | 상태 |
|---|---|---|---|---|---|---|
| SC-AUTH-01 인증과 프로필 설정 | 비로그인, A/P/C | AUTH-01, AUTH-02, AUTH-03, AUTH-04. USER-01은 MVP reference이며 Sprint 1 구현 제외 | `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`, `GET /oauth2/authorization/{provider}`, `GET /api/v1/auth/oauth2/callback/{provider}`, `POST /api/v1/auth/password-reset/request`, `POST /api/v1/auth/password-reset/confirm`, `PATCH /api/v1/users/me/profile` | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` | S01, S02, S03 | 연결 완료 |
| SC-C-01 회사 관리자 초기 세팅 | C | COMPANY-01, COMPANY-02, DEPT-01~04, MEMBER-01~04, RBAC-01 | `POST /api/v1/companies`, `GET/PATCH /api/v1/companies/{companyId}`, `POST /api/v1/companies/{companyId}/logo`, `GET/POST /api/v1/companies/{companyId}/departments`, `PATCH/DELETE /api/v1/departments/{departmentId}`, `GET /api/v1/companies/{companyId}/company-members`, `GET /api/v1/users/lookup`, `POST/GET /api/v1/companies/{companyId}/invitations`, `POST /api/v1/invitations/{invitationId}/accept`, `POST /api/v1/invitations/{invitationId}/decline`, `POST /api/v1/invitations/{invitationId}/cancel`, `PATCH /api/v1/company-members/{companyMemberId}` | `companies`, `departments`, `company_members`, `invitations`, `users` | S13, S14, S04, S12 | 연결 완료 |
| SC-C-02 회사 관리자 모니터링과 의사결정 | C | PROJECT-01, ISSUE-01, THREAD-01, NOTI-01, COMPANY-04 | `GET /api/v1/companies/{companyId}/projects`, `GET /api/v1/projects/{projectId}/issues`, `GET /api/v1/projects/{projectId}/threads`, `POST /api/v1/threads/{threadId}/comments`, `GET /api/v1/notifications`, `GET /api/v1/companies/{companyId}/stats` | `projects`, `issues`, `threads`, `thread_comments`, `notification_events`, `notification_recipients`, `companies` | S04, S05, S10, S11, S13 | 연결 완료 |
| SC-P-01 프로젝트 관리자 프로젝트 운영 | P | PROJECT-02, PROJECT-03, PROJECT-04, PROJECT-MEMBER-01, PROJECT-MEMBER-02, SPRINT-01, SPRINT-02, ISSUE-01, ISSUE-02, ISSUE-03, KANBAN-01, KANBAN-02, DOC-01, DOC-02, GRAPH-01, GRAPH-02, NOTI-01 | `POST /api/v1/companies/{companyId}/projects`, `GET/PATCH /api/v1/projects/{projectId}`, `POST /api/v1/projects/{projectId}/archive`, `GET /api/v1/projects/{projectId}/project-members`, `POST/GET /api/v1/projects/{projectId}/invitations`, `PATCH/DELETE /api/v1/project-members/{projectMemberId}`, `GET/POST /api/v1/projects/{projectId}/sprints`, `POST /api/v1/sprints/{sprintId}/start`, `POST /api/v1/sprints/{sprintId}/complete`, `GET/POST /api/v1/projects/{projectId}/issues`, `PATCH /api/v1/issues/{issueId}/status`, `GET/POST /api/v1/projects/{projectId}/documents`, `GET /api/v1/projects/{projectId}/documents/graph` | `projects`, `project_members`, `invitations`, `sprints`, `issues`, `issue_status_history`, `documents`, `document_issue_links`, `notification_events` | S04, S05, S06, S07, S08, S10, S11, S12 | 연결 완료 |
| SC-A-01 일반 사용자 업무 수행 | A | KANBAN-01, KANBAN-02, ISSUE-01, ISSUE-02, THREAD-01, CHAT-01, CHAT-02, CHAT-03, DOC-01, DOC-02, LINK-01, LINK-02, GRAPH-01, NOTI-01 | `GET /api/v1/notifications`, `GET /api/v1/projects/{projectId}/issues`, `PATCH /api/v1/issues/{issueId}/status`, `GET/POST /api/v1/projects/{projectId}/threads`, `POST /api/v1/threads/{threadId}/comments`, `GET/POST /api/v1/projects/{projectId}/messages`, `GET/POST /api/v1/companies/{companyId}/direct-messages/rooms`, `GET/POST /api/v1/direct-messages/rooms/{roomId}/messages`, `GET/POST /api/v1/projects/{projectId}/documents`, `POST /api/v1/projects/{projectId}/document-issue-links` | `notification_events`, `notification_recipients`, `issues`, `threads`, `thread_comments`, `messages`, `direct_message_rooms`, `direct_messages`, `documents`, `document_issue_links` | S04, S05, S06, S07, S08, S09, S10, S11 | 연결 완료 |
| SC-LINK-01 문서-이슈-메시지 연결 | A/P/C | LINK-01, LINK-02, LINK-03, THREAD-01, THREAD-02, DOC-01, CHAT-01, GRAPH-01, GRAPH-02, NOTI-01 | `POST /api/v1/projects/{projectId}/issues/from-message`, `GET /api/v1/messages/{messageId}`, `GET /api/v1/issues/{issueId}/documents`, `GET /api/v1/documents/{documentId}/issues`, `POST /api/v1/projects/{projectId}/document-issue-links`, `DELETE /api/v1/document-issue-links/{linkId}`, `GET /api/v1/projects/{projectId}/documents/graph`, `POST /api/v1/projects/{projectId}/threads`, `POST /api/v1/threads/{threadId}/comments` | `messages`, `issues`, `documents`, `document_issue_links`, `threads`, `thread_comments`, `notification_events` | S05, S07, S08, S09, S10, S11 | 연결 완료 |

---

## 4. 기능 그룹별 API/ERD 추적표

| 기능 그룹 | 기능 ID | 우선순위 | 사용자 흐름 | API 범위 | ERD 범위 | 정합성 메모 |
|---|---|---|---|---|---|---|
| 인증 | AUTH-01, AUTH-02 | Sprint 1 확정 | SC-AUTH-01 | 회원가입, 로그인, 로그아웃, token refresh, 현재 사용자 컨텍스트 | `users`, `refresh_tokens` | 이메일 기반 진입과 refresh token rotation은 Sprint 1 인증 범위 |
| 소셜 로그인 | AUTH-03 | Sprint 1 확정 | SC-AUTH-01, SC-C-01 | OAuth 시작/callback | `oauth_accounts`, `users` | Sprint 1 제외 후보에서 확정 기능으로 이동 |
| 비밀번호 재설정 | AUTH-04 | Sprint 1 확정 | SC-AUTH-01 | reset 요청/확정 | `password_reset_tokens`, `users` | token 원문 저장 금지, hash/만료/사용 시각 관리 |
| Invitation | INVITE-01~04 | Sprint 1.5 | SC-C-01, SC-P-01 | 사용자 lookup, 회사/프로젝트 초대 생성/목록, 내 초대 목록, accept/decline/cancel | `invitations`, `users`, `company_members`, `project_members` | 등록 사용자 app-internal invitation, 기본 7일 만료, lazy expiration, 프로젝트 초대는 같은 회사 ACTIVE member 대상 |
| 프로필 | USER-01 | MVP | SC-AUTH-01, SC-C-01 | 내 프로필 수정 | `users.avatar_url` | 파일 업로드가 아니라 URL 저장으로 시작 |
| 권한 | RBAC-01 | MVP | 전체 시나리오 | 모든 인증 API 권한 검사 | `company_members.role`, `project_members.role` | 사용자 단일 role 필드 금지 |
| 회사/부서 | COMPANY-01, COMPANY-02, COMPANY-04, DEPT-01~04 | MVP | SC-C-01, SC-C-02 | 회사 생성/조회/수정/로고/통계, 부서 CRUD | `companies`, `departments`, `company_members` | 부서 삭제 시 멤버 `department_id = NULL` |
| 구성원/권한 | MEMBER-01~04, INVITE-01~04 | Sprint 1.5 | SC-C-01, SC-P-01 | 등록 사용자 lookup, 회사/프로젝트 invitation 생성/조회, 내 초대 조회, accept/decline/cancel, 역할 변경, PM 지정 | `users`, `company_members`, `project_members`, `invitations` | membership은 invitation accept 시 생성한다. 프로젝트 invitation은 같은 회사 ACTIVE member에게만 생성한다 |
| 프로젝트 | PROJECT-01~04, PROJECT-MEMBER-01~02 | MVP | SC-C-02, SC-P-01 | 프로젝트 목록/생성/조회/수정/아카이빙, 멤버 관리 | `projects`, `project_members` | 삭제 대신 `ARCHIVED`, 복원은 보류 |
| Sprint | SPRINT-01, SPRINT-02 | MVP | SC-P-01, SC-A-01 | Sprint 목록/생성/상세/수정/시작/종료/이슈 배정 | `sprints`, `issues`, `notification_events` | 시작 시 알림 발송 후보 |
| 이슈/칸반 | ISSUE-01~04, KANBAN-01~02 | MVP | SC-P-01, SC-A-01, SC-LINK-01 | 이슈 목록/생성/상세/수정/상태 변경/휴지통/복원/from-message | `issues`, `issue_status_history`, `messages` | 5컬럼 고정, A는 `DONE` 이동 불가 |
| 문서/회의록 | DOC-01~04 | MVP | SC-P-01, SC-A-01, SC-LINK-01 | 문서 목록/생성/상세/수정/휴지통/복원 | `documents` | `.md` 저장 기준, 영구 삭제 보류 |
| 문서-이슈 연결/그래프 | LINK-02, GRAPH-01, GRAPH-02 | MVP | SC-P-01, SC-A-01, SC-LINK-01 | 문서-이슈 연결 생성/해제/조회, 그래프 조회 | `document_issue_links`, `documents`, `issues` | S07/S08 그래프 뷰에 edge 반영 |
| 프로젝트 채팅 | CHAT-01, CHAT-03, LINK-01 | MVP | SC-A-01, SC-LINK-01 | 프로젝트 메시지 목록/작성/상세/읽음/unread/from-message | `messages`, `project_chat_member_states`, `issues` | 프로젝트 메시지만 이슈 전환 가능 |
| 1:1 DM | CHAT-02, CHAT-03 | MVP | SC-A-01 | DM 방 목록/생성, DM 메시지 조회/전송/읽음/unread | `direct_message_rooms`, `direct_messages`, `dm_room_user_states` | 다자간 DM, 상대 read receipt는 보류 |
| 쓰레드 | THREAD-01~04 | MVP | SC-C-02, SC-P-01, SC-A-01, SC-LINK-01 | 대상별 쓰레드 조회/생성, 상세, topic 수정, 댓글 작성 | `threads`, `thread_comments`, `notification_events` | 대상 1개당 대표 쓰레드 1개 |
| 알림 | NOTI-01~03 | MVP | 전체 시나리오 | 알림 목록/unread/read/read-all, 알림 설정 조회/수정 | `notification_events`, `notification_recipients`, `notification_preferences` | Sprint 1은 GLOBAL scope만 허용 |

---

## 5. 조건부 확장 추적표

| 기능 | 기능 ID | 연결 시나리오 | API | ERD/기술 원장 | Sprint 1 처리 | 보류/제외 경계 |
|---|---|---|---|---|---|---|
| 프로젝트 활동 피드 | FEED-01 | 조건부 확장 흐름 | `GET /api/v1/projects/{projectId}/activity-events` | `activity_events`, STOMP push, Redis cache | 기본 범위 제외, 조건부 구현 가능 | NoSQL feed store 제외 |
| 통합 검색 | SEARCH-01 | 조건부 확장 흐름 | `GET /api/v1/search` | MySQL `LIKE` 또는 FULLTEXT | 기본 범위 제외, MySQL 기반 제한 검색 우선 | Typesense/Meilisearch는 보류 |
| 프로젝트 홈/대시보드 | DASH-01~04 | 조건부 확장 흐름 | `GET /api/v1/projects/{projectId}/dashboard` | MySQL aggregate, `project_dashboard_summaries`, Redis cache | 기본 범위 제외, 조건부 구현 가능 | 별도 OLAP/검색 엔진 제외 |
| 파일/이미지 첨부 | ATTACH-01 | MVP 이후/보류 흐름 | `/api/v1/attachments` 미제공 | attachments 테이블/S3 presigned URL 정책 | 선택, S3 사용 확정, 구현은 Sprint 1 이후 | 회사 로고와 프로필 URL은 별도 처리 |

---

## 6. 보류/Stub 역추적표

| 보류 항목 | 기능 ID 또는 API 코드 | 최종 처리 | 필수 시나리오 잔존 여부 | API/ERD 잔존 방식 |
|---|---|---|---|---|
| 플랫폼 관리자 F/S15 | DEFER-01, S15 | Sprint 2 최우선 후보 | 필수 시나리오 없음 | 화면정의서에 보류 화면으로만 남김 |
| NoSQL 알림/활동 저장소 | DEFER-02 | Sprint 1 제외 | 필수 시나리오 없음 | ERD 미포함, MySQL 원장 사용 |
| Redis 원본 저장 | 없음 | 제외 | 필수 시나리오 없음 | ERD 미포함, cache/push 보조만 허용 |
| 칸반 컬럼 커스터마이징 | DEFER-03 | Sprint 2 | 필수 시나리오 없음 | 5컬럼 enum만 API/ERD 반영 |
| 동시 문서 편집 | DEFER-04 | Sprint 2 | 필수 시나리오 없음 | 문서 수정 API만 제공 |
| 다자간 DM | DEFER-05 | Sprint 2 | 필수 시나리오 없음 | `direct_message_rooms`는 1:1 unique 기준 |
| DM 상대 read receipt | DEFER-06 | Sprint 2 | 필수 시나리오 없음 | 방 단위 unread/read position만 제공 |
| DM 메시지→이슈 전환 | `DM_MESSAGE_CANNOT_CREATE_ISSUE` | 미제공 | 필수 시나리오에서 제외 조건 명시 | 프로젝트 메시지 from-message만 제공 |
| 이슈/문서 영구 삭제 | `PERMANENT_DELETE_DEFERRED` | 미제공 | 필수 시나리오 없음 | 휴지통/복원만 제공 |
| 프로젝트 복원 | `PROJECT_RESTORE_DEFERRED` | 미제공 | 필수 시나리오 없음 | archive만 제공 |
| 프로젝트별 알림 설정 | `PROJECT_NOTIFICATION_SCOPE_DEFERRED` | 미제공 | 필수 시나리오 없음 | `notification_preferences`는 GLOBAL scope |
| Typesense/Meilisearch | SEARCH 보류 후보 | Sprint 1 보류 | 필수 시나리오 없음 | ERD 미포함, MySQL 검색 우선 |

---

## 7. API-ERD 핵심 불변식

| 불변식 | 적용 문서 | 검증 결과 |
|---|---|---|
| OAuth provider 정보는 API의 provider enum과 ERD `oauth_accounts.provider`가 일치해야 한다. | API 명세서 v2.0, ERD v2.0 | 일치 |
| 비밀번호 reset token은 API에서 원문을 받고 ERD에는 hash로 저장한다. | API 명세서 v2.0, ERD v2.0 | 일치 |
| refresh token은 raw token이 아니라 `refresh_tokens.token_hash`로 저장하고 rotate/revoke/expire 상태를 추적한다. | API 명세서 v2.3, ERD v2.0 | 일치 |
| invitation은 `invitations`를 생성하고 대상자가 accept할 때 membership을 만든다. | API 명세서 v2.3, ERD v2.0, 화면정의서_v3 | 일치 |
| 권한은 `users.role`이 아니라 회사/프로젝트 소속 테이블에서 판정한다. | 기능 분해표 v3.0, ERD v2.0, 화면정의서_v3 | 일치 |
| 프로젝트 삭제는 물리 삭제가 아니라 `ARCHIVED` 상태로 처리한다. | API 명세서 v2.0, ERD v2.0, 화면정의서_v3 | 일치 |
| 이슈/문서는 영구 삭제 없이 휴지통/복원까지만 제공한다. | API 명세서 v2.0, 화면정의서_v3, 보류 기능 최종표 v1.0 | 일치 |
| DM은 1:1만 허용하고 다자간 DM은 Sprint 2로 보류한다. | 사용자 시나리오 v3.0, API 명세서 v2.0, ERD v2.0 | 일치 |
| 활동 피드와 알림은 역할을 분리한다. `activity_events`는 활동 원장, `notification_events`는 알림 사건 원본이다. | ERD v2.0, 보류 기능 최종표 v1.0 | 6일차 운영 기준 추가 검토 필요 |
| Redis는 원본 저장소가 아니라 cache/push 보조 계층이다. | ERD v2.0, 보류 기능 최종표 v1.0, 조건부 확장 계획서 | 일치 |

---

## 8. 검증 체크리스트

- [x] 모든 주요 사용자 시나리오가 기능 분해표의 기능 ID와 연결된다.
- [x] 모든 주요 사용자 시나리오가 API 명세서의 endpoint 또는 명시적 비API 정책과 연결된다.
- [x] API에서 생성·수정·조회하는 주요 데이터가 ERD 테이블과 연결된다.
- [x] `소셜 로그인`, `비밀번호 찾기/재설정`, `프로필 사진`은 Sprint 1/MVP 범위로 추적된다.
- [x] `FEED/SEARCH/DASH`는 보류가 아니라 조건부 확장으로 추적된다.
- [x] 보류 기능은 필수 시나리오에서 제거되거나 제한 조건으로 명시된다.
- [x] Redis, STOMP push, summary table은 원본 데이터가 아닌 조건부 확장 계층으로 추적된다.
- [ ] 6일차에 `activity_events`와 `notification_events` 운영상 분리/통합 기준을 확정한다.
- [x] S3 사용 범위는 확정했다. attachments 테이블과 presigned URL 상세 정책은 Sprint 1 이후 구현 단계에서 확정한다.

---

## Phase B invitation 반영 메모

> **생성일**: 2026-06-16
> **상태**: 공식 결정 반영 완료
> **기준**: `.sisyphus/evidence/task-5-invitation-impact-matrix.md`의 affected row와 2026-06-16 공식 decision set을 사용한다.

### 반영 범위

1. Phase B는 Sprint 1.5의 등록 사용자 대상 app-internal invitation 정책을 반영한다.
2. Sprint 1은 Auth-only이며 invitation을 구현하지 않는다.
3. Sprint 1.5부터 회사/프로젝트 멤버 추가는 즉시 membership 생성이 아니라 `invitations` 생성과 대상자 accept 후 membership 생성으로 처리한다.
4. Sprint 2는 actual invitee email sending, token link acceptance, unregistered onboarding, resend, email template, delivery failure handling, batch cleanup/operations의 후보 시점이며 확정 구현 commitment는 아니다.

### invitation 공식 결정

1. Sprint 1.5 포함 범위는 `invitations` table, `PENDING / ACCEPTED / DECLINED / CANCELED / EXPIRED`, company invitation, project invitation, exact email lookup for registered users, received invitation list, accept/decline/cancel, membership creation on accept, internal invitation-related `notification_events`다.
2. Sprint 1.5 제외 범위는 actual email sending to invitees, invitation token link, unregistered-user invitation, signup/onboarding auto-accept, resend다.
3. `notification_events`에는 필요 시 `INVITATION_CREATED`, `INVITATION_ACCEPTED`, `INVITATION_DECLINED`, `MEMBER_ADDED`를 내부 event 원장으로 추가한다. Push 알림, 별도 Invitation notification preference, S11 사용자 알림 UI 필수 구현은 Sprint 1.5 범위가 아니다.
4. Sprint 1.5에서는 user-facing `notification_preferences`에 별도 Invitation category를 노출하지 않고 기존 global notification preference policy를 따른다.
5. `EXPIRED`와 `expires_at`을 유지하고 기본 만료 기간은 7일로 둔다. read/accept/decline/cancel 시 lazy expiration을 확인하며, 만료된 `PENDING` invitation accept는 `409 INVITATION_EXPIRED`로 거절한다.
6. Sprint 1.5에는 필수 batch scheduler를 추가하지 않는다. Batch cleanup/sweeper는 Sprint 2 이후 admin list hygiene, retention, metrics, operational cleanup 필요에 따라 검토한다.
