---
title: "기획주간 5일차 Sprint 1 Backlog 검증 기준"
type: planning-note
status: draft
created: 2026-06-09
updated: 2026-06-17
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

5일차는 Week 2 개발 착수와 6일차 보고용 정리를 동시에 가능하게 만들기 위해 **Sprint 1 목표, Sprint 1 Backlog, 담당 그룹/역할 분담, 완료 조건, 검증 방법, 의존성/blocker**를 확정하는 날이다.

2026-06-17 기준 이 문서는 `05_기획주간_5일차_자료/최종 산출물/`에 정리된 최종본을 반영한다. Sprint 1은 Auth-only로 제한하고, 회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림/프로필 수정은 Sprint 1 구현 범위에서 제외한다. invitation은 Sprint 1에 넣지 않고 Sprint 1.5의 등록 사용자 대상 app-internal invitation으로 분리한다.

## 최신 반영 기준

| 항목 | 최신 기준 |
|---|---|
| 최종 산출물 위치 | `05_기획주간_5일차_자료/최종 산출물/` |
| 최종 README | `05_기획주간_5일차_자료/최종 산출물/README.md` |
| 최종 확인용 본문 | 17개 Markdown 본문. Phase B invitation 반영본 13개, Phase A 기준본/보정본 4개 |
| Sprint 1 범위 | Auth-only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows |
| Sprint 1 DB migration | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` |
| Sprint 1 Auth API | signup, login, logout, refresh, `/auth/me`, Google OAuth, password reset request/confirm |
| `/auth/me` 응답 | minimal `data.user.id/email/nickname/avatarUrl` |
| JWT 정책 | access 30분, refresh 14일, refresh token hash 저장, rotation, logout invalidation |
| 비밀번호 재설정 | Google SMTP 실제 발송, reset token hash 저장, expires/used 처리 |
| Java 기준 | Java 17 |
| invitation 기준 | Sprint 1 제외. Sprint 1.5에서 registered-user app-internal invitation으로 처리 |

## 전회차 입력값

| 입력값 | 현재 처리 상태 |
|---|---|
| PR 제출 기준 | `PR Template v1.0 정합도-v1.md` 최종 산출물에 포함됨 |
| CI 검증 명령 | `CI 기준 확정 v1.0 정합도-v1.md` 최종 산출물에 포함됨. 실제 workflow 파일은 Backend/Frontend skeleton 이후 생성 |
| AGENTS.md / CLAUDE.md / CONTRIBUTING.md | root 규칙 문서와 `docs/AGENTS.md`, `docs/CLAUDE.md` 정리 완료 |
| AI 사용 규칙 | `AI 사용 규칙 v1.0 정합도-v1.md` 최종 산출물에 포함됨 |
| 화면 정의서 최종본 / 화면-API 매핑표 | `화면정의서_v3 정합도-v1 invitation-v1.md`, `기능-시나리오-API-ERD 추적표 v1.0 정합도-v1 invitation-v1.md` 최종 산출물에 포함됨 |
| API/ERD 기준 | `API 명세서 v2.3 정합도-v1 invitation-v1.md`, `ERD v2.0 정합도-v1 invitation-v1.md` 최종 산출물에 포함됨 |
| Sprint 1 Backlog | `Sprint1_Backlog 정합도-v1 invitation-v1.md` 최종 산출물에 포함됨 |
| 담당 그룹/역할 분담 | `Sprint1_담당그룹_역할분담 v1.1 정합도-v1.md` 최종 산출물에 포함됨 |
| 보류 기능 최종표 | `보류 기능 최종표 v1.0 정합도-v1 invitation-v1.md` 최종 산출물에 포함됨 |
| 승인 필요 작업 | PR Template와 DoD 기준에 반영됨. DB schema, 인증/인가, CI, AWS, secret 변경은 승인 필요 작업으로 표시 |
| GitHub repository와 clone 확인 | Week 2 시작 전 최종 확인 항목으로 유지 |

## 해야될 목록

| 번호 | 해야될 일 | 최종 산출물 | 완료 기준 | 상태 |
|---:|---|---|---|---|
| 1 | Sprint 1 목표 확정 | `Sprint 1_detail 정합도-v1 invitation-v1.md` | Auth-only 목표 문장으로 Week 2 개발 방향을 설명할 수 있음 | 완료 |
| 2 | Sprint 1 Backlog 확정 | `Sprint1_Backlog 정합도-v1 invitation-v1.md` | 작업이 1~2일 단위로 쪼개지고 Auth 흐름 중심으로 제한됨 | 완료 |
| 3 | 담당 그룹/역할 분담 확정 | `Sprint1_담당그룹_역할분담 v1.1 정합도-v1.md` | 각 작업에 지원 담당, Owner, Reviewer가 있음 | 완료 |
| 4 | Definition of Done 작성 | `Definition of Done_작업 유형별 완료 조건 v2 정합도-v1 invitation-v1.md` | Backend/Frontend/DB/문서/PR 완료 기준이 검증 가능함 | 완료 |
| 5 | 검증 방법 작성 | `검증 방법_test-build-API-화면 확인 기준 v2 정합도-v1 invitation-v1.md` | test/build/API/화면/DB/문서/PR 확인 방법이 있음 | 완료 |
| 6 | 의존성/blocker 정리 | Sprint 1 detail, Backend/Frontend DoD, 보류 기능 최종표 | Week 2 시작 전 막히는 항목과 해결 담당이 보임 | 완료 |
| 7 | invitation 영향 반영 | Phase B invitation 반영본 13개 | Sprint 1.5 invitation 정책이 관련 문서에 반영됨 | 완료 |
| 8 | 최종 산출물 묶음 정리 | `최종 산출물/README.md` | 최종 확인용 문서 목록과 구성 기준이 보임 | 완료 |

## Sprint 1 목표 문장

```text
Sprint 1에서는 Spring Boot, React + Next.js, MySQL 8.4 기반을 구축하고,
이메일 로그인, 소셜 로그인(Google OAuth), 비밀번호 재설정을 포함한
인증 흐름을 local 환경에서 끝까지 검증 가능한 상태로 만든다.
```

짧게 보고할 때는 아래 문장을 사용한다.

```text
Sprint 1 목표는 개발 기반 구축과 인증 흐름 end-to-end 검증이다.
```

## 5일차 완료 목표

5일차 종료 시점에는 아래 항목을 6일차 보고용 정리와 PPT 제작의 입력값으로 넘긴다.

| 산출물 | 최소 포함 내용 | 보고/PPT 연결 |
|---|---|---|
| Sprint 1 목표 | Auth-only 목표, 개발 기반 구축, 인증 흐름 검증 | Week 2 방향 보고 |
| Sprint 1 Backlog | Backend, Frontend, DB, 협업/CI 작업과 우선순위 | 개발 착수 준비 상태 요약 |
| 담당 그룹/역할 분담 | 작업별 지원 담당, Owner, Reviewer | 팀 역할 정리 |
| Definition of Done | Backend/Frontend/DB/문서/PR 완료 기준 | 품질 기준 요약 |
| 검증 방법 | `./gradlew test`, `./gradlew build`, API 요청, 브라우저 확인, 문서 검토 기준 | 검증 기준 요약 |
| 의존성/blocker | Java 17, MySQL 8.4, React + Next 구조, Google OAuth, Google SMTP, JWT 정책 | 리스크 메모와 Week 2 확인 항목 입력값 |
| invitation 정책 | Sprint 1 제외, Sprint 1.5 registered-user app-internal invitation | Sprint 1 범위와 후속 범위 분리 설명 |
| 최종 산출물 README | 17개 최종 확인용 본문 목록과 구성 기준 | 제출/공유 대상 안내 |

## 회의에서 확정된 것

| 결정 항목 | 최종 기준 | 결과물 |
|---|---|---|
| Sprint 1 목표 | Auth-only + 개발 기반 구축 | Sprint 1 detail, Sprint1 Backlog |
| Sprint 1 DB 범위 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` | ERD, Backend DoD, Backlog |
| JWT 정책 | access 30분, refresh 14일, hash 저장, rotation, logout invalidation | API, Backend DoD, 검증 기준 |
| `/auth/me` | `data.user.id/email/nickname/avatarUrl` minimal 응답 | API 명세서 |
| 비밀번호 재설정 | Google SMTP 실제 발송, token hash, expires/used 처리 | API, Backend DoD, 검증 기준 |
| Java version | Java 17 | Backend blocker, 검증 기준 |
| invitation | Sprint 1 제외, Sprint 1.5 registered-user app-internal invitation | Phase B invitation 반영본 |
| AWS | EC2/RDS/S3 사용 방향은 확정하되 실제 배포와 S3 파일 저장 구현은 Sprint 1 이후 | DoD, 보류 기능 최종표 |

