---
title: "기획주간 3일차 ERD API Architecture 초안"
type: planning-note
status: draft
created: 2026-06-09
updated: 2026-06-11
tags:
  - project
  - team-project
  - acorn
  - planning
  - erd
  - api
  - architecture
aliases:
  - 3일차 ERD API Architecture
  - Acorn 최종 프로젝트 3일차 기획
related:
  - "[[02_기획주간_2일차_사용자흐름_기능분해]]"
  - "[[07_260609_Spring Boot 프로젝트에서 test-build-lint 기준 잡기]]"
context: "Acorn 최종 프로젝트 기획주간 3일차 ERD, API, Architecture 초안 작성 운영안"
---

# 기획주간 3일차 ERD API Architecture 초안

## 목적

3일차는 2일차 최종 산출물인 **사용자 시나리오, 기능 분해표, 화면 정의서**를 바탕으로 **ERD, API 목록, 권한 정책, AWS Architecture 초안**을 만든다. 4일차 협업 규칙과 문서 운영 기준에 반영할 위험 변경 영역도 함께 정리한다.

추가로 2일차에서 이월된 **기획서 초안, 보류 기능 최종표, repository 이름/owner와 GitHub repository 초기 세팅, README 한 줄 설명 초안**을 먼저 정리한다.

## 전회차 입력값

| 입력값 | 없을 때 처리 |
|---|---|
| 필수 기능 목록 | ERD 작성 전 MVP 범위 재확정 |
| 화면 흐름 | API 요청/응답 기준 재정리 |
| 사용자 권한 범위 | A 일반 사용자, P 프로젝트 관리자, C 회사 관리자 기준으로 정리. F/S15는 MVP 보류 |
| entity/API 후보 | 2일차 최종 산출물의 Day 3 입력값을 기준으로 우선 후보 작성 |
| 기획서 초안 | 2일차에서 이월. 최종 사용자 시나리오/기능 분해표/화면 정의서를 입력값으로 사용 |
| 보류 기능 최종표 | 2일차에서 이월. 기능 분해표의 보류 항목을 기준으로 확정 |
| repository 이름/owner | 2일차에서 이월. 값 확정 필요 |
| GitHub repository 초기 세팅 | 2일차에서 이월. repo 생성, 팀원 초대, clone 확인 필요 |
| README 한 줄 설명 초안 | 2일차에서 이월. repo 생성 시 임시 README에 반영 |

## 해야될 목록

| 번호 | 해야될 일 | 산출물 | 권장 담당 그룹 |
|---:|---|---|---|
| 1 | 2일차 이월 항목 정리 | 기획서 초안, 보류 기능 최종표, repo 결정값, README 한 줄 설명 | 황스일 주도, 전체 확인 |
| 2 | GitHub repository 초기 세팅 | repository URL, 팀원 초대, clone 확인 | 황스일 주도, 1~2명 |
| 3 | 핵심 entity 확정 | entity 목록 | 여도현 우선, 2~3명 |
| 4 | ERD 초안 작성 | ERD 초안, ERD 제출본 초안 | 2~3명 |
| 5 | API 목록 초안 작성 | endpoint 표, API 명세서 초안 | 3~4명 |
| 6 | 권한 정책 정리 | A/P/C 권한표 | 2명 |
| 7 | AWS 사용 범위 정리 | EC2/S3 중심 판단표 | 2명 |
| 8 | 위험 변경 항목 도출 | 승인 필요 작업 목록 | 황스일 종합, 2명 검토 |

## 회의에서 먼저 정해야 할 것

