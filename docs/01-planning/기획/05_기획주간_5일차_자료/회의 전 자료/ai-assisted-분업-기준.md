> **작성자**: 황스일  
> **작성일**: 2026-06-15  
> **최종 수정**: 2026-06-15  
> **버전**: v1.0  
> **상태**: 검토 중

# AI-assisted 분업 기준

## 목적

Sprint 1 Backlog를 작성한 뒤, 모든 팀원이 Codex, Claude Code, OpenCode 같은 AI coding tool을 사용하는 상황에서 작업을 어떻게 나눌지 정리한다.

이 문서는 AI 도구 성능이 아니라 **기능 owner, 변경 파일 범위, 검증 책임, reviewer**를 기준으로 분업하기 위한 회의 전 자료다.

## 기본 원칙

| 원칙 | 설명 |
|---|---|
| AI 도구가 아니라 작업 단위로 나눈다 | Codex 사용자는 작은 일, agent 사용자는 큰 일처럼 나누지 않는다. |
| 한 작업에는 owner를 둔다 | AI가 만든 결과물도 owner가 이해하고 설명할 수 있어야 한다. |
| 같은 파일을 여러 명이 동시에 수정하지 않는다 | `build.gradle`, `package.json`, routing, entity, migration, CI 파일은 충돌 가능성이 높다. |
| PR에는 한 가지 논리 변경만 담는다 | 작은 PR이 review와 충돌 해결에 유리하다. |
| reviewer는 owner와 분리한다 | 작성자가 놓친 요구사항, 보안, 검증 누락을 다른 사람이 확인한다. |
| 민감 변경은 팀 승인 후 진행한다 | DB schema, 인증/인가, 보안, CI, AWS, secret 관련 변경은 AI 제안만으로 진행하지 않는다. |

## 분업 기준

| 기준 | 의미 | 적용 예시 |
|---|---|---|
| 기능 owner | 해당 기능의 최종 책임자 | 로그인 API owner, 프로젝트 API owner |
| 변경 파일 범위 | 같은 파일을 여러 명이 동시에 수정하지 않게 제한 | `auth/*`, `projects/*`, `frontend/login/*` |
| 검증 책임 | AI가 작성했어도 사람이 직접 실행 확인 | `./gradlew test`, curl, 브라우저 확인 |
| reviewer | owner와 다른 사람이 PR 확인 | Backend 작업은 DB/API 이해자가 검토 |
| 승인 필요 작업 | 영향 큰 변경은 팀 확인 후 진행 | DB schema, 인증, CI, AWS, secret |

## Sprint 1 분업 예시

| Backlog | Owner | Reviewer | AI 사용 방식 |
|---|---|---|---|
| Spring Boot 기본 프로젝트 생성 | 황스일 | 여도현 | 구조 생성 보조, Gradle 설정 확인 |
| MySQL 8.4 연결 확인 | 여도현 | 황스일 | 에러 분석, 설정 예시 확인 |
| 핵심 entity 초안 | 여도현 | 김민정 | ERD 기준 entity 초안 작성 보조 |
| 로그인 API 초안 | 김민정 | 여도현 | Controller, DTO, test 후보 생성 |
| 프로젝트/이슈 API 초안 | 김민경 | 여도현 | endpoint 흐름, curl 예시 작성 |
| Frontend 프로젝트 구조 생성 | 정철진 | 황스일 | route, layout 초안 생성 |
| 로그인/프로젝트 홈 화면 | 정철진 + 1명 | 김민정 | 화면 초안, API 연결 후보 정리 |
| CI/PR 기준 적용 | 황스일 | 전체 | workflow 초안, PR checklist 보조 |

## 작업 배정 절차

1. Sprint 1 Backlog를 먼저 기능 단위로 쪼갠다.
2. 각 Backlog마다 owner 1명을 정한다.
3. owner가 수정할 주요 파일 범위를 적는다.
4. owner와 다른 reviewer 1명을 정한다.
5. 검증 방법을 같이 적는다.
6. DB, 인증, CI, AWS, secret 변경이 있으면 승인 필요 작업으로 표시한다.

## 회의에서 확인할 질문

| 질문 | 확인 이유 |
|---|---|
| 이 작업의 owner가 누구인가? | 최종 책임자를 분명히 하기 위해 |
| 이 작업이 건드릴 파일 범위는 어디인가? | 같은 파일 동시 수정과 merge conflict를 줄이기 위해 |
| reviewer는 누구인가? | AI 결과물의 사람 검토 책임을 분리하기 위해 |
| 어떤 명령이나 화면으로 검증할 것인가? | AI가 만든 코드도 실제 동작 확인이 필요하기 때문에 |
| 승인 필요 작업인가? | DB, 인증, CI, AWS, secret 변경을 안전하게 관리하기 위해 |

## 5일차 문서에 반영할 요약 문장

```text
Sprint 1 분업은 AI 도구 종류가 아니라 기능 owner, 변경 파일 범위, 검증 책임을 기준으로 나눈다. 각 작업은 owner 1명과 reviewer 1명을 두고, AI가 만든 결과물도 owner가 직접 이해하고 실행 검증한 뒤 PR로 공유한다.
```
