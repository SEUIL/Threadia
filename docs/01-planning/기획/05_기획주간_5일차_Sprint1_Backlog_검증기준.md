---
title: "기획주간 5일차 Sprint 1 Backlog 검증 기준"
type: planning-note
status: draft
created: 2026-06-09
updated: 2026-06-12
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

5일차는 Week 2 개발 착수를 위해 **Sprint 1 Backlog, 담당 그룹, 완료 조건, 검증 방법, 의존성/blocker**를 확정하는 날이다.

기획주간을 다시 6일 구조로 운영하므로, 5일차에서는 발표 목차/시연 흐름/리스크 대응표를 확정하지 않는다. 해당 항목은 6일차 발표 정리와 리스크 점검에서 최종화한다.

## 전회차 입력값

| 입력값 | 없을 때 처리 |
|---|---|
| PR 제출 기준 | Sprint 작업의 완료 조건을 정할 수 없음 |
| CI 검증 명령 | merge 기준이 불명확함 |
| AGENTS.md / CLAUDE.md / CONTRIBUTING.md | 완료. commit message와 AI GitHub 표시 원칙은 이 기준을 사용 |
| AI 사용 규칙 | 기본 원칙은 완료. PR Template의 AI 사용 범위 체크는 후속 작업 |
| docs 구조 | 작업 결과를 어디에 남길지 모름 |
| 승인 필요 작업 | DB/배포/인증 변경 충돌 가능성 증가 |
| 화면-API 매핑표 | Frontend/Backend 작업 경계가 흐려짐 |
| AWS 사용 범위 판단표 | 배포/파일 저장 작업을 Sprint 1에 넣을지 판단하기 어려움 |
| 보류 기능 최종표 | Sprint 1에 보류 기능이 섞일 수 있음 |
| GitHub repository와 clone 확인 | 작업 배정 전에 팀원 local 환경 확인 필요 |
| README 한 줄 설명 | 프로젝트 설명/발표 초안의 기준 문장으로 사용 |

## 해야될 목록

| 번호 | 해야될 일 | 산출물 | 권장 담당 그룹 |
|---:|---|---|---|
| 1 | 확정 기술 스택 적용 항목 정리 | MySQL 8.4, React + Next, AWS EC2/S3 적용 계획 | 2~3명 |
| 2 | Sprint 1 목표 확정 | Sprint 1 목표 문장 | 황스일 종합, 전체 확인 |
| 3 | Sprint 1 작업 후보 도출 | Backlog 후보 | 3~4명 |
| 4 | 작업별 담당 그룹 크기 결정 | 1~4명 배정 기준 | 황스일 종합 |
| 5 | 완료 조건 작성 | Definition of Done | 2명 |
| 6 | 검증 방법 작성 | test/build/API/화면 확인 기준 | 여도현 우선, 2명 |
| 7 | 의존성 정리 | blocker/dependency 표 | 2명 |
| 8 | 6일차로 넘길 발표/리스크 입력값 정리 | 발표에 반영할 Backlog/검증/의존성 요약 | 황스일 종합 |

## 회의에서 먼저 정해야 할 것

| 결정 항목 | 기준 | 결과물 |
|---|---|---|
| Frontend framework 기준 | React + Next 기준 구조 | Frontend 작업 blocker 제거 |
| MySQL 8.4 실행 기준 | 설치 가능성, 수업 환경, 배포 가능성 | DB 작업 기준 |
| Sprint 1 목표 | 기능 완성보다 기반 구축 중심 | Week 2 방향 |
| Sprint 1 범위 | 1주 안에 구현/검증 가능한 작업만 포함 | Backlog 후보 필터 |
| 작업 크기 기준 | 1~4명 배정 가능 | 담당 그룹 표 |
| 검증 기준 | `./gradlew test` 우선, 화면/API는 직접 실행 | 완료 기준 |
| 의존성 기준 | 먼저 해야 하는 작업과 병렬 가능한 작업 구분 | blocker 목록 |

## 분업 가이드라인

| 작업 크기 | 인원 기준 | 적용 예시 |
|---|---:|---|
| 작음 | 1명 | 문서 초안, 자료 조사, 단일 화면 목록 정리 |
| 보통 | 2명 | API 목록, 검증 기준, 간단한 Backlog 정리 |
| 큼 | 3명 | Sprint 1 작업 도출, 기능-화면-API 매핑 |
| 중요/복합 | 3~4명 | 기술 스택 결정, 통합 의존성, 개발 착수 blocker 정리 |

정철진은 Frontend 구조 생성, 화면/라우팅, Frontend-API 연동 기준에 우선 배치한다. 여도현은 DB/API/검증 기준/의존성처럼 난도가 높은 작업에 우선 배치한다. 김민정, 김민경은 특정 업무에 고정하지 않고, 작업 크기에 따라 황스일이 배정한다.