| 결정 항목 | 기준 | 다음 작업에 미치는 영향 |
|---|---|---|
| 2일차 이월 항목 | 기획서 초안, 보류 기능, repository, README 처리 순서 결정 | 오전 우선 작업 확정 |
| repository 기준 | repo 이름, owner, 기본 branch, README 한 줄 설명 | GitHub repository 생성 기준 |
| 핵심 entity | 필수 기능에 필요한 것만 포함 | ERD 범위 결정 |
| MySQL 8.4 기준 | 수업 환경, local 실행, AWS 배포 가능성 | DB 설계와 배포 방식 결정 |
| API URL 규칙 | REST API 기준으로 단순화 | Frontend/Backend 협업 기준 |
| 권한 범위 | A/P/C 기준, F/S15는 MVP 보류 | 인증/인가 구현 범위 |
| AWS 서비스 | EC2/RDS/S3 중심 | 발표와 배포 계획 |

## 분업 가이드라인

| 작업 | 권장 인원 | 우선 배치 | 검토자 |
|---|---:|---|---|
| ERD/entity 정리 | 2~3명 | 여도현 우선 | 황스일 |
| API 목록 정리 | 3~4명 | 여도현 포함 가능 | 황스일, 정철진 |
| API 명세서 초안 정리 | 2명 | 여도현 포함 가능 | 황스일 |
| 화면-API 매핑 | 2~3명 | 정철진 우선 | 황스일 |
| AWS 범위 조사 | 1~2명 | 기술 이해도 높은 인원 | 황스일 |
| 위험 변경 목록 | 2명 | 미배정 | 황스일 |
| 2일차 이월 항목 마감 | 1~2명 | 황스일 주도 | 전체 기준 확인 |

## 오전 회의 운영안

| 안건 | 결정할 내용 | 회의 결과 |
|---|---|---|
| 2일차 이월 항목 확인 | 기획서 초안, 보류 기능, repository, README 처리 순서 | 오전 우선 작업 확정 |
| repository 기준 확정 | repo 이름, owner, 기본 branch, README 한 줄 설명 | GitHub repository 생성 기준 |
| 기능 범위 확인 | 필수/선택/보류 기능 재확인 | 설계 대상 고정 |
| entity 후보 검토 | User, Role, Project, Issue, Document, Notice 등 | ERD 작업 기준 |
| API 규칙 합의 | `/api/...` 형태, REST 기준 | endpoint 작성 기준 |
| 작업 묶음 배정 | ERD/API/AWS/권한 그룹 배정 | 담당 그룹 확정 |

## 팀원 작업 시간 운영

### 작업 묶음 0. 2일차 이월 항목 마감

| 이월 항목 | 3일차 처리 기준 | 산출물 |
|---|---|---|
| 기획서 초안 | 최종 사용자 시나리오, 기능 분해표, 화면 정의서를 입력값으로 배경/목표/주요 사용자/MVP 범위/기대효과 작성 | 기획서 초안 |
| 보류 기능 최종표 | 기능 분해표의 `선택/보류` 항목을 검토해 MVP 제외 사유 정리 | 보류 기능 최종표 |
| repository 이름/owner | 팀장 기준으로 값 확정. 임의 값 생성 금지 | repo 생성 기준 |
| GitHub repository 초기 세팅 | repo 생성, 팀원 초대, clone 확인 | repository URL, clone 확인 결과 |
| README 한 줄 설명 초안 | 프로젝트 한 줄 설명만 우선 작성. 실행 방법은 4일차 이후 보강 | README 초안 |

### 작업 묶음 1. 핵심 entity

| Entity | 목적 | 비고 |
|---|---|---|
| User | 사용자 계정 | Role, Project, Issue와 연결 |
| Role | 일반 사용자/관리자 권한 | 단순 권한 기준 |
| Project | 프로젝트/팀 작업 공간 | User, Issue, Document와 연결 |
| Issue | 업무/이슈 | 담당자, 상태, 마감일 관리 |
| Document | 문서/회의록 | 프로젝트 결정사항과 지식 공유 |
| Notice | 공지/알림 | 팀 공지와 알림성 메시지 |
| Attachment | 파일 메타데이터 | S3 선택 기능 포함 시 사용 |
| Comment | 댓글 | 선택 기능 |

