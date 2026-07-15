> **작성자**: Threadia 팀
> **작성일**: 2026-06-16
> **최종 수정**: 2026-07-04
> **버전**: WBS 기준 일정·배정 동기화 v2
> **상태**: 확정
> **태그**: sprint-1, backlog, auth, invitation
> **수정 근거**: Threadia-기획문서-검수리포트.md, DB-schema-관리-정책.md, Sprint 1 schema.sql 작업 반영, WBS 본시트 1.x 기준 일정·배정 동기화

# Sprint 1 Backlog v0.2, 6일차 수정본

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows다. DB schema는 `docs/03-design/ERD.md`와 `db/schema.sql`을 기준으로 관리하고, Spring Boot/JPA는 schema 생성이 아니라 mapping 검증에 사용한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.

## 작업 묶음 1. Sprint 1 Backlog 초안

| WBS ID | 계획 구간 | 작업 | Owner | Reviewer | 지원 담당 | 완료 조건 |
|---|---|---|---|---|---|---|
| 1.1.1 | Day 1 | Spring Boot 기본 프로젝트 생성 | 여도현 | 황스일 | 여도현 | local에서 서버 실행 가능 |
| 1.1.2 | Day 1 | MySQL 8.4 연결 확인 | 여도현 | 황스일 | 여도현 | test DB 연결 성공 |
| 1.1.2-A | Day 1 | ERD.md 기준 db/schema.sql 생성 | 김민정 | 황스일 | 김태준, 김민경 | `docs/03-design/ERD.md` 기준 MySQL 8.4용 `db/schema.sql` 생성 |
| 1.1.3 | Day 1 | Frontend 프로젝트 구조 생성 | 정철진 | 김민정 | 정철진 | 라우팅, 기본 layout 구성 |
| 1.1.4 | Day 1 | CI 기준 확인 + AGENTS.md 등 규칙 확정 | 황스일 | 김태준 | 황스일 | CI workflow 기준과 root/docs 규칙 검토 완료 |
| 1.1.5 | Day 1 | PR Template 생성 확인 | 황스일 | 김민정 | 황스일 | `.github/PULL_REQUEST_TEMPLATE.md` 생성 또는 존재 확인 |
| 1.1.6 | Day 1 | Google OAuth 앱 등록 | 황스일 | 여도현 | 황스일 | Google OAuth Client ID/Secret 발급 및 callback URL 등록 |
| 1.2.1 | Day 2 | 인증 JPA Entity 구현 | 김태준 | 황스일 | 김민정 | Auth JPA Entity 클래스 구현 |
| 1.2.2 | Day 2 | 로그인/회원가입 화면 레이아웃 작업 | 정철진 | 김민정 | 정철진 | 로그인/회원가입 화면 UI 구현 |
| 1.2.3 | Day 2 | JWT/Auth hardening 정책 반영 확인 | 여도현 | 김태준 | 여도현 | JWT/Auth hardening 결정 기준 확인 |
| 1.3.1 | Day 3 AM | 이메일 회원가입/로그인 API 구현 | 김민경 | 황스일 | 김민정 | 이메일 Auth API 구현 |
| 1.3.2 | Day 3 AM | 소셜 로그인 API 구현 | 여도현 | 황스일 | 여도현 | Google OAuth 로그인 API 구현 |
| 1.3.3 | Day 3 AM | 로그인/회원가입 화면 마무리 작업 | 정철진 | 김태준 | 정철진 | Auth 화면 UI 완성과 API 연동 준비 |
| 1.4.1 | Day 3 PM | 비밀번호 재설정 API 구현 | 김태준 | 황스일 | 여도현 | 비밀번호 재설정 API 구현 |
| 1.4.2 | Day 3 PM | 로그인/회원가입 화면 API 연동 | 정철진 | 김민정 | - | Auth 화면 API 연동 |
| 1.4.3 | Day 3 PM | Apidog(postman 대체) 컬렉션 정리 / API 문서화 | 김민정 | 김태준 | - | Auth API 요청/응답 예시 정리 |
| 1.5.1 | Day 3 QA | 인증 흐름 end-to-end 테스트 | 여도현 | 정철진 | 김태준 | Auth E2E 검증 결과 |
| 1.5.2 | Day 3 QA | 버그 수정 / 버퍼 | 김민경 | 여도현 | - | 통합 테스트 발견 이슈 수정 결과 |
| 1.5.3 | Day 3 QA | 6일차 보고 입력값 정리 | 김민정 | 황스일 | - | 6일차 보고용 요약 입력값 |

