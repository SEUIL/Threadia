> **작성자**: 여도현
> **작성일**: 2026-07-02
> **최종 수정**: 2026-07-02
> **버전**: v1.0
> **상태**: 검토 중
> **태그**: oauth, google, auth, spring-security, sprint-1

# Google OAuth 로그인 API 구현 조사

이 문서는 Sprint 1 WBS `1.3.2 소셜 로그인 API 구현`을 시작하기 전에 Google OAuth 로그인 구현 조건과 프로젝트 적용 기준을 정리한다.

## 1. 조사 결론

Threadia의 1.3.2 구현은 Spring Security OAuth2 Login의 기본 시작 endpoint를 그대로 사용하고, callback endpoint만 프로젝트 API 계약에 맞춰 커스터마이즈하는 방향이 맞다.

| 항목 | 적용 기준 |
|---|---|
| OAuth 시작 endpoint | `GET /oauth2/authorization/google` |
| OAuth callback endpoint | `GET /api/v1/auth/oauth2/callback/google` |
| Sprint 1 provider | `GOOGLE`만 구현 |
| 사용자 식별 기준 | Google `sub`를 `oauth_accounts.provider_user_id`에 저장 |
| 이메일 처리 | `trim + lowercase` 정규화 |
| 계정 연결 | 이메일만 같다는 이유의 blind auto-link 금지. 명시적 연결 또는 신뢰 가능한 provider 이메일 검증 조건은 별도 판단 |
| 인증 성공 조건 | `users.status = ACTIVE` 사용자만 허용 |
| token 발급 | callback 성공 후 이메일 로그인과 동일한 Threadia JWT access/refresh token pair 발급 |
| 검증 방식 | 브라우저 redirect 우선, Postman/curl은 보조 |

## 2. WBS 1.3.2 조건 정리

WBS 1.3.2는 `AUTH-03`에 해당하는 Google OAuth 로그인 API 구현 작업이다.

| 구분 | 내용 | 근거 |
|---|---|---|
| 작업명 | 소셜 로그인 API 구현 | `docs/00-project/sprint-운영/sprint-1-상세계획.md:54` |
| Owner | 여도현 + 김태준 + 김민경, 구현 중심은 여도현 | `docs/00-project/sprint-운영/sprint-1-상세계획.md:54`, `docs/00-project/sprint-운영/sprint-1-상세계획.md:96` |
| 완료 조건 | `GET /oauth2/authorization/{provider}`, `GET /api/v1/auth/oauth2/callback/{provider}` 구현, blind auto-link 금지 | `docs/00-project/sprint-운영/sprint-1-상세계획.md:54` |
| 의존성 | 인증 Entity, Google OAuth Client ID/Secret 발급 완료 | `docs/00-project/sprint-운영/sprint-1-상세계획.md:54`, `docs/00-project/sprint-운영/sprint-1-상세계획.md:81` |
| 검증 | OAuth 시작/callback 구현 후 브라우저 redirect 확인 | `docs/00-project/sprint-운영/sprint-1-상세계획.md:96` |

## 3. 프로젝트 확정 기준

### 3.1 API 계약

API 명세서 v2.3은 소셜 로그인 시작과 callback을 Sprint 1 확정 API로 둔다.

```http
GET /oauth2/authorization/{provider}
GET /api/v1/auth/oauth2/callback/{provider}
```

프로젝트 기준은 다음과 같다.

- Sprint 1 구현 provider는 `GOOGLE`이다. `KAKAO`, `NAVER`는 후속 확장 enum으로만 유지한다.
- OAuth 계정 연결에도 정규화된 이메일을 사용한다.
- provider 식별자는 `oauth_accounts`에 저장한다.
- callback 성공 시 `oauth_accounts`를 통해 `users`를 매핑하거나 생성한 뒤 Threadia JWT access token과 refresh token pair를 발급한다.

근거:

- `docs/04-api/API-명세서.md:108`
- `docs/04-api/API-명세서.md:121`
- `docs/04-api/API-명세서.md:122`
- `docs/04-api/API-명세서.md:145`
- `docs/04-api/API-명세서.md:156`

### 3.2 보안/계정 정책

OAuth 구현은 API 공통 Auth hardening 기준을 그대로 따라야 한다.