### 작업 묶음 2. API 목록

| 영역 | Method | Endpoint 후보 | 목적 |
|---|---|---|---|
| 인증 | POST | `/api/auth/login` | 로그인 |
| 사용자 | GET | `/api/users/me` | 현재 사용자 정보 조회 |
| 업무/이슈 | GET | `/api/issues` | 업무/이슈 목록 |
| 업무/이슈 | GET | `/api/issues/{id}` | 업무/이슈 상세 |
| 업무/이슈 | POST | `/api/issues` | 업무/이슈 등록 |
| 문서 | GET | `/api/documents` | 문서/회의록 목록 |
| 문서 | POST | `/api/documents` | 문서/회의록 작성 |
| 공지 | GET | `/api/notices` | 공지 목록 |
| 공지 | POST | `/api/notices` | 공지 작성 |
| 관리자 | GET | `/api/admin/users` | 사용자 관리 |
| 파일 | POST | `/api/files` | 파일 업로드, 선택 기능 |

### 작업 묶음 3. Architecture

```text
Frontend React + Next
→ Spring Boot REST API
→ MySQL 8.4
→ AWS S3, 선택 기능 파일 저장
→ AWS EC2, Backend 배포
```

### 작업 묶음 4. AWS 범위 판단

| AWS 서비스 | 우선순위 | 판단 |
|---|---:|---|
| EC2 | 필수 | Spring Boot 배포 후보 |
| S3 | 선택 | 파일 업로드 포함 시 사용 |
| RDS 또는 DB 배포 방식 | 선택 후보 | MySQL 배포 필요 시 검토 |
| Route 53 | 보류 | 도메인 필요 시 후순위 |
| CloudFront | 보류 | MVP에서는 과할 수 있음 |

## 결과 공유 양식

```text
작업 묶음:
참여 인원:
작성한 산출물:
확정 가능한 내용:
확인 필요/TBD:
4일차 협업 규칙에 반영할 위험 항목:
```

## 황스일 종합 항목

- ERD 초안
- ERD 제출본 초안
- API 목록 초안
- API 명세서 초안
- 권한 정책표
- AWS 사용 범위 판단표
- MySQL 8.4 local/배포 확인 필요 사항
- 4일차 `AGENTS.md`와 PR Template에 넣을 승인 필요 작업
- 기획서 초안
- 보류 기능 최종표
- GitHub repository URL과 clone 확인 결과
- README 한 줄 설명 초안

## 마무리 회의 운영안

| 안건 | 확인할 내용 | 결과 |
|---|---|---|
| ERD 공유 | 필수 기능을 감당하는지 | entity 범위 확정 |
| API 공유 | 화면 흐름과 연결되는지 | API 초안 확정 |
| 2일차 이월 항목 공유 | 기획서 초안, 보류 기능표, repo, README가 정리됐는지 | 4일차 협업 하네스 입력값 확정 |
| AWS 공유 | EC2/RDS/S3 범위가 현실적인지 | AWS 후보 확정 |
| 위험 변경 공유 | DB, 배포, 인증/인가, 보안 기준 | 승인 필요 작업 확정 |
| 다음날 브리핑 | 협업 규칙과 문서 운영 설계 안내 | 4일차 입력값 확정 |

## 4일차로 넘길 입력값

- [ ] ERD 초안
- [ ] ERD 제출본 초안
- [ ] API 목록 초안
- [ ] API 명세서 초안
- [ ] 권한 정책 초안
- [ ] AWS 사용 범위
- [ ] 승인 필요 작업 목록
- [ ] MySQL 8.4 local/배포 확인 필요 사항
- [ ] 기획서 초안
- [ ] 보류 기능 최종표
- [ ] GitHub repository URL
- [ ] 팀원 clone 확인 결과
- [ ] README 한 줄 설명 초안
