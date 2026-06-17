> **작성자**: Threadia 팀
> **작성일**: 2026-06-16
> **최종 수정**: 2026-06-17
> **버전**: 6일차 수정본 v2
> **상태**: 6일차 수정본
> **수정 근거**: Threadia-기획문서-검수리포트.md

# Sprint 1 Backlog v0.2, 6일차 수정본

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows이며, Sprint 1 DB migration은 `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens`만 포함한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.

## 작업 묶음 1. Sprint 1 Backlog 초안

| 작업 | 순서 | 권장 담당 그룹 | 완료 조건 | 검증 방법 | 의존성 |
| --- | --- | --- | --- | --- | --- |
| Spring Boot 기본 프로젝트 생성 | 1 | 여도현 + 김태준 | local에서 서버 실행 가능 | `./gradlew bootRun` | repository clone, Java/Gradle 환경 |
| MySQL 8.4 연결 확인 | 2 | 여도현 | test DB 연결 성공 | 연결 test 또는 간단 query | DB 실행 방식 결정, Spring Boot 기반 |
| Frontend 프로젝트 구조 생성 | 3 | 정철진 | 라우팅, 기본 layout 구성 | 브라우저 실행 | React + Next.js App Router vs Pages Router 결정 |
| 인증 JPA Entity 구현 | 4 | 여도현 + 김태준 + 김민경 | ERD v2.0 기준 `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` JPA Entity 클래스 구현. `users.role` 필드 없이 구현, `users.password_hash`는 소셜 전용 계정 NULL 허용 | `./gradlew build` 통과 | ERD v2.0, MySQL 연결 확인 |
| 이메일 회원가입/로그인 API 구현 | 5 | 여도현 + 김태준 + 김민경 | API 명세서 v2.3 기준 `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me` 구현. email `trim + lowercase`, `ACTIVE` 사용자 인증 기준 반영 | Postman 또는 curl | 인증 Entity, JWT 정책 결정 |
| 소셜 로그인 API 구현 (Google OAuth) | 6 | 여도현 + 김태준 + 김민경 | API 명세서 v2.3 기준 `GET /oauth2/authorization/{provider}`, `GET /api/v1/auth/oauth2/callback/{provider}` 구현. 이메일만 같은 blind auto-link 금지 | Postman 또는 브라우저 | 인증 Entity, Google OAuth Client ID/Secret 발급 완료 |
| 비밀번호 재설정 API 구현 | 7 | 여도현 + 김태준 + 김민경 | API 명세서 v2.3 기준 `POST /api/v1/auth/password-reset/request`, `POST /api/v1/auth/password-reset/confirm` 구현. confirm 성공 시 기존 `refresh_tokens` 전부 revoke. 이메일 발송은 Sprint 1에서 Google SMTP 실제 발송으로 검증하며, log/local console은 보조 debugging evidence로만 사용 | Postman 또는 curl | password_reset_tokens, refresh_tokens Entity |
| 로그인/회원가입 화면 구현 | 8 | 정철진 | 로그인 화면, 회원가입 화면 route 존재, 로그인 API 연동 완료 | 브라우저 확인 | Frontend 구조, 로그인 API 응답 형식 |
| GitHub Actions CI 도입 기준 확인 | 9 | 황스일 + 김민정 | `./gradlew test` 우선, skeleton 이후 workflow 생성 기준 합의 | CI 기준 문서 확인 | CI 기준 확정 v1.0, Spring Boot skeleton 생성 이후 |
| AGENTS.md/CLAUDE.md/CONTRIBUTING.md 최소 규칙 확정 | 10 | 황스일 + 김민정 | root 문서와 docs 하위 규칙, commit 규칙 검토 완료 | 파일 존재와 링크 확인 | 4일차 협업 하네스 최신화 |
| PR Template 실제 생성 여부 결정 | 11 | 황스일 | `.github/PULL_REQUEST_TEMPLATE.md` 생성 완료 | 파일 존재 확인 | PR Template v1.0 |

> 회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림 Entity·API는 이 Backlog에서 제외했다. 다음 스프린트 Backlog 작성 시 ERD v2.0 영역 2~4(이전 v0.1 버전 참고)를 그대로 가져와 쓰면 된다.

---

## 작업 묶음 1-A. 일자별 분할 (Day 1~5)

> 가정: Sprint 1 = Week 2, 5영업일 운영.
> 인원: 황스일, 여도현, 정철진, 김민정, 김민경, 김태준 6명.
> 배치 원칙: 범위가 인증으로 좁혀지면서 Backend 페어를 둘로 나눌 필요가 없어졌다. **인증 Backend**: 여도현 + 김태준 + 김민경이 entity부터 3개 API까지 순서대로 담당. **Frontend**: 정철진 단독. **협업/지원**: 황스일 + 김민정 — CI/문서/OAuth 설정/통합테스트 보조. 작업량이 줄어든 만큼 Day 4~5는 테스트와 버퍼에 더 쓴다.