| 정책 | 구현 기준 | 근거 |
|---|---|---|
| email normalization | OAuth 계정 연결에서도 email을 `trim + lowercase`로 정규화한다 | `docs/04-api/API-명세서.md:59`, `docs/04-api/API-명세서.md:60` |
| ACTIVE-only | OAuth callback은 `users.status = ACTIVE` 사용자만 성공 처리한다 | `docs/04-api/API-명세서.md:32`, `docs/03-design/ERD.md:145` |
| refresh token revoke | 사용자가 `SUSPENDED` 또는 `DELETED`로 바뀌면 기존 refresh token을 revoke한다 | `docs/04-api/API-명세서.md:33`, `docs/03-design/ERD.md:145` |
| blind auto-link 금지 | 이메일만 같다는 이유로 기존 로컬 계정에 자동 연결하지 않는다. 이미 로그인한 사용자이거나 provider 이메일 검증을 신뢰할 수 있는 경우의 명시적 연결 여지는 canonical 기준에 맞춰 별도 구현한다 | `docs/04-api/API-명세서.md:153`, `docs/03-design/ERD.md:160` |
| secret 비노출 | OAuth client secret은 코드, 문서, PR, log에 기록하지 않는다 | `docs/08-members/황스일/OAuth/2026-07-01-Google-OAuth-앱-등록-작업기록.md:75` |

### 3.3 ERD 기준

`oauth_accounts`는 provider 계정과 Threadia user를 연결하는 테이블이다.

| 컬럼 | 구현 시 의미 | 근거 |
|---|---|---|
| `user_id` | 연결된 Threadia 사용자 ID | `docs/03-design/ERD.md:152` |
| `provider` | Sprint 1에서는 `GOOGLE`만 사용 | `docs/03-design/ERD.md:153` |
| `provider_user_id` | Google OIDC `sub` 저장 대상 | `docs/03-design/ERD.md:154` |
| `provider_email` | Google에서 받은 이메일 저장 대상 | `docs/03-design/ERD.md:155` |
| unique key | `provider`, `provider_user_id` 조합으로 중복 연결 방지 | `docs/03-design/ERD.md:159` |

요구사항 정의서도 `AUTH-03` 수용 기준을 `oauth_accounts` 기반 사용자 매핑 또는 생성과 blind auto-link 금지로 둔다.

근거:

- `docs/02-requirements/요구사항-정의서.md:116`
- `docs/02-requirements/요구사항-정의서.md:186`
- `docs/02-requirements/기능-시나리오-API-ERD-추적표.md:45`
- `docs/02-requirements/기능-시나리오-API-ERD-추적표.md:59`

## 4. 현재 코드베이스 상태

현재 backend는 Spring Boot skeleton과 MySQL 연결 검증 중심이다. OAuth/Auth 구현은 아직 없다.

| 확인 항목 | 현재 상태 | 근거 |
|---|---|---|
| Spring Boot | `3.5.16` | `backend/build.gradle:3` |
| Java | Java 17 toolchain | `backend/build.gradle:10` |
| 현재 의존성 | actuator, jdbc, validation, web, MySQL connector, test만 존재 | `backend/build.gradle:20` |
| 누락 의존성 | `spring-boot-starter-security`, `spring-boot-starter-oauth2-client` 없음 | `backend/build.gradle:20` |
| 실코드 상태 | `BackendApplication` 중심 skeleton | `backend/src/main/java/com/threadia/backend/BackendApplication.java:1` |

따라서 1.3.2 구현 전에 최소한 아래 작업이 선행되어야 한다.

1. Auth Entity와 Repository 준비
2. Spring Security dependency 추가
3. OAuth2 Client dependency 추가
4. Google client 설정을 환경 변수 기반으로 주입
5. callback success handler에서 Threadia user mapping과 JWT 발급 연결

## 5. Google OAuth 앱 등록 기준

황스일 작업 기록에 따르면 Google OAuth Client는 local backend callback 기준으로 준비되어 있다.

