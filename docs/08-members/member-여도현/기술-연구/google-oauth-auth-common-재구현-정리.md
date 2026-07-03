> **작성자**: 여도현
> **작성일**: 2026-07-03
> **최종 수정**: 2026-07-03
> **버전**: v1.0
> **상태**: 검토 중
> **태그**: auth, oauth, google, spring-security, jwt

# Google OAuth auth-common 재구현 정리

이 문서는 `feature/auth-common` 최신 commit `4f0bb87` 이후 기준에 맞춰 Google OAuth backend를 다시 구현한 내용을 정리한다. 이전 임시 구현은 `common.auth` 중심 token service와 `auth.controller` DTO 구조를 사용했지만, 최신 기준은 `auth.dto`, `config.jwt`, `config.token`, `config.cookie`, `user` package를 사용한다.

## 1. 최신 기준

| 영역 | 기준 |
|---|---|
| Email auth controller | `auth.controller.AuthController` |
| Email auth service | `auth.service.AuthService` |
| Auth DTO | `auth.dto.*` |
| `/auth/me` | `user.MeController`, `user.MeService`, `user.MeData` |
| access token | `config.jwt.JwtTokenService` |
| refresh token raw/hash | `config.token.RefreshTokenGenerator` |
| refresh cookie | `config.cookie.RefreshTokenCookieProvider` |
| OAuth entity | `auth.domain.OAuthAccount`, `OAuthProvider.GOOGLE` |
| 공통 응답 | `common.response.ApiResponse` |
| 공통 user projection | `common.auth.UserResponse` |

## 2. 구현 범위

