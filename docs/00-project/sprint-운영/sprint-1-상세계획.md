# Sprint 1 Detail 6일차 수정본 v2

> **작성자**: Threadia 팀  
> **작성일**: 2026-06-16  
> **최종 수정**: 2026-06-29
> **버전**: 개발주간 schema.sql 작업 반영 v1
> **상태**: 확정
> **태그**: sprint-1, plan, auth
> **수정 근거**: Threadia-기획문서-검수리포트.md, DB-schema-관리-정책.md, Sprint 1 schema.sql 작업 반영

> 기준: `05_기획주간_5일차_Sprint1_Backlog_검증기준.md` + `5일차_오전회의.md`
> ERD 반영: `ERD v2.0.md` + `기능-시나리오-API-ERD 추적표 v1.0.md` + `보류 기능 최종표 v1.0.md` + `API 명세서 v2.0.md`
> **6일차 수정본 v2 기준**: Sprint 1 범위를 로그인 인증 구현까지로 축소. 회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림 Entity·API는 Sprint 1에서 제외하고 다음 스프린트로 이동.

---

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows다. DB schema는 `docs/03-design/ERD.md`와 `db/schema.sql`을 기준으로 관리하고, Spring Boot/JPA는 schema 생성이 아니라 mapping 검증에 사용한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.


## Sprint 1 목표

```
Sprint 1에서는 Spring Boot, React + Next.js, MySQL 8.4 기반을 구축하고,
이메일 로그인, 소셜 로그인(Google OAuth), 비밀번호 재설정을 포함한
인증 흐름을 local 환경에서 끝까지 검증 가능한 상태로 만든다.
```

---

## Sprint 1 확정 범위 (v0.2 축소)

| 구분 | 항목 |
| --- | --- |
| Sprint 1 확정 | 개발 기반 구축(Spring Boot, MySQL, Frontend), 이메일 회원가입/로그인/로그아웃, 소셜 로그인(Google OAuth), 비밀번호 재설정, 로그인/회원가입 화면 |
| Sprint 1 제외 → 다음 스프린트 | 회사/부서/구성원, 프로젝트, Sprint, 이슈/칸반, 문서/회의록, 문서-이슈 연결, 프로젝트 채팅, 1:1 DM, 쓰레드, 알림, 프로필 수정 |
| 조건부 확장 (변경 없음) | 활동 피드, 통합 검색, 프로젝트 홈/대시보드 |
| Sprint 1 이후 (변경 없음) | 파일/이미지 첨부(S3), AWS 실제 배포 |
| 제외 (변경 없음) | Redis 원본 저장, NoSQL 알림/활동 저장소, Typesense/Meilisearch, Elasticsearch, Supabase Realtime |

> 회사/프로젝트/이슈 등 도메인 Entity·API는 ERD v2.0·API 명세서 v2.0에는 이미 Sprint 1 확정으로 적혀 있지만, 실제 1주 분량으로는 과하다고 판단해 이번 Backlog에서 다음 스프린트로 미뤘다. **6일차 수정본은 Sprint 1을 Auth-only로 유지하고, invitation은 Sprint 1.5 별도 범위로 분리한다.**

---

## 작업 묶음 1. Sprint 1 Backlog 기준

