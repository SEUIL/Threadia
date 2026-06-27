> **작성자**: Threadia Backend 분업
> **작성일**: 2026-06-16
> **최종 수정**: 2026-06-27
> **버전**: 개발주간 DB정책 반영 v1
> **상태**: 확정
> **수정 근거**: Threadia-기획문서-검수리포트.md, DB-schema-관리-정책.md

# Sprint 1 백엔드 의존성 / 기술 스택 / DoD v1.1

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows다. DB schema는 `docs/03-design/ERD.md`와 `db/schema.sql`을 기준으로 관리하고, Spring Boot/JPA는 schema 생성이 아니라 mapping 검증에 사용한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.


이 문서는 Sprint 1 Backend 작업 착수 전 필요한 범위, 기술 스택, 의존성, blocker, 완료 기준을 정리한다.

기준 문서:
- `Sprint 1_detail.md` v0.2
- `Sprint1_Backlog.md`
- `Sprint1_담당그룹_역할분담 v1.1.md`
- `API 명세서 v2.3`
- `Definition of Done_작업 유형별 완료 조건 v2.md`
- `검증 방법_test-build-API-화면 확인 기준 v2.md`
- 메일 provider 정책은 excluded source 없이 Task 2 conflict map의 unresolved 항목으로만 추적
- `ERD v2.0.md`
- `프로젝트 기획서 v2.0.md`
- `CI 기준 확정 v1.0.md`

---

## 1. 문서 정보

| 항목 | 내용 |
|---|---|
| 문서 목적 | Sprint 1 Backend 착수 기준, 의존성, blocker, DoD 정리 |
| 적용 범위 | Sprint 1 Backend Auth API, 인증 DB, 인증 검증 |
| 제외 범위 | 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정 API |
| 기준 기술 | Spring Boot 3.x, MySQL 8.4, REST API, auth/domain logic |
| 문서 상태 | 개발 착수 전 정리본, 일부 항목은 개발 초반 확정 필요 |

---

## 2. 요약

Sprint 1 Backend는 인증 기능만 구현한다. 핵심 목표는 이메일 회원가입, 로그인, 로그아웃, `/auth/me`, Google OAuth, 비밀번호 재설정 API를 Spring Boot 3.x와 MySQL 8.4 기반으로 동작시키는 것이다.

| 구분 | 요약 |
|---|---|
| 포함 | 이메일 signup/login/logout, `/auth/me`, Google OAuth, password reset |
| DB 대상 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` |
| 인증 방식 | JWT Bearer Token, refresh token 사용 |
| 공식 결정 | Java 17 기준, access token 30분, refresh token 14일, refresh token hash 저장/rotation/logout/password reset/status change invalidation, access token memory + refresh token HttpOnly SameSite=Lax cookie 권장, UTC 시각 기준, email `trim + lowercase` 정규화 |
| 검증 방식 | Gradle wrapper, curl 또는 Postman, MySQL client, application log, 관련 API 응답 |

---

## 3. Sprint 1 Backend 범위

Sprint 1 Backend는 인증 중심 API와 인증에 필요한 최소 DB 모델을 만든다.

| 구분 | 결정 |
|---|---|
| 포함 기능 | 이메일 회원가입, 이메일 로그인, 로그아웃, `/auth/me`, Google OAuth, 비밀번호 재설정 |
| 포함 API | Auth API와 OAuth callback, password reset request/confirm |
| 포함 DB | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` |
| 포함 검증 | API 정상/실패 케이스, DB 저장 결과, token 관련 최소 동작 |
| 제외 API | company/project/issue/document/chat/DM/thread/notification/profile edit API |
| 제외 DB 구현 | 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정 전용 테이블 구현 |

Auth 연동 대상:
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- `GET /oauth2/authorization/{provider}`
- `GET /api/v1/auth/oauth2/callback/{provider}`
- `POST /api/v1/auth/password-reset/request`
- `POST /api/v1/auth/password-reset/confirm`

---

## 4. 기획 단계 확정 사항