## 분업 가이드라인

| 작업 크기 | 인원 기준 | 적용 예시 |
|---|---:|---|
| 작음 | 1명 | 문서 확인, PR Template 확인, 단일 화면 레이아웃 |
| 보통 | 2명 | Spring Boot skeleton, MySQL 연결, CI 기준 확인 |
| 큼 | 3명 | 인증 Entity, Auth API, password reset API |
| 중요/복합 | 2~4명 | JWT 정책 반영, Google OAuth 설정, 통합 테스트 |

정철진은 Frontend 구조 생성, 로그인/회원가입 화면, Frontend-API 연동 기준에 우선 배치한다. 여도현은 Backend 기반, DB 연결, OAuth/API 검증 기준에 우선 배치한다. 김태준과 김민경은 인증 Backend 작업과 테스트/버퍼에 배치한다. 황스일과 김민정은 CI/문서/OAuth 설정/보고 입력값 정리에 배치한다.

AI coding tool을 사용하는 상황에서의 분업 기준은 [AI-assisted 분업 기준](./05_기획주간_5일차_자료/회의%20전%20자료/ai-assisted-분업-기준.md)을 회의 전 자료로 먼저 검토한다.

## Sprint 1 Backlog 확정 요약

| 순서 | 작업 | 담당 | 완료 조건 | 검증 방법 | 의존성 |
|---:|---|---|---|---|---|
| 1 | Spring Boot 기본 프로젝트 생성 | 여도현 + 김태준 + 김민경 | local에서 서버 실행 가능 | `./gradlew bootRun` | repository clone, Java 17/Gradle 환경 |
| 2 | MySQL 8.4 연결 확인 | 여도현 | test DB 연결 성공 | 연결 test 또는 간단 query | DB 실행 방식 결정, Spring Boot 기반 |
| 3 | Frontend 프로젝트 구조 생성 | 정철진 | 라우팅, 기본 layout 구성 | 브라우저 실행 | React + Next.js 구조 결정 |
| 4 | 인증 JPA Entity 구현 | 여도현 + 김태준 + 김민경 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` 구현 | `./gradlew build` | ERD v2.0, MySQL 연결 |
| 5 | 이메일 회원가입/로그인 API 구현 | 여도현 + 김태준 + 김민경 | signup/login/logout/refresh/me 구현 | Postman 또는 curl | 인증 Entity, JWT 정책 |
| 6 | 소셜 로그인 API 구현 | 여도현 + 김태준 + 김민경 | Google OAuth 시작/callback 구현 | Postman 또는 브라우저 | Google OAuth Client ID/Secret |
| 7 | 비밀번호 재설정 API 구현 | 여도현 + 김태준 + 김민경 | request/confirm, Google SMTP 발송, token hash/만료/사용 처리 | Postman 또는 curl, 수신 email 확인 | password reset Entity, SMTP 설정 |
| 8 | 로그인/회원가입 화면 구현 | 정철진 | 로그인/회원가입 route와 API 연동 | 브라우저 확인 | Frontend 구조, Auth API 응답 |
| 9 | CI 기준 확인 | 황스일 + 김민정 | skeleton 이후 workflow 생성 기준 합의 | CI 기준 문서 확인 | CI 기준 확정 v1.0 |
| 10 | 협업 규칙 확인 | 황스일 + 김민정 | root/docs 규칙과 commit 규칙 검토 | 파일 존재와 링크 확인 | AGENTS/CLAUDE/CONTRIBUTING |
| 11 | PR Template 생성 확인 | 황스일 | `.github/PULL_REQUEST_TEMPLATE.md` 존재 | 파일 존재 확인 | PR Template v1.0 |

회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림 Entity·API는 Sprint 1 Backlog에서 제외한다. 해당 기능은 전체 MVP reference contract로 유지하고, 다음 sprint Backlog 작성 시 ERD/API/기능 분해표의 해당 영역을 가져와 재분해한다.

## 일자별 분할

### Day 1 — 개발 기반 구축

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
|---|---|---|---|
| Spring Boot 기본 프로젝트 생성 | 여도현 + 김태준 + 김민경 | `./gradlew bootRun` 성공 | repository clone, Java 17 통일 |
| MySQL 8.4 연결 확인 | 여도현 | test DB 연결 성공 | Docker 등 실행 방식 통일 |
| Frontend 프로젝트 구조 생성 | 정철진 | 라우팅, 기본 layout 구성 | App Router vs Pages Router 결정 |
| CI 기준 확인 + AGENTS.md 등 규칙 확인 | 황스일 + 김민정 | CI workflow 기준 합의, root/docs 규칙 검토 완료 | 4일차 협업 하네스 |
| PR Template 생성 확인 | 황스일 | `.github/PULL_REQUEST_TEMPLATE.md` 생성 완료 | PR Template v1.0 |
| Google OAuth 앱 등록 | 황스일 + 김민정 | Client ID/Secret 발급, callback URL 등록 | Sprint 1 착수 전 필수 선결 조건 |

### Day 2 — 인증 Entity 구현

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
|---|---|---|---|
| 인증 JPA Entity 구현 | 여도현 + 김태준 + 김민경 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` 구현 | Day 1 MySQL 연결 |
| 로그인/회원가입 화면 레이아웃 작업 | 정철진 | 화면 UI 우선 구현 | 화면정의서_v3, API 없이 진행 가능 |
| JWT 정책 반영 확인 | 황스일 + 여도현 | access 30분, refresh 14일, hash 저장, rotation/logout invalidation 반영 확인 | Day 3 API 구현 전 확인 |

