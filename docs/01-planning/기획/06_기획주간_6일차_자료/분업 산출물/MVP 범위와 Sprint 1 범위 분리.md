---
title: "MVP 범위와 Sprint 1 범위 분리"
type: planning-output
status: 6일차 수정본
version: "6일차 수정본 v2"
created: 2026-06-17
updated: 2026-06-17
tags:
  - project
  - team-project
  - acorn
  - planning
  - scope
  - sprint
  - presentation
aliases:
  - MVP Sprint 범위 분리
  - Sprint 1 범위 요약
  - 6일차 범위 요약
related:
  - "[[06_기획주간_6일차_발표정리_리스크점검]]"
  - "[[기능-분해표]]"
  - "[[보류-기능-최종표]]"
  - "[[Sprint-1-상세계획]]"
context: "6일차 발표자료에 반영할 전체 MVP reference와 Sprint 1 Auth-only 구현 범위 구분"
owner: 황스일
---

# MVP 범위와 Sprint 1 범위 분리

## 전달 목적

이 문서는 6일차 발표자료를 준비하는 팀원들에게 전달할 **범위 요약 입력값**이다.

발표자료에서는 전체 MVP로 설명할 기능과 Week 2 Sprint 1에서 실제 구현할 기능을 분리해서 말해야 한다. 기능 분해표, API, ERD, 화면정의서에 남아 있는 전체 MVP 항목은 제품의 최종 reference이며, Sprint 1의 구현 약속이 아니다.

## 발표에 넣을 핵심 메시지

```text
Threadia의 전체 MVP는 회사/프로젝트/이슈/문서/채팅/알림까지 포함하지만,
Sprint 1 구현 범위는 Auth-only로 제한한다.
```

```text
Sprint 1은 개발 기반 구축과 인증 흐름 end-to-end 검증에 집중하고,
도메인 기능은 후속 sprint reference contract로 유지한다.
```

```text
Invitation은 Sprint 1에 넣지 않고 Sprint 1.5의 등록 사용자 대상 app-internal invitation으로 분리한다.
```

## 범위 구분 표

| 구분 | 발표에서 말할 범위 | 구현 commitment | 주요 근거 문서 |
|---|---|---|---|
| 전체 MVP reference | A/P/C 역할, 회사/부서/구성원, 프로젝트, Sprint, 이슈/칸반, 문서/회의록, 문서-이슈 연결, 프로젝트 채팅, 1:1 DM, 쓰레드, 알림, 프로필, 그래프 뷰 | Sprint 1 구현 약속이 아니라 후속 sprint 설계 기준 | 기능 분해표, 화면정의서, API 명세서, ERD |
| Sprint 1 | Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, 이메일 회원가입/로그인/로그아웃, Google OAuth, 비밀번호 재설정, 로그인/회원가입 화면 | Week 2에 실제 구현하고 local에서 검증할 범위 | Sprint 1_detail, Sprint1_Backlog |
| Sprint 1.5 | 등록 사용자 대상 app-internal invitation, `invitations` table, 기본 7일 만료, lazy expiration, 수신 초대 목록, accept/decline/cancel, accept 후 membership 생성 | Sprint 1 이후 첫 후속 범위 | 6일차 수정본 기능 분해표, API 명세서, ERD, Sprint 1.5 Backlog |
| Sprint 2 이후 | 회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림 구현, invitation email/token link/unregistered onboarding/resend/batch cleanup 고도화 | 후속 sprint 후보. 발표에서 Sprint 1 범위처럼 말하지 않음 | 기능 분해표, 보류 기능 최종표 |
| 조건부 확장 | 활동 피드, 통합 검색, 프로젝트 홈/대시보드 | Sprint 1 기본 범위 제외. 여유와 기술 조건 충족 시 검토 | 보류 기능 최종표 |
| 선택/보류/제외 | 파일/이미지 첨부, 플랫폼 관리자 F/S15, NoSQL 알림/활동 저장소, 칸반 컬럼 커스터마이징, 동시 문서 편집, 다자간 DM, Redis 원본 저장, 전용 검색 엔진 | Sprint 1 제외. 일부는 Sprint 2 후보, 일부는 제외 | 보류 기능 최종표 |

## Sprint 1 구현 범위

Sprint 1은 아래 항목만 실제 구현 범위로 발표한다.