| 파일 | 내용 |
|---|---|
| `auth/repository/OAuthAccountRepository.java` | `provider + providerUserId`, `userId + provider` 조회 method 추가 |
| `auth/service/GoogleOAuthLoginService.java` | Google user info 매핑, OAuth user 생성, 기존 account 조회, token/cookie 발급 |
| `auth/service/GoogleOAuthUserInfo.java` | Google `sub`, `email`, `email_verified`, `name`, `picture` 추출과 email normalization |
| `auth/service/OAuthLoginException.java` | OAuth 인증 실패를 기존 401 흐름과 연결 |
| `auth/service/OAuthLoginResult.java` | OAuth login 결과 body/cookie 전달 객체 |
| `auth/controller/OAuth2LoginSuccessHandler.java` | OAuth 성공 후 refresh cookie 설정과 frontend redirect |
| `auth/controller/OAuth2LoginFailureHandler.java` | Google 인증 실패 시 frontend failure redirect |
| `config/GoogleOAuthClientConfig.java` | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`이 있을 때만 Google client 등록 |
| `config/SecurityConfig.java` | Spring Security filter chain, OAuth endpoint, callback handler 연결 |
| `config/AuthOAuthProperties.java` | OAuth success/failure redirect URL 설정 |
| `build.gradle` | OAuth2 client, spring-security-test dependency 추가 |
| `application.properties` | `.env` import, OAuth redirect URL 기본값 추가 |

## 3. OAuth 흐름

```text
GET /oauth2/authorization/google
→ Google 로그인
→ GET /api/v1/auth/oauth2/callback/google
→ OAuth2LoginSuccessHandler
→ GoogleOAuthLoginService
→ users / oauth_accounts / refresh_tokens 저장
→ Set-Cookie: refreshToken=...
→ http://localhost:3000/hub redirect
```

실패 시에는 다음 URL로 redirect한다.

```text
http://localhost:3000/login?oauth=failed
```

## 4. 계정 매핑 정책

| 정책 | 구현 |
|---|---|
| provider 식별 | Google `sub`를 `oauth_accounts.provider_user_id`로 저장 |
| email 검증 | Google `email_verified = true`인 계정만 허용 |
| email normalization | Google email을 `trim + lowercase` 처리 |
| blind auto-link 금지 | 같은 email의 local user가 있어도 OAuth account가 없으면 자동 연결하지 않음 |
| OAuth-only user | 신규 OAuth user는 `password_hash = NULL` |
| ACTIVE-only | 연결된 user가 `ACTIVE`가 아니면 OAuth 로그인 실패 |
| refresh token | raw token은 cookie로만 내려주고 DB에는 hash만 저장 |
| cookie | 기존 `RefreshTokenCookieProvider` 기준 `HttpOnly`, `SameSite=Lax`, `Path=/`, 14일 |
| prod cookie secure | `prod` profile에서는 `THREADIA_COOKIE_SECURE=true`가 아니면 startup 실패 |

## 5. 환경 변수

OAuth client는 아래 두 값이 있을 때만 활성화된다.

```properties
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
```

callback URI 기본값은 다음 template이다.

```properties
GOOGLE_REDIRECT_URI={baseUrl}/api/v1/auth/oauth2/callback/{registrationId}
```

로컬 Google Console redirect URI는 아래 값과 일치해야 한다.

```text
http://localhost:8080/api/v1/auth/oauth2/callback/google
```

`.env`는 repository root 또는 `backend/` 아래에 둘 수 있게 `spring.config.import`를 추가했다. 실제 secret 값은 문서, PR, log에 남기지 않는다.

Spring Boot 실행(`test`, `build`, `bootRun`)은 `.env`를 자동 import하므로 별도 `source .env`가 필요 없다. 다만 shell에서 `$THREADIA_DB_USERNAME` 같은 변수를 직접 펼치는 `docker exec mysql` 명령은 Spring Boot 밖에서 실행되므로 `set -a; source .env; set +a`가 필요하다.

## 6. 테스트 추가

| 테스트 | 검증 내용 |
|---|---|
| `GoogleOAuthLoginServiceTest` | 신규 OAuth user 생성, 기존 account login, blind auto-link 금지, non-ACTIVE 차단, `email_verified` 검증 |
| `OAuth2LoginSuccessHandlerTest` | refresh cookie header와 success/failure redirect |
| `OAuth2LoginFailureHandlerTest` | Google 인증 실패 redirect |

## 7. Gradle 검증 명령

`THREADIA_JWT_SECRET`이 없으면 기존 `BackendApplicationTests`가 context load에서 실패한다. 현재 local `.env`에 `THREADIA_JWT_SECRET`을 넣어 두었다면 prefix 없이 실행해도 된다.

```bash
cd /Users/yeodohyeon/Documents/GitHub/Threadia/backend
./gradlew --no-daemon test
./gradlew --no-daemon build
```

만약 shell이나 IDE가 `.env`를 읽지 못하는 상황이면 test용 secret을 shell 환경변수로 먼저 주입한 뒤 확인한다. 실제 값은 문서, PR, log에 남기지 않는다.

```bash
./gradlew --no-daemon test
./gradlew --no-daemon build
```

## 8. Local 실행 준비

### 8.1 `.env` 확인

OAuth browser flow 확인 전 repository root `.env`에 아래 값이 있어야 한다. 실제 값은 문서, PR, log에 남기지 않는다.

```properties
THREADIA_DB_URL=jdbc:mysql://localhost:3307/threadia_test
THREADIA_DB_USERNAME=threadia
THREADIA_DB_PASSWORD=...
THREADIA_JWT_SECRET=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
```

선택 값은 아래와 같다. 기본값을 그대로 쓰면 생략 가능하다.

```properties
GOOGLE_REDIRECT_URI={baseUrl}/api/v1/auth/oauth2/callback/{registrationId}
THREADIA_OAUTH_SUCCESS_REDIRECT_URL=http://localhost:3000/hub
THREADIA_OAUTH_FAILURE_REDIRECT_URL=http://localhost:3000/login?oauth=failed
```

Google Cloud Console의 Authorized redirect URI는 아래와 일치해야 한다.

```text
http://localhost:8080/api/v1/auth/oauth2/callback/google
```

### 8.2 MySQL 실행

```bash
cd /Users/yeodohyeon/Documents/GitHub/Threadia
docker compose --env-file .env -f compose.mysql.yml up -d mysql
```

schema 재적용이 필요하면 아래 명령을 사용한다. local DB 데이터가 삭제될 수 있으므로 필요한 경우에만 실행한다.

```bash
set -a
source .env
set +a

docker exec -i threadia-mysql84 \
  mysql -u"$THREADIA_DB_USERNAME" -p"$THREADIA_DB_PASSWORD" "${THREADIA_DB_NAME:-threadia_test}" \
  < db/schema.sql
```

### 8.3 Backend 실행

```bash
cd /Users/yeodohyeon/Documents/GitHub/Threadia/backend
./gradlew --no-daemon bootRun
```

성공 기준은 다음 로그다.

```text
Tomcat started on port 8080
Started BackendApplication
```

## 9. Postman 확인 방법

Postman은 Google 계정 로그인 전체 flow보다 OAuth 시작 endpoint와 redirect 설정 확인에 적합하다. 실제 Google 로그인과 callback DB 저장은 브라우저로 확인한다.

### 9.1 OAuth 시작 endpoint 302 확인

| 항목 | 값 |
|---|---|
| Method | `GET` |
| URL | `http://localhost:8080/oauth2/authorization/google` |
| Body | 없음 |
| Authorization | 없음 |

Postman Settings에서 `Automatically follow redirects`를 끄고 요청한다.

성공 기준:

