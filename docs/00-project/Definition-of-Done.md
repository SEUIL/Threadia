> **작성자**: Threadia 팀
> **작성일**: 2026-06-16
> **최종 수정**: 2026-06-27
> **버전**: 개발주간 DB정책 반영 v1
> **상태**: 확정
> **수정 근거**: Threadia-기획문서-검수리포트.md, DB-schema-관리-정책.md

# 작업 유형별 완료 조건 v2

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows다. DB schema는 `docs/03-design/ERD.md`와 `db/schema.sql`을 기준으로 관리하고, Spring Boot/JPA는 schema 생성이 아니라 mapping 검증에 사용한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.


## 1. 목적

이 문서는 Threadia Sprint 1 작업을 완료로 판단하기 위한 Definition of Done을 정리한다.

완료 기준은 "무엇을 만족해야 완료인가"를 정의한다. 실제 확인 방법과 실행 명령은 `test-build-API-화면 확인 기준 v2.md`에서 다룬다.

## 2. Sprint 1 적용 범위

| 구분 | Sprint 1 기준 |
|---|---|
| Backend | Spring Boot skeleton, Auth entity, 로그인/회원가입/로그아웃/`/auth/me`/Google OAuth/비밀번호 재설정 API, MySQL 연결 확인 |
| Frontend | React + Next 구조 생성 이후 로그인, 회원가입, 비밀번호 재설정 route와 Auth API 연동 확인 |
| DB | MySQL 8.4 local 기준, `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` 정합성 확인 |
| Infra/AWS | EC2/RDS/S3 사용 범위와 계정/비용/배포 시점 문서화. 실제 배포와 S3 파일 저장 구현은 Sprint 1 이후 |
| CI | 초기에는 `./gradlew test` 우선. skeleton 안정화 이후 `./gradlew build`와 GitHub Actions로 확대 |
| AI 사용 | AI 도구 종류가 아니라 owner, 변경 파일 범위, 검증 책임, reviewer 기준으로 관리 |

## 3. 완료 판정 흐름

```mermaid
flowchart TD
    A[작업 범위 확인] --> B[작업 유형별 DoD 확인]
    B --> C[관련 검증 실행]
    C --> D{검증 통과?}
    D -- 예 --> E[증빙 기록]
    D -- 아니오 --> F[원인과 후속 조치 기록]
    E --> G[reviewer가 재확인 가능]
    F --> H[완료 아님]
    G --> I[완료]
```

## 4. 공통 완료 기준

모든 작업은 아래 조건을 기본으로 만족해야 한다.

| 기준 | 완료 조건 |
|---|---|
| 범위 | 요구사항, Backlog, 이슈에 명시된 범위가 반영되었다 |
| 정상 케이스 | 대표 성공 흐름을 실행 또는 문서 검토로 확인했다 |
| 예외 케이스 | 필수값 누락, 권한 부족, 존재하지 않는 리소스 등 주요 실패 조건을 확인했다 |
| 기존 영향 | 변경이 기존 기능, 문서, API, DB 기준에 미치는 영향을 확인했다 |
| 보안 | token, password, 개인정보, secret이 코드와 문서, 로그에 노출되지 않고, password reset과 계정 상태 변경 시 refresh token revoke 기준을 확인했다 |
| 정리 | 임시 로그, 디버그 코드, 불필요한 주석 처리 코드가 남아 있지 않다 |
| 증빙 | 실행 명령, 결과, 화면 캡처, 요청/응답 예시 중 필요한 증빙이 남아 있다 |
| 후속 조치 | 미완료 또는 미확인 항목은 사유, owner, 처리 시점이 기록되어 있다 |

## 5. 작업 유형별 최소 통과 조건