| 항목 | 결정 | 비고 |
|---|---|---|
| Backend framework | Spring Boot 3.x | Sprint 1 Backend 기준 |
| DB | MySQL 8.4 | Sprint 1 인증 저장소 기준 |
| API 방식 | REST API | API 명세서 v2.3 기준 |
| 인증 범위 | Auth only | 이메일 인증 흐름, Google OAuth, password reset |
| 인증 방식 | JWT Bearer Token | refresh token 사용 |
| 응답 형식 | `{ data, meta }` | 단일 응답 기준 |
| 에러 형식 | `{ error: { code, message, details }, meta }` | API 명세서 v2.3 기준 |
| 권한 role | `users.role` 추가 안 함 | 권한은 후속 `company_members.role`, `project_members.role`에서 처리 |
| Password reset token | raw token 저장 금지 | `password_reset_tokens.token_hash`에 hash만 저장 |
| Password reset 상태 | expires/used 처리 필요 | 만료와 사용 완료 상태를 API에서 반영 |
| Password reset 세션 정책 | confirm 성공 시 해당 user의 모든 기존 `refresh_tokens` revoke | 기존 access token은 별도 blacklist 없이 자연 만료 |
| 이메일 정책 | Google SMTP 실제 발송 | log/local console은 보조 debugging evidence이며 공식 Sprint 1 검증은 실제 수신 email, token hash, expiration, used-state 확인이다 |
| 이메일 정규화 | 모든 진입점에서 `trim + lowercase` 적용 | signup, login, password reset, OAuth, invitation lookup 공통 |
| 이메일 provider 분리 | `EmailSender` interface 뒤로 분리 | `PasswordResetService`는 Google SMTP/AWS SES 등 특정 provider를 직접 알지 않도록 구현 |
| OAuth 준비 | Google OAuth 앱 등록과 redirect URI 필요 | 구현 직전에 실제 환경 기준으로 등록 |
| OAuth 계정 연결 | blind auto-link 금지 | 이미 로그인한 사용자 또는 신뢰 가능한 provider 이메일 검증 시에만 기존 계정 연결 |
| 계정 상태 인증 | `users.status = ACTIVE`만 인증 성공 | SUSPENDED/DELETED는 login/OAuth callback/token refresh/`auth/me` 거부, 상태 변경 시 refresh token revoke |

---

## 5. 개발 초반 확정 / 확인 항목

| 항목 | 상태 | 확인 시점 | 담당 |
|---|---|---|---|
| Java 기준 | Java 17 기준 | Sprint 1 local development와 CI 기준 | Spring Boot skeleton Owner 여도현, Reviewer 황스일 |
| JWT access/refresh 만료 시간 | access token 30분, refresh token 14일 | Auth 구현 기준 | JWT policy meeting Owner 황스일, Support 여도현, Reviewer 김태준 |
| refresh token rotation | refresh 성공 시 rotate | Auth 구현 기준 | JWT policy meeting Owner 황스일, Support 여도현, Reviewer 김태준 |
| refresh token invalidation | logout 시 invalidation | logout 구현 기준 | JWT policy meeting Owner 황스일, Support 여도현, Reviewer 김태준 |
| refresh token security revoke | password reset 성공, SUSPENDED/DELETED 상태 변경 시 해당 user의 기존 refresh token 전부 revoke | Auth 구현 기준 | JWT policy meeting Owner 황스일, Support 여도현, Reviewer 김태준 |
| cookie/security/CORS 정책 | access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie 권장. HTTPS는 `Secure`, local 예외 문서화 | Auth/Frontend 연동 기준 | JWT policy meeting Owner 황스일, Support 여도현, Reviewer 김태준 |
| 서버 시각 기준 | 서버 생성/만료 시각은 UTC 기준 생성·저장·비교, API 응답은 ISO-8601 UTC `Z` | Auth/Invitation 구현 기준 | Backend Owner, Reviewer 황스일 |
| email normalization | signup/login/password reset/OAuth/invitation lookup 모두 `trim + lowercase` 적용 | Auth/Invitation 구현 기준 | Backend Owner, Reviewer 황스일 |
| Google OAuth 앱 등록 | 예정 | OAuth 구현 직전 | Owner 황스일, Support 김민정, Reviewer 여도현 |
| Google OAuth redirect URI | 예정 | OAuth 구현 직전 | Owner 황스일, Support 김민정, Reviewer 여도현 |
| SMTP 설정 방식 | Google SMTP 실제 발송 기준 | password reset request 구현 전 | Password reset API Owner 김태준, Reviewer 황스일 |
| 이메일 provider 추상화 | `EmailSender` style boundary 필수 | password reset request 구현 전 | Password reset API Owner 김태준, Reviewer 황스일 |
| AWS SES 도입 여부 | Sprint 1 범위 아님 | 배포/운영 메일 정책 정리 시 | 배포/운영 단계에서 도입 여부를 결정하며 제외된 folder의 세부 내용은 source evidence로 사용하지 않는다 |
| CI workflow 생성 | 대기 | Spring Boot skeleton/Gradle wrapper 준비 후 | CI 기준 확인 + AGENTS.md Owner 황스일, Reviewer 김태준 |

