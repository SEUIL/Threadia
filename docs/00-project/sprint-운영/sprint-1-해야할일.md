---
title: "Sprint 1 해야 할 일"
type: planning-note
status: 작성 중
created: 2026-06-10
updated: 2026-06-17
tags:
  - project
  - team-project
  - acorn
  - todo
  - sprint
aliases:
  - 개발주간 2주차 해야 할 일
  - 개발 1주차 해야 할 일
  - Acorn 최종 프로젝트 Sprint 1 TODO
related:
  - "[[sprint-1-프로젝트-기반구축]]"
context: "Acorn 최종 프로젝트 전체 일정 2주차이자 개발 1주차인 Sprint 1 Auth-only kickoff TODO"
---

# Sprint 1 해야 할 일

## 기준

기획주간 최종 산출물은 `06_기획주간_6일차_자료/최종 산출물/README.md` 기준으로 닫혔다. 전체 일정 2주차이자 개발 1주차의 TODO는 미정 기획을 다시 정하는 목록이 아니라, Sprint 1 Auth-only 개발을 시작하기 위한 kickoff checklist다.

Sprint 1은 Auth-only다. invitation은 Sprint 1.5, company/project/issue/document/chat/DM/thread/notification은 Sprint 2 이후로 분리한다.

## Sprint 1 kickoff에서 먼저 확인할 것

| 확인 항목 | 기준 | 상태 |
|---|---|---|
| repository/clone | 현재 repository 기준으로 clone 착수 가능 | 착수 기준 확정 |
| branch/PR 원칙 | `main` 직접 push 금지, PR 기반 merge | 착수 기준 확정 |
| Sprint 1 범위 | Auth-only | 착수 기준 확정 |
| Sprint 1.5 범위 | registered-user app-internal company invitation | 착수 기준 확정 |
| Sprint 2 이후 범위 | broader MVP domain work | 착수 기준 확정 |
| Java 기준 | Java 17 | 착수 기준 확정 |
| DB 기준 | MySQL 8.4 | 착수 기준 확정 |
| Auth 외부 연동 | Google OAuth, Google SMTP | 설정 확인 필요 |
| Token 정책 | JWT access/refresh, rotation, logout invalidation | 구현 전 확인 필요 |

## Sprint 1 Auth-only TODO

