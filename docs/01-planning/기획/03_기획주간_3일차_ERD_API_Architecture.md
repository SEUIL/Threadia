---
title: "기획주간 3일차 ERD API Architecture 초안"
type: planning-note
status: draft
created: 2026-06-09
updated: 2026-06-12
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

3일차는 2일차 최종 산출물인 **사용자 시나리오, 기능 분해표, 화면 정의서**를 바탕으로 **ERD 초안, API 명세서 초안, 권한 정책 초안**을 만들고, 4일차 협업 하네스에 넘길 입력값을 정리하는 날이다.

2026-06-12 기준으로 ERD/API/권한 정책은 `v1.4` 초안까지 작성되었고, AWS 범위와 승인 필요 작업 목록은 4일차 협업 규칙 문서로 이월한다.

## 전회차 입력값 처리 결과

| 입력값 | 처리 결과 | 비고 |
|---|---|---|
| 필수 기능 목록 | 완료 | `사용자 시나리오 v2`, `기능 분해표 v2`, `화면 정의서 v2` 기준으로 설계 범위 반영 |
| 화면 흐름 | 완료 | API 명세서 작성 기준으로 사용. 상세 화면-API 매핑표는 4일차 이월 |
| 사용자 권한 범위 | 완료 | API 명세서 v1.4의 A/P/C 권한 정책으로 반영 |
| entity/API 후보 | 완료 | ERD 초안 v1.4, API 명세서 초안 v1.4로 구체화 |
| 기획서 초안 | 완료 | `03_기획주간_3일차_자료/최종 산출물/프로젝트 기획서(이미지 포함)/프로젝트 기획서.md` 생성 |
| 보류 기능 최종표 | 이월 | 기능 분해표 v2 기준으로 별도 표 정리 필요 |
| repository 이름/owner | 완료 | `Threadia`, `SEUIL/Threadia` |
| GitHub repository 초기 세팅 | 부분 완료 | remote: `https://github.com/SEUIL/Threadia`; 팀원 clone 확인은 이월 |
| README 한 줄 설명 초안 | 이월 | 현재 root README는 제목만 있음. 한 줄 설명 보강 필요 |

## 해야될 목록 처리 결과

| 번호 | 해야될 일 | 산출물 | 상태 | 후속 처리 |
|---:|---|---|---|---|
| 1 | 2일차 이월 항목 정리 | 기획서 초안, repo 결정값 | 부분 완료 | 보류 기능 최종표, README 한 줄 설명은 4일차 이월 |
| 2 | GitHub repository 초기 세팅 | repository URL | 부분 완료 | 팀원 초대/clone 확인 결과는 4일차 이월 |
| 3 | 핵심 entity 확정 | entity 목록 | 완료 | ERD 초안 v1.4에 반영 |
| 4 | ERD 초안 작성 | ERD 초안 | 완료 | `ERD 초안_v1.4.md` |
| 5 | API 목록 초안 작성 | API 명세서 초안 | 완료 | `API 명세서 초안_v1.4.md` |
| 6 | 권한 정책 정리 | A/P/C 권한표 | 완료 | API 명세서 v1.4의 권한 정책 섹션에 반영 |
| 7 | AWS 사용 범위 정리 | EC2/S3/RDS 판단표 | 이월 | 4일차 협업 하네스 문서에서 승인/운영 기준과 함께 정리 |
| 8 | 위험 변경 항목 도출 | 승인 필요 작업 목록 | 이월 | DB, 배포, 인증/인가, 보안, 대규모 구조 변경 기준으로 4일차 작성 |

## 확정된 산출물

| 산출물 | 위치 | 확인 내용 |
|---|---|---|
| 사용자 시나리오 v2 | `03_기획주간_3일차_자료/최종 산출물/사용자 시나리오 v2.md` | 2일차 검토 반영본 |
| 기능 분해표 v2 | `03_기획주간_3일차_자료/최종 산출물/기능 분해표 v2.md` | MVP/선택/보류 기준 반영본 |
| 화면 정의서 v2 | `03_기획주간_3일차_자료/최종 산출물/화면 정의서 v2.md` | 주요 화면 흐름 반영본 |
| 프로젝트 기획서 | `03_기획주간_3일차_자료/최종 산출물/프로젝트 기획서(이미지 포함)/프로젝트 기획서.md` | 기획서 초안 산출물 |
| ERD 초안 v1.4 | `03_기획주간_3일차_자료/최종 산출물/ERD 초안_v1.4.md` | entity, 관계, 주요 column 정리 |
| API 명세서 초안 v1.4 | `03_기획주간_3일차_자료/최종 산출물/API 명세서 초안_v1.4.md` | `/api/v1`, `{ data, meta }`, 권한 정책, endpoint 초안 정리 |

