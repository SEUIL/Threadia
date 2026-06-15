---
title: "기획주간 5일차 Sprint 1 Backlog 검증 기준"
type: planning-note
status: draft
created: 2026-06-09
updated: 2026-06-15
tags:
  - project
  - team-project
  - acorn
  - planning
  - sprint
  - backlog
  - testing
aliases:
  - 5일차 Sprint 1 Backlog 검증 기준
  - Acorn 최종 프로젝트 5일차 기획
related:
  - "[[04_기획주간_4일차_협업하네스_문서운영]]"
  - "[[06_기획주간_6일차_발표정리_리스크점검]]"
  - "[[07_260609_Spring Boot 프로젝트에서 test-build-lint 기준 잡기]]"
context: "Acorn 최종 프로젝트 기획주간 5일차 Sprint 1 작업 목록, 담당 그룹, 완료 조건, 검증 기준, 의존성 정리"
---

# 기획주간 5일차 Sprint 1 Backlog 검증 기준

## 목적

5일차는 Week 2 개발 착수와 6일차 보고용 정리를 동시에 가능하게 만들기 위해 **Sprint 1 목표, Sprint 1 Backlog, 담당 그룹/역할 분담, 완료 조건, 검증 방법, 의존성/blocker**를 최대한 확정하는 날이다.

기획주간을 다시 6일 구조로 운영하므로, 5일차에서는 PPT 제작이나 보고 순서를 직접 작성하지 않는다. 해당 항목은 6일차 보고 정리와 리스크 점검에서 기획 내용 종합용으로 정리한다. 단, 6일차 보고가 흔들리지 않도록 5일차 종료 시점에는 개발 착수 기준 6개가 초안 수준이 아니라 팀 공유 가능한 상태여야 한다.

## 전회차 입력값

| 입력값 | 없을 때 처리 |
|---|---|
| PR 제출 기준 | 4일차 `PR Template v1.0.md` 확정. `.github/PULL_REQUEST_TEMPLATE.md` 생성 완료 |
| CI 검증 명령 | 4일차 `CI 기준 확정 v1.0.md` 확정. 실제 workflow 파일은 Backend/Frontend skeleton 이후 생성 |
| AGENTS.md / CLAUDE.md / CONTRIBUTING.md | root 규칙 문서와 `docs/AGENTS.md`, `docs/CLAUDE.md` 정리 완료 |
| AI 사용 규칙 | PR Template에는 AI 사용 여부 섹션을 두지 않음. 민감정보 금지와 사람 검토 책임만 기준으로 유지 |
| docs 구조 | `docs/`와 `docs/README.md` 기준으로 작업 결과 위치를 정함. `docs/00_INDEX.md`는 후속 결정 |
| 승인 필요 작업 | PR Template v1.0의 보안/승인 체크 기준으로 반영 완료 |
| 화면 정의서 최종본 / 화면-API 매핑표 | `화면정의서_v3.md`, `기능-시나리오-API-ERD 추적표 v1.0.md` 작성 완료 |
| AWS 사용 범위 판단표 | EC2/RDS/S3 사용 확정. 계정은 직접 생성하고 비용은 본인 부담. 실제 배포와 S3 파일 저장 구현은 Sprint 1 이후로 배치 |
| 보류 기능 최종표 | `보류 기능 최종표 v1.0.md` 작성 완료 |
| API/ERD 기술 검토 필요 사항 | `API 명세서 v2.0`, `ERD v2.0`, `보류 기능 최종표 v1.0`의 6일차 이월 목록으로 정리 |
| GitHub repository와 clone 확인 | repo 문서에는 팀원 clone 확인 결과가 아직 남아 있지 않음 |
| README 한 줄 설명 | root README는 아직 `# Threadia`만 있음 |

## 해야될 목록