| 작업 | 순서 | 권장 담당 그룹 | 완료 조건 | 검증 방법 | 의존성 |
| --- | --- | --- | --- | --- | --- |
| Spring Boot 기본 프로젝트 생성 | 1 | 여도현 + 김태준 | local에서 서버 실행 가능 | `./gradlew bootRun` | repository clone, Java/Gradle 환경 |
| MySQL 8.4 연결 확인 | 2 | 여도현 | test DB 연결 성공 | 연결 test 또는 간단 query | DB 실행 방식 결정, Spring Boot 기반 |
| ERD.md 기준 db/schema.sql 생성 | 2-A | 김민정 + 김태준 + 김민경 | `docs/03-design/ERD.md` 완료 기준으로 MySQL 8.4용 `db/schema.sql` 생성. 전체 MVP DDL 기준을 준비하되 Sprint 1 검증은 `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` 중심 | schema.sql 실행 또는 검토, Auth 4개 table/column/constraint 정합성 확인 | ERD.md 상세 설계 완료, `db/.gitkeep`, MySQL 8.4 연결 확인 |
| Frontend 프로젝트 구조 생성 | 3 | 정철진 | 라우팅, 기본 layout 구성 | 브라우저 실행 | React + Next.js App Router vs Pages Router 결정 |
| 인증 JPA Entity 구현 | 4 | 여도현 + 김태준 + 김민경 | ERD v2.0 기준 `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` JPA Entity 클래스 구현. `users.role` 필드 없이 구현, `users.password_hash`는 소셜 전용 계정 NULL 허용 | `./gradlew build` 통과 | ERD v2.0, `db/schema.sql`, MySQL 연결 확인 |
| 이메일 회원가입/로그인 API 구현 | 5 | 여도현 + 김태준 + 김민경 | API 명세서 v2.3 기준 `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me` 구현. email `trim + lowercase`, `ACTIVE` 사용자 인증 기준 반영 | Postman 또는 curl | 인증 Entity, JWT 정책 결정 |
| 소셜 로그인 API 구현 (Google OAuth) | 6 | 여도현 + 김태준 + 김민경 | API 명세서 v2.3 기준 `GET /oauth2/authorization/{provider}`, `GET /api/v1/auth/oauth2/callback/{provider}` 구현. 이메일만 같은 blind auto-link 금지 | Postman 또는 브라우저 | 인증 Entity, Google OAuth Client ID/Secret 발급 완료 |
| 비밀번호 재설정 API 구현 | 7 | 여도현 + 김태준 + 김민경 | API 명세서 v2.3 기준 `POST /api/v1/auth/password-reset/request`, `POST /api/v1/auth/password-reset/confirm` 구현. confirm 성공 시 기존 `refresh_tokens` 전부 revoke. 이메일 발송은 Sprint 1에서 Google SMTP 실제 발송으로 검증하며, log/local console은 보조 debugging evidence로만 사용 | Postman 또는 curl | password_reset_tokens, refresh_tokens Entity |
| 로그인/회원가입 화면 구현 | 8 | 정철진 | 로그인 화면, 회원가입 화면 route 존재, 로그인 API 연동 완료 | 브라우저 확인 | Frontend 구조, 로그인 API 응답 형식 |
| GitHub Actions CI 도입 기준 확인 | 9 | 황스일 + 김민정 | `./gradlew test` 우선, skeleton 이후 workflow 생성 기준 합의 | CI 기준 문서 확인 | CI 기준 확정 v1.0, Spring Boot skeleton 생성 이후 |
| AGENTS.md/CLAUDE.md/CONTRIBUTING.md 최소 규칙 확정 | 10 | 황스일 + 김민정 | root 문서와 docs 하위 규칙, commit 규칙 검토 완료 | 파일 존재와 링크 확인 | 4일차 협업 하네스 최신화 |
| PR Template 실제 생성 여부 결정 | 11 | 황스일 | `.github/PULL_REQUEST_TEMPLATE.md` 생성 완료 | 파일 존재 확인 | PR Template v1.0 |

> 회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림 Entity·API는 이 Backlog에서 제외했다. 다음 스프린트 Backlog 작성 시 ERD v2.0 영역 2~4(이전 v0.1 버전 참고)를 그대로 가져와 쓰면 된다.

---

## 작업 묶음 1-A. 일자별 분할 (Day 1~5)

> 가정: Sprint 1 = Week 2, 5영업일 운영.
> 인원: 황스일, 여도현, 정철진, 김민정, 김민경, 김태준 6명.
> 배치 원칙: 범위가 인증으로 좁혀지면서 Backend 페어를 둘로 나눌 필요가 없어졌다. **인증 Backend**: 여도현 + 김태준 + 김민경이 entity부터 3개 API까지 순서대로 담당. **Frontend**: 정철진 단독. **협업/지원**: 황스일 + 김민정 — CI/문서/OAuth 설정/통합테스트 보조. 작업량이 줄어든 만큼 Day 4~5는 테스트와 버퍼에 더 쓴다.

