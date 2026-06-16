> **작성자**: Threadia Backend 분업
> **작성일**: 2026-06-16
> **최종 수정**: 2026-06-16
> **버전**: v1.1
> **상태**: 작성 중

# Sprint 1 백엔드 의존성 / 기술 스택 / DoD v1.1

이 문서는 Sprint 1 Backend 작업 착수 전 필요한 범위, 기술 스택, 의존성, blocker, 완료 기준을 정리한다.

기준 문서:
- `Sprint 1_detail.md` v0.2
- `Sprint1_Backlog.md`
- `Sprint1_담당그룹_역할분담 v1.1.md`
- `API 명세서 v2.3`
- `Definition of Done_작업 유형별 완료 조건 v2.md`
- `검증 방법_test-build-API-화면 확인 기준 v2.md`
- `AWS SES/AWS-SES.md`
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
| DB 대상 | `users`, `oauth_accounts`, `password_reset_tokens` |
| 인증 방식 | JWT Bearer Token, refresh token 사용 |
| 미확정 | Java 정확 버전, JWT 만료/rotation/invalidation, cookie/security/CORS 세부 정책 |
| 검증 방식 | Gradle wrapper, curl 또는 Postman, MySQL client, application log, 관련 API 응답 |

---

## 3. Sprint 1 Backend 범위

Sprint 1 Backend는 인증 중심 API와 인증에 필요한 최소 DB 모델을 만든다.

| 구분 | 결정 |
|---|---|
| 포함 기능 | 이메일 회원가입, 이메일 로그인, 로그아웃, `/auth/me`, Google OAuth, 비밀번호 재설정 |
| 포함 API | Auth API와 OAuth callback, password reset request/confirm |
| 포함 DB | `users`, `oauth_accounts`, `password_reset_tokens` |
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
| 이메일 정책 | 실제 이메일 발송 | 최신 사용자 결정은 개발 환경 Google SMTP 우선, 배포/운영은 AWS SES 전환 가능 |
| 이메일 provider 분리 | `EmailSender` interface 뒤로 분리 | `PasswordResetService`는 Google SMTP/AWS SES 등 특정 provider를 직접 알지 않도록 구현 |
| OAuth 준비 | Google OAuth 앱 등록과 redirect URI 필요 | 구현 직전에 실제 환경 기준으로 등록 |

---

## 5. 개발 초반 확정 / 확인 항목

| 항목 | 상태 | 확인 시점 | 담당 |
|---|---|---|---|
| Java 정확 버전 | 미정 | Spring Boot skeleton 확정 시 | Spring Boot skeleton Owner 여도현, Reviewer 황스일 |
| JWT access/refresh 만료 시간 | 미정 | Day 2 또는 Auth 구현 초반 | JWT policy meeting Owner 황스일, Support 여도현, Reviewer 김태준 |
| refresh token rotation | 미정 | Day 2 또는 Auth 구현 초반 | JWT policy meeting Owner 황스일, Support 여도현, Reviewer 김태준 |
| refresh token invalidation | 미정 | logout 구현 전 | JWT policy meeting Owner 황스일, Support 여도현, Reviewer 김태준 |
| cookie/security/CORS 정책 | cookie 선택 시 확정 필요 | Frontend JWT 저장 방식 확정 시 | JWT policy meeting Owner 황스일, Support 여도현, Reviewer 김태준 |
| Google OAuth 앱 등록 | 예정 | OAuth 구현 직전 | Owner 황스일, Support 김민정, Reviewer 여도현 |
| Google OAuth redirect URI | 예정 | OAuth 구현 직전 | Owner 황스일, Support 김민정, Reviewer 여도현 |
| SMTP 설정 방식 | 확인 필요 | password reset request 구현 전 | Password reset API Owner 김태준, Reviewer 황스일 |
| 이메일 provider 추상화 | 확인 필요 | password reset request 구현 전 | Password reset API Owner 김태준, Reviewer 황스일 |
| AWS SES ADR 차이 | 조율 필요 | 배포/운영 메일 정책 정리 시 | 기존 AWS SES ADR은 local mock/console 후 SES 전환을 말하므로, 최신 Google SMTP 우선 결정과 조율 필요 |
| CI workflow 생성 | 대기 | Spring Boot skeleton/Gradle wrapper 준비 후 | CI 기준 확인 + AGENTS.md Owner 황스일, Reviewer 김태준 |

> Spring Boot 3.x는 Java 17 이상이 필요하다. CI 예시는 Java 21을 언급하므로, 정확한 개발/CI Java 버전은 skeleton 안정화 시점에 확정한다.

---

## 6. 작업 일정 / 순서