| 번호 | 해야될 일 | 5일차 최종 산출물 | 완료 기준 | 권장 담당 그룹 |
|---:|---|---|---|---|
| 1 | Sprint 1 목표 확정 | Sprint 1 목표 문장 | 한 문장으로 Week 2 개발 방향을 설명할 수 있음 | 황스일 종합, 전체 확인 |
| 2 | Sprint 1 Backlog 확정 | Backlog 표 | 작업이 1~2일 단위로 쪼개지고 우선순위가 있음 | 3~4명 |
| 3 | 담당 그룹/역할 분담 확정 | 작업별 담당 그룹 표 | 각 backlog 항목에 1~4명 규모와 우선 담당이 있음 | 황스일 종합 |
| 4 | Definition of Done 작성 | 작업 유형별 완료 조건 | Backend/Frontend/DB/문서/PR 완료 기준이 검증 가능함 | 2명 |
| 5 | 검증 방법 작성 | test/build/API/화면 확인 기준 | 각 작업 유형별 실행 명령 또는 확인 방법이 있음 | 여도현 우선, 2명 |
| 6 | 의존성/blocker 정리 | dependency/blocker 표 | Week 2 시작 전 막히는 항목과 해결 담당이 보임 | 2명 |
| 7 | 6일차 보고 입력값 요약 | PPT 제작에 넘길 개발 착수 요약 | 목표/Backlog/DoD/검증/blocker가 6일차 문서에 바로 옮겨질 수 있음 | 황스일 종합 |
| 8 | 발표용 화면 설계서 최종본 작성 | 화면 설계서 최종본 또는 PPT 정리본 | `철진_화면설계서.pptx`를 기준으로 6일차 보고/PPT에 사용할 화면 흐름과 주요 화면이 정리됨 | 정철진 우선, 1~2명 |

### Sprint 1 목표 문장 예시

Sprint 1 목표 문장은 기능 목록을 길게 나열하지 않고, Week 2 개발 방향을 한 문장으로 고정하는 용도다.

```text
Sprint 1에서는 Spring Boot, React + Next, MySQL 기반을 구축하고 로그인, 프로젝트, 이슈의 기본 흐름을 local 환경에서 검증 가능한 상태로 만든다.
```

더 짧게 정리하면 아래처럼 쓸 수 있다.

```text
Sprint 1 목표는 개발 기반 구축과 핵심 흐름 착수다.
```

## 5일차 완료 목표

5일차 종료 시점에는 아래 6개 항목을 6일차 보고용 정리와 PPT 제작의 입력값으로 넘긴다.

| 산출물 | 최소 포함 내용 | 보고/PPT 연결 |
|---|---|---|
| Sprint 1 목표 | 기반 구축 중심 목표, Sprint 1에서 만들 핵심 흐름 | Week 2 방향 보고 |
| Sprint 1 Backlog | Backend, Frontend, DB, 협업/CI 작업 후보와 우선순위 | 개발 착수 준비 상태 요약 |
| 담당 그룹/역할 분담 | 작업별 1~4명 규모, 우선 담당, 검토자 | 팀 역할 정리 |
| Definition of Done | Backend/Frontend/DB/문서/PR 완료 기준 | 품질 기준 요약 |
| 검증 방법 | `./gradlew test`, API 요청, 브라우저 확인, 문서 검토 기준 | 검증 기준 요약 |
| 의존성/blocker | repo clone, Spring Boot, React + Next, MySQL, AWS, API/ERD 기술 결정 | 리스크 메모와 Week 2 확인 항목 입력값 |
| 발표용 화면 설계서 최종본 | `02_기획주간_2일차_자료/1차 분업 산출물/철진_화면설계서.pptx` 기준 주요 화면과 화면 흐름 | 6일차 보고/PPT의 화면 설명 입력값 |

## 회의에서 먼저 정해야 할 것

| 결정 항목 | 기준 | 결과물 |
|---|---|---|
| Frontend framework 기준 | React + Next 기준 구조 | Frontend 작업 blocker 제거 |
| MySQL 8.4 실행 기준 | 설치 가능성, 수업 환경, 배포 가능성 | DB 작업 기준 |
| Sprint 1 목표 | 기능 완성보다 기반 구축 중심 | Week 2 방향 |
| Sprint 1 범위 | 1주 안에 구현/검증 가능한 작업만 포함 | Backlog 후보 필터 |
| 발표용 화면 설계서 범위 | `철진_화면설계서.pptx`에서 6일차 보고/PPT에 넣을 주요 화면과 흐름 선정 | 화면 설계서 최종본 |
| 작업 크기 기준 | 1~4명 배정 가능 | 담당 그룹 표 |
| 검증 기준 | `./gradlew test` 우선, 화면/API는 직접 실행 | 완료 기준 |
| 의존성 기준 | 먼저 해야 하는 작업과 병렬 가능한 작업 구분 | blocker 목록 |

## 분업 가이드라인

