---
title: "CI 검증 기준 회의전 설명"
type: pre-meeting-draft
status: draft
created: 2026-06-14
updated: 2026-06-14
tags:
  - project
  - team-project
  - acorn
  - planning
  - ci
related:
  - "[[04_기획주간_4일차_협업하네스_문서운영]]"
context: "4일차 회의 전 local 검증과 CI 검증 기준을 설명하기 위한 자료"
---

# CI 검증 기준 회의전 설명

## 목적

CI를 아직 잘 모르는 팀원이 회의 전에 local 검증과 GitHub Actions PR check의 차이를 이해하도록 돕는다. 현재 repository에는 Gradle 프로젝트 파일과 CI workflow 파일이 아직 없으므로, 이 문서는 설정 파일이 아니라 기준을 정하기 위한 설명 자료다.

## 초안 내용

### local 검증과 CI의 차이

| 구분 | 의미 | 실행 위치 | 왜 필요한가 |
|---|---|---|---|
| local 검증 | 개발자가 자기 PC에서 직접 test/build를 실행하는 것 | 개인 개발 환경 | PR을 올리기 전에 기본 오류를 먼저 잡기 위해 필요 |
| CI | PR이나 push가 올라오면 GitHub Actions 같은 도구가 자동으로 test/build를 실행하는 것 | GitHub 서버 | 사람마다 다른 local 환경 차이를 줄이고 merge 전 최소 품질을 확인하기 위해 필요 |

### Gradle 명령 후보

| 명령 | 의미 | 4일차 기준 |
|---|---|---|
| `./gradlew test` | test만 실행 | 초기 PR 최소 검증 후보 |
| `./gradlew build` | compile, test, packaging까지 포함하는 build | 프로젝트가 안정화되면 merge 기준 후보 |
| `./gradlew check` | test 외 lint/check 도구까지 묶어 실행할 때 사용 | lint/check 도입 후 후보 |

### GitHub Actions PR check 후보

| 항목 | 설명 |
|---|---|
| 실행 시점 | PR 생성 또는 PR update 때 자동 실행 |
| 초기 기준 | Backend Gradle 프로젝트가 생긴 뒤 `./gradlew test` 실행 |
| 안정화 후 기준 | test 시간이 감당 가능하면 `./gradlew build`로 확대 |
| 실패 처리 | 빨간 체크가 뜨면 merge 전에 원인을 확인하고 수정 |

### 현재 repository 상태

| 항목 | 상태 |
|---|---|
| Gradle 프로젝트 파일 | 아직 없음 |
| GitHub Actions workflow 파일 | 아직 없음 |
| `.github/` 폴더 | 아직 없음 |
| 지금 만들 파일 | 없음. 회의 전 설명 Markdown만 작성 |

## 팀이 리뷰하고 결정할 것

| 결정 항목 | 선택지 | 회의 결과 |
|---|---|---|
| 초기 PR 검증 명령 | `./gradlew test` / `./gradlew build` / 프로젝트 생성 후 재논의 |  |
| CI 도입 시점 | Spring Boot 프로젝트 생성 직후 / 첫 Backend PR 전 / Sprint 1 중간 |  |
| CI 실패 시 merge 기준 | 실패하면 merge 금지 / 사유 기록 후 예외 허용 |  |
| Frontend 검증 기준 | 브라우저 확인만 우선 / 이후 npm 명령 추가 |  |
| 문서화 위치 | PR Template / CONTRIBUTING.md / 별도 docs 문서 |  |
