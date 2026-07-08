> **작성자**: 여도현
> **작성일**: 2026-07-03
> **최종 수정**: 2026-07-07
> **버전**: v1.1
> **상태**: 검토 중
> **태그**: auth, logout, cors, refresh-token, security

# Auth common logout/CORS 변경점 정리

이 문서는 `feature/auth-common` 최신화 후 추가 확인한 backend 변경점과 후속 주의점을 정리한다. 중간의 `c9ce9de Merge branch 'dev' into feature/auth-common`는 frontend 개발 내역이 포함된 dev merge이므로 이 문서의 분석 범위에서 제외한다.

## 1. 확인한 commit

| Commit | 내용 | 판단 |
|---|---|---|
| `200b796` | `POST /api/v1/auth/logout` 구현 | backend Auth flow 변경 |
| `1c389ea` | 이미 revoke된 refresh token 재로그아웃 401 처리 | logout 보안 보강 |
| `27ba01f` | frontend 연동용 CORS 정책 추가 | cookie 기반 frontend 연동 보강 |
| `88411ce` | `CorsConfig` 회귀 방지 테스트 추가 | CORS 자동 검증 보강 |

## 2. logout 변경점

`POST /api/v1/auth/logout`가 추가됐다.

요청 조건은 다음과 같다.

| 입력 | 위치 | 용도 |
|---|---|---|
| access token | `Authorization: Bearer <accessToken>` | 요청 user 식별과 token 유효성 검증 |
| refresh token | `refreshToken` cookie | revoke 대상 refresh token 식별 |

처리 흐름은 다음과 같다.

```text
AuthController.logout()
→ Authorization header에서 access token 추출
→ AuthService.logout(accessToken, refreshToken)
→ JwtTokenService.parseAccessToken(accessToken)
→ refresh token raw 값을 hash
→ refresh_tokens.token_hash 조회
→ token.user.id == accessToken.userId 검증
→ revoked_at == null 검증
→ RefreshToken.revoke(nowUtc())
→ Set-Cookie: refreshToken=; Max-Age=0
→ { success: true }
```

성공 시 refresh token row의 `revoked_at`, `updated_at`이 같은 시각으로 갱신된다. cookie는 동일한 이름과 path로 `Max-Age=0`을 내려 browser 저장값을 삭제한다.

## 3. revoke 재요청 차단

`1c389ea`에서 이미 revoke된 refresh token을 다시 logout에 쓰면 401로 거부하도록 보강했다.

핵심 조건은 다음과 같다.

```java
.filter(token -> token.getRevokedAt() == null)
```

이 변경의 의미는 다음과 같다.

| 항목 | 효과 |
|---|---|
| 감사 로그 보존 | 최초 logout 시각인 `revoked_at`을 재요청 시각으로 덮어쓰지 않음 |
| token 재사용 차단 | revoke된 refresh token을 유효 token처럼 다시 쓰지 못하게 함 |
| refresh endpoint 대비 | 이후 refresh 구현이 같은 조회 패턴을 재사용할 때 revoke token 재사용 위험을 줄임 |

테스트도 추가됐다.

- 정상 logout 시 revoke + clear cookie 반환
- refresh cookie 누락 시 401
- DB에 없는 refresh token 시 401
- 다른 user의 refresh token 시 401
- 이미 revoke된 refresh token 재사용 시 401, `revoked_at` 보존, `save()` 미호출

## 4. CORS 변경점

`CorsConfig`가 추가되어 `/api/**` 경로에 frontend 연동용 CORS 정책을 적용한다.

현재 기본 정책은 다음과 같다.

| 항목 | 값 |
|---|---|
| mapping | `/api/**` |
| 기본 허용 origin | `http://localhost:3000` |
| 설정 변수 | `THREADIA_CORS_ALLOWED_ORIGIN` |
| credentials | `true` |
| methods | `GET`, `POST`, `PATCH`, `DELETE`, `OPTIONS` |
| headers | `*` |