### Day 3 — 이메일 로그인 API + 소셜 로그인 API

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
|---|---|---|---|
| 이메일 회원가입/로그인 API 구현 | 여도현 + 김태준 + 김민경 | signup/login/logout/refresh/me 구현, Postman 검증 | 인증 Entity, JWT 정책 |
| 소셜 로그인 API 구현 | 여도현 + 김태준 + 김민경 | Google OAuth 시작/callback 구현, 브라우저 redirect 확인 | Google OAuth Client ID/Secret |
| 로그인/회원가입 화면 마무리 작업 | 정철진 | 화면 UI 완성, API 연동 준비 | API 응답 형식 확인 |

### Day 4 — 비밀번호 재설정 API + 화면 연동

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
|---|---|---|---|
| 비밀번호 재설정 API 구현 | 여도현 + 김태준 + 김민경 | reset 요청/확인, Google SMTP 실제 발송과 수신 email 확인 | `password_reset_tokens` |
| 로그인/회원가입 화면 API 연동 | 정철진 | 실제 API 호출, 브라우저 동작 확인 | Day 3 인증 API |
| Postman 컬렉션 정리 / API 문서화 | 황스일 + 김민정 | 인증 API 전체 요청/응답 예시 정리 | API 명세서 v2.3 |

### Day 5 — 통합 테스트 + 버퍼 + 보고 준비

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
|---|---|---|---|
| 인증 흐름 end-to-end 테스트 | 여도현, 정철진, 김민경, 김태준 | 회원가입 → 로그인 → refresh → `/auth/me` → 로그아웃 → 소셜 로그인 → 비밀번호 재설정 흐름 통과 | Day 1~4 전체 완료 |
| 버그 수정 / 버퍼 | 여도현 + 김태준 + 김민경 | 통합 테스트에서 발견된 Auth 이슈 수정 | 일정 여유 확보용 |
| 6일차 보고 입력값 정리 | 황스일 | Sprint 1 목표/Backlog/DoD/검증/blocker/invitation 후속 범위 요약 | 6일차 보고용 |