| 작업 유형 | 최소 완료 조건 | 필수 증빙 |
|---|---|---|
| Backend | API 또는 서버 로직이 요구사항대로 동작하고 `./gradlew test`가 통과한다 | test 결과, API 요청/응답 예시 |
| Frontend | 대상 route나 화면에서 주요 사용자 흐름을 브라우저로 확인할 수 있다 | 화면 캡처 또는 확인 기록 |
| DB | ERD, `db/schema.sql`, entity, table 관계가 일치하고 local DB 생성 기준이 확인되었다 | 연결 결과, query 또는 schema.sql 실행 결과 |
| Infra/AWS | EC2/RDS/S3 사용 범위와 계정/비용/배포 시점, 설정값 관리 방식이 문서화되었다 | 문서 링크, 승인 필요 항목 표시 |
| 문서 | 팀원이 문서만 보고 다음 행동을 할 수 있다 | 문서 링크, 검토 결과 |
| PR | 변경 내용, 검증 결과, 영향 범위, 보안/승인 체크가 작성되었다 | PR 본문 또는 작업 기록 |
| AI-assisted | AI 산출물을 owner가 이해하고 직접 검증했으며 reviewer가 분리되어 있다 | AI 사용 범위, 검증 결과, reviewer |

## 6. Backend 완료 기준

Backend 작업은 API, server logic, 인증/인가, validation, DB 연동 변경을 포함한다.

| 항목 | 완료 조건 |
|---|---|
| API 스펙 | 요청 method, URL, request body, response body가 API 초안 또는 합의된 기준과 일치한다 |
| 상태 코드 | 성공, validation 실패, 인증 실패, 권한 부족, 리소스 없음 응답이 구분된다 |
| 인증/인가 | 로그인이 필요한 API는 token 없거나 권한 부족인 요청을 차단하고, Auth API는 `users.status = ACTIVE`인 사용자만 성공 처리한다 |
| 비즈니스 로직 | 핵심 rule이 service 또는 domain layer에서 확인 가능하다 |
| DB 반영 | 생성, 수정, 삭제가 있는 경우 DB 반영 결과까지 확인했다 |
| test | 변경 범위에 맞는 test가 있고 `./gradlew test`가 통과한다 |
| 설정 | 환경 변수, DB 연결, secret 관리 방식이 문서화되었다 |

## 7. Frontend 완료 기준

Frontend 작업은 화면, route, component, 상태 관리, API 연동 변경을 포함한다.

| 항목 | 완료 조건 |
|---|---|
| 화면 진입 | 대상 route에 진입할 수 있고 흰 화면, 무한 로딩, runtime error가 없다 |
| 사용자 흐름 | 로그인, 목록 조회, 상세 진입, 생성/수정 등 작업 범위의 핵심 흐름이 끝까지 동작한다 |
| API 상태 | 성공, 실패, loading, empty 상태 중 변경 범위에 필요한 상태가 화면에서 구분된다 |
| 입력 검증 | 필수값, 형식 오류, 실패 메시지를 사용자가 이해할 수 있다 |
| 화면 기준 | Desktop 기준 화면을 우선 확인하고, Sprint 1에서 필요한 경우 Mobile 375px을 추가 확인한다 |
| 증빙 | 화면 변경은 캡처 또는 확인한 route, 브라우저, 화면 크기를 기록한다 |

## 8. DB 완료 기준

DB 작업은 ERD, `db/schema.sql`, entity, table, query 변경을 포함한다.

| 항목 | 완료 조건 |
|---|---|
| ERD 정합성 | table, column, 관계가 ERD v2.0과 일치하거나 차이가 문서화되었다 |
| 제약 조건 | id, unique, foreign key, null 허용 여부, 기본값, email `trim + lowercase`, UTC 시간 저장 기준이 요구사항과 맞다 |
| local 확인 | MySQL 8.4 local 기준으로 연결 또는 `db/schema.sql` 기준 DB 생성 가능 여부를 확인했다 |
| API 영향 | DB 변경이 API request/response, validation, 화면 표시 값에 미치는 영향을 확인했다 |
| 데이터 변경 | 삭제나 대량 수정이 있으면 대상 범위와 실행 전후 확인 방법을 기록했다 |