> 회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림 Entity·API는 이 Backlog에서 제외했다. 다음 스프린트 Backlog 작성 시 ERD v2.0 영역 2~4(이전 v0.1 버전 참고)를 그대로 가져와 쓰면 된다.

---

## 작업 묶음 1-A. 일자별 분할 (WBS 1.x 압축 일정)

> 기준: Google Sheets `WBS` 본시트 1.x row. Sprint 1 운영 기간은 2026-07-01부터 2026-07-07까지이며, Day 3를 AM/PM/QA로 압축해 인증 API, 화면 연동, 통합 검증을 같은 날 처리한다.
> 인원: 황스일, 여도현, 정철진, 김민정, 김민경, 김태준 6명.
> 배치 원칙: Owner, Reviewer, 지원 담당은 WBS 본시트 L:N 값을 기준으로 한다. 지원 담당이 비어 있는 row는 WBS 상 미기재로 두고, 실제 지원은 PR/작업 기록에서 보강한다.

### Day 1, 2026-07-01 — 개발 기반 구축

| WBS ID | 작업 | Owner | Reviewer | 지원 담당 | 완료 조건 |
|---|---|---|---|---|---|
| 1.1.1 | Spring Boot 기본 프로젝트 생성 | 여도현 | 황스일 | 여도현 | `./gradlew bootRun` 성공 |
| 1.1.2 | MySQL 8.4 연결 확인 | 여도현 | 황스일 | 여도현 | test DB 연결 성공 |
| 1.1.2-A | ERD.md 기준 db/schema.sql 생성 | 김민정 | 황스일 | 김태준, 김민경 | 전체 MVP 기준 `db/schema.sql` 생성, Sprint 1 Auth 4개 table DDL 기준 확인 |
| 1.1.3 | Frontend 프로젝트 구조 생성 | 정철진 | 김민정 | 정철진 | 라우팅, 기본 layout 구성 |
| 1.1.4 | CI 기준 확인 + AGENTS.md 등 규칙 확정 | 황스일 | 김태준 | 황스일 | CI workflow 기준과 root/docs 규칙 검토 완료 |
| 1.1.5 | PR Template 생성 확인 | 황스일 | 김민정 | 황스일 | `.github/PULL_REQUEST_TEMPLATE.md` 생성 또는 존재 확인 |
| 1.1.6 | Google OAuth 앱 등록 | 황스일 | 여도현 | 황스일 | Google OAuth Client ID/Secret 발급 및 callback URL 등록 |

### Day 2, 2026-07-02 — 인증 데이터/정책