## Definition of Done 요약

| 작업 유형 | 완료 기준 |
|---|---|
| Backend | 관련 API가 실행되고 `./gradlew test` 또는 `./gradlew build`가 통과한다 |
| Frontend | 브라우저에서 화면 또는 route 확인이 가능하다 |
| DB | Auth table/entity 관계가 ERD v2.0과 코드에 반영되고 local 연결이 확인된다 |
| 문서 | 팀원이 문서만 보고 다음 행동을 할 수 있다 |
| PR | 작업 내용, 검증, 보안/승인 체크가 작성된다 |
| invitation | Sprint 1에는 구현하지 않고 Sprint 1.5 기준 문서에만 반영한다 |

## 검증 기준 요약

| 단계 | 명령/확인 | 적용 |
|---|---|---|
| 로컬 실행 | `./gradlew bootRun` | 서버 실행과 DB 연결 확인 |
| 최소 test | `./gradlew test` | PR 전 기본 검증 |
| build | `./gradlew build` | Spring Boot skeleton 안정화 후 merge 기준 |
| API 확인 | Postman 또는 curl | signup/login/logout/refresh/me/OAuth/password reset 검증 |
| DB 확인 | MySQL client, application log, 관련 API 응답 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` 확인 |
| 화면 확인 | 브라우저 실행 | 로그인/회원가입 화면과 API 연동 확인 |
| CI | GitHub Actions 초록 체크 | workflow 생성 이후 PR 자동 검증 |

## 의존성/blocker 정리

### 영역별 blocker

| 작업/영역 | 의존성 | blocker | 영향 | 해결 담당 | 결정 시점 | 후속 처리 |
|---|---|---|---|---|---|---|
| Repository | GitHub repo, 팀원 초대 | clone 안 되는 팀원 발생 가능 | 개발 착수 지연 | 황스일 | 6일차 전 | 팀원별 clone 확인 |
| Backend | Java, Gradle, Spring Boot | Java version 불일치 가능 | 서버 실행 실패 | 황스일 + 여도현 | Sprint 1 시작 전 | Java 17 기준 적용 |
| DB | MySQL 8.4 local 실행 | 일부 팀원 DB 실행 불가 가능 | API 개발 지연 | 여도현 | 6일차 전 | local DB 실행 방식 통일 후 공유 |
| Frontend | React + Next 구조 | App Router vs Pages Router 결정 필요 | 화면 작업 충돌 | 정철진 | Sprint 1 시작 전 | 팀 전체 합의 후 기본 구조 생성 |
| 인증 | JWT 정책 | 공식 정책을 코드/문서에 누락할 가능성 | login/logout/refresh/me 기준 불일치 | 여도현 + 황스일 | Day 2 | access 30분, refresh 14일, hash 저장, rotation/logout invalidation 반영 확인 |
| OAuth | Google Cloud 설정 | Client ID/Secret, callback URL 미등록 가능 | 소셜 로그인 지연 | 황스일 + 김민정 | Day 1 | secret 비노출 방식으로 등록 |
| Password reset | Google SMTP 설정 | SMTP credential 관리와 실제 발송 확인 필요 | password reset 검증 지연 | 여도현 + 황스일 | Day 4 전 | secret 비노출, 수신 email 확인 |
| invitation | 회사/프로젝트 멤버 추가 정책 | Sprint 1 범위에 섞일 가능성 | 범위 과확장 | 황스일 | 6일차 보고 전 | Sprint 1.5 registered-user app-internal invitation으로 분리 설명 |
| AWS | EC2/RDS/S3 사용 방향 | 실제 배포와 S3 파일 저장이 Sprint 1을 압박할 가능성 | Sprint 1 범위 과확장 | 황스일 | 확정됨 | 실제 배포와 S3 파일 저장은 Sprint 1 이후 |

### 기능별 blocker

| 기능 | 의존성 | blocker | 해결 담당 |
|---|---|---|---|
| 이메일 로그인 API | 인증 entity, JWT 기준 | token pair 발급과 `/auth/me` minimal 응답 누락 가능 | 여도현 + 황스일 |
| 소셜 로그인 API | `oauth_accounts`, Google OAuth 설정 | Google Cloud Console 앱 등록과 callback URL 필요 | 황스일 + 김민정 |
| 비밀번호 재설정 API | `password_reset_tokens`, Google SMTP | raw token 저장 금지, expires/used 처리, 실제 수신 email 확인 필요 | 여도현 + 황스일 |
| Frontend 로그인 화면 | React + Next 구조, Auth API | 로그인 API 응답 형식 확인 필요 | 정철진 + 여도현 |
| CI 적용 | Spring Boot skeleton, Gradle 명령 | 프로젝트 구조 생성 전이라 workflow 작성 불가 | 황스일 |
| invitation 후속 범위 | 회사/프로젝트 membership 정책 | Sprint 1.5 범위와 Sprint 2 후보를 섞을 가능성 | 황스일 |

## invitation 후속 범위

| 구분 | 기준 |
|---|---|
| Sprint 1 | invitation 구현 없음 |
| Sprint 1.5 포함 | `invitations` table, `PENDING / ACCEPTED / DECLINED / CANCELED / EXPIRED`, company invitation, project invitation, exact email lookup for registered users, received invitation list, accept/decline/cancel, membership creation on accept, invitation-related notification events |
| Sprint 1.5 제외 | actual email sending to invitees, invitation token link, unregistered-user invitation, signup/onboarding auto-accept, resend |
| Sprint 2 후보 | email template, delivery failure handling, batch cleanup/operations, unregistered onboarding 확장 |

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
invitation 후속 범위:
6일차 보고용 정리에 넘길 요약:
```