## ERD 초안 v1.4 반영 내용

| 항목 | 반영 내용 |
|---|---|
| 사용자 표시명 | `display_name` 대신 `nickname` 사용 |
| 초대 정책 | `Invitation` 테이블 제거, 즉시 등록 정책 반영 |
| 회사/부서 | `companies`, `departments`, `company_members` 포함 |
| 프로젝트 | `projects`, `project_members`, `sprints`, `issues` 포함 |
| 문서/그래프 | `documents`, `document_issue_links` 포함 |
| 채팅/DM | `messages`, `direct_message_rooms`, `direct_messages`, 읽음 상태 테이블 포함 |
| 쓰레드 | `threads`, `thread_comments` 포함 |
| 알림 | `notification_events`, `notification_recipients`, `notification_preferences` 포함 |

## API 명세서 초안 v1.4 반영 내용

| 항목 | 반영 내용 |
|---|---|
| Base URL | `/api/v1` prefix 통일 |
| 공통 응답 | `{ data, meta }` 형식 확정 |
| 인증 | JWT Bearer Token 기준 |
| 권한 | A 일반 사용자, P 프로젝트 관리자, C 회사 관리자 기준 |
| 사용자 DTO | `displayName` 대신 `nickname` 사용 |
| 삭제 조회 | `includeTrashed` 기본값 `false` 명시 |
| 메시지 수정 | 메시지 수정 API 미제공 기준으로 `messages.updated_at` 제거 |
| Invitation | 즉시 등록 정책 확정으로 초대 테이블/API 제외 |

## 4일차로 넘길 이월 항목

| 이월 항목 | 넘기는 이유 | 4일차 처리 위치 |
|---|---|---|
| 보류 기능 최종표 | 기능 분해표 v2는 있으나 별도 공유용 표가 필요 | 협업 하네스 입력값/팀 공유 문장 |
| 화면-API 매핑 | API 명세서 초안은 있으나 화면 단위 연결표는 미작성 | docs 구조 또는 API 규칙 문서 |
| AWS 사용 범위 | EC2/RDS/S3 판단 기준을 협업/승인 규칙과 함께 정해야 함 | 승인 필요 작업 목록, CI/배포 기준 |
| 승인 필요 작업 목록 | DB, 배포, 인증/인가, 보안, 구조 변경 기준을 AGENTS/PR Template에 넣어야 함 | AGENTS.md, PR Template |
| 팀원 clone 확인 결과 | repo remote는 확정됐지만 팀원별 clone 확인 기록 필요 | 4일차 오전 확인 |
| README 한 줄 설명 | root README가 제목만 있어 설명 보강 필요 | repository 문서 구조 작업 |
| MySQL 8.4 local/배포 확인 | ERD/API는 작성됐지만 local/배포 운영 기준은 미확정 | CI/배포 기준 작업 |

## 3일차 마감 체크

- [x] 2일차 최종 산출물 검토 결과 반영
- [x] 사용자 시나리오 v2 작성
- [x] 기능 분해표 v2 작성
- [x] 화면 정의서 v2 작성
- [x] 프로젝트 기획서 초안 작성
- [x] repository 이름/owner 확정
- [x] ERD/entity 정리
- [x] ERD 초안 v1.4 작성
- [x] API 명세서 초안 v1.4 작성
- [x] 권한 정책 초안 작성
- [ ] 보류 기능 최종표 별도 정리
- [ ] 화면-API 매핑표 작성
- [ ] AWS 사용 범위 정리
- [ ] 승인 필요 작업 목록 작성
- [ ] 팀원 clone 확인 결과 기록
- [ ] README 한 줄 설명 보강