| WBS ID | 작업 | Owner | Reviewer | 지원 담당 | 완료 조건 |
|---|---|---|---|---|---|
| 1.2.1 | 인증 JPA Entity 구현 | 김태준 | 황스일 | 김민정 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` JPA Entity 구현 |
| 1.2.2 | 로그인/회원가입 화면 레이아웃 작업 | 정철진 | 김민정 | 정철진 | 로그인/회원가입 화면 UI 구현 |
| 1.2.3 | JWT/Auth hardening 정책 반영 확인 | 여도현 | 김태준 | 여도현 | access/refresh token, rotation/logout invalidation, email 정규화, OAuth blind auto-link 금지, non-ACTIVE 인증 차단 기준 확인 |

### Day 3 AM, 2026-07-07 — Auth API/OAuth

| WBS ID | 작업 | Owner | Reviewer | 지원 담당 | 완료 조건 |
|---|---|---|---|---|---|
| 1.3.1 | 이메일 회원가입/로그인 API 구현 | 김민경 | 황스일 | 김민정 | signup/login/logout/me 구현과 검증 |
| 1.3.2 | 소셜 로그인 API 구현 | 여도현 | 황스일 | 여도현 | Google OAuth 시작/callback 구현과 redirect 확인 |
| 1.3.3 | 로그인/회원가입 화면 마무리 작업 | 정철진 | 김태준 | 정철진 | Auth 화면 UI 완성과 API 연동 준비 |

### Day 3 PM, 2026-07-07 — Password Reset/연동

| WBS ID | 작업 | Owner | Reviewer | 지원 담당 | 완료 조건 |
|---|---|---|---|---|---|
| 1.4.1 | 비밀번호 재설정 API 구현 | 김태준 | 황스일 | 여도현 | reset 요청/확인 흐름 구현, refresh token revoke 기준 반영 |
| 1.4.2 | 로그인/회원가입 화면 API 연동 | 정철진 | 김민정 | - | 로그인/회원가입 화면에서 실제 API 호출 확인 |
| 1.4.3 | Apidog(postman 대체) 컬렉션 정리 / API 문서화 | 김민정 | 김태준 | - | Auth API 요청/응답 예시 정리 |

### Day 3 QA, 2026-07-07 — 통합 테스트/버퍼

| WBS ID | 작업 | Owner | Reviewer | 지원 담당 | 완료 조건 |
|---|---|---|---|---|---|
| 1.5.1 | 인증 흐름 end-to-end 테스트 | 여도현 | 정철진 | 김태준 | signup → login → logout → OAuth → password reset 흐름 검증 |
| 1.5.2 | 버그 수정 / 버퍼 | 김민경 | 여도현 | - | 통합 테스트 발견 이슈 수정 결과 정리 |
| 1.5.3 | 6일차 보고 입력값 정리 | 김민정 | 황스일 | - | Sprint 1 목표, Backlog, DoD, 검증, blocker 요약 |

> 기존 Day 4~5 표기는 WBS 본시트 기준 Day 3 PM/QA 압축 일정으로 대체한다. Sprint 1 범위는 Auth-only로 유지하며, 회사/프로젝트 기반 기능은 Sprint 2 계획 문서에서 WBS 3.x 기준으로 다룬다.

---


## 작업 묶음 1-B. Sprint 1.5 Invitation Backlog

> Sprint 1은 Auth-only로 유지한다. 아래 작업은 Sprint 1 완료 후 별도 Sprint 1.5로 착수한다.

| WBS ID | 작업 | Owner | Reviewer | 지원 담당 | 완료 조건 | 검증 방법 |
|---|---|---|---|---|---|---|
| 2.1 | `invitations` DB/entity와 seed 검증 기반 구현 | 여도현 | 정철진 | - | ERD 기준 `invitations`와 users/companies/company_members 관계 mapping 완료 | repository test 또는 `db/schema.sql` 기준 DB 확인 |
| 2.2 | 등록 사용자 exact email lookup API 구현 | 김민경 | 여도현 | - | exact email lookup으로 등록 사용자만 반환 | curl 또는 Postman |
| 2.3 | 회사 invitation 생성/목록 API 구현 | 김태준 | 정철진 | - | create/list, duplicate pending, already member, 7일 expiresAt 처리 | curl 또는 Postman |
| 2.4 | 내 invitation 목록과 accept/decline/cancel 구현 | 여도현 | 황스일 | 김태준 | 수락 시 membership 생성, decline/cancel/expired 실패 처리 | API 정상/실패 케이스 |
| 2.5 | S14 구성원·권한 관리 화면 invitation 연결 | 정철진 | 김민정 | 정철진 | pending/accepted/declined/canceled/expired 상태 표시 | 브라우저 확인 |
| 2.6 | Sprint 1.5 문서와 DoD 동기화, QA gate | 김민정 | 황스일 | - | API, ERD, 화면, 추적표, DoD가 seed bootstrap, notification 분리, UTC, email 정규화, OAuth link, refresh token revoke, ACTIVE 인증 기준과 충돌하지 않음 | 문서 리뷰 |

### Sprint 1.5 완료 기준

- Sprint 1 Auth-only 산출물을 건드리지 않고 invitation 기능을 별도 범위로 검증한다.
- `expiresAt`은 기본 7일 값으로 응답하며 null 예시를 쓰지 않는다. 서버 생성/만료 시각은 UTC 기준으로 생성·저장·비교하고 API 응답은 ISO-8601 UTC `Z` 형식을 사용한다.
- membership은 invitation accept 시점에만 생성한다.
- Sprint 1.5 검증용 최초 회사와 `COMPANY_ADMIN`은 local/dev/demo seed로 준비하고, `POST /api/v1/companies`는 Sprint 1.5 구현 범위에서 제외한다.
- actual email sending, token link, 미가입 사용자 초대, signup auto-accept, resend, 필수 batch scheduler는 제외 범위로 남긴다.

---

## Phase B invitation 반영 메모

> **생성일**: 2026-06-16
> **상태**: 공식 결정 반영 완료
> **기준**: `.sisyphus/evidence/task-5-invitation-impact-matrix.md`의 affected row와 2026-06-16 공식 decision set을 사용한다.

### 반영 범위

1. Phase B는 Sprint 1.5의 등록 사용자 대상 app-internal invitation 정책을 반영한다.
2. Sprint 1은 Auth-only이며 invitation을 구현하지 않는다.
3. Sprint 1.5부터 회사 멤버 추가는 즉시 membership 생성이 아니라 `invitations` 생성과 대상자 accept 후 membership 생성으로 처리한다. 프로젝트 invitation과 project_members accept 흐름은 Backlog 4.2 이후로 분리하고, Sprint 2는 project_members 조회/역할 기준 skeleton만 WBS 3.x 계획 범위에서 다룬다.
4. Project invitation, actual invitee email sending, token link acceptance, unregistered onboarding, resend, email template, delivery failure handling, batch cleanup/operations는 Sprint 2 계획 backlog가 아니라 후속 backlog 또는 후보 범위다.

### invitation 공식 결정

1. Sprint 1.5 포함 범위는 `invitations` table, `PENDING / ACCEPTED / DECLINED / CANCELED / EXPIRED`, company invitation, exact email lookup for registered users, received invitation list, accept/decline/cancel, company membership creation on accept다.
2. Sprint 1.5 제외 범위는 project invitation, project membership creation, actual email sending to invitees, invitation token link, unregistered-user invitation, signup/onboarding auto-accept, resend다.
3. `notification_events`에는 필요 시 `INVITATION_CREATED`, `INVITATION_ACCEPTED`, `INVITATION_DECLINED`, `MEMBER_ADDED`를 내부 event 원장으로 기록할 수 있으나 Sprint 1.5 완료 조건에는 포함하지 않는다. Push 알림, 별도 Invitation notification preference, S11 사용자 알림 UI 필수 구현은 Sprint 1.5 범위가 아니다.
4. Sprint 1.5에서는 user-facing `notification_preferences`에 별도 Invitation category를 노출하지 않고 기존 global notification preference policy를 따른다.
5. `EXPIRED`와 `expires_at`을 유지하고 기본 만료 기간은 7일로 둔다. read/accept/decline/cancel 시 lazy expiration을 확인하며, 만료된 `PENDING` invitation accept는 `409 INVITATION_EXPIRED`로 거절한다.
6. Sprint 1.5에는 필수 batch scheduler를 추가하지 않는다. Batch cleanup/sweeper는 Sprint 2 이후 admin list hygiene, retention, metrics, operational cleanup 필요에 따라 검토한다.

### Phase C 추가 동기화 결정

1. Sprint 1.5 invitation 검증은 local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명을 사용한다. `POST /api/v1/companies`는 Sprint 1.5 구현 범위에 포함하지 않고 MVP/Sprint 2 reference로 유지하며 운영 데이터에는 seed를 포함하지 않는다.
2. `activity_events`는 활동 원장, `notification_events`는 알림 사건 원본으로 별도 운영한다. 두 저장소를 통합하지 않고, 필요 시 activity 이벤트에서 notification 이벤트를 생성하는 연계만 허용한다.
3. 서버 생성 시각과 만료 시각은 UTC 기준으로 생성, 저장, 비교한다. ERD의 `DATETIME` 타입은 유지하고 API 응답은 ISO-8601 UTC `Z` 형식으로 반환한다.
4. 로그인 이메일은 모든 진입점에서 `trim + lowercase`로 정규화한 값을 저장, 조회, 비교한다. 이 기준은 signup, login, password reset, OAuth 계정 연결, invitation lookup에 동일하게 적용한다.
5. OAuth 계정 연결은 이메일만 같다는 이유로 blind auto-link하지 않는다. 이미 로그인한 사용자이거나 provider 이메일 검증을 신뢰할 수 있는 경우에만 기존 `users` 계정과 연결한다.
6. `password-reset/confirm` 성공 시 해당 사용자의 모든 기존 `refresh_tokens`를 revoke한다. 기존 access token은 별도 blacklist 없이 짧은 만료 시간에 따라 자연 만료시킨다.
7. login, OAuth callback, token refresh, `/auth/me`는 `users.status = ACTIVE`인 사용자만 성공 처리한다. `SUSPENDED` 또는 `DELETED` 사용자는 인증을 거부하며, 상태 변경 시 기존 `refresh_tokens`를 revoke한다.