| 영역 | Sprint 1 항목 | 완료 기준 |
|---|---|---|
| Backend 기반 | Spring Boot 기본 프로젝트, Java 17, Gradle, MySQL 8.4 연결 | local에서 서버 실행과 DB 연결 확인 |
| Frontend 기반 | React + Next.js 구조, 로그인/회원가입 route와 기본 layout | 브라우저에서 화면 또는 route 확인 |
| DB | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` | 인증 JPA Entity와 migration 범위 일치 |
| Email Auth | 이메일 회원가입, 로그인, 로그아웃, `/auth/me` | Postman 또는 curl로 인증 흐름 확인 |
| OAuth | Google OAuth 로그인 | 브라우저 redirect와 callback 흐름 확인 |
| Password Reset | 비밀번호 재설정 요청/확인 | Google SMTP 실제 발송과 수신 email 확인 |
| Token | access token 30분, refresh token 14일, refresh token hash 저장, rotation, logout invalidation, access token memory + refresh token `HttpOnly`, `SameSite=Lax` cookie 권장 | API 구현과 검증 기준에 반영 |
| 협업 기준 | CI 기준 확인, PR Template, AGENTS/CLAUDE/CONTRIBUTING 기준 | PR 전 검증과 문서 기준 확인 |

## Sprint 1에서 제외한 범위

아래 항목은 전체 MVP에는 남기지만 Sprint 1 구현 범위에서는 제외한다.

| 제외 항목 | 제외 이유 | 후속 처리 |
|---|---|---|
| 회사/부서/구성원 | Auth 이후 권한/소속 모델이 필요해 1주 인증 구현과 섞이면 범위가 커진다 | Sprint 2 이후 도메인 구현 |
| 프로젝트/Sprint | 회사/구성원 모델과 연결되어 Auth-only 범위를 넘는다 | Sprint 2 이후 도메인 구현 |
| 이슈/칸반 | 프로젝트와 권한 체계가 선행되어야 한다 | Sprint 2 이후 도메인 구현 |
| 문서/회의록/그래프 | 이슈/문서 연결과 권한 검증이 필요하다 | Sprint 2 이후 도메인 구현 |
| 프로젝트 채팅/1:1 DM/쓰레드 | 실시간 처리와 알림 정책이 함께 필요하다 | Sprint 2 이후 도메인 구현 |
| 알림 | 이벤트 원장, 수신자, preference 정책이 필요하다 | Sprint 2 이후 도메인 구현 |
| 프로필 수정 | Sprint 1의 `/auth/me`는 최소 user 정보만 반환한다 | 후속 사용자 기능에서 구현 |
| invitation | 인증과 membership 생성 흐름을 분리해야 한다 | Sprint 1.5 registered-user app-internal invitation |
| 실제 AWS 배포/S3 파일 저장 | Sprint 1 일정 리스크가 커진다 | Sprint 1 이후 배포/파일 저장 단계 |

## 전체 MVP reference로 유지할 항목

발표에서 제품의 전체 방향을 설명할 때는 아래 항목을 MVP reference로 사용할 수 있다.

| MVP 영역 | 설명 | 주의할 표현 |
|---|---|---|
| 역할 기반 협업 | A 일반 사용자, P 프로젝트 관리자, C 회사 관리자 역할을 기준으로 기능을 분리한다 | `Sprint 1에서 구현한다`가 아니라 `전체 MVP 기준이다`로 표현 |
| 회사/프로젝트 관리 | 회사, 부서, 구성원, 프로젝트, 프로젝트 멤버와 권한을 관리한다 | Auth-only 범위와 분리 |
| 업무 관리 | 이슈, 칸반 5컬럼, Sprint 백로그와 상태 변경을 제공한다 | 후속 sprint 구현 범위로 표현 |
| 문서/회의록 | Markdown 문서와 회의록을 작성하고 이슈와 연결한다 | 후속 sprint 구현 범위로 표현 |
| 채팅/DM/쓰레드 | 프로젝트 채팅, 1:1 DM, 대상별 쓰레드를 제공한다 | 실시간/알림 리스크와 함께 후속 범위로 표현 |
| 알림 | MySQL 기반 알림 목록과 읽음 처리, global preference를 제공한다 | NoSQL 저장소는 Sprint 1 제외로 표현 |
| 그래프 뷰 | 문서·회의록과 이슈 관계를 그래프로 조회한다 | 제품 가치 설명용으로 사용 |

## Sprint 1.5 invitation 구분

Invitation은 Sprint 1과 Sprint 2 사이의 별도 후속 범위로 설명한다.

| 구분 | 포함 | 제외 |
|---|---|---|
| Sprint 1.5 invitation | 등록 사용자 대상 app-internal invitation, company invitation, project invitation, exact email lookup, received invitation list, accept/decline/cancel, 기본 7일 만료와 lazy expiration, accept 후 membership 생성, invitation 관련 notification event | 초대 email 발송, token link, 미가입자 invitation, signup/onboarding auto-accept, resend, email template, delivery failure handling, 필수 batch scheduler |

발표에서는 아래처럼 말한다.

```text
초대는 Sprint 1 인증 구현과 섞지 않고, Sprint 1.5에서 등록 사용자 대상 앱 내부 초대로 먼저 구현한다.
이메일 초대 링크와 미가입자 onboarding은 Sprint 2 이후 고도화 후보로 분리한다.
```

## 발표자료 반영 위치

| 발표 파트 | 반영할 내용 | 권장 분량 |
|---|---|---|
| 프로젝트 목표 | 전체 MVP가 해결하려는 협업 흐름 | 1 slide |
| 전체 MVP 범위 | 기능 분해표 기준 제품 범위 | 1 slide |
| Sprint 1 범위 | Auth-only 구현 범위와 제외 범위 | 1 slide |
| Roadmap | Sprint 1, Sprint 1.5, Sprint 2 이후 구분 | 1 slide |
| 리스크/대응 | 범위 과확장 방지, invitation 분리, AWS 분리 | 1 slide |

## 슬라이드 문장 후보

### 범위 구분

```text
전체 MVP는 협업 플랫폼의 완성 흐름을 보여주는 reference이고,
Sprint 1은 그중 인증 기반을 먼저 검증하는 실행 범위다.
```

```text
Sprint 1에서 회사/프로젝트/이슈까지 동시에 구현하지 않고,
Auth-only로 제한해 통합 검증 가능한 기반을 먼저 만든다.
```

### 후속 범위

```text
Sprint 1.5에서는 등록 사용자 대상 invitation을 붙이고,
Sprint 2 이후에는 회사/프로젝트/이슈/문서/채팅/알림을 순차 구현한다.
```

```text
보류 기능은 삭제한 기능이 아니라 일정과 기술 리스크를 고려해 후속 검토로 분리한 기능이다.
```

## 발표자료 반영 체크리스트

- [ ] 전체 MVP와 Sprint 1 구현 범위가 같은 표에서 구분되어 있다.
- [ ] Sprint 1은 Auth-only로 표현되어 있다.
- [ ] Sprint 1 DB 범위가 `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens`로 제한되어 있다.
- [ ] Sprint 1 JWT 기준이 access 30분, refresh 14일, hash 저장, rotation, logout invalidation, refresh cookie 권장안으로 표현되어 있다.
- [ ] 회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림은 후속 sprint 범위로 표현되어 있다.
- [ ] Invitation은 Sprint 1이 아니라 Sprint 1.5로 표현되어 있다.
- [ ] Invitation 만료는 기본 7일과 lazy expiration으로 표현되어 있다.
- [ ] Sprint 2 이후 후보가 Sprint 1 구현 약속처럼 보이지 않는다.
- [ ] 조건부 확장과 보류 기능이 삭제된 기능처럼 보이지 않는다.
- [ ] 실제 AWS 배포와 S3 파일 저장은 Sprint 1 이후로 표현되어 있다.

## 발표자료에 넣을 표 추천

발표자료에 표를 1개만 넣는다면 아래 표를 추천한다.

| 구분 | 이번 발표에서의 의미 | 구현 시점 |
|---|---|---|
| 전체 MVP reference | Threadia가 최종적으로 제공할 협업 플랫폼 기능 묶음 | 후속 sprint 포함 전체 계획 |
| Sprint 1 | 개발 기반 구축과 인증 흐름 end-to-end 검증 | Week 2 |
| Sprint 1.5 | 등록 사용자 대상 app-internal invitation | Sprint 1 이후 |
| Sprint 2 이후 | 회사/프로젝트/이슈/문서/채팅/알림과 invitation 고도화 | 후속 개발 범위 |
| 보류/선택 기능 | 일정과 기술 리스크를 고려해 별도 검토할 항목 | 필요 시 재검토 |

## 기준 문서

- `06_기획주간_6일차_자료/최종 산출물/README.md`
- `06_기획주간_6일차_자료/최종 산출물/기능-분해표.md`
- `06_기획주간_6일차_자료/최종 산출물/API-명세서.md`
- `06_기획주간_6일차_자료/최종 산출물/ERD.md`
- `06_기획주간_6일차_자료/최종 산출물/Sprint-관리/Sprint-1-상세계획.md`
- `06_기획주간_6일차_자료/최종 산출물/Sprint-관리/Sprint-1-Backlog.md`
