---
title: "Sprint 1 프로젝트 기반 구축"
type: planning-note
status: 작성 중
created: 2026-06-10
updated: 2026-06-29
tags:
  - project
  - team-project
  - acorn
  - planning
  - sprint
  - development
  - sprint-1
  - auth
  - setup
aliases:
  - 개발주간 2주차 프로젝트 기반 구축
  - 개발 1주차 프로젝트 기반 구축
  - Acorn 최종 프로젝트 Sprint 1 계획
related:
  - "[[260609_Acorn_최종_프로젝트_기획_방법]]"
  - "[[05_기획주간_5일차_Sprint1_Backlog_검증기준]]"
context: "Acorn 최종 프로젝트 전체 일정 2주차이자 개발 1주차인 Sprint 1 Auth-only 개발 기반 구축 계획"
---

# Sprint 1 프로젝트 기반 구축

## 목적

전체 일정 기준 2주차는 개발 기준 1주차이며, Sprint 1 시작 주차다. 목표는 전체 MVP 구현이 아니라 **Auth-only 개발 기반 구축, Spring Boot/MySQL/Frontend skeleton, PR/CI 검증 흐름, 문서와 코드 기준 정렬**이다.

Sprint 1은 회원가입, 로그인, 로그아웃, me, Google OAuth, Google SMTP 기반 password reset, JWT 정책을 우선 구현 범위로 둔다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림은 Sprint 2 이후 reference로 유지한다.

## Sprint 1 핵심 목표

| 목표 | 완료 기준 |
|---|---|
| repository 협업 기반 확인 | 현재 repository 기준으로 clone, branch, PR 흐름 착수 가능 |
| Spring Boot 기본 구조 생성 | Java 17 기준 local 서버 실행 가능 |
| MySQL 8.4 연결 확인 | Auth table DDL 기준과 local DB 연결 확인 |
| db/schema.sql 생성 기준 확인 | ERD.md 완료 후 MySQL 8.4 기준 `db/schema.sql` 생성 |
| React + Next 기본 구조 생성 | Auth 화면 routing과 기본 layout 확인 가능 |
| Auth domain/API 기반 작성 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` 중심 설계 반영 |
| Google OAuth 준비 | client 설정, callback URL, secret 비노출 기준 확인 |
| Google SMTP 준비 | password reset email 발송 검증 기준 확인 |
| JWT 정책 적용 기준 확정 | access/refresh token, rotation, logout invalidation 기준 반영 |
| CI/PR 검증 흐름 시작 | PR에서 test 또는 build 확인 가능 |
| 문서와 코드 기준 정렬 | 최종 산출물, README, PR Template, CI 기준이 개발 작업과 충돌하지 않음 |

## 확정 기술 스택

| 영역 | 결정 |
|---|---|
| Backend | Spring Boot |
| Java | Java 17 |
| DB | MySQL 8.4 |
| Frontend | React + Next |
| Auth | 일반 회원가입/로그인, Google OAuth, Google SMTP password reset |
| Token | JWT access/refresh token 정책 |
| AWS | EC2, RDS, S3 사용 방향은 확정. 실제 배포와 S3 파일 저장 구현은 Sprint 1 이후 |
| 협업 | GitHub Issue, branch, PR, review, merge, CI |

## Sprint 1에 할 일

| 번호 | 해야 할 일 | 산출물 | 권장 담당 그룹 |
|---:|---|---|---|
| 1 | repository clone과 branch/PR 흐름 확인 | clone 확인, branch naming, PR 규칙 | 황스일 주도, 1~2명 |
| 2 | Spring Boot 프로젝트 skeleton 작성 | backend 기본 실행 | Backend 1~2명 |
| 3 | Java 17 기준 확인 | local 실행 기준 | Backend 1명 |
| 4 | MySQL 8.4 local 연결 확인 | DB 연결 결과, 설정 문서 | Backend/DB 2명 |
| 5 | Auth DDL 기준 확인 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` | Backend/DB 2명 |
| 6 | ERD.md 기준 db/schema.sql 생성 | `db/schema.sql` | 김민정 Owner, 김태준·김민경 지원, 황스일 Review |
| 7 | Auth API 기본 흐름 작성 | signup, login, logout, me | Backend 2명 |
| 8 | Google OAuth 흐름 작성 | OAuth login callback 기준 | Backend 1~2명 |
| 9 | Google SMTP password reset 흐름 작성 | reset request, reset confirm 기준 | Backend 1~2명 |
| 10 | JWT 정책 반영 | access/refresh 발급, rotation, invalidation 기준 | Backend 1~2명 |
| 11 | React + Next skeleton 작성 | frontend 기본 실행 | Frontend 1~2명 |
| 12 | Auth 화면 routing 작성 | 로그인, 회원가입, 비밀번호 재설정, OAuth 진입 화면 | Frontend 2명 |
| 13 | README와 local 실행 문서 정리 | backend/frontend/DB 실행 방법 | 1명 |
| 14 | CI 초안 적용 | PR test/build check | 1~2명 |
| 15 | 개발 문서 동기화 | 최종 산출물과 코드 변경 기준 정렬 | 황스일/여도현 중심, 2명 |