### Day 1 — 개발 기반 구축

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| (1) Spring Boot 기본 프로젝트 생성 | 여도현 + 김태준 + 김민경 | `./gradlew bootRun` 성공 | repository clone, Java version 통일 |
| (2) MySQL 8.4 연결 확인 | 여도현 | test DB 연결 성공 | Docker 등 실행 방식 통일 |
| ERD.md 기준 db/schema.sql 생성 | 김민정 + 김태준 + 김민경 | 전체 MVP 기준 `db/schema.sql` 생성, Sprint 1 Auth 4개 table DDL 기준 확인 | ERD.md 상세 설계 완료 후 진행. Reviewer: 황스일 |
| (3) Frontend 프로젝트 구조 생성 | 정철진 | 라우팅, 기본 layout 구성 | App Router vs Pages Router 결정 |
| (9)(10) CI 기준 확인 + AGENTS.md 등 규칙 확정 | 황스일 + 김민정 | CI workflow 기준 합의, root/docs 규칙 검토 완료 | 4일차 협업 하네스 |
| (11) PR Template 생성 확인 | 황스일 | `.github/PULL_REQUEST_TEMPLATE.md` 생성 완료 | PR Template v1.0 |
| Google OAuth 앱 등록 | 황스일 + 김민정 | Google Cloud Console Client ID/Secret 발급, callback URL 등록 | Sprint 1 착수 전 필수 선결 조건 |