## 9. Infra/AWS 완료 기준

Sprint 1에서 AWS는 실제 배포 구현이 아니라 사용 범위와 절차 정리가 완료 기준이다.

| 항목 | 완료 조건 |
|---|---|
| 사용 범위 | EC2, RDS, S3를 어떤 목적으로 사용할지 문서화되었다 |
| 책임 | AWS 계정은 직접 생성하고 비용은 본인 부담이라는 기준이 기록되었다 |
| 구현 시점 | 실제 EC2 배포와 S3 파일 저장 구현은 Sprint 1 이후로 분리되었다 |
| 설정 관리 | region, DB endpoint, bucket, secret 등 값은 노출하지 않고 관리 방식만 적는다 |
| 승인 | 배포, DB, secret, cloud resource 변경은 승인 필요 작업으로 표시한다 |

## 10. 문서 완료 기준

| 항목 | 완료 조건 |
|---|---|
| 목적 | 문서의 목적, 대상 독자, 적용 범위가 명확하다 |
| 현행성 | API, ERD, 화면 정의서, Backlog 등 관련 문서와 충돌하지 않는다 |
| 실행 가능성 | 팀원이 문서만 보고 다음 행동이나 검증 절차를 수행할 수 있다 |
| 표기 | 파일명, 메타정보, 버전, 상태가 문서 작성 규칙과 맞다 |
| 링크 | 관련 문서 링크, 이미지, 표, code block이 정상적으로 읽힌다 |
| 미확정 | 미확정 항목은 owner, 결정 시점, 후속 처리가 적혀 있다 |

## 11. PR 완료 기준

| 항목 | 완료 조건 |
|---|---|
| 변경 요약 | 무엇을 왜 바꿨는지 reviewer가 이해할 수 있다 |
| 범위 | 포함 범위와 제외 범위가 구분되어 있다 |
| 검증 | `./gradlew test`, API 확인, 브라우저 확인 등 변경 범위에 맞는 검증 결과가 있다 |
| 보안 | secret, 개인정보, token 노출 여부를 확인했다 |
| 승인 필요 작업 | DB schema, 인증/인가, CI, AWS, secret 변경 여부가 표시되어 있다 |
| AI 사용 | AI 사용 여부와 사용 범위가 PR 본문 또는 작업 기록에 남아 있다 |
| review | reviewer가 지정되어 있고 코멘트 반영 또는 미반영 사유가 답변되어 있다 |

## 12. AI-assisted 작업 완료 기준

AI를 사용한 작업은 AI가 만든 결과물이 아니라 owner가 책임지는 작업으로 본다.

| 기준 | 완료 조건 |
|---|---|
| owner | 각 작업에는 최종 책임자 1명이 있다 |
| 파일 범위 | 같은 파일을 여러 명이 동시에 수정하지 않도록 주요 변경 파일을 정했다 |
| 이해 | owner가 AI 산출물의 변경 이유와 동작 방식을 설명할 수 있다 |
| 검증 | AI가 작성했더라도 사람이 직접 test, curl, 브라우저 확인을 수행했다 |
| reviewer | owner와 다른 사람이 review한다 |
| 민감 변경 | DB schema, 인증/인가, CI, AWS, secret은 팀 승인 후 진행한다 |

## 13. 완료로 보지 않는 경우

다음 중 하나라도 해당하면 완료로 보지 않는다.

| 경우 | 이유 |
|---|---|
| 구현만 있고 검증 결과가 없다 | reviewer가 재확인할 수 없다 |
| 정상 케이스만 확인했다 | 실패 케이스에서 실제 사용자가 막힐 수 있다 |
| test 또는 build 실패가 남아 있는데 사유가 없다 | merge 가능 상태로 판단할 수 없다 |
| 화면 변경인데 화면 증빙이 없다 | 사용자 관점 동작을 확인할 수 없다 |
| DB 변경인데 ERD/API 영향 확인이 없다 | 데이터 정합성과 API 충돌 가능성이 남는다 |
| AWS, secret, 인증 변경인데 승인 필요 표시가 없다 | 보안과 비용 리스크가 관리되지 않는다 |
| AI 사용 산출물을 owner가 설명하지 못한다 | 책임 소재와 유지보수 가능성이 불명확하다 |
| 미확인 항목의 owner와 처리 시점이 없다 | 후속 작업으로 추적되지 않는다 |

