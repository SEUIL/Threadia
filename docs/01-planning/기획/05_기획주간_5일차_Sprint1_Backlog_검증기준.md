---
title: "기획주간 5일차 Sprint 1 Backlog 검증 발표 리스크 정리"
type: planning-note
status: draft
created: 2026-06-09
updated: 2026-06-10
tags:
  - project
  - team-project
  - acorn
  - planning
  - sprint
  - backlog
  - testing
  - presentation
  - risk
aliases:
  - 5일차 Sprint 1 Backlog 검증 기준
  - Acorn 최종 프로젝트 5일차 기획
related:
  - "[[04_기획주간_4일차_협업하네스_문서운영]]"
  - "[[07_260609_Spring Boot 프로젝트에서 test-build-lint 기준 잡기]]"
context: "Acorn 최종 프로젝트 기획주간 5일차 Sprint 1 작업 목록, 검증 기준, 발표 구조, 리스크 점검, Week 2 진입 조건 확정"
---

# 기획주간 5일차 Sprint 1 Backlog 검증 발표 리스크 정리

## 목적

5일차는 Week 2 개발 착수를 위해 **Sprint 1 Backlog, 담당 그룹, 완료 조건, 검증 방법, 의존성, 발표 구조, 리스크 대응**을 확정하는 날이다. 기존 6일차 발표 정리와 리스크 점검은 5일차 후반으로 흡수한다.

## 전회차 입력값

| 입력값 | 없을 때 처리 |
|---|---|
| PR 제출 기준 | Sprint 작업의 완료 조건을 정할 수 없음 |
| CI 검증 명령 | merge 기준이 불명확함 |
| AI 사용 규칙 | AI 사용 작업의 검증 책임이 불명확함 |
| docs 구조 | 작업 결과를 어디에 남길지 모름 |
| 승인 필요 작업 | DB/배포/인증 변경 충돌 가능성 증가 |
| 2일차 repository | repo URL과 clone 가능 여부를 먼저 확인 |

## 해야될 목록

| 번호 | 해야될 일 | 산출물 | 권장 담당 그룹 |
|---:|---|---|---|
| 1 | 확정 기술 스택 적용 항목 정리 | MySQL 8.4, React + Next, AWS EC2/S3 적용 계획 | 2~3명 |
| 2 | Sprint 1 작업 후보 도출 | Backlog 후보 | 3~4명 |
| 3 | 작업별 담당 그룹 크기 결정 | 1~4명 배정 기준 | 황스일 종합 |
| 4 | 완료 조건 작성 | Definition of Done | 2명 |
| 5 | 검증 방법 작성 | test/build/API/화면 확인 기준 | 여도현 우선, 2명 |
| 6 | 의존성 정리 | blocker/dependency 표 | 2명 |
| 7 | 발표 목차와 시연 흐름 정리 | 발표 목차, 3~5분 시연 흐름 | 정철진 우선, 2~3명 |
| 8 | 리스크와 Week 2 진입 조건 점검 | 리스크 대응표, 진입 조건 checklist | 여도현 우선, 2명 |
| 9 | 제출 문서 5종 진행 상태 점검 | 기획서, 화면정의서, ERD, API 명세서, 개발 명세서 checklist | 황스일 종합, 2명 검토 |

## 회의에서 먼저 정해야 할 것

| 결정 항목 | 기준 | 결과물 |
|---|---|---|
| Frontend framework 기준 | React + Next 기준 구조 | Frontend 작업 blocker 제거 |
| MySQL 8.4 실행 기준 | 설치 가능성, 수업 환경, 배포 가능성 | DB 작업 기준 |
| Sprint 1 목표 | 기능 완성보다 기반 구축 중심 | Week 2 방향 |
| 작업 크기 기준 | 1~4명 배정 가능 | 담당 그룹 표 |
| 검증 기준 | `./gradlew test` 우선 | 완료 기준 |
| 발표 구조 | 배경→목표→기능→설계→협업→일정→리스크 | 발표 초안 |
| Week 2 시작 조건 | Backlog, 담당 그룹, 검증 기준, blocker 대응 | 개발 착수 판단 |

## 분업 가이드라인