> Sprint 1 local development와 CI는 Java 17 기준이다. Spring Boot 3.x의 최소 요구 version은 Java 17이고 Threadia는 현재 더 높은 Java version 전용 기능이 필요하지 않으므로 future upgrade는 별도 결정으로 둔다.

---

## 6. 작업 일정 / 순서

| 순서 | Backend 작업 | 완료 조건 |
|---|---|---|
| 1 | Spring Boot 3.x skeleton 생성, Gradle wrapper 실행 확인 | `./gradlew bootRun`으로 애플리케이션 기동 확인 |
| 2 | MySQL 8.4 연결 설정, local DB 연결 확인 | application log 또는 MySQL client로 연결 확인 |
| 3 | Auth JPA Entity 작성 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` entity와 기본 mapping 확인 |
| 4 | 이메일 signup/login/logout, `/auth/me` 구현 | curl 또는 Postman으로 정상/실패 응답 확인 |
| 5 | JWT policy 결정 반영 | access/refresh token 처리 기준을 코드와 문서에 반영 |
| 6 | Google OAuth 앱 등록, redirect URI 반영, social login API 구현 | OAuth 시작과 callback 흐름 확인 |
| 7 | password reset request/confirm 구현 | Google SMTP 발송, token hash 저장, expires/used 처리 확인 |
| 8 | 테스트와 build 안정화 | `./gradlew test`, skeleton 안정화 후 `./gradlew build` 통과 |

역할 분담 기준:

| 작업 | 지원 | Owner | Reviewer |
|---|---|---|---|
| Spring Boot skeleton | 여도현/김태준/김민경 | 여도현 | 황스일 |
| MySQL 8.4 connection | 여도현 | 여도현 | 황스일 |
| CI 기준 확인 + AGENTS.md | 황스일/김민정 | 황스일 | 김태준 |
| Google OAuth app registration | 황스일/김민정 | 황스일 | 여도현 |
| Auth JPA Entity | 여도현/김태준/김민경 | 김태준 | 황스일 |
| JWT policy meeting | 황스일/여도현 | 황스일 | 김태준 |
| Email signup/login API | 여도현/김태준/김민경 | 김민경 | 황스일 |
| Social login API | 여도현/김태준/김민경 | 여도현 | 황스일 |
| Password reset API | 여도현/김태준/김민경 | 김태준 | 황스일 |
| Postman collection/API docs | 황스일/김민정 | 김민정 | 김태준 |
| Auth end-to-end test | 여도현/정철진/김민경/김태준 | 김태준 | 황스일 |
| Bug fix/buffer | 여도현/김태준/김민경 | 김민경 | 황스일 |

---

## 7. API / DB 연동 기준

### 7.1 API 기준

| 항목 | 내용 | 상태 |
|---|---|---|
| Base URL (dev) | `http://localhost:8080/api/v1` | API 명세 기준 |
| 응답 형식 | `{ data, meta }` | 확정 |
| 에러 형식 | `{ error: { code, message, details }, meta }` | 확정 |
| 인증 header | `Authorization: Bearer {token}` | JWT 기준 |
| API 검증 | curl 또는 Postman | 필수 |
| 실패 케이스 | validation, 인증 실패, token 없음/만료, reset token 만료/사용 완료, non-ACTIVE 사용자 인증 거부 | 필수 |

Frontend JWT 저장 방향은 cookie 또는 memory다. cookie가 선택되면 Backend는 `Set-Cookie`, `HttpOnly`, `Secure`, `SameSite`, CORS credential 정책을 함께 확정해야 한다. memory가 선택되면 API 응답과 refresh 흐름 기준을 Frontend와 맞춘다.

### 7.2 DB 기준