이 설정은 frontend가 `axios` 또는 `fetch`에서 credential 포함 요청을 보낼 때 필요하다. refresh token이 HttpOnly cookie로 전달되므로 browser 호출에서는 `Access-Control-Allow-Credentials: true`와 명시적 origin 허용이 필요하다.

`88411ce`에서는 CORS 회귀 방지 테스트가 추가됐다.

| 테스트 | 기대 결과 |
|---|---|
| 허용 origin preflight | 200, `Access-Control-Allow-Origin`, `Access-Control-Allow-Credentials` 존재 |
| 미허용 origin preflight | 403 |
| 허용 origin 실제 요청 | 응답에 CORS 허용 header 존재 |
| 미허용 origin 실제 요청 | CORS 허용 header 없음 |

## 5. 개인 범위에서 바로 처리한 항목

아래 항목은 정책 결정 없이 개인 작업 범위에서 바로 정리했다.

| 항목 | 처리 |
|---|---|
| `AuthController` stale 주석 | `signup/login/logout` 기준으로 수정 |
| `AuthController` logout 설명 | access token + refresh cookie 검증, revoke, clear cookie 흐름으로 갱신 |
| `CorsConfigTest` class 선언 formatting | `class CorsConfigTest`로 정리 |
| `BackendApplicationTests` class 선언 formatting | `class BackendApplicationTests`로 정리 |
| 불필요한 끝 빈 줄 | 제거 |
| backend 구조 문서 최신화 | logout/CORS 구현 상태와 남은 refresh 후속 과제 반영 |
| 개인 OAuth 보안 gap 문서 최신화 | logout 완료, 이후 최신 기준에서 refresh 구현 완료와 CSRF/Origin 후속 과제 분리 |

## 6. 남은 주의점

### 6.1 개인이 후속 처리해도 되는 항목

| 항목 | 이유 | 권장 처리 |
|---|---|---|
| CORS 테스트 formatting commit | 이미 수정한 formatting을 별도 `fix/test` commit으로 정리 | commit 전 테스트 결과 포함 |

### 6.2 팀 또는 정책 결정이 필요한 항목

| 항목 | 왜 개인 단독 처리하지 않는가 | 문서화 기준 |
|---|---|---|
| `/api/v1/auth/refresh` 구현 | 최신 `dev` 기준 구현 완료 | stale 항목으로 보정 |
| Google OAuth access token handoff | URL 노출 금지와 frontend memory 저장 정책이 충돌하지 않게 설계 필요 | `/refresh` 기반 복구안 권장 |
| logout 시 expired access token 허용 여부 | 현재 logout은 valid access token과 refresh cookie를 모두 요구함. UX/보안 trade-off가 있음 | 정책 결정 필요 |
| CSRF/Origin 방어 | CORS는 browser 허용 정책이고 CSRF 방어 자체는 아님 | cookie 기반 `logout/refresh` endpoint 추가 정책으로 정리 |
| CORS multiple origin 지원 | 운영, preview, local이 동시에 필요하면 단일 origin 설정으로 부족할 수 있음 | 배포 환경 수에 따라 list parsing 여부 결정 |
| SecurityFilterChain endpoint별 제한 | 현재 `permitAll` 구조가 남아 있어 API 증가 시 실수 위험이 있음 | protected/public endpoint 기준 확정 필요 |

## 7. 현재 판단

logout과 CORS는 Sprint 1 Auth common 흐름에 필요한 기반으로 들어왔다. 최신 `dev` 기준 `/refresh`와 OAuth access token 복구 경로도 구현됐다. 다만 production-ready 인증으로 보기에는 아직 CSRF/Origin 방어, endpoint별 security policy, refresh token reuse detection, rate limiting이 남아 있다.

개인 범위에서는 stale 주석과 formatting 정리, 관련 문서 최신화까지 처리할 수 있다. refresh, CSRF, OAuth handoff, logout 만료 token 정책은 팀 합의 후 구현하는 것이 안전하다.