| 작업 크기 | 인원 기준 | 적용 예시 |
|---|---:|---|
| 작음 | 1명 | 문서 초안, 자료 조사, 단일 화면 목록 정리 |
| 보통 | 2명 | API 목록, 검증 기준, 간단한 Backlog 정리 |
| 큼 | 3명 | Sprint 1 작업 도출, 화면 정의서 최종본, 기능-화면-API 매핑 |
| 중요/복합 | 3~4명 | 기술 스택 결정, 통합 의존성, 개발 착수 blocker 정리 |

정철진은 Frontend 구조 생성, 화면/라우팅, Frontend-API 연동 기준에 우선 배치한다. 여도현은 DB/API/검증 기준/의존성처럼 난도가 높은 작업에 우선 배치한다. 김민정, 김민경은 특정 업무에 고정하지 않고, 작업 크기에 따라 황스일이 배정한다.

AI coding tool을 사용하는 상황에서의 분업 기준은 [AI-assisted 분업 기준](./05_기획주간_5일차_자료/회의%20전%20자료/ai-assisted-분업-기준.md)을 회의 전 자료로 먼저 검토한다.

## 오전 회의 운영안

| 안건 | 결정할 내용 | 회의 결과 |
|---|---|---|
| 4일차 규칙 확인 | PR/CI/docs 기준과 AI 민감정보 금지 기준 | 완료 기준 입력값 확정 |
| 기술 스택 적용 기준 확인 | MySQL 8.4, React + Next. AWS는 EC2/RDS/S3 사용 확정, 실제 배포는 Sprint 1 이후 | Sprint 1 기준 확정 |
| Sprint 1 목표 합의 | 기반 구축 중심으로 제한 | Sprint 목표 확정 |
| 작업 묶음 배정 | 작업 크기와 인원수 결정 | 담당 그룹 확정 |
| 6일차 입력값 확인 | 보고용 정리에 넘길 요약 항목 | 6일차 준비 범위 확정 |

## 팀원 작업 시간 운영

### 작업 묶음 1. Sprint 1 Backlog 초안

| 작업 | 권장 담당 그룹 | 완료 조건 | 검증 방법 | 의존성 |
|---|---:|---|---|---|
| Spring Boot 기본 프로젝트 생성 | 1~2명 | local에서 서버 실행 가능 | `./gradlew bootRun` | repository, Java/Gradle 환경 |
| MySQL 8.4 연결 확인 | 2명 | test DB 연결 성공 | 연결 test 또는 간단 query | DB 실행 방식 결정 |
| 핵심 entity 초안 작성 | 2명 | User, Company, Project, Issue 중심 entity 초안 | test/build 통과 | ERD 초안 v1.4 |
| 로그인 API 초안 | 2명 | 로그인 요청/응답 흐름 구현 | Postman 또는 curl | User entity, JWT 기준 |
| 프로젝트/이슈 API 초안 | 2명 | 목록/상세/작성 기본 API | Postman 또는 curl | Project/Issue entity |
| Frontend 프로젝트 구조 생성 | 정철진 중심, 1~2명 | 라우팅, 기본 layout 구성 | 브라우저 실행 | framework 결정 |
| 로그인/프로젝트 홈 화면 초안 | 정철진 중심, 2명 | 로그인 화면과 프로젝트 홈 route 존재 | 브라우저 확인 | Frontend 구조 |
| GitHub Actions CI 도입 기준 확인 | 1~2명 | `./gradlew test` 우선, skeleton 이후 workflow 생성 기준 합의 | CI 기준 문서 확인 | CI 기준 확정 v1.0 |
| AGENTS.md/CLAUDE.md/CONTRIBUTING.md 최소 규칙 확정 | 1~2명 | root 문서와 docs 하위 규칙, commit 규칙 검토 완료 | 파일 존재와 링크 확인 | 4일차 협업 하네스 최신화 |
| PR Template 실제 생성 여부 결정 | 1명 | `.github/PULL_REQUEST_TEMPLATE.md` 생성 완료 | 파일 존재 확인 | PR Template v1.0 |

### 작업 묶음 2. Definition of Done