| 번호 | 해야 할 일 | 산출물 | 담당 기준 | 상태 |
|---:|---|---|---|---|
| 1 | repository clone과 branch 생성 확인 | clone 확인 결과 | 전체 | 착수 기준 확정 |
| 2 | PR Template과 branch/PR 규칙 확인 | PR 체크리스트 | 1명 | 착수 기준 확정 |
| 3 | Backend Spring Boot skeleton 작성 | backend 기본 실행 | Backend 1~2명 | 미완료 |
| 4 | Java 17 local 실행 기준 확인 | 실행 환경 기록 | Backend 1명 | 미완료 |
| 5 | MySQL 8.4 local 연결 확인 | DB 연결 결과 | Backend/DB 2명 | 미완료 |
| 6 | Sprint 1 Auth migration 작성 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` | Backend/DB 2명 | 미완료 |
| 7 | auth entity 작성 | user, oauth account, reset token, refresh token entity | Backend 2명 | 미완료 |
| 8 | signup API 작성 | 회원가입 API | Backend 1~2명 | 미완료 |
| 9 | login API 작성 | 로그인 API, JWT 발급 | Backend 1~2명 | 미완료 |
| 10 | logout API 작성 | refresh token invalidation | Backend 1명 | 미완료 |
| 11 | me API 작성 | 현재 사용자 조회 API | Backend 1명 | 미완료 |
| 12 | Google OAuth 설정과 흐름 작성 | OAuth login/callback 기준 | Backend 1~2명 | 미완료 |
| 13 | password reset request/confirm 작성 | Google SMTP 기반 재설정 흐름 | Backend 1~2명 | 미완료 |
| 14 | JWT 정책 적용 | access/refresh, rotation, hash 저장, logout invalidation | Backend 1~2명 | 미완료 |
| 15 | Frontend React + Next skeleton 작성 | frontend 기본 실행 | Frontend 1~2명 | 미완료 |
| 16 | Auth 화면 routing 작성 | 로그인, 회원가입, 비밀번호 재설정, OAuth 진입 화면 | Frontend 2명 | 미완료 |
| 17 | Auth API 연동 기준 정리 | 요청/응답/error handling 기준 | Backend/Frontend | 미완료 |
| 18 | test/build 검증 실행 | backend test, frontend build 또는 lint 기준 | 각 담당 | 미완료 |
| 19 | GitHub Actions CI 초안 작성 | PR test/build check | 1~2명 | 미완료 |
| 20 | README local 실행 방법 갱신 | backend/frontend/DB 실행 방법 | 1명 | 미완료 |
| 21 | 문서와 코드 동기화 | 변경 문서, PR 검증 기록 | 황스일/여도현 중심 | 미완료 |

## Sprint 1 우선순위

| 우선순위 | 작업 |
|---:|---|
| 1 | repository clone, branch, PR 기준 확인 |
| 2 | Backend/Frontend skeleton 작성 |
| 3 | Java 17, MySQL 8.4 local 실행 확인 |
| 4 | Auth migration과 entity 작성 |
| 5 | signup/login/logout/me API 작성 |
| 6 | Google OAuth, Google SMTP password reset 작성 |
| 7 | Auth 화면 routing과 API 연동 |
| 8 | test/build, PR/CI, README, 문서 동기화 |

## 오늘 바로 처리할 것

- [ ] 팀원별 repository clone 가능 여부 확인
- [ ] Sprint 1 Auth-only 범위 공유
- [ ] Sprint 1.5 invitation 분리 기준 공유
- [ ] Sprint 2 이후 broader MVP 분리 기준 공유
- [ ] Backend/Frontend skeleton 작업 branch 생성
- [ ] Java 17과 MySQL 8.4 local 환경 확인
- [ ] Google OAuth 설정 담당과 callback URL 확인
- [ ] Google SMTP 설정 담당과 credential 관리 기준 확인
- [ ] JWT 정책을 backend 작업 기준에 반영
- [ ] PR Template과 CI 기준 확인

## Sprint 1.5 invitation TODO

Sprint 1.5는 Sprint 1 Auth 기반이 준비된 뒤 착수한다.

| 번호 | 해야 할 일 | 기준 | 상태 |
|---:|---|---|---|
| 1 | invitation entity/API 기준 확인 | 등록 사용자 대상 app-internal company invitation | Sprint 1 이후 |
| 2 | exact email lookup 정책 확인 | 가입 사용자만 조회 | Sprint 1 이후 |
| 3 | invitation 상태 정의 | pending, accepted, declined, canceled | Sprint 1 이후 |
| 4 | accept 처리 정의 | 수락 시 membership 생성 | Sprint 1 이후 |
| 5 | decline/cancel 처리 정의 | 거절/취소 상태 변경 | Sprint 1 이후 |
| 6 | 제외 범위 재확인 | email 발송, token link, 미가입자 onboarding, resend, batch scheduler 제외 | Sprint 1 이후 |

## Sprint 2 이후 broader MVP TODO

| 영역 | 후속 TODO | 상태 |
|---|---|---|
| Company | 회사 생성, 멤버 관리, 권한 관리 | Sprint 2 이후 |
| Project | 프로젝트 생성, 참여, 설정 | Sprint 2 이후 |
| Issue | 칸반, 이슈 상태, 담당자, 댓글 | Sprint 2 이후 |
| Document | 문서, 회의록, 첨부 파일 | Sprint 2 이후 |
| Chat/DM/Thread | 메시징, DM, 쓰레드 | Sprint 2 이후 |
| Notification | 알림, 읽음 처리, 전달 정책 | Sprint 2 이후 |
| Invitation 확장 | email 발송, token link, 미가입자 onboarding, resend, 운영 batch | Sprint 2 이후 |