### Day 2 — 인증 Entity 구현

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| (4) 인증 JPA Entity 구현 | 여도현 + 김태준 + 김민경 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` 구현, `./gradlew build` 통과 | Day 1 MySQL 연결 확인, `db/schema.sql` 생성 |
| 로그인/회원가입 화면 레이아웃 작업 | 정철진 | 화면 UI만 우선 구현 (API 연동 전) | 화면정의서_v3, API 없이 진행 가능 |
| JWT 정책 결정 회의 | 황스일 + 여도현 | access/refresh token 만료 시간, 갱신 전략, 저장 위치, password reset/status change refresh token revoke, UTC 시간 기준 결정 | Day 3 API 구현 전 확정 필요 |

### Day 3 — 이메일 로그인 API + 소셜 로그인 API

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| (5) 이메일 회원가입/로그인 API 구현 | 여도현 + 김태준 + 김민경 | signup/login/logout/me 구현, Postman 검증 | 인증 Entity, JWT 정책 결정 (Day 2 완료분) |
| (6) 소셜 로그인 API 구현 (Google OAuth) | 여도현 + 김태준 + 김민경 | OAuth 시작/callback 구현, 브라우저로 redirect 확인 | Google OAuth Client ID/Secret (Day 1 완료분) |
| 로그인/회원가입 화면 마무리 작업 | 정철진 | 화면 UI 완성, API 연동 준비 | API 응답 형식 확인 (황스일 + 김민정 지원) |

### Day 4 — 비밀번호 재설정 API + 화면 연동

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| (7) 비밀번호 재설정 API 구현 | 여도현 + 김태준 + 김민경 | reset 요청/확인 흐름 구현, Google SMTP 실제 발송과 수신 email 확인 | password_reset_tokens Entity |
| (8) 로그인/회원가입 화면 API 연동 | 정철진 | 로그인/회원가입 화면에서 실제 API 호출, 브라우저에서 동작 확인 | Day 3 인증 API 완료 |
| Postman 컬렉션 정리 / API 문서화 | 황스일 + 김민정 | 인증 API 전체 요청/응답 예시 정리 | API 명세서 v2.0 |

### Day 5 — 통합 테스트 + 버퍼 + 6일차 준비

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| 인증 흐름 end-to-end 테스트 | 전체 | 회원가입 → 로그인 → 로그아웃 → 소셜 로그인 → 비밀번호 재설정 전체 시나리오 통과 | Day 1~4 전체 완료 |
| 버그 수정 / 버퍼 | 여도현 + 김태준 + 김민경 | 통합 테스트에서 발견된 이슈 수정 | 일정 여유 확보용 |
| 6일차 보고 입력값 정리 | 황스일 | Sprint 1 목표/Backlog/DoD/검증/blocker 요약 | 6일차 보고용 |

> 작업량이 줄어든 만큼 Day 5는 의도적으로 통합 테스트와 버퍼로 비워뒀다. 일정이 당겨지면 다음 스프린트 도메인 Entity(회사/프로젝트/이슈) 설계를 미리 검토하는 시간으로 써도 된다.

---

## 작업 묶음 2. Definition of Done

| 작업 유형 | 완료 기준 |
| --- | --- |
| Backend | 관련 API가 실행되고 `./gradlew test` 또는 `./gradlew build`가 통과 |
| Frontend | 브라우저에서 화면 또는 route 확인 가능 |
| DB | `db/schema.sql`과 table/entity 관계가 ERD v2.0과 코드에 반영되고 연결 확인 |
| 문서 | 팀원이 문서만 보고 다음 행동을 할 수 있음 |
| PR | 작업 내용, 검증, 보안/승인 체크가 작성됨 |

---

## 작업 묶음 3. 검증 기준

| 단계 | 명령/확인 | 적용 |
| --- | --- | --- |
| 로컬 실행 | `./gradlew bootRun` | 서버 실행 확인 |
| 최소 test | `./gradlew test` | PR 전 기본 검증 |
| build | `./gradlew build` | 안정화 후 merge 기준 |
| API 확인 | Postman 또는 curl | Backend 기능 검증 |
| 화면 확인 | 브라우저 실행 | Frontend 기능 검증 |
| CI | GitHub Actions 초록 체크 | PR 자동 검증 |

---

## 작업 묶음 4. 의존성/blocker 정리

### 영역별 blocker

| 작업/영역 | 의존성 | blocker | 영향 | 해결 담당 | 결정 시점 | 후속 처리 |
| --- | --- | --- | --- | --- | --- | --- |
| Repository | GitHub repo, 팀원 초대 | clone 안 되는 팀원 발생 가능 | 개발 착수 지연 | 황스일 | 6일차 전 | 팀원별 clone 확인 |
| Backend | Java, Gradle, Spring Boot | Java 17 기준 통일 필요 | 서버 실행 실패 | 황스일 + 여도현 | Sprint 1 시작 전 | Java 17 기준 적용 |
| DB | MySQL 8.4 local 실행 | 일부 팀원 DB 실행 불가 가능 (Mac/Windows OS별 방식 차이) | API 개발 지연 | 여도현 | 6일차 전 | local DB 실행 방식 통일 후 공유 |
| DB schema | ERD.md 상세 설계 완료, `db/schema.sql` 생성 | schema.sql 생성이 늦어지면 Auth Entity와 실제 table 기준이 어긋날 수 있음 | 인증 Entity 구현 지연 또는 재작업 | 김민정 + 김태준 + 김민경 | Day 1 | 황스일 review 후 Day 2 Entity 구현 기준으로 사용 |
| Frontend | React + Next 구조 | App Router vs Pages Router 미결 | 화면 작업 충돌 | 정철진 | Sprint 1 시작 전 | 팀 전체 합의 후 기본 구조 먼저 확정 |
| 인증 | JWT/Auth 정책 | access token 30분, refresh token 14일, hash 저장, rotation, logout/password reset/status change invalidation, access token memory + refresh token HttpOnly SameSite=Lax cookie 권장, email `trim + lowercase`, ACTIVE 사용자 인증, OAuth blind auto-link 금지, UTC 시간 기준 확정 | API 구현 방향 충돌 방지 | 여도현 + 황스일 | Day 2까지 반영 | API/ERD/Backend DoD 기준과 동기화 |

### 기능별 blocker

| 기능 | 의존성 | blocker | 해결 담당 |
| --- | --- | --- | --- |
| 소셜 로그인 API | oauth_accounts entity, Google OAuth 설정 | Google Cloud Console 앱 등록 + Client ID/Secret 발급 필요, callback URL 등록 필요 | 황스일 + 김민정 (Day 1) |
| 비밀번호 재설정 API | password_reset_tokens entity | Sprint 1은 Google SMTP 실제 발송으로 검증한다. AWS SES는 Sprint 1 범위가 아니며 배포/운영 단계에서 도입 여부를 결정한다 | 여도현 + 황스일 |
| 이메일 로그인 API | 인증 entity, JWT 기준 | access token 30분, refresh token 14일, hash 저장, rotation, logout/password reset/status change invalidation, email 정규화, ACTIVE 사용자 인증, refresh cookie 기준 반영 | 여도현 + 황스일 |
| Frontend 로그인 화면 | React + Next 구조, 로그인 API | 로그인 API 응답 형식 확인 필요 | 정철진 + 여도현 |
| CI 적용 | Spring Boot skeleton, Gradle 명령 | 프로젝트 구조 생성 전이라 workflow 작성 불가 | 황스일 |

---

## 주의 포인트 / 팀원과 의논해야 할 항목

### 1. 범위 축소를 팀 전체가 합의해야 한다

이번 Backlog는 회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림을 전부 Sprint 1에서 빼고 인증만 남겼다. ERD v2.0과 API 명세서 v2.0에는 이 기능들이 이미 "Sprint 1 확정"으로 적혀 있기 때문에, 이 변경은 임의로 진행하지 말고 **5일차 회의에서 팀 전체 합의를 받고 6일차 보고에도 반영**해야 한다. 합의 없이 진행하면 6일차 보고 내용과 실제 진행이 어긋난다.

---

### 2. 소셜 로그인 — Google OAuth 앱 등록이 Day 1 선결 조건

Google Cloud Console에서 OAuth 앱 등록 + Client ID/Secret 발급, callback URL 등록(`http://localhost:8080/api/v1/auth/oauth2/callback/google`)을 Day 1에 끝내지 못하면 Day 3 소셜 로그인 API 작업이 막힌다. 황스일 + 김민정이 Day 1에 처리하도록 배정했다.