## 오전 회의 운영안

| 안건 | 결정할 내용 | 회의 결과 |
|---|---|---|
| 4일차 규칙 확인 | PR/CI/AI/docs 기준 | 완료 기준 입력값 확정 |
| 기술 스택 적용 기준 확인 | MySQL 8.4, React + Next, AWS EC2/S3 | Sprint 1 기준 확정 |
| Sprint 1 목표 합의 | 기반 구축 중심으로 제한 | Sprint 목표 확정 |
| 작업 묶음 배정 | 작업 크기와 인원수 결정 | 담당 그룹 확정 |
| 6일차 입력값 확인 | 발표/리스크에 넘길 요약 항목 | 6일차 준비 범위 확정 |

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
| GitHub Actions CI 초안 | 1~2명 | PR에서 test 또는 build 실행 | CI 초록 체크 | Spring Boot 프로젝트 |
| AGENTS.md/CLAUDE.md/CONTRIBUTING.md 반영 | 완료 | root 문서와 commit 규칙 존재 | 파일 존재와 링크 확인 | 4일차 규칙 |
| PR Template 반영 | 1명 | PR 양식 존재 | 첫 PR에서 확인 | 4일차 규칙 |

### 작업 묶음 2. Definition of Done

| 작업 유형 | 완료 기준 |
|---|---|
| Backend | 관련 API가 실행되고 `./gradlew test` 또는 `./gradlew build`가 통과 |
| Frontend | 브라우저에서 화면 또는 route 확인 가능 |
| DB | table/entity 관계가 문서와 코드에 반영되고 연결 확인 |
| Infra/AWS | 설정 값과 절차가 문서화되고 담당자가 재현 가능 |
| 문서 | 팀원이 문서만 보고 다음 행동을 할 수 있음 |
| PR | 작업 내용, 검증, AI 사용 여부, 보안 체크가 작성됨 |

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

| 구분 | 확인할 내용 | 후속 처리 |
|---|---|---|
| Repository | 모든 팀원이 clone 가능한지 | 안 되는 팀원은 6일차 전 해결 |
| Backend | Java/Gradle/Spring Boot 실행 가능 여부 | Sprint 1 시작 조건에 반영 |
| DB | MySQL 8.4 local 실행 기준 | DB 담당자와 연결 test 진행 |
| Frontend | React + Next 구조 생성 가능 여부 | routing/layout 선행 작업 배정 |
| API | 화면-API 매핑과 endpoint 초안 충돌 여부 | Backlog에서 담당 그룹 조정 |
| AWS | EC2/RDS/S3 중 Sprint 1 포함 여부 | 포함 시 승인 필요 작업으로 표시 |

## 결과 공유 양식

```text
작업 묶음:
참여 인원:
Backlog 항목:
완료 조건:
검증 방법:
의존성/blocker:
6일차 발표/리스크 정리에 넘길 내용:
```

## 황스일 종합 항목

- Sprint 1 목표
- Sprint 1 Backlog
- 작업별 권장 담당 그룹 크기
- Definition of Done
- 검증 기준
- 의존성/blocker
- 6일차 발표/리스크 정리에 넘길 요약

## 마무리 회의 운영안

| 안건 | 확인할 내용 | 결과 |
|---|---|---|
| Backlog 공유 | 작업이 1~2일 단위로 쪼개졌는지 | Sprint 1 Backlog 확정 |
| 담당 그룹 확인 | 1~4명 배정이 적절한지 | 배정 기준 확정 |
| 완료 조건 확인 | 말이 아니라 검증 가능한 기준인지 | Definition of Done 확정 |
| 의존성 확인 | Week 2 시작 blocker가 있는지 | blocker 목록 확정 |
| 6일차 입력값 확인 | 발표/리스크/Week 2 진입 점검에 필요한 요약이 있는지 | 6일차 준비 완료 |

## 6일차로 넘길 입력값

- [ ] 최종 MVP 범위 후보
- [ ] Sprint 1 목표
- [ ] Sprint 1 Backlog
- [ ] 작업별 권장 담당 그룹 크기
- [ ] 완료 조건
- [ ] 검증 방법
- [ ] 의존성/blocker 목록
- [ ] GitHub repository와 clone 확인 결과
- [ ] Spring Boot local 실행 기준
- [ ] React + Next 기본 구조 기준
- [ ] MySQL 8.4 local 실행 기준
- [x] AGENTS.md / CLAUDE.md / CONTRIBUTING.md 기준
- [ ] PR Template과 CI 기준
- [ ] 제출 문서 5종 진행 상태