| 작업 유형 | 완료 기준 |
|---|---|
| Backend | 관련 API가 실행되고 `./gradlew test` 또는 `./gradlew build`가 통과 |
| Frontend | 브라우저에서 화면 또는 route 확인 가능 |
| DB | table/entity 관계가 문서와 코드에 반영되고 연결 확인 |
| Infra/AWS | EC2/RDS/S3 사용 범위, 계정/비용 책임, 배포 시점, 설정 값과 절차가 문서화되고 담당자가 재현 가능 |
| 문서 | 팀원이 문서만 보고 다음 행동을 할 수 있음 |
| PR | 작업 내용, 검증, 보안/승인 체크가 작성됨 |

### 작업 묶음 3. 검증 기준

| 단계 | 명령/확인 | 적용 |
|---|---|---|
| 로컬 실행 | `./gradlew bootRun` | 서버 실행 확인 |
| 최소 test | `./gradlew test` | PR 전 기본 검증 |
| build | `./gradlew build` | 안정화 후 merge 기준 |
| API 확인 | Postman 또는 curl | Backend 기능 검증 |
| 화면 확인 | 브라우저 실행 | Frontend 기능 검증 |
| CI | GitHub Actions 초록 체크 | PR 자동 검증 |

### 작업 묶음 4. 의존성/blocker 정리

아래 표는 최종 dependency/blocker 표가 아니라 **1차 확인 예시**다. 5일차 회의에서는 이 항목들을 확인한 뒤, 실제 Sprint 1 Backlog 기준으로 **영역별 blocker와 기능별 blocker**를 나눠 작성한다.

| 구분 | 확인할 내용 | 후속 처리 |
|---|---|---|
| Repository | 모든 팀원이 clone 가능한지 | 안 되는 팀원은 6일차 전 해결 |
| Backend | Java/Gradle/Spring Boot 실행 가능 여부 | Sprint 1 시작 조건에 반영 |
| DB | MySQL 8.4 local 실행 기준 | DB 담당자와 연결 test 진행 |
| Frontend | React + Next 구조 생성 가능 여부 | routing/layout 선행 작업 배정 |
| API | 화면 정의서 최종본, 화면-API 매핑, endpoint 초안 충돌 여부 | Backlog에서 담당 그룹 조정 |
| API/ERD 검토 | JWT 갱신, pagination, thread 자동 생성, unread count, activity/notification 통합 등 | Sprint 1 포함 전 담당 그룹과 결정 시점 지정 |
| AWS | EC2/RDS/S3 사용 범위와 배포 시점 | 계정은 직접 생성하고 비용은 본인 부담. 실제 배포와 S3 파일 저장 구현은 Sprint 1 이후로 배치하며, 배포/인프라 변경은 승인 필요 작업으로 표시 |

최종 산출물은 아래처럼 해결 담당과 결정 시점까지 포함한다.

| 작업/영역 | 의존성 | blocker | 영향 | 해결 담당 | 결정 시점 | 후속 처리 |
|---|---|---|---|---|---|---|
| Repository | GitHub repo, 팀원 초대 | clone 안 되는 팀원 발생 가능 | 개발 착수 지연 | 황스일 | 6일차 전 | 팀원별 clone 확인 |
| Backend | Java, Gradle, Spring Boot | Java version 불일치 가능 | 서버 실행 실패 | 황스일 + Backend 담당 | Sprint 1 시작 전 | 기준 Java version 정리 |
| DB | MySQL 8.4 local 실행 | 일부 팀원 DB 실행 불가 가능 | API 개발 지연 | 여도현 | 6일차 전 | local DB 실행 방식 통일 |
| Frontend | React + Next 구조 | routing/layout 기준 미정 | 화면 작업 충돌 | 정철진 | Sprint 1 시작 전 | 기본 구조 먼저 확정 |
| API | 화면-API 매핑 | 요청/응답 형식 충돌 가능 | Frontend/Backend 병렬 작업 지연 | API 담당 + Frontend 담당 | Backlog 확정 전 | endpoint 예시 공유 |
| API/ERD | JWT, pagination, thread, unread count | Sprint 1 포함 여부 미정 | 범위 과확장 | 여도현 + 황스일 | 5일차 종료 전 | 포함/보류 결정 |
| AWS | EC2/RDS/S3 사용 확정 | 실제 배포는 Sprint 1 이후 | Sprint 1 범위에서 제외 필요 | 황스일 | 확정됨 | 승인 필요 작업으로 표시 |

기능별 blocker도 함께 적어야 한다. 예시는 아래와 같다.