## Sprint 1에 하지 않을 것

- company/project/issue/document/chat/DM/thread/notification 구현
- invitation 구현
- 실제 email invitation 발송, token link invitation, unregistered onboarding
- AWS 배포 완성 및 S3 파일 저장 구현
- 실시간 채팅 구현
- 전체 MVP를 한 번에 완성하려는 작업
- 검증 없이 `main`에 직접 push

## Sprint 1.5 후속 범위

Sprint 1.5는 Sprint 1 Auth 기반 위에서 등록 사용자 대상 app-internal company invitation을 다룬다.

| 항목 | Sprint 1.5 기준 |
|---|---|
| 초대 대상 | 이미 가입된 사용자 |
| 조회 방식 | 정확한 email lookup |
| 초대 상태 | pending, accepted, declined, canceled |
| 수락 처리 | accept 시 membership 생성 |
| 거절/취소 | decline, cancel 처리 |
| 제외 | 실제 email 발송, token link, 미가입자 onboarding, resend, batch scheduler |

## Sprint 2 이후 reference

| 영역 | 처리 기준 |
|---|---|
| Company | 조직 생성, 멤버 관리, 권한 관리 |
| Project | 프로젝트 생성, 참여, 설정 |
| Issue | 칸반, 이슈 상태, 담당자, 댓글 |
| Document | 문서, 회의록, 첨부 파일 |
| Chat/DM/Thread | 메시징과 쓰레드 흐름 |
| Notification | 알림, 읽음 처리, 전달 정책 |
| Invitation 확장 | email 발송, token link, 미가입자 onboarding, resend, 운영 batch |

## Sprint 1 Backlog 기준

| 작업 | 완료 조건 | 검증 방법 | 의존성 |
|---|---|---|---|
| repository/clone 확인 | 팀원 작업 착수 가능 | 팀원별 clone 확인 | repository 접근 권한 |
| Spring Boot skeleton | 서버 local 실행 | `./gradlew bootRun` 또는 test 실행 | Java 17 |
| MySQL 연결 | Auth DB 연결 성공 | 연결 test 또는 query | MySQL 8.4 실행 환경 |
| Auth DDL 기준 | Sprint 1 Auth table 생성 기준 존재 | ERD/schema.sql review, DB 확인 | ERD Auth 기준 |
| db/schema.sql 생성 | ERD.md 기준 MySQL 8.4 DDL 존재 | schema.sql 실행 또는 검토, Auth 4개 table 정합성 확인 | ERD.md 상세 설계 완료 |
| Auth API | signup/login/logout/me 흐름 존재 | API test, backend test | JWT 정책 |
| Google OAuth | OAuth login 흐름 존재 | callback 동작 확인 | Google OAuth 설정 |
| Password reset | reset request/confirm 흐름 존재 | SMTP 수신 또는 mock 검증 | Google SMTP 설정 |
| Frontend skeleton | 기본 페이지 실행 | 브라우저 확인 | React + Next |
| Auth screens | Auth 화면 routing 가능 | 화면 이동 확인 | Auth API 계약 |
| PR Template/CI | PR에서 test/build check 가능 | GitHub Actions check | skeleton 구조 |
| 문서 동기화 | 코드 변경과 문서 기준 충돌 없음 | PR 체크리스트 확인 | 최종 산출물 README |

## Week 2 종료 조건

- [ ] 팀원이 repository를 clone하고 branch를 만들 수 있다.
- [ ] `main` 직접 push 금지 원칙이 공유되었다.
- [ ] Java 17 기준이 확인되었다.
- [ ] Spring Boot 서버가 local에서 실행된다.
- [ ] React + Next 기본 화면이 브라우저에서 열린다.
- [ ] MySQL 8.4 연결 기준이 확인되었다.
- [ ] Sprint 1 Auth table DDL 기준이 있다.
- [ ] ERD.md 기준 `db/schema.sql`이 생성되었다.
- [ ] signup/login/logout/me API 기준이 구현 또는 검증 가능한 상태다.
- [ ] Google OAuth 설정과 callback 기준이 확인되었다.
- [ ] Google SMTP password reset 검증 기준이 확인되었다.
- [ ] JWT 발급, refresh, logout invalidation 기준이 코드와 문서에 맞다.
- [ ] PR에서 최소 test 또는 build를 확인할 수 있다.
- [ ] 개발 문서가 Sprint 1 Auth-only 범위와 맞게 갱신되었다.