---

### 3. 비밀번호 재설정 — 이메일 발송 방식 합의 필요

Sprint 1 비밀번호 재설정은 Google SMTP 실제 발송을 공식 검증 방법으로 사용한다. reset token 생성, token hash 저장, 만료, used-state handling, 수신 email을 확인하며, application log/local console 출력은 보조 debugging evidence로만 사용한다. AWS SES는 Sprint 1 범위가 아니며 배포/운영 단계에서 도입 여부를 결정한다.

---

### 4. users.role 필드 금지

ERD v2.0 불변식: `users`에 `role` 필드를 만들지 않는다. 권한 판정 체계(`company_members.role`, `project_members.role`)는 다음 스프린트에서 회사/프로젝트 entity가 들어갈 때 적용되지만, 인증 entity를 짤 때도 `users`에 `role`을 넣지 않도록 미리 주의해야 한다.

---

### 5. JWT 만료/갱신 방식 — 6일차 수정본 기준 확정

JWT 정책은 access token 30분, refresh token 14일, refresh token hash 저장, refresh 성공 시 rotation, logout 시 invalidation으로 확정한다. Browser 저장 기준은 access token memory, refresh token `HttpOnly`, `SameSite=Lax` cookie 권장안으로 두며 HTTPS 환경에서는 `Secure`를 사용한다. Day 2에는 이 기준을 인증 API 구현과 Frontend 연동에 반영한다.

---

### 6. Java version 기준 — Java 17 확정

Sprint 1 local development와 CI는 Java 17 기준으로 통일한다. Spring Boot 3.x의 최소 요구 version은 Java 17이고, Threadia는 현재 더 높은 Java version 전용 기능이 필요하지 않으므로 future upgrade는 별도 결정으로 둔다.

---

### 7. React + Next.js App Router vs Pages Router 결정 필요