| 기능 | 의존성 | blocker 예시 | 해결 담당 |
|---|---|---|---|
| 로그인 API | User entity, JWT 기준 | access/refresh token 정책 미정 | 여도현 + 로그인 API 담당 |
| 프로젝트 API | Project entity, 권한 정책 | 프로젝트 생성/수정 권한 기준 충돌 가능 | API 담당 + 여도현 |
| 이슈 API | Issue entity, 칸반 상태값 | 담당자/우선순위/마감일 필드 기준 미정 | API 담당 + ERD 담당 |
| Frontend 로그인 화면 | React + Next 구조, 로그인 API | 로그인 API 응답 형식 미정 | 정철진 + Backend 담당 |
| 프로젝트 홈 화면 | 프로젝트 목록 API, 화면 정의서 | 화면에서 필요한 summary 데이터 범위 미정 | 정철진 + API 담당 |
| CI 적용 | Spring Boot skeleton, Gradle 명령 | 프로젝트 구조 생성 전이라 workflow 작성 불가 | 황스일 |

## 결과 공유 양식

```text
작업 묶음:
참여 인원:
Sprint 1 목표:
Backlog 항목:
담당 그룹/역할:
Definition of Done:
검증 방법:
의존성/blocker:
6일차 보고용 정리에 넘길 요약:
```

## 황스일 종합 항목

- Sprint 1 목표
- Sprint 1 Backlog
- 작업별 권장 담당 그룹 크기
- Definition of Done
- 검증 기준
- 의존성/blocker
- 6일차 보고용 정리에 넘길 요약

## 마무리 회의 운영안

| 안건 | 확인할 내용 | 결과 |
|---|---|---|
| Backlog 공유 | 작업이 1~2일 단위로 쪼개졌는지 | Sprint 1 Backlog 확정 |
| 담당 그룹 확인 | 1~4명 배정이 적절한지 | 배정 기준 확정 |
| 완료 조건 확인 | 말이 아니라 검증 가능한 기준인지 | Definition of Done 확정 |
| 의존성 확인 | Week 2 시작 blocker가 있는지 | blocker 목록 확정 |
| 6일차 입력값 확인 | 보고용 정리와 Week 2 진입 점검에 필요한 요약이 있는지 | 6일차 준비 완료 |

## 6일차로 넘길 입력값

- [ ] 최종 MVP 범위 후보: 4일차 기능 분해표/보류표/추적표 기준으로 요약
- [ ] Sprint 1 목표: 5일차 종료 전 한 문장으로 확정
- [ ] Sprint 1 Backlog: 우선순위와 담당 그룹 포함
- [ ] 작업별 권장 담당 그룹 크기: 1~4명 기준과 우선 담당 포함
- [ ] 완료 조건: Definition of Done으로 정리
- [ ] 검증 방법: Backend/Frontend/API/DB/문서 기준으로 정리
- [ ] 의존성/blocker 목록: 해결 담당과 결정 시점 포함
- [ ] 발표용 화면 설계서 최종본: `철진_화면설계서.pptx` 기준으로 6일차 보고/PPT에 사용할 주요 화면과 흐름 정리
- [ ] GitHub repository와 clone 확인 결과
- [ ] Spring Boot local 실행 기준
- [ ] React + Next 기본 구조 기준
- [ ] MySQL 8.4 local 실행 기준
- [x] AWS 사용 범위: EC2/RDS/S3 사용 확정, 실제 배포와 S3 파일 저장 구현은 Sprint 1 이후
- [x] AGENTS.md / CLAUDE.md / CONTRIBUTING.md 기준
- [x] PR Template과 CI 기준: 4일차 초안 리뷰 결과 반영
- [ ] 제출 문서 5종 진행 상태: 6일차에서 기획서/화면정의서/ERD/API 명세서/개발 명세서 상태표로 확정

## 5일차 종료 체크리스트

- [ ] Sprint 1 목표가 한 문장으로 정리되었다.
- [ ] Sprint 1 Backlog가 작업 단위, 우선순위, 담당 그룹을 포함한다.
- [ ] 담당 그룹/역할 분담이 1~4명 기준으로 정리되었다.
- [ ] Definition of Done이 작업 유형별로 작성되었다.
- [ ] 검증 방법이 명령 또는 직접 확인 방식으로 작성되었다.
- [ ] 의존성/blocker가 해결 담당과 결정 시점을 포함한다.
- [ ] 6일차 보고용 정리에 넘길 요약이 작성되었다.