| Table / Entity | Sprint 1 사용 목적 | 주요 기준 |
|---|---|---|
| `users` | 이메일 사용자와 OAuth 연결 대상 사용자 | `users.role` 추가 안 함, email `trim + lowercase` 저장, `ACTIVE`만 인증 허용 |
| `oauth_accounts` | Google OAuth provider 계정 연결 | provider, provider user id, user mapping 확인, blind auto-link 금지 |
| `password_reset_tokens` | 비밀번호 재설정 token 검증 | `token_hash`만 저장, raw token 저장 금지, expires/used 처리, confirm 성공 시 refresh token 전부 revoke |
| `refresh_tokens` | refresh token rotation과 logout/password reset/status change invalidation | `token_hash`만 저장, expires/rotated/revoked 처리 |

DB 검증 방식:
- MySQL client로 row 생성과 변경 결과를 확인한다.
- application log에서 connection, query, transaction 오류를 확인한다.
- 관련 API 응답으로 signup/login/password reset 결과를 확인한다.
- secret, raw reset token, password, 개인정보가 log에 노출되지 않는지 확인한다.

---

## 8. 의존성 / Blocker

### 8.1 영역별 의존성 / Blocker

| 영역 | 의존성 | Blocker | 영향 | 해결 기준 | 상태 |
|---|---|---|---|---|---|
| Spring Boot skeleton | Spring Boot 3.x, Gradle wrapper, Java 17 기준 | Java 17 기준 적용 | CI와 local 실행 환경 통일 | local/CI 모두 Java 17 확인 | 확정 |
| MySQL 연결 | MySQL 8.4, application datasource 설정 | local DB 계정과 schema 준비 필요 | API 구현 전 저장소 검증 지연 가능 | connection log와 MySQL client 확인 | 확인 필요 |
| JWT 정책 | JWT Bearer access token + refresh token | access 30분, refresh 14일, hash 저장, rotation, logout/password reset/status change invalidation | login/logout/refresh/me 기준 확정 | Auth 구현 시 반영 | 확정 |
| cookie/security/CORS | access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie 권장 | HTTPS는 `Secure`, local 예외 문서화 | FE-BE 인증 연동 기준 확정 | Auth/FE 구현 시 반영 | 확정 |
| Google OAuth | Google app registration, redirect URI | 앱 등록을 구현 직전에 진행 | OAuth API 구현 전 준비 필요 | 등록 정보와 redirect URI 확인 | 예정 |
| Password reset email | Google SMTP + `EmailSender` boundary | secret 비노출과 실제 수신 email 확인 | reset request E2E 검증 기준 확정 | Google SMTP delivery, token hash, expiration, used-state 확인 | 확정 |
| 메일 provider 정책 | Sprint 1은 Google SMTP 실제 발송 | AWS SES는 Sprint 1 범위 아님 | 운영 메일 정책은 배포/운영 단계에서 결정 | provider boundary 유지 | 확정 |
| CI | Spring Boot skeleton, Gradle wrapper | workflow 생성은 skeleton/Gradle wrapper 준비 후 가능 | PR 검증 자동화 지연 가능 | 초기 Backend CI 기준은 `./gradlew test`, 안정화 후 `./gradlew build` | 대기 |

### 8.2 기능별 의존성 / Blocker

| 기능 | 의존성 | Blocker | 완료 기준 | 상태 |
|---|---|---|---|---|
| 회원가입 | `users`, password hashing, validation | 이메일 중복과 validation error 형식 확인 | signup 정상/실패 API 확인 | 확인 필요 |
| 로그인 | `users`, JWT access/refresh token 발급 | access 30분, refresh 14일, refresh hash 저장, email 정규화, `ACTIVE` status 확인 | login 성공/실패, token pair 발급, non-ACTIVE 거부 확인 | 확정 |
| 로그아웃 | refresh token invalidation 기준 | logout 시 refresh token invalidation | logout 후 인증 상태 제거 확인 | 확정 |
| `/auth/me` | JWT 검증, minimal user context DTO | `data.user.id/email/nickname/avatarUrl`만 포함 | token 있음/없음/만료 응답 확인 | 확정 |
| Google OAuth | Google OAuth app, redirect URI, `oauth_accounts` | 앱 등록과 callback URI 준비 필요, blind auto-link 금지 | OAuth 시작/callback/user mapping, provider email 검증 신뢰 조건 확인 | 예정 |
| 비밀번호 재설정 | Google SMTP, `EmailSender`, `password_reset_tokens`, `refresh_tokens` | 실제 수신 email, token hash, expiration, used-state 검증, confirm 성공 시 refresh token revoke | request/confirm 성공/실패, `PasswordResetService` provider 비의존 확인, 기존 refresh token revoke 확인 | 확정 |