| 순서 | Backend 작업 | 완료 조건 |
|---|---|---|
| 1 | Spring Boot 3.x skeleton 생성, Gradle wrapper 실행 확인 | `./gradlew bootRun`으로 애플리케이션 기동 확인 |
| 2 | MySQL 8.4 연결 설정, local DB 연결 확인 | application log 또는 MySQL client로 연결 확인 |
| 3 | Auth JPA Entity 작성 | `users`, `oauth_accounts`, `password_reset_tokens` entity와 기본 mapping 확인 |
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
| 실패 케이스 | validation, 인증 실패, token 없음/만료, reset token 만료/사용 완료 | 필수 |

Frontend JWT 저장 방향은 cookie 또는 memory다. cookie가 선택되면 Backend는 `Set-Cookie`, `HttpOnly`, `Secure`, `SameSite`, CORS credential 정책을 함께 확정해야 한다. memory가 선택되면 API 응답과 refresh 흐름 기준을 Frontend와 맞춘다.

### 7.2 DB 기준

| Table / Entity | Sprint 1 사용 목적 | 주요 기준 |
|---|---|---|
| `users` | 이메일 사용자와 OAuth 연결 대상 사용자 | `users.role` 추가 안 함 |
| `oauth_accounts` | Google OAuth provider 계정 연결 | provider, provider user id, user mapping 확인 |
| `password_reset_tokens` | 비밀번호 재설정 token 검증 | `token_hash`만 저장, raw token 저장 금지, expires/used 처리 |

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
| Spring Boot skeleton | Spring Boot 3.x, Gradle wrapper, Java 17 이상 | Java 정확 버전 미정 | CI와 local 실행 환경 차이 가능 | skeleton 안정화 시 Java 버전 확정 | 미결 |
| MySQL 연결 | MySQL 8.4, application datasource 설정 | local DB 계정과 schema 준비 필요 | API 구현 전 저장소 검증 지연 가능 | connection log와 MySQL client 확인 | 확인 필요 |
| JWT 정책 | JWT Bearer Token, refresh token 사용 | 만료, rotation, invalidation 세부 미정 | login/logout/me 동작 기준 불명확 | Day 2 또는 Auth 초반 결정 | 일부 미결 |
| cookie/security/CORS | Frontend JWT 저장 방식 | cookie vs memory 최종 결정 필요 | FE-BE 인증 연동 방식 충돌 가능 | Frontend와 함께 정책 확정 | 일부 미결 |
| Google OAuth | Google app registration, redirect URI | 앱 등록을 구현 직전에 진행 | OAuth API 구현 전 준비 필요 | 등록 정보와 redirect URI 확인 | 예정 |
| Password reset email | Google SMTP, 향후 AWS SES 가능, `EmailSender` interface | SMTP credential과 secret 관리 방식 필요 | reset request E2E 검증 지연 가능 | 개발 환경 SMTP 발송 확인, `PasswordResetService`의 provider 직접 의존 방지 | 확인 필요 |
| AWS SES ADR 차이 | 기존 AWS SES ADR | ADR은 local mock/console 후 SES 전환을 말하지만 최신 결정은 개발 환경 Google SMTP 우선임 | 운영 메일 정책 정리 필요 | 개발은 Google SMTP 우선, 배포/운영은 SES 전환 여부 조율 | 조율 필요 |
| CI | Spring Boot skeleton, Gradle wrapper | workflow 생성은 skeleton/Gradle wrapper 준비 후 가능 | PR 검증 자동화 지연 가능 | 초기 Backend CI 기준은 `./gradlew test`, 안정화 후 `./gradlew build` | 대기 |

### 8.2 기능별 의존성 / Blocker

| 기능 | 의존성 | Blocker | 완료 기준 | 상태 |
|---|---|---|---|---|
| 회원가입 | `users`, password hashing, validation | 이메일 중복과 validation error 형식 확인 | signup 정상/실패 API 확인 | 확인 필요 |
| 로그인 | `users`, JWT 발급, refresh token 정책 | token 만료와 저장 방식 세부 미정 | login 성공/실패, token 발급 확인 | 일부 미결 |
| 로그아웃 | refresh token invalidation 기준 | invalidation 세부 미정 | logout 후 인증 상태 제거 확인 | 일부 미결 |
| `/auth/me` | JWT 검증, user context DTO | Sprint 1 최소 응답 범위 확인 필요 | token 있음/없음/만료 응답 확인 | 확인 필요 |
| Google OAuth | Google OAuth app, redirect URI, `oauth_accounts` | 앱 등록과 callback URI 준비 필요 | OAuth 시작/callback/user mapping 확인 | 예정 |
| 비밀번호 재설정 | Google SMTP, `EmailSender`, `password_reset_tokens` | SMTP 설정, provider 분리, token hash 검증 필요 | request email 발송, confirm 성공/실패, `PasswordResetService` provider 비의존 확인 | 확인 필요 |

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
| 후속 조치 | 미확정 항목은 owner 또는 조율 대상, 처리 시점을 기록한다 | TODO 또는 blocker 기록 |

### 9.2 기능별 DoD