```text
Status: 302 Found
Location: https://accounts.google.com/...
```

실패 기준과 원인:

| 증상 | 가능 원인 |
|---|---|
| `404` | backend가 최신 코드가 아니거나 OAuth client bean이 비활성화됨 |
| `500` 또는 startup 실패 | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `THREADIA_JWT_SECRET` 설정 누락 가능 |
| Google 화면에서 `redirect_uri_mismatch` | Google Console redirect URI가 `/api/v1/auth/oauth2/callback/google`와 불일치 |

### 9.2 curl 보조 확인

Postman과 같은 확인을 terminal에서 할 수 있다.

```bash
curl -i http://localhost:8080/oauth2/authorization/google
```

성공 기준은 `HTTP/1.1 302`와 `Location: https://accounts.google.com/...`이다.

## 10. 브라우저 확인 방법

브라우저는 실제 Google 로그인, callback, DB 저장, refresh cookie 발급까지 확인하는 주 검증 방법이다.

### 10.1 Backend 단독 확인

브라우저 주소창에 아래 URL을 입력한다.

```text
http://localhost:8080/oauth2/authorization/google
```

성공 흐름:

```text
Google 로그인
→ http://localhost:8080/api/v1/auth/oauth2/callback/google
→ http://localhost:3000/hub
```

frontend가 실행 중이 아니면 최종 `/hub` 화면은 뜨지 않을 수 있다. 그래도 주소가 `/hub`로 이동했고 DB row가 생성되면 backend OAuth callback은 성공한 것이다.

실패 흐름:

```text
http://localhost:3000/login?oauth=failed
```

backend log에서 아래 메시지를 확인한다.

```text
Google OAuth authentication failed: ...
Google OAuth callback failed: ...
```

### 10.2 Frontend까지 같이 확인

frontend가 준비되어 있으면 frontend dev server를 실행한 뒤 로그인 화면의 Google OAuth 버튼으로 시작한다. 버튼은 내부적으로 아래 이동을 수행해야 한다.

```text
window.location.href = "http://localhost:8080/oauth2/authorization/google"
```

확인 기준:

| 항목 | 기대 결과 |
|---|---|
| Google 로그인 시작 | Google 계정 선택/동의 화면으로 이동 |
| callback | `/api/v1/auth/oauth2/callback/google`로 돌아옴 |
| 최종 redirect | `http://localhost:3000/hub` |
| refresh cookie | DevTools Application 또는 Network에서 `refreshToken` cookie 확인 |
| URL token 노출 | access token이 URL query/string에 노출되지 않음 |

## 11. DB 확인 방법

Google 로그인 성공 후 DB row를 확인한다.

```bash
cd /Users/yeodohyeon/Documents/GitHub/Threadia
set -a
source .env
set +a

docker exec -e MYSQL_PWD="$THREADIA_DB_PASSWORD" threadia-mysql84 \
  mysql -u"$THREADIA_DB_USERNAME" "${THREADIA_DB_NAME:-threadia_test}" \
  -e "SELECT id,email,password_hash,nickname,status FROM users ORDER BY id DESC LIMIT 5;
      SELECT id,provider,provider_user_id,provider_email,user_id FROM oauth_accounts ORDER BY id DESC LIMIT 5;
      SELECT user_id,token_hash,expires_at,revoked_at FROM refresh_tokens ORDER BY id DESC LIMIT 5;"
```

성공 기준:

| 테이블 | 확인 기준 |
|---|---|
| `users` | Gmail 계정 row 존재, OAuth 신규 user는 `password_hash = NULL`, `status = ACTIVE` |
| `oauth_accounts` | `provider = GOOGLE`, `provider_user_id`가 Google `sub`, `provider_email`이 정규화된 email |
| `refresh_tokens` | `token_hash` 존재, `revoked_at = NULL`, raw token 저장 없음 |

## 12. 주의할 점

- OAuth callback 후 access token 전달 방식은 아직 확정되지 않았다. 현재 구현은 access token을 URL에 노출하지 않고 refresh cookie만 설정한 뒤 `/hub`로 redirect한다.
- `/api/v1/auth/refresh` 구현이 없으면 frontend가 `/hub` 도착 후 access token을 복구하는 흐름은 별도 작업이 필요하다.
- Postman으로 Google OAuth callback을 직접 재현하려면 authorization code와 state/session 처리가 필요해서 실무 검증은 브라우저가 더 적합하다.
- 같은 email의 local user가 이미 있으면 blind auto-link 금지 정책에 따라 OAuth login은 실패 redirect로 가야 한다.
- `SUSPENDED`, `DELETED` user와 연결된 OAuth account는 인증 실패 처리되어야 한다.
