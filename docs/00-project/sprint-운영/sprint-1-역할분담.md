# Sprint 1 Owner / Reviewer 배정안

> **작성자**: Threadia 팀
> **작성일**: 2026-06-16
> **최종 수정**: 2026-06-29
> **버전**: 개발주간 schema.sql 작업 반영 v1
> **상태**: 확정
> **태그**: sprint-1, role, owner, reviewer
> **수정 근거**: Threadia-기획문서-검수리포트.md, DB-schema-관리-정책.md, Sprint 1 schema.sql 작업 반영

> 기준
>
> * Owner는 `지원 담당` 안에서 지정한다.
> * Reviewer는 `지원 담당`에 포함되지 않은 사람 중에서 지정한다.
> * DB, 인증, OAuth, JWT, API처럼 위험도가 높은 작업은 가능한 한 황스일 님이 Reviewer로 들어가도록 배치한다.
> * 단, 황스일 님이 이미 지원 담당에 포함된 작업은 규칙상 Reviewer로 둘 수 없으므로 Owner 또는 지원 역할로 둔다.

---

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows다. DB schema는 `docs/03-design/ERD.md`와 `db/schema.sql`을 기준으로 관리하고, Spring Boot/JPA는 schema 생성이 아니라 mapping 검증에 사용한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.


## Day 1 — 개발 기반 구축

| 순서 | 작업                           | 지원 담당         | Owner | Reviewer | 비고                |
| -: | ---------------------------- | ------------- | ----- | -------- | ----------------- |
|  1 | Spring Boot 기본 프로젝트 생성       | 여도현, 김태준, 김민경 | 여도현   | 황스일      | Backend 초기 구조     |
|  2 | MySQL 8.4 연결 확인              | 여도현           | 여도현   | 황스일      | DB 연결 확인          |
|  3 | ERD.md 기준 db/schema.sql 생성       | 김태준, 김민경      | 김민정   | 황스일      | ERD 완료 후 DB 생성 기준 |
|  4 | Frontend 프로젝트 구조 생성          | 정철진           | 정철진   | 김민정      | Front 초기 구조       |
|  5 | CI 기준 확인 + AGENTS.md 등 규칙 확정 | 황스일, 김민정      | 황스일   | 김태준      | 협업 규칙 / CI 기준     |
|  6 | PR Template 생성 확인            | 황스일           | 황스일   | 김민정      | PR 문서 확인          |
|  7 | Google OAuth 앱 등록            | 황스일, 김민정      | 황스일   | 여도현      | OAuth / Secret 관련 |

---

## Day 2 — 인증 Entity 구현