| 기능 | 완료 조건 | 최소 증빙 |
|---|---|---|
| Spring Boot skeleton | Gradle wrapper 기준으로 실행 가능하다 | `./gradlew bootRun` |
| MySQL connection | MySQL 8.4에 연결되고 Auth entity 저장을 확인한다 | MySQL client 또는 application log |
| Auth JPA Entity | `users`, `oauth_accounts`, `password_reset_tokens` mapping이 동작한다 | repository test 또는 관련 API 결과 |
| 회원가입 | 이메일 회원가입 성공, 중복, validation 실패를 처리한다 | curl 또는 Postman 응답 |
| 로그인 | 이메일 로그인 성공, credential 실패를 처리한다 | curl 또는 Postman 응답 |
| 로그아웃 | refresh token 또는 session invalidation 기준에 따라 인증 상태를 정리한다 | logout API 응답과 후속 인증 확인 |
| `/auth/me` | token 있음, token 없음, token 만료 케이스를 구분한다 | `/auth/me` 요청/응답 예시 |
| Google OAuth | Google OAuth 시작, callback, user mapping이 동작한다 | redirect/callback 확인 기록 |
| 비밀번호 재설정 | 실제 이메일 발송, provider 분리, hash 저장, 만료/사용 완료 처리가 동작한다 | SMTP 발송 확인, `EmailSender` 의존 확인, DB 확인, API 응답 |

### 9.3 완료로 보지 않는 경우

| 경우 | 이유 |
|---|---|
| `./gradlew bootRun` 실행 확인이 없다 | 애플리케이션 기동 가능 여부를 확인할 수 없다 |
| Auth API 요청/응답 예시가 없다 | API 명세와 실제 구현 일치 여부를 확인할 수 없다 |
| 정상 케이스만 확인했다 | 실패 케이스에서 인증 보안 문제가 남을 수 있다 |
| `password_reset_tokens.token_hash`에 raw token을 저장했다 | token 탈취 위험이 있다 |
| `users.role`을 추가했다 | Sprint 1 권한 설계와 충돌한다 |
| JWT refresh token 세부 정책이 불명확한데 완료 처리했다 | logout, `/auth/me`, token 갱신 기준이 추적되지 않는다 |
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
- [x] Auth table/entity: `users`, `oauth_accounts`, `password_reset_tokens`
- [x] `users.role` 추가 안 함
- [x] `password_reset_tokens.token_hash`에는 hash만 저장
- [x] password reset expires/used 처리 필요
- [x] refresh token 사용
- [ ] Java 정확 버전 확정
- [ ] JWT access/refresh 만료 시간 확정
- [ ] refresh token rotation/invalidation 기준 확정
- [ ] cookie/security/CORS 정책 확정, cookie 선택 시 필수
- [ ] Google OAuth 앱 등록
- [ ] Google OAuth redirect URI 확정
- [ ] Google SMTP 설정과 secret 관리 방식 확인
- [ ] `EmailSender` interface로 provider-specific sending 분리
- [ ] `PasswordResetService`가 email provider를 직접 알지 않도록 구현 기준 확인
- [ ] AWS SES ADR의 local mock/console 후 SES 전환 기준과 최신 Google SMTP 우선 정책 차이 조율
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
| Java 정확 버전 | Spring Boot 3.x는 Java 17 이상 필요, CI 예시는 Java 21 언급 | skeleton 안정화 시점 |
| JWT access/refresh expiration | 보안과 UX 기준을 함께 맞춰야 함 | Day 2 또는 Auth 구현 초반 |
| refresh token rotation | login/logout/me 흐름과 함께 결정해야 함 | Day 2 또는 Auth 구현 초반 |
| refresh token invalidation | logout 완료 기준과 직접 연결됨 | logout 구현 전 |
| cookie/security/CORS | Frontend JWT 저장 방식이 cookie인지 memory인지에 따라 달라짐 | Frontend 저장 방식 확정 시 |
| Google OAuth app registration | 구현 직전 실제 redirect URI 기준으로 등록하는 것이 안전함 | OAuth API 구현 전 |
| Google OAuth redirect URI | local/dev 환경 값 확정 필요 | OAuth API 구현 전 |
| SMTP credential 관리 | secret 노출 없이 개발 환경에서 실제 발송해야 함 | password reset 구현 전 |
| EmailSender interface 적용 | `PasswordResetService`가 특정 email provider를 직접 알면 Google SMTP와 AWS SES 전환 비용이 커짐 | password reset 구현 전 |
| AWS SES 전환 여부 | 기존 ADR은 local mock/console 후 SES 전환을 말하지만, 최신 결정은 개발 환경 Google SMTP 우선임 | 배포/운영 정책 정리 시 |
| CI workflow 생성 | Spring Boot skeleton/Gradle wrapper가 있어야 workflow 기준을 정확히 만들 수 있음 | skeleton/Gradle wrapper 준비 후 |
| `/auth/me` 응답 최소 범위 | Sprint 1 인증 유지에 필요한 user context만 우선 필요 | `/auth/me` 구현 전 |
