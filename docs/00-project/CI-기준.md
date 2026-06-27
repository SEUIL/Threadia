# CI 기준 확정 v1.0

> **작성자**: Threadia 팀
> **작성일**: 2026-06-15
> **최종 수정**: 2026-06-27
> **버전**: 개발주간 DB정책 반영 v1
> **상태**: 확정
> **태그**: ci, backend, build
> **수정 근거**: Threadia-기획문서-검수리포트.md, DB-schema-관리-정책.md
> **기준 문서**: CI_검증_기준_회의전_설명, 4일차 회의 결정

---

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows다. DB schema는 `docs/03-design/ERD.md`와 `db/schema.sql`을 기준으로 관리하고, Spring Boot/JPA는 schema 생성이 아니라 mapping 검증에 사용한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.


## 1. 결정 사항

| 항목 | 결정 |
|---|---|
| CI 도입 시점 | Backend/Frontend skeleton 구조가 잡히고 FE-BE 연결 기준이 생긴 뒤 생성 |
| 초기 Backend 기준 | `./gradlew test` |
| 안정화 후 Backend 기준 | `./gradlew build` |
| Frontend 기준 | 초기에는 브라우저 route 직접 확인, npm script 확정 후 CI에 추가 |
| CI 실패 처리 | 실패하면 merge 전 원인 확인과 수정이 기본 |
| 현재 작업 | workflow 파일 생성은 보류, `main`/`dev` ruleset은 적용 |

---

## 2. Local 검증과 CI의 차이

| 구분 | 의미 | 실행 위치 | 목적 |
|---|---|---|---|
| local 검증 | 개발자가 자기 PC에서 test/build를 실행 | 개인 개발 환경 | PR 전 기본 오류 확인 |
| CI | PR/push 시 GitHub Actions가 test/build 실행 | GitHub 서버 | 환경 차이와 merge 전 품질 확인 |

---

## 3. Gradle 명령 기준

| 단계 | 명령 | 적용 기준 |
|---|---|---|
| 초기 | `./gradlew test` | Backend 프로젝트 생성 직후 PR 최소 검증 |
| 안정화 | `./gradlew build` | compile, test, packaging까지 merge 기준으로 확대 |
| 확장 | `./gradlew check` | lint/check 도구 도입 후 적용 |

---

## 4. PR 검증 기준

| 변경 유형 | 필수 검증 |
|---|---|
| Backend 기능/API | 관련 test 또는 직접 API 요청 확인 + `./gradlew test` |
| DB/ERD | `db/schema.sql` 영향, API/ERD 문서 정합성 확인 |
| Frontend 화면 | 브라우저에서 route와 주요 액션 확인 |
| 문서 | 링크, 버전, 관련 문서명 확인 |
| 인증/인가/보안 | 팀 승인 + test/build 또는 재현 가능한 수동 검증. email 정규화, OAuth link, refresh token revoke, ACTIVE 사용자 기준 포함 |

---

## 5. GitHub Actions 도입 후보

```yaml
name: backend-ci

on:
  pull_request:
  push:
    branches: [main, dev]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - run: ./gradlew test
```

위 YAML은 기준 예시다. 실제 파일 생성은 Spring Boot skeleton과 Gradle wrapper가 확정된 뒤 진행한다.

---


## 6. 6일차 v2 검증 추가 기준

- Auth 변경 PR은 email `trim + lowercase`, OAuth blind auto-link 금지, `password-reset/confirm` 후 `refresh_tokens` revoke, non-ACTIVE 인증 차단을 검증한다.
- DB 또는 invitation 변경 PR은 UTC 저장/응답 기준, seed 회사/`COMPANY_ADMIN` 검증 범위, `activity_events`와 `notification_events` 분리 기준을 확인한다.
- Sprint 1.5 invitation 변경은 `POST /api/v1/companies`, actual email sending, token link, 미가입 사용자 초대, resend, 필수 batch scheduler가 범위에 들어오지 않았는지 확인한다.

---

## 7. 5일차/6일차 이월

- Backend 프로젝트 구조와 Gradle wrapper 생성 시점.
- Frontend package manager와 npm script 확정.
- CI workflow 실제 생성 시점.
- `main`/`dev` ruleset은 적용됨. required status check는 workflow 생성 뒤 활성화.

---

## 8. 6일차 수정본 v2 공통 기준

1. Sprint 1.5 invitation 검증은 local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명을 사용한다. `POST /api/v1/companies`는 Sprint 1.5 구현 범위에 포함하지 않는다.
2. `activity_events`와 `notification_events`는 통합하지 않고 각각 활동 원장과 알림 사건 원본으로 별도 운영한다.
3. 서버 생성 시각과 만료 시각은 UTC 기준으로 생성, 저장, 비교하고 API 응답은 ISO-8601 UTC `Z` 형식으로 반환한다.
4. 로그인 이메일은 signup, login, password reset, OAuth 계정 연결, invitation lookup에서 모두 `trim + lowercase`로 정규화한다.
5. OAuth 계정은 이메일만 같다는 이유로 blind auto-link하지 않는다.
6. `password-reset/confirm` 성공 시 해당 사용자의 모든 기존 `refresh_tokens`를 revoke한다.
7. login, OAuth callback, token refresh, `/auth/me`는 `users.status = ACTIVE`인 사용자만 성공 처리한다.