---

## 9. Backend DoD

### 9.1 공통 DoD

| 기준 | 완료 조건 | 최소 증빙 |
|---|---|---|
| 범위 | Sprint 1 Auth API와 인증 DB만 반영한다 | 작업 범위 기록 |
| 실행 | Spring Boot 애플리케이션이 기동된다 | `./gradlew bootRun` 실행 결과 |
| 테스트 | 관련 단위/통합 테스트가 통과한다 | `./gradlew test` 실행 결과 |
| build | skeleton 안정화 후 build가 통과한다 | `./gradlew build` 실행 결과 |
| API 일치 | API 명세서 v2.3 Auth request/response/error 형식과 일치한다 | curl 또는 Postman 요청/응답 예시 |
| DB 일치 | Sprint 1 Auth table/entity 기준을 지킨다 | MySQL client, log, API 응답 확인 |
| 보안 | secret, password, raw reset token, 개인정보가 코드/문서/log에 노출되지 않는다 | secret 비노출 확인 |
| 시간 | 서버 생성/만료 시각은 UTC로 저장하고 API 응답은 ISO-8601 UTC `Z` 형식이다 | DB 값과 API 응답 확인 |
| 후속 조치 | 미확정 항목은 owner 또는 조율 대상, 처리 시점을 기록한다 | TODO 또는 blocker 기록 |

### 9.2 기능별 DoD

