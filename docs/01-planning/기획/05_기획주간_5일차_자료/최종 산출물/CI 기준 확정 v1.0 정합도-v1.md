# CI 기준 확정 v1.0

> **작성자**: Threadia 팀  
> **작성일**: 2026-06-15  
> **최종 수정**: 2026-06-15  
> **버전**: v1.0  
> **상태**: 확정  
> **기준 문서**: CI_검증_기준_회의전_설명, 4일차 회의 결정

---

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows이며, Sprint 1 DB migration은 `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens`만 포함한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.


## 1. 결정 사항

| 항목 | 결정 |
|---|---|
| CI 도입 시점 | Backend/Frontend skeleton 구조가 잡히고 FE-BE 연결 기준이 생긴 뒤 생성 |
| 초기 Backend 기준 | `./gradlew test` |
| 안정화 후 Backend 기준 | `./gradlew build` |
| Frontend 기준 | 초기에는 브라우저 route 직접 확인, npm script 확정 후 CI에 추가 |
| CI 실패 처리 | 실패하면 merge 전 원인 확인과 수정이 기본 |
| 현재 작업 | workflow 파일 생성은 보류, 기준 문서만 확정 |

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
| DB/ERD | migration 영향, API/ERD 문서 정합성 확인 |
| Frontend 화면 | 브라우저에서 route와 주요 액션 확인 |
| 문서 | 링크, 버전, 관련 문서명 확인 |
| 인증/인가/보안 | 팀 승인 + test/build 또는 재현 가능한 수동 검증 |

---

## 5. GitHub Actions 도입 후보

```yaml
name: backend-ci

on:
  pull_request:
  push:
    branches: [main, develop]

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

## 6. 5일차/6일차 이월

- Backend 프로젝트 구조와 Gradle wrapper 생성 시점.
- Frontend package manager와 npm script 확정.
- CI workflow 실제 생성 시점.
- merge protection 적용 여부.