### Day 1 — 개발 기반 구축

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| (1) Spring Boot 기본 프로젝트 생성 | 여도현 + 김태준 + 김민경 | `./gradlew bootRun` 성공 | repository clone, Java version 통일 |
| (2) MySQL 8.4 연결 확인 | 여도현 | test DB 연결 성공 | Docker 등 실행 방식 통일 |
| (3) Frontend 프로젝트 구조 생성 | 정철진 | 라우팅, 기본 layout 구성 | App Router vs Pages Router 결정 |
| (9)(10) CI 기준 확인 + AGENTS.md 등 규칙 확정 | 황스일 + 김민정 | CI workflow 기준 합의, root/docs 규칙 검토 완료 | 4일차 협업 하네스 |
| (11) PR Template 생성 확인 | 황스일 | `.github/PULL_REQUEST_TEMPLATE.md` 생성 완료 | PR Template v1.0 |
| Google OAuth 앱 등록 | 황스일 + 김민정 | Google Cloud Console Client ID/Secret 발급, callback URL 등록 | Sprint 1 착수 전 필수 선결 조건 |

### Day 2 — 인증 Entity 구현

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| (4) 인증 JPA Entity 구현 | 여도현 + 김태준 + 김민경 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` 구현, `./gradlew build` 통과 | Day 1 MySQL 연결 확인 |
| 로그인/회원가입 화면 레이아웃 작업 | 정철진 | 화면 UI만 우선 구현 (API 연동 전) | 화면정의서_v3, API 없이 진행 가능 |
| JWT 정책 결정 회의 | 황스일 + 여도현 | access/refresh token 만료 시간, 갱신 전략, 저장 위치, password reset/status change refresh token revoke, UTC 시간 기준 결정 | Day 3 API 구현 전 확정 필요 |

### Day 3 — 이메일 로그인 API + 소셜 로그인 API

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| (5) 이메일 회원가입/로그인 API 구현 | 여도현 + 김태준 + 김민경 | signup/login/logout/me 구현, Postman 검증 | 인증 Entity, JWT 정책 결정 (Day 2 완료분) |
| (6) 소셜 로그인 API 구현 (Google OAuth) | 여도현 + 김태준 + 김민경 | OAuth 시작/callback 구현, 브라우저로 redirect 확인 | Google OAuth Client ID/Secret (Day 1 완료분) |
| 로그인/회원가입 화면 마무리 작업 | 정철진 | 화면 UI 완성, API 연동 준비 | API 응답 형식 확인 (황스일 + 김민정 지원) |

### Day 4 — 비밀번호 재설정 API + 화면 연동

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| (7) 비밀번호 재설정 API 구현 | 여도현 + 김태준 + 김민경 | reset 요청/확인 흐름 구현, Google SMTP 실제 발송과 수신 email 확인 | password_reset_tokens Entity |
| (8) 로그인/회원가입 화면 API 연동 | 정철진 | 로그인/회원가입 화면에서 실제 API 호출, 브라우저에서 동작 확인 | Day 3 인증 API 완료 |
| Postman 컬렉션 정리 / API 문서화 | 황스일 + 김민정 | 인증 API 전체 요청/응답 예시 정리 | API 명세서 v2.0 |

### Day 5 — 통합 테스트 + 버퍼 + 6일차 준비

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| 인증 흐름 end-to-end 테스트 | 전체 | 회원가입 → 로그인 → 로그아웃 → 소셜 로그인 → 비밀번호 재설정 전체 시나리오 통과 | Day 1~4 전체 완료 |
| 버그 수정 / 버퍼 | 여도현 + 김태준 + 김민경 | 통합 테스트에서 발견된 이슈 수정 | 일정 여유 확보용 |
| 6일차 보고 입력값 정리 | 황스일 | Sprint 1 목표/Backlog/DoD/검증/blocker 요약 | 6일차 보고용 |

> 작업량이 줄어든 만큼 Day 5는 의도적으로 통합 테스트와 버퍼로 비워뒀다. 일정이 당겨지면 다음 스프린트 도메인 Entity(회사/프로젝트/이슈) 설계를 미리 검토하는 시간으로 써도 된다.

---


## 작업 묶음 1-B. Sprint 1.5 Invitation Backlog

> Sprint 1은 Auth-only로 유지한다. 아래 작업은 Sprint 1 완료 후 별도 Sprint 1.5로 착수한다.

| 작업 | 권장 담당 | 완료 조건 | 검증 방법 | 의존성 |
|---|---|---|---|---|
| `invitations` DB/entity 구현 | 여도현 + 김태준 + 김민경 | ERD 기준 `invitations`와 users/companies/company_members 관계 mapping 완료 | repository test 또는 migration 확인 | Sprint 1 Auth DB 완료, 회사 seed와 company_members reference schema 확인 |
| 등록 사용자 lookup API | 여도현 + 김민경 | exact email lookup으로 등록 사용자만 반환 | curl 또는 Postman | `users` 구현 |
| 회사 invitation API | 김태준 + 김민경 | create/list, duplicate pending, already member, 7일 expiresAt 처리 | curl 또는 Postman | `invitations`, `company_members` |
| 내 invitation 목록과 accept/decline/cancel | 여도현 + 김태준 + 김민경 | 수락 시 membership 생성, decline/cancel/expired 실패 처리 | API 정상/실패 케이스 | invitation 상태 전이 기준 |
| 화면 S14 연결 | 정철진 | pending/accepted/declined/canceled/expired 상태 표시 | 브라우저 확인 | API 응답 형식 확정 |
| 문서와 DoD 동기화 | 황스일 + 김민정 | API, ERD, 화면, 추적표, DoD가 seed bootstrap, notification 분리, UTC, email 정규화, OAuth link, refresh token revoke, ACTIVE 인증 기준과 충돌하지 않음 | 문서 리뷰 | 검수리포트 이슈 1, 3, 4, 9 |

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
