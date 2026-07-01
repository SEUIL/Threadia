> **작성자**: 황스일
> **작성일**: 2026-07-01
> **최종 수정**: 2026-07-01
> **버전**: v1.0
> **상태**: 검토 중
> **태그**: oauth, google, auth, sprint-1, secret-management

# Google OAuth 앱 등록 작업 기록

이 문서는 Sprint 1 WBS `1.1.6 Google OAuth 앱 등록` 작업의 진행 결과와 후속 구현자가 확인해야 할 설정 기준을 정리한다.

## 1. 작업 목적

Sprint 1의 Google OAuth 로그인 구현을 위해 Google Cloud Console에서 OAuth 동의 화면과 Web application OAuth Client를 준비한다.

| 항목 | 내용 |
|---|---|
| WBS ID | `1.1.6` |
| 작업명 | Google OAuth 앱 등록 |
| Owner | 황스일 |
| Reviewer | 여도현 |
| 지원 | 김민정 |
| 관련 기능 | `AUTH-03` 소셜 로그인 |
| 후속 구현 | `GET /oauth2/authorization/google`, `GET /api/v1/auth/oauth2/callback/google` |

## 2. 등록 기준

Google OAuth Client는 local backend callback 기준으로 생성했다.

| 항목 | 값 |
|---|---|
| OAuth Client type | Web application |
| Redirect URI | `http://localhost:8080/api/v1/auth/oauth2/callback/google` |
| Scope | `openid`, `email`, `profile` |
| Secret 기록 | 저장소, Sheet, PR, 문서에 기록하지 않음 |

Redirect URI는 Google Cloud Console에 등록한 값과 backend 구현에서 사용하는 값이 정확히 일치해야 한다. 공백, 줄바꿈, trailing slash가 있으면 `redirect_uri_mismatch`가 발생할 수 있다.

## 3. 완료한 작업

- Google Cloud Console에서 Threadia용 OAuth 동의 화면 설정을 진행했다.
- OAuth Client를 `Web application` 유형으로 생성했다.
- Redirect URI를 `http://localhost:8080/api/v1/auth/oauth2/callback/google`로 등록했다.
- Google Cloud Console에서 내려받은 OAuth client JSON 파일을 저장소 외부 개인 보관 경로에 저장했다.
- 로컬 실행을 위해 저장소 root에 `.env` 파일을 생성했다.
- `.env`가 root `.gitignore`의 `.env`, `.env.*` 규칙으로 Git 추적 대상에서 제외되는 것을 확인했다.

## 4. 로컬 환경 변수 기준

로컬 실행 시 필요한 환경 변수는 다음 이름을 사용한다.

```env
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=http://localhost:8080/api/v1/auth/oauth2/callback/google
```

실제 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` 값은 이 문서에 기록하지 않는다. 구현 담당자에게는 비공개 채널로만 전달한다.

## 5. 구현 담당자 확인 사항

Backend OAuth 구현 시 아래 기준을 맞춘다.

| 항목 | 기준 |
|---|---|
| OAuth 시작 endpoint | `GET /oauth2/authorization/google` |
| OAuth callback endpoint | `GET /api/v1/auth/oauth2/callback/google` |
| Provider | Google |
| Email 처리 | `trim + lowercase` 정규화 기준 유지 |
| Account linking | 이메일만 같다는 이유로 기존 계정에 blind auto-link하지 않음 |
| 인증 허용 사용자 | `users.status = ACTIVE` 기준 |

`application.properties` 또는 profile 설정에는 secret 원문을 직접 적지 않는다. 환경 변수 또는 local-only 설정 파일을 통해 주입한다.

## 6. 보안 기준

- `GOOGLE_CLIENT_SECRET`은 GitHub, Sheet, 문서, PR 본문, issue comment, 로그에 기록하지 않는다.
- OAuth client JSON 파일은 저장소 안에 두지 않는다.
- `.env` 파일은 commit하지 않는다.
- 팀 공유 시 secret 값은 비공개 채널로 구현 담당자에게만 전달한다.
- 향후 배포 환경에서는 local callback과 별도로 dev/prod callback URI를 분리 등록한다.

## 7. WBS 반영 메모

WBS `1.1.6 Google OAuth 앱 등록`은 Console 설정과 local secret 준비가 완료되었으므로 진척률 `100%`로 변경 가능하다. 단, OAuth API 구현 자체는 WBS `1.3.2 소셜 로그인 API 구현` 범위로 분리한다.