| 순서 | 작업                  | 지원 담당         | Owner | Reviewer | 비고                                                 |
| -: | ------------------- | ------------- | ----- | -------- | -------------------------------------------------- |
|  8 | 인증 JPA Entity 구현    | 여도현, 김태준, 김민경 | 김태준   | 황스일      | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens`, `db/schema.sql` |
|  9 | 로그인/회원가입 화면 레이아웃 작업 | 정철진           | 정철진   | 김민정      | 화면 UI                                              |
| 10 | JWT/Auth hardening 정책 반영 확인 | 황스일, 여도현      | 황스일   | 김태준      | access 30분, refresh 14일, rotation/logout invalidation, email 정규화, OAuth blind auto-link 금지, non-ACTIVE 인증 차단 확인 |

---

## Day 3 — 이메일 로그인 API + 소셜 로그인 API

| 순서 | 작업                  | 지원 담당         | Owner | Reviewer | 비고                     |
| -: | ------------------- | ------------- | ----- | -------- | ---------------------- |
| 11 | 이메일 회원가입/로그인 API 구현 | 여도현, 김태준, 김민경 | 김민경   | 황스일      | signup/login/logout/me |
| 12 | 소셜 로그인 API 구현       | 여도현, 김태준, 김민경 | 여도현   | 황스일      | Google OAuth           |
| 13 | 로그인/회원가입 화면 마무리 작업  | 정철진           | 정철진   | 김태준      | API 연동 준비              |

---

## Day 4 — 비밀번호 재설정 API + 화면 연동

| 순서 | 작업                       | 지원 담당         | Owner | Reviewer | 비고                 |
| -: | ------------------------ | ------------- | ----- | -------- | ------------------ |
| 14 | 비밀번호 재설정 API 구현          | 여도현, 김태준, 김민경 | 김태준   | 황스일      | password reset     |
| 15 | 로그인/회원가입 화면 API 연동       | 정철진           | 정철진   | 황스일      | Front ↔ Backend 연동 |
| 16 | Postman 컬렉션 정리 / API 문서화 | 황스일, 김민정      | 김민정   | 김태준      | API 요청/응답 예시       |

---

## Day 5 — 통합 테스트 + 버퍼 + 보고 준비

| 순서 | 작업                   | 지원 담당              | Owner | Reviewer | 비고            |
| -: | -------------------- | ------------------ | ----- | -------- | ------------- |
| 17 | 인증 흐름 end-to-end 테스트 | 여도현, 정철진, 김민경, 김태준 | 김태준   | 황스일      | 전체 인증 흐름 검증   |
| 18 | 버그 수정 / 버퍼           | 여도현, 김태준, 김민경      | 김민경   | 황스일      | 인증 Backend 수정 |
| 19 | 6일차 보고 입력값 정리        | 황스일                | 황스일   | 김민정      | 보고용 문서 정리     |

---

## 역할 배정 메모

| 항목          | 내용                                                                                  |
| ----------- | ----------------------------------------------------------------------------------- |
| 3번 작업       | Day 1 기존 Owner로 배정된 여도현, 정철진, 황스일을 제외하고 김민정을 Owner로 둔다. 김태준, 김민경은 지원 담당이고 황스일은 Reviewer다. |
| 17번 작업      | 원래 지원 담당이 전체 인원이면 Reviewer를 지원 담당 밖에서 정할 수 없다. 그래서 실제 테스트 실행 인원 중심으로 지원 담당을 줄인 안이다. |
| 황스일 님 리뷰 배치 | 인증, DB, OAuth, API, E2E 테스트처럼 위험도 높은 작업 위주로 Reviewer에 배치했다.                         |
| 김태준 배치      | 인증 Entity, 비밀번호 재설정 API, E2E 테스트 쪽 Owner/Reviewer로 들어가도록 배치했다.                      |
| 여도현 배치      | 초기 Backend 세팅, DB 연결, 소셜 로그인 쪽 Owner로 배치했다.                                         |
| 김민정 배치      | `db/schema.sql` 생성 Owner와 API 문서화 Owner로 배치했다.                                         |
| 김민경 배치      | 이메일 로그인 API, 버그 수정 쪽 Owner로 배치했다.                                                   |

---


## 6일차 v2 공통 정책 확인 역할

| 영역 | Owner | Reviewer | 확인 기준 |
|---|---|---|---|
| Auth hardening | 황스일 | 김태준 | email 정규화, OAuth blind auto-link 금지, reset 후 refresh token revoke, non-ACTIVE 인증 차단 |
| Sprint 1.5 invitation | 김민정 | 황스일 | seed 회사/`COMPANY_ADMIN`, accept 시 membership 생성, actual email/token link/resend 제외 |
| Notification 분리 | 김민정 | 김태준 | `activity_events`와 `notification_events` 분리, Push/S11 UI 필수 구현 제외 |
| 문서 정합성 | 황스일 | 김민정 | 6일차 수정본 v2 metadata와 README 목록 일치 |

---

## 6일차 수정본 v2 공통 기준

1. Sprint 1.5 invitation 검증은 local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명을 사용한다. `POST /api/v1/companies`는 Sprint 1.5 구현 범위에 포함하지 않는다.
2. `activity_events`와 `notification_events`는 통합하지 않고 각각 활동 원장과 알림 사건 원본으로 별도 운영한다.
3. 서버 생성 시각과 만료 시각은 UTC 기준으로 생성, 저장, 비교하고 API 응답은 ISO-8601 UTC `Z` 형식으로 반환한다.
4. 로그인 이메일은 signup, login, password reset, OAuth 계정 연결, invitation lookup에서 모두 `trim + lowercase`로 정규화한다.
5. OAuth 계정은 이메일만 같다는 이유로 blind auto-link하지 않는다.
6. `password-reset/confirm` 성공 시 해당 사용자의 모든 기존 `refresh_tokens`를 revoke한다.
7. login, OAuth callback, token refresh, `/auth/me`는 `users.status = ACTIVE`인 사용자만 성공 처리한다.