---


## 13-A. Sprint 1.5 Invitation 완료 기준

Sprint 1.5 invitation 작업은 Sprint 1 Auth-only 범위와 분리해 판정한다.

| 항목 | 완료 조건 |
|---|---|
| DB | `invitations` table과 `users`, `companies`, `company_members` 관계가 ERD와 일치한다. `projects`, `project_members`는 Sprint 2 reference로 둔다 |
| 상태 | `PENDING`, `ACCEPTED`, `DECLINED`, `CANCELED`, `EXPIRED` 전이가 API와 화면 기준에 맞다 |
| 생성 | 등록 사용자 exact email lookup 후 company invitation을 생성한다. lookup email은 `trim + lowercase`로 정규화한다. 프로젝트 invitation은 Sprint 2로 분리한다 |
| Role | 회사 invitation은 `COMPANY_ADMIN` 또는 `MEMBER`만 허용한다. 프로젝트 invitation role은 Sprint 2에서 `PROJECT_ADMIN` 또는 `MEMBER`로 제한한다 |
| Membership | Sprint 1.5에서는 accept 시점에만 회사 membership을 생성한다 |
| 만료 | 기본 7일 `expiresAt`을 반환하고 read/accept/decline/cancel 시 lazy expiration을 확인한다. 서버 생성/만료 시각은 UTC로 저장하고 ISO-8601 UTC `Z`로 응답한다 |
| 예외 | `ALREADY_COMPANY_MEMBER`, `INVITATION_ALREADY_PENDING`, `INVITATION_EXPIRED`, `INVITATION_CANCELED`를 공통 error 형식으로 반환한다. `ALREADY_PROJECT_MEMBER`는 Sprint 2 project invitation에서 처리한다 |
| 알림 | invitation 관련 `notification_events`는 필요 시 내부 event 원장 기록을 남길 수 있으나 Sprint 1.5 완료 조건에는 포함하지 않는다. Push 알림, 별도 Invitation notification preference, S11 사용자 알림 UI 필수 구현은 제외한다. `activity_events`와 `notification_events`는 별도 원장으로 운영한다 |
| Bootstrap/제외 범위 | Sprint 1.5 검증은 local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명을 사용한다. actual email sending, token link, 미가입 사용자 invitation, signup auto-accept, resend, `POST /api/v1/companies`, 필수 batch scheduler를 구현 완료 조건에 넣지 않는다 |
| 검증 | invitation create/list/my invitations/accept/decline/cancel 정상과 실패 케이스를 curl 또는 Postman으로 확인한다 |

---

## Phase B invitation 반영 메모

> **생성일**: 2026-06-16
> **상태**: 공식 결정 반영 완료
> **기준**: `.sisyphus/evidence/task-5-invitation-impact-matrix.md`의 affected row와 2026-06-16 공식 decision set을 사용한다.

### 반영 범위

1. Phase B는 Sprint 1.5의 등록 사용자 대상 app-internal invitation 정책을 반영한다.
2. Sprint 1은 Auth-only이며 invitation을 구현하지 않는다.
3. Sprint 1.5부터 회사 멤버 추가는 즉시 membership 생성이 아니라 `invitations` 생성과 대상자 accept 후 membership 생성으로 처리한다. 프로젝트 멤버 추가와 project invitation은 Sprint 2로 분리한다.
4. Sprint 2는 project invitation, project membership 생성, actual invitee email sending, token link acceptance, unregistered onboarding, resend, email template, delivery failure handling, batch cleanup/operations의 후보 시점이며 확정 구현 commitment는 아니다.

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