Next.js 13 이상 기준으로 App Router가 기본이지만 팀 경험에 따라 Pages Router가 더 안정적일 수 있다. 정철진이 Frontend 구조를 만들기 전에 팀 전체와 방향을 합의해야 한다.

---

### 8. MySQL 8.4 실행 방식 통일

Mac/Windows 혼재 시 설치 방법이 달라진다. Docker로 통일하는 게 재현성이 높지만 Docker 미설치 팀원이 있을 수 있다. 여도현이 실행 방식을 먼저 결정하고 공유해야 한다.

---

### 9. 작업량이 줄어든 만큼 Day 5는 여유 시간

인증만 구현하면 되므로 6명이 5일을 다 채우지 않을 가능성이 높다. Day 5를 통합 테스트와 버퍼로 비워뒀고, 여유가 있으면 다음 스프린트 도메인 Entity(회사/프로젝트/이슈) 설계를 미리 검토하는 데 쓰는 것을 제안한다.

---


## 작업 묶음 1-B. Sprint 1.5 Invitation Backlog

> Sprint 1은 Auth-only로 유지한다. 아래 작업은 Sprint 1 완료 후 별도 Sprint 1.5로 착수한다.

| 작업 | 권장 담당 | 완료 조건 | 검증 방법 | 의존성 |
|---|---|---|---|---|
| `invitations` DB/entity 구현 | 여도현 + 김태준 + 김민경 | ERD 기준 `invitations`와 users/companies/company_members 관계 mapping 완료 | repository test 또는 `db/schema.sql` 기준 DB 확인 | Sprint 1 Auth DB 완료, 회사 seed와 company_members reference schema 확인 |
| 등록 사용자 lookup API | 여도현 + 김민경 | exact email lookup으로 등록 사용자만 반환 | curl 또는 Postman | `users` 구현 |
| 회사 invitation API | 김태준 + 김민경 | create/list, duplicate pending, already member, 7일 expiresAt 처리 | curl 또는 Postman | `invitations`, `company_members` |
| 내 invitation 목록과 accept/decline/cancel | 여도현 + 김태준 + 김민경 | 수락 시 membership 생성, decline/cancel/expired 실패 처리 | API 정상/실패 케이스 | invitation 상태 전이 기준 |
| 화면 S14 연결 | 정철진 | pending/accepted/declined/canceled/expired 상태 표시 | 브라우저 확인 | API 응답 형식 확정 |
| 문서와 DoD 동기화 | 황스일 + 김민정 | API, ERD, 화면, 추적표, DoD가 seed bootstrap, notification 분리, UTC, email 정규화, OAuth link, refresh token revoke, ACTIVE 인증 기준과 충돌하지 않음 | 문서 리뷰 | 검수리포트 이슈 1, 3, 4, 9 |

### Sprint 1.5 완료 기준

- Sprint 1 Auth-only 산출물을 건드리지 않고 invitation 기능을 별도 범위로 검증한다.
- `expiresAt`은 기본 7일 값으로 응답하며 null 예시를 쓰지 않는다. 서버 생성/만료 시각은 UTC 기준으로 생성·저장·비교하고 API 응답은 ISO-8601 UTC `Z` 형식을 사용한다.
- membership은 invitation accept 시점에만 생성한다.
- Sprint 1.5 검증용 최초 회사와 `COMPANY_ADMIN`은 local/dev/demo seed로 준비하고, `POST /api/v1/companies`는 Sprint 1.5 구현 범위에서 제외한다.
- actual email sending, token link, 미가입 사용자 초대, signup auto-accept, resend, 필수 batch scheduler는 제외 범위로 남긴다.

---

## Phase B invitation 반영 메모

> **생성일**: 2026-06-16
> **상태**: 확정
> **기준**: `.sisyphus/evidence/task-5-invitation-impact-matrix.md`의 affected row와 2026-06-16 공식 decision set을 사용한다.

### 반영 범위