| 기능 | 완료 조건 | 최소 증빙 |
|---|---|---|
| Spring Boot skeleton | Gradle wrapper 기준으로 실행 가능하다 | `./gradlew bootRun` |
| MySQL connection | MySQL 8.4에 연결되고 Auth entity 저장을 확인한다 | MySQL client 또는 application log |
| Auth JPA Entity | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` mapping이 동작한다 | repository test 또는 관련 API 결과 |
| 회원가입 | 이메일 회원가입 성공, 중복, validation 실패, email `trim + lowercase` 저장을 처리한다 | curl 또는 Postman 응답, DB row 확인 |
| 로그인 | 이메일 로그인 성공, credential 실패, non-ACTIVE 사용자 거부를 처리한다 | curl 또는 Postman 응답 |
| 로그아웃 | refresh token 또는 session invalidation 기준에 따라 인증 상태를 정리한다 | logout API 응답과 후속 인증 확인 |
| `/auth/me` | token 있음, token 없음, token 만료, non-ACTIVE 사용자 케이스를 구분한다 | `/auth/me` 요청/응답 예시 |
| Google OAuth | Google OAuth 시작, callback, user mapping이 동작하고 blind auto-link를 하지 않는다 | redirect/callback 확인 기록 |
| 비밀번호 재설정 | 실제 이메일 발송, provider 분리, hash 저장, 만료/사용 완료, refresh token revoke 처리가 동작한다 | SMTP 발송 확인, `EmailSender` 의존 확인, DB 확인, API 응답 |

### 9.3 완료로 보지 않는 경우

| 경우 | 이유 |
|---|---|
| `./gradlew bootRun` 실행 확인이 없다 | 애플리케이션 기동 가능 여부를 확인할 수 없다 |
| Auth API 요청/응답 예시가 없다 | API 명세와 실제 구현 일치 여부를 확인할 수 없다 |
| 정상 케이스만 확인했다 | 실패 케이스에서 인증 보안 문제가 남을 수 있다 |
| `password_reset_tokens.token_hash`에 raw token을 저장했다 | token 탈취 위험이 있다 |
| `users.role`을 추가했다 | Sprint 1 권한 설계와 충돌한다 |
| JWT refresh token 정책(access 30분, refresh 14일, hash 저장, rotation, logout invalidation)을 반영하지 않고 완료 처리했다 | logout, refresh, `/auth/me` 기준이 구현과 어긋난다 |
| password reset 성공 후 기존 refresh token을 유지한다 | 탈취된 장기 세션이 계속 살아남을 수 있다 |
| `SUSPENDED` 또는 `DELETED` 사용자의 login/OAuth callback/token refresh/`auth/me`를 허용한다 | 계정 상태 정책이 인증에 반영되지 않는다 |
| 이메일을 정규화하지 않고 저장·조회한다 | 대소문자/공백 차이로 중복 가입, 로그인 실패, invitation lookup 실패가 발생할 수 있다 |
| cookie 선택 시 CORS/security 정책을 확인하지 않았다 | Frontend 인증 연동이 깨질 수 있다 |
| Google SMTP 또는 OAuth secret이 코드/문서/log에 노출되었다 | 보안 사고 가능성이 있다 |
| `PasswordResetService`가 Google SMTP 또는 AWS SES 구현체를 직접 안다 | 이메일 provider 교체와 테스트가 어려워진다 |

---

## 10. 검증 기준

| 검증 영역 | 명령 / 방법 | 통과 기준 |
|---|---|---|
| 실행 | `./gradlew bootRun` | Spring Boot 애플리케이션이 오류 없이 기동된다 |
| 테스트 | `./gradlew test` | Auth 관련 테스트가 통과한다 |
| build | `./gradlew build` | skeleton 안정화 후 전체 build가 통과한다 |
| API | curl 또는 Postman | Auth API 정상/실패 응답이 API 명세서 v2.3 형식과 일치한다 |
| DB | MySQL client | Auth table row 생성, 조회, 상태 변경을 확인한다 |
| 로그 | application log | DB connection, transaction, security 오류가 없고 secret이 노출되지 않는다 |
| 이메일 | 실제 수신함 또는 SMTP log | password reset 이메일 발송과 reset confirm 흐름을 확인한다 |
| OAuth | 브라우저 redirect, curl/Postman 보조 확인 | Google OAuth callback과 user mapping을 확인한다 |
| CI | GitHub Actions 또는 합의된 CI workflow | skeleton/Gradle wrapper 준비 후 초기 `./gradlew test`, 안정화 후 `./gradlew build` 기준을 적용한다 |

검증 기록에는 최소한 다음 정보를 남긴다.
- 실행 명령과 결과
- API endpoint, request 예시, response 예시
- DB 확인 방식과 확인한 table
- 실패 케이스 확인 결과
- 미확정 항목과 후속 처리 시점

---

## 11. 착수 전 체크리스트

- [x] Sprint 1 Backend 범위: Auth only
- [x] Backend stack: Spring Boot 3.x, MySQL 8.4
- [x] REST API/auth/domain logic 중심 구현
- [x] 포함 API: signup/login/logout/me/Google OAuth/password reset
- [x] 제외 API: company/project/issue/document/chat/DM/thread/notification/profile edit
- [x] Auth table/entity: `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens`
- [x] `users.role` 추가 안 함
- [x] `password_reset_tokens.token_hash`에는 hash만 저장
- [x] password reset expires/used 처리 필요
- [x] refresh token 사용
- [x] Java 17 기준 확정
- [x] JWT access token 30분, refresh token 14일 확정
- [x] refresh token rotation on refresh, invalidation on logout 확정
- [x] password reset 성공과 SUSPENDED/DELETED 상태 변경 시 기존 refresh token revoke 확정
- [x] login/OAuth callback/token refresh/auth me는 ACTIVE 사용자만 허용
- [x] 서버 생성/만료 시각 UTC 저장과 ISO-8601 UTC `Z` 응답 확정
- [x] email `trim + lowercase` 정규화 확정
- [x] OAuth blind auto-link 금지 확정
- [x] access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie 권장 확정
- [ ] Google OAuth 앱 등록
- [ ] Google OAuth redirect URI 확정
- [ ] Google SMTP 설정과 secret 관리 방식 확인
- [ ] `EmailSender` interface로 provider-specific sending 분리
- [ ] `PasswordResetService`가 email provider를 직접 알지 않도록 구현 기준 확인
- [x] password reset email 발송 방식 Google SMTP 실제 발송으로 확정
- [ ] Spring Boot skeleton/Gradle wrapper 준비 후 CI workflow 생성
- [ ] `./gradlew bootRun` 실행 확인
- [ ] `./gradlew test` 실행 확인
- [ ] skeleton 안정화 후 `./gradlew build` 실행 확인
- [ ] curl 또는 Postman API 검증 기준 준비
- [ ] MySQL client, application log, API 응답 기반 DB 검증 기준 준비

---

## 12. 개발 중으로 넘기는 항목

| 항목 | 넘기는 이유 | 처리 시점 |
|---|---|---|
| Java 기준 | Java 17 기준. Future Java upgrade는 별도 결정 | Sprint 1 local/CI 기준 |
| JWT access/refresh expiration | access token 30분, refresh token 14일 | Sprint 1 Auth 기준 |
| refresh token rotation | refresh 성공 시 rotate | Sprint 1 Auth 기준 |
| refresh token invalidation | logout, password reset 성공, SUSPENDED/DELETED 상태 변경 시 invalidation | Sprint 1 Auth 기준 |
| cookie/security/CORS | access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie 권장. HTTPS는 `Secure` | Sprint 1 Auth/FE 연동 기준 |
| Google OAuth app registration | 구현 직전 실제 redirect URI 기준으로 등록하는 것이 안전함 | OAuth API 구현 전 |
| Google OAuth redirect URI | local/dev 환경 값 확정 필요 | OAuth API 구현 전 |
| SMTP credential 관리 | secret 노출 없이 개발 환경에서 실제 발송해야 함 | password reset 구현 전 |
| EmailSender interface 적용 | `PasswordResetService`가 특정 email provider를 직접 알면 Google SMTP와 AWS SES 전환 비용이 커짐 | password reset 구현 전 |
| AWS SES 전환 여부 | Sprint 1 범위 아님. 배포/운영 단계에서 도입 여부 결정 | 배포/운영 정책 정리 시 |
| CI workflow 생성 | Spring Boot skeleton/Gradle wrapper가 있어야 workflow 기준을 정확히 만들 수 있음 | skeleton/Gradle wrapper 준비 후 |
| `/auth/me` 응답 최소 범위 | `data.user.id/email/nickname/avatarUrl`만 포함 | Sprint 1 Auth 기준 |

---


## 12-A. Sprint 1.5 Invitation Backend 범위와 DoD

Sprint 1 Backend 범위는 Auth-only와 인증 DB 4개 table로 유지한다. 아래 항목은 Sprint 1.5에 별도 착수한다.

### Sprint 1.5 DB/API 범위

| 구분 | 범위 |
|---|---|
| DB | `invitations`, `users`, `companies`, `company_members` 관계. `projects`, `project_members`는 Sprint 2 reference |
| API | `GET /api/v1/users/lookup`, company invitation create/list, `GET /api/v1/users/me/invitations`, accept/decline/cancel |
| Bootstrap | local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명을 사용. `POST /api/v1/companies`는 Sprint 1.5 구현 범위 제외 |
| 상태 | `PENDING`, `ACCEPTED`, `DECLINED`, `CANCELED`, `EXPIRED` |
| 대상/Role | 회사 invitation role은 `COMPANY_ADMIN/MEMBER`로 제한한다. 프로젝트 invitation 대상/role 검증은 Sprint 2에서 처리한다 |
| 만료 | 기본 7일, lazy expiration, 필수 batch scheduler 없음 |
| 알림 | 필요 시 내부 `notification_events` 원장 기록 가능. Sprint 1.5 완료 조건에는 포함하지 않으며 Push 알림, 별도 Invitation notification preference, S11 사용자 알림 UI 필수 구현 제외 |
| 제외 | actual email sending, token link, unregistered-user invitation, signup auto-accept, resend |

### Sprint 1.5 Backend DoD

| 항목 | 완료 조건 | 최소 증빙 |
|---|---|---|
| Entity | `invitations` entity와 FK mapping이 동작한다 | repository test 또는 `db/schema.sql` 기준 DB 확인 |
| 생성 | 회사 invitation 생성 시 7일 `expiresAt`을 반환하고 대상/role validation을 적용한다 | curl 또는 Postman 응답 |
| 중복 방지 | 이미 멤버이거나 pending invitation이 있으면 지정 error code를 반환한다 | 실패 케이스 응답 |
| 상태 전이 | accept/decline/cancel/expired 전이가 정책과 일치한다 | 상태별 API 응답과 DB 확인 |
| Membership | Sprint 1.5에서는 accept 시점에만 company membership을 생성한다 | DB row 확인 |
| 보안 | 등록 사용자 lookup은 `trim + lowercase` 정규화 email 기준이며 불필요한 개인정보를 노출하지 않는다 | 응답 DTO 확인 |

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