| 항목 | 값 | 근거 |
|---|---|---|
| OAuth Client type | Web application | `docs/08-members/황스일/OAuth/2026-07-01-Google-OAuth-앱-등록-작업기록.md:32` |
| Redirect URI | `http://localhost:8080/api/v1/auth/oauth2/callback/google` | `docs/08-members/황스일/OAuth/2026-07-01-Google-OAuth-앱-등록-작업기록.md:33` |
| Scope | `openid`, `email`, `profile` | `docs/08-members/황스일/OAuth/2026-07-01-Google-OAuth-앱-등록-작업기록.md:34` |
| 환경 변수 | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` | `docs/08-members/황스일/OAuth/2026-07-01-Google-OAuth-앱-등록-작업기록.md:52` |

주의할 점은 Google Cloud Console의 redirect URI와 Spring Security의 `redirectUri` 설정이 정확히 일치해야 한다는 점이다. 공백, trailing slash, port 차이도 `redirect_uri_mismatch` 원인이 될 수 있다.

근거:

- `docs/08-members/황스일/OAuth/2026-07-01-Google-OAuth-앱-등록-작업기록.md:37`
- Google OpenID Connect 문서: <https://developers.google.com/identity/openid-connect/reference>

## 6. Spring Security 적용 기준

Spring Security 6.5 기준으로 OAuth2 Login은 다음 기본값을 가진다.

| 항목 | Spring Security 기준 | Threadia 적용 |
|---|---|---|
| OAuth 시작 base URI | `/oauth2/authorization` | 그대로 사용 |
| OAuth 시작 URL | `/oauth2/authorization/{registrationId}` | `/oauth2/authorization/google` |
| 기본 callback URI | `/login/oauth2/code/*` | 사용하지 않음 |
| custom callback | `redirectionEndpoint().baseUri(...)`로 변경 가능 | `/api/v1/auth/oauth2/callback/*` |
| client redirect URI | callback base URI와 일치해야 함 | `{baseUrl}/api/v1/auth/oauth2/callback/{registrationId}` |

외부 근거:

- Spring Security OAuth2 Login advanced configuration: <https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html>
- Spring Security authorization grants: <https://docs.spring.io/spring-security/reference/servlet/oauth2/client/authorization-grants.html>
- Spring Boot OAuth2 client reference: <https://docs.spring.io/spring-boot/reference/security/oauth2.html>
- Spring Security source, default authorization base URI: <https://github.com/spring-projects/spring-security/blob/9161eced5ba4b65d18fc0585fdf0e29b9f670235/oauth2/oauth2-client/src/main/java/org/springframework/security/oauth2/client/web/DefaultOAuth2AuthorizationRequestResolver.java#L71-L75>
- Spring Security source, default callback URI: <https://github.com/spring-projects/spring-security/blob/9161eced5ba4b65d18fc0585fdf0e29b9f670235/oauth2/oauth2-client/src/main/java/org/springframework/security/oauth2/client/web/OAuth2LoginAuthenticationFilter.java#L102-L107>

Threadia에서는 아래 형태가 구현 방향이다.

```java
http.oauth2Login(oauth2 -> oauth2
    .redirectionEndpoint(redirection -> redirection
        .baseUri("/api/v1/auth/oauth2/callback/*")
    )
);
```

그리고 client registration의 redirect URI는 다음과 맞춰야 한다.

```properties
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/api/v1/auth/oauth2/callback/{registrationId}
```

실제 값 기준으로는 local에서 다음 URI가 되어야 한다.

```text
http://localhost:8080/api/v1/auth/oauth2/callback/google
```

## 7. 계정 매핑 판단 기준

Google OIDC에서 안정적인 사용자 식별자는 email이 아니라 `sub`다. Google 문서 기준으로 `sub`는 unique하고 재사용되지 않는 subject identifier다. 반대로 email은 변경될 수 있고 primary key로 쓰기 어렵다.

따라서 Threadia OAuth 매핑 기준은 다음과 같이 둔다.

1. Google callback에서 `sub`, `email`, `email_verified`, `name`, `picture`를 읽는다.
2. `provider = GOOGLE`, `provider_user_id = sub`로 `oauth_accounts`를 먼저 조회한다.
3. 기존 `oauth_accounts`가 있으면 연결된 `users`를 조회한다.
4. 연결된 user가 `ACTIVE`가 아니면 인증 실패 처리한다.
5. 기존 `oauth_accounts`가 없으면 새 OAuth user 생성 또는 명시적 계정 연결 흐름을 사용한다.
6. 이메일만 같은 기존 local user가 있더라도 자동 연결하지 않는다. 단, 이미 로그인한 사용자가 명시적으로 연결하거나 provider 이메일 검증을 신뢰할 수 있는 경우는 canonical 정책에 따라 별도 연결 흐름으로 처리한다.
7. 새 user를 만들 때 email은 `trim + lowercase`로 정규화해 저장한다.

이 기준은 프로젝트의 blind auto-link 금지 정책과 Google OIDC의 `sub` 중심 식별 기준에 모두 부합한다.

## 8. 구현 체크리스트

### 8.1 dependency

현재 `backend/build.gradle`에는 OAuth 구현에 필요한 security dependency가 없다. 구현 PR에서는 최소 아래 dependency 추가가 필요하다.

```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
```

JPA Entity 구현 방식이 확정되어 있다면 JPA starter도 별도 작업 범위에서 확인한다.

### 8.2 configuration

`application.properties` 또는 local profile에는 secret 원문을 직접 쓰지 않는다. `client-id`와 `client-secret`은 환경 변수로 주입한다.

Spring Security 설정에서는 환경별 host/proxy 차이를 줄이기 위해 `redirect-uri`를 `{baseUrl}`/`{registrationId}` template 방식으로 두는 것을 우선 권장한다.

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,email,profile
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/api/v1/auth/oauth2/callback/{registrationId}
```

Google Cloud Console에 등록된 local callback의 실제 값은 아래 URI와 일치해야 한다.

```text
http://localhost:8080/api/v1/auth/oauth2/callback/google
```

`GOOGLE_REDIRECT_URI=http://localhost:8080/api/v1/auth/oauth2/callback/google`처럼 절대 URI를 환경 변수로 주입하는 방식도 local에서는 가능하지만, dev/prod와 proxy 환경에서는 template 방식보다 깨지기 쉬우므로 보조 대안으로만 둔다.

### 8.3 success handler

OAuth callback 성공 후에는 Spring Security session 로그인 완료로 끝내지 않고 Threadia JWT 발급 흐름으로 연결해야 한다.

필요 처리:

- Google OIDC attributes 추출
- `oauth_accounts` 조회 또는 생성
- `users.status = ACTIVE` 확인
- Threadia access token 발급
- Threadia refresh token 발급 및 hash 저장
- refresh token cookie 정책 적용 여부 확인
- frontend redirect 방식 확정

프론트엔드 구조 문서는 OAuth가 axios 호출이 아니라 브라우저 redirect 방식이라고 정리한다. callback 후 access token 전달 방식은 아직 백엔드와 추가 협의가 필요하다고 되어 있다.

근거:

- `docs/05-architecture/프론트엔드-구조.md:255`
- `docs/05-architecture/프론트엔드-구조.md:260`
- `docs/05-architecture/프론트엔드-구조.md:268`
- `docs/05-architecture/프론트엔드-구조.md:274`

## 9. 검증 방법

1. `./gradlew test`로 security 설정과 context load를 확인한다.
2. 브라우저에서 `http://localhost:8080/oauth2/authorization/google`로 접근한다.
3. Google 로그인 후 `http://localhost:8080/api/v1/auth/oauth2/callback/google`로 돌아오는지 확인한다.
4. callback 성공 시 `oauth_accounts` row가 생성되거나 기존 row로 user가 매핑되는지 확인한다.
5. `users.status != ACTIVE` 사용자에 대해서는 callback 성공 후에도 Threadia 인증을 거부하는지 확인한다.
6. 같은 email을 가진 local user가 있을 때 provider 계정을 자동 연결하지 않는지 확인한다.
7. 성공 케이스에서 Threadia JWT access token과 refresh token pair가 발급되는지 확인한다.
8. OAuth client secret, raw token, 개인정보가 log, PR 본문, 문서에 노출되지 않았는지 확인한다.

Backend DoD는 Google OAuth 완료 조건을 “OAuth 시작, callback, user mapping이 동작하고 blind auto-link를 하지 않는다”로 둔다.

근거:

- `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:221`
- `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md:254`

## 10. 구현 전 남은 결정 사항

| 항목 | 상태 | 판단 |
|---|---|---|
| callback 후 frontend 전달 방식 | 미확정 | URL fragment, 임시 redirect page, cookie 방식 중 선택 필요 |
| refresh token 저장 위치 | 권장안 있음 | access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie 권장 기준 확인 필요 |
| local/dev/prod redirect URI | local만 확인 | dev/prod 배포 전 Google Console에 환경별 URI 별도 등록 필요 |
| JPA vs JDBC 구현 경계 | 현재 skeleton은 JDBC 중심 | Entity 구현 작업 결과에 맞춰 repository 계층 결정 필요 |

## 11. 요약

1. WBS 1.3.2는 Google OAuth 시작/callback API 구현과 blind auto-link 금지 검증이 핵심이다.
2. Spring Security 기본 시작 endpoint가 프로젝트 API 계약과 일치하므로 `/oauth2/authorization/google`은 그대로 사용한다.
3. callback은 프로젝트 계약에 맞춰 `/api/v1/auth/oauth2/callback/google`로 커스터마이즈해야 한다.
4. Google Console redirect URI, Spring Security `redirectionEndpoint`, client registration `redirect-uri`가 모두 같은 값을 사용해야 한다.
5. 계정 매핑은 email이 아니라 Google `sub`를 `oauth_accounts.provider_user_id`로 저장하는 방식이 안전하다.
6. 같은 email만으로 기존 계정에 자동 연결하지 않는 정책은 프로젝트 요구사항과 Google OIDC 식별 기준에 모두 맞다. 명시적 연결 또는 신뢰 가능한 provider 이메일 검증 조건은 blind auto-link와 구분한다.
7. 현재 backend에는 OAuth/Security 의존성과 구현 코드가 없으므로 1.3.2 구현 PR에서 dependency, security config, success handler, user mapping, JWT 발급까지 연결해야 한다.