1. Phase B는 Sprint 1.5의 등록 사용자 대상 app-internal invitation 정책을 반영한다.
2. Sprint 1은 Auth-only이며 invitation을 구현하지 않는다.
3. Sprint 1.5부터 회사 멤버 추가는 즉시 membership 생성이 아니라 `invitations` 생성과 대상자 accept 후 membership 생성으로 처리한다. 프로젝트 멤버 추가와 project invitation은 Sprint 2로 분리한다.
4. Sprint 2는 project invitation, project membership 생성, actual invitee email sending, token link acceptance, unregistered onboarding, resend, email template, delivery failure handling, batch cleanup/operations의 후보 시점이며 확정 구현 commitment는 아니다.

### invitation 공식 결정

1. Sprint 1.5 포함 범위는 `invitations` table, `PENDING / ACCEPTED / DECLINED / CANCELED / EXPIRED`, company invitation, exact email lookup for registered users, received invitation list, accept/decline/cancel, company membership creation on accept다.
2. Sprint 1.5 제외 범위는 project invitation, project membership creation, actual email sending to invitees, invitation token link, unregistered-user invitation, signup/onboarding auto-accept, resend다.
3. `notification_events`에는 필요 시 `INVITATION_CREATED`, `INVITATION_ACCEPTED`, `INVITATION_DECLINED`, `MEMBER_ADDED`를 내부 event 원장으로 기록할 수 있으나 Sprint 1.5 완료 조건에는 포함하지 않는다. Push 알림, 별도 Invitation notification preference, S11 사용자 알림 UI 필수 구현은 Sprint 1.5 범위가 아니다.
4. Sprint 1.5에서는 user-facing `notification_preferences`에 별도 Invitation category를 노출하지 않고 기존 global notification preference policy를 따른다.
5. `EXPIRED`와 `expires_at`을 유지하고 기본 만료 기간은 7일로 둔다. read/accept/decline/cancel 시 lazy expiration을 확인하며, 만료된 `PENDING` invitation accept는 `409 INVITATION_EXPIRED`로 거절한다.
6. Sprint 1.5에는 필수 batch scheduler를 추가하지 않는다. Batch cleanup/sweeper는 Sprint 2 이후 admin list hygiene, retention, metrics, operational cleanup 필요에 따라 검토한다.

### Phase C 추가 동기화 결정

1. Sprint 1.5 invitation 검증은 local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명을 사용한다. `POST /api/v1/companies`는 Sprint 1.5 구현 범위에 포함하지 않고 MVP/Sprint 2 reference로 유지하며 운영 데이터에는 seed를 포함하지 않는다.
2. `activity_events`는 활동 원장, `notification_events`는 알림 사건 원본으로 별도 운영한다. 두 저장소를 통합하지 않고, 필요 시 activity 이벤트에서 notification 이벤트를 생성하는 연계만 허용한다.
3. 서버 생성 시각과 만료 시각은 UTC 기준으로 생성, 저장, 비교한다. ERD의 `DATETIME` 타입은 유지하고 API 응답은 ISO-8601 UTC `Z` 형식으로 반환한다.
4. 로그인 이메일은 모든 진입점에서 `trim + lowercase`로 정규화한 값을 저장, 조회, 비교한다. 이 기준은 signup, login, password reset, OAuth 계정 연결, invitation lookup에 동일하게 적용한다.
5. OAuth 계정 연결은 이메일만 같다는 이유로 blind auto-link하지 않는다. 이미 로그인한 사용자이거나 provider 이메일 검증을 신뢰할 수 있는 경우에만 기존 `users` 계정과 연결한다.
6. `password-reset/confirm` 성공 시 해당 사용자의 모든 기존 `refresh_tokens`를 revoke한다. 기존 access token은 별도 blacklist 없이 짧은 만료 시간에 따라 자연 만료시킨다.
7. login, OAuth callback, token refresh, `/auth/me`는 `users.status = ACTIVE`인 사용자만 성공 처리한다. `SUSPENDED` 또는 `DELETED` 사용자는 인증을 거부하며, 상태 변경 시 기존 `refresh_tokens`를 revoke한다.