## 황스일 종합 항목

- Sprint 1 목표
- Sprint 1 Backlog
- 작업별 지원 담당, Owner, Reviewer
- Definition of Done
- 검증 기준
- 의존성/blocker
- invitation Sprint 1.5 후속 범위
- 최종 산출물 README
- 6일차 보고용 정리에 넘길 요약

## 마무리 회의 운영안

| 안건 | 확인할 내용 | 결과 |
|---|---|---|
| Backlog 공유 | Auth-only로 좁혀졌는지 | Sprint 1 Backlog 확정 |
| 담당 그룹 확인 | 지원 담당, Owner, Reviewer가 적절한지 | 배정 기준 확정 |
| 완료 조건 확인 | 말이 아니라 검증 가능한 기준인지 | Definition of Done 확정 |
| 의존성 확인 | Week 2 시작 blocker가 있는지 | blocker 목록 확정 |
| invitation 확인 | Sprint 1.5로 분리되었는지 | 후속 범위 확정 |
| 6일차 입력값 확인 | 보고용 정리와 Week 2 진입 점검에 필요한 요약이 있는지 | 6일차 준비 완료 |

## 6일차로 넘길 입력값

- [x] 최종 MVP 범위 후보: 기능 분해표/보류표/추적표 기준으로 요약
- [x] Sprint 1 목표: Auth-only 목표 문장 확정
- [x] Sprint 1 Backlog: 우선순위와 담당 그룹 포함
- [x] 작업별 담당 그룹/역할: 지원 담당, Owner, Reviewer 포함
- [x] 완료 조건: Definition of Done으로 정리
- [x] 검증 방법: Backend/Frontend/API/DB/문서/PR 기준으로 정리
- [x] 의존성/blocker 목록: 해결 담당과 결정 시점 포함
- [x] invitation 후속 범위: Sprint 1.5 registered-user app-internal invitation으로 분리
- [x] GitHub repository와 clone 확인 항목
- [x] Spring Boot local 실행 기준
- [x] React + Next 기본 구조 기준
- [x] MySQL 8.4 local 실행 기준
- [x] AWS 사용 범위: EC2/RDS/S3 사용 확정, 실제 배포와 S3 파일 저장 구현은 Sprint 1 이후
- [x] AGENTS.md / CLAUDE.md / CONTRIBUTING.md 기준
- [x] PR Template과 CI 기준
- [x] 제출 문서 상태: 최종 산출물 README 기준 17개 본문으로 정리

## 5일차 종료 체크리스트

- [x] Sprint 1 목표가 한 문장으로 정리되었다.
- [x] Sprint 1 Backlog가 작업 단위, 우선순위, 담당 그룹을 포함한다.
- [x] 담당 그룹/역할 분담이 지원 담당, Owner, Reviewer 기준으로 정리되었다.
- [x] Definition of Done이 작업 유형별로 작성되었다.
- [x] 검증 방법이 명령 또는 직접 확인 방식으로 작성되었다.
- [x] 의존성/blocker가 해결 담당과 결정 시점을 포함한다.
- [x] invitation이 Sprint 1 범위에서 제외되고 Sprint 1.5 후속 범위로 분리되었다.
- [x] 6일차 보고용 정리에 넘길 요약이 작성되었다.