| 작업 크기 | 인원 기준 | 적용 예시 |
|---|---:|---|
| 작음 | 1명 | 문서 초안, 자료 조사, 단일 화면 목록 정리 |
| 보통 | 2명 | API 목록, 검증 기준, 간단한 Backlog 정리 |
| 큼 | 3명 | Sprint 1 작업 도출, 기능-화면-API 매핑 |
| 중요/복합 | 3~4명 | 기술 스택 결정, 통합 의존성, 발표에 직접 연결되는 작업 |

정철진은 Frontend 구조 생성, 화면/라우팅, Frontend-API 연동 기준에 우선 배치한다. 여도현은 DB/API/검증 기준/의존성처럼 난도가 높은 작업에 우선 배치한다. 김민정, 김민경은 이 문서에서 특정 업무에 고정하지 않고, 작업 크기에 따라 황스일이 배정한다.

## 오전 회의 운영안

| 안건 | 결정할 내용 | 회의 결과 |
|---|---|---|
| 4일차 규칙 확인 | PR/CI/AI/docs 기준 | 완료 기준 입력값 확정 |
| 기술 스택 적용 기준 확인 | MySQL 8.4, React + Next, AWS EC2/S3 | Sprint 1 기준 확정 |
| Sprint 1 목표 합의 | 기반 구축 중심으로 제한 | Sprint 목표 확정 |
| 작업 묶음 배정 | 작업 크기와 인원수 결정 | 담당 그룹 확정 |
| 발표/리스크 범위 합의 | 발표 목차, 시연 흐름, blocker 기준 | 마지막 정리 범위 확정 |

## 팀원 작업 시간 운영

### 작업 묶음 1. Sprint 1 Backlog 초안

| 작업 | 권장 담당 그룹 | 완료 조건 | 검증 방법 | 의존성 |
|---|---:|---|---|---|
| Spring Boot 기본 프로젝트 생성 | 1~2명 | local에서 서버 실행 가능 | `./gradlew bootRun` | repository |
| MySQL 8.4 연결 확인 | 2명 | test DB 연결 성공 | 연결 test 또는 간단 query | DB 실행 방식 결정 |
| 기본 entity 초안 작성 | 2명 | User, Role, Post, Schedule entity 초안 | test/build 통과 | ERD 초안 |
| 로그인 API 초안 | 2명 | 로그인 요청/응답 흐름 구현 | Postman 또는 curl | User entity |
| 게시판 API 초안 | 2명 | 목록/상세/작성 기본 API | Postman 또는 curl | Post entity |
| Frontend 프로젝트 구조 생성 | 정철진 중심, 1~2명 | 라우팅, 기본 layout 구성 | 브라우저 실행 | framework 결정 |
| 로그인/메인 화면 초안 | 정철진 중심, 2명 | 로그인 화면과 메인 route 존재 | 브라우저 확인 | Frontend 구조 |
| GitHub Actions CI 초안 | 1~2명 | PR에서 test 또는 build 실행 | CI 초록 체크 | Spring Boot 프로젝트 |
| AGENTS.md/PR Template 반영 | 1명 | root 문서와 PR 양식 존재 | 첫 PR에서 확인 | 4일차 규칙 |

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

### 작업 묶음 4. 발표 구조와 리스크 점검

| 작업 | 권장 담당 그룹 | 산출물 | 검토 기준 |
|---|---:|---|---|
| 기획 산출물 점검 | 2명 | 산출물 체크리스트 | 1~5일차 산출물이 빠지지 않았는지 |
| 제출 문서 체크리스트 작성 | 2명 | 기획서/화면정의서/ERD/API 명세서/개발 명세서 상태표 | 제출 문서별 초안과 보정 시점이 있는지 |
| MVP freeze | 황스일 주도, 전체 합의 | 최종 MVP 범위표 | 6주 안에 구현 가능하고 시연 가능한지 |
| 발표 목차 작성 | 2~3명 | 발표 목차 초안 | 배경→목표→기능→설계→협업→일정→리스크 흐름인지 |
| 시연 시나리오 작성 | 정철진 우선, 2~3명 | 3~5분 시연 흐름 | 화면/API 우선순위와 맞는지 |
| 리스크 대응표 작성 | 여도현 우선, 2명 | 리스크 대응표 | blocker와 대응 담당이 분명한지 |
| Week 2 진입 조건 점검 | 2명 | 시작 조건 checklist | 바로 개발을 시작할 수 있는지 |

발표 목차 예시:

| 순서 | 발표 파트 | 내용 |
|---:|---|---|
| 1 | 프로젝트 배경 | 왜 그룹웨어 클라우드 플랫폼인가 |
| 2 | 프로젝트 목표 | 한 줄 목표, 주요 사용자 |
| 3 | 팀 구성과 역할 | 황스일, 정철진, 담당 그룹 구조 |
| 4 | MVP 범위 | 필수/선택/보류 기능 |
| 5 | 사용자 시나리오 | 일반 사용자/관리자 흐름 |
| 6 | 화면 구성 | 주요 화면 목록 |
| 7 | DB/API 설계 | ERD 초안, 주요 endpoint |
| 8 | System Architecture | Spring Boot, MySQL 8.4, AWS EC2/S3 구조 |
| 9 | 협업 방식 | AGENTS, PR Template, CI, docs 운영 |
| 10 | 6주 개발 일정 | Sprint 구조와 Week 2 Backlog |
| 11 | 리스크와 대응 | AWS, DB, 인증, 일정 리스크 |

리스크 대응표 예시:

| 리스크 | 영향 | 대응 |
|---|---|---|
| MySQL 8.4 실행 방식 미정 | DB 연결과 배포 계획 지연 | local 실행 방식과 배포 방식을 분리해 확정 |
| React + Next 구조 과설계 | Frontend 구조 지연 | 초기에는 기본 routing/layout 중심으로 제한 |
| AWS DB 배포 방식 미정 | DB 배포 방식 변경 가능 | 2주차에는 EC2/S3 중심, DB 배포는 별도 결정 가능 |
| 인증/인가 복잡도 | 구현 지연 | 일반 사용자/관리자 2단계 권한으로 제한 |
| 기능 범위 과다 | 통합 실패 | MVP 필수 기능 3~5개로 freeze |
| CI 설정 지연 | PR 품질 관리 실패 | 초기에는 `./gradlew test`만 적용 |

## 결과 공유 양식

```text
작업 묶음:
참여 인원:
Backlog 항목:
완료 조건:
검증 방법:
의존성/blocker:
발표/리스크 정리에 반영할 내용:
```

## 황스일 종합 항목

- Sprint 1 목표
- Sprint 1 Backlog
- 작업별 권장 담당 그룹 크기
- Definition of Done
- 검증 기준
- 의존성/blocker
- 최종 MVP 범위표
- 발표 목차와 시연 시나리오
- 리스크 대응표
- Week 2 진입 조건 checklist
- 제출 문서 5종 진행 상태표

## 마무리 회의 운영안

| 안건 | 확인할 내용 | 결과 |
|---|---|---|
| Backlog 공유 | 작업이 1~2일 단위로 쪼개졌는지 | Sprint 1 Backlog 확정 |
| 담당 그룹 확인 | 1~4명 배정이 적절한지 | 배정 기준 확정 |
| 완료 조건 확인 | 말이 아니라 검증 가능한 기준인지 | Definition of Done 확정 |
| 의존성 확인 | Week 2 시작 blocker가 있는지 | blocker 목록 확정 |
| 발표 구조 확인 | 흐름이 자연스럽고 빠진 내용이 없는지 | 발표 구조 확정 |
| 리스크 확인 | 숨긴 blocker가 없는지 | 대응 계획 확정 |
| Week 2 조건 확인 | 개발 시작 가능한지 | 시작 조건 통과/보완 판단 |

## Week 2 진입 조건

- [ ] 최종 MVP 범위가 확정되었다.
- [ ] 보류 기능 목록이 있다.
- [ ] Sprint 1 목표
- [ ] Sprint 1 Backlog
- [ ] 작업별 권장 담당 그룹 크기
- [ ] 완료 조건
- [ ] 검증 방법
- [ ] 의존성/blocker 목록
- [ ] GitHub repository가 생성되었고 팀원이 clone할 수 있다.
- [ ] Spring Boot local 실행 기준이 있다.
- [ ] React + Next 기본 구조 기준
- [ ] MySQL 8.4 local 실행 기준
- [ ] PR Template과 CI 기준이 정해졌다.
- [ ] 발표 목차 초안이 있다.
- [ ] 기획서 초안이 있다.
- [ ] 화면정의서 초안이 있다.
- [ ] ERD 제출본 초안이 있다.
- [ ] API 명세서 초안이 있다.
- [ ] 개발 명세서 초안이 있다.
