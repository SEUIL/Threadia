# Sprint 1 Owner / Reviewer 배정안

> **작성자**: Threadia 팀
> **작성일**: 2026-06-16
> **최종 수정**: 2026-07-04
> **버전**: WBS 기준 Owner/Reviewer 동기화 v2
> **상태**: 확정
> **태그**: sprint-1, role, owner, reviewer
> **수정 근거**: Threadia-기획문서-검수리포트.md, DB-schema-관리-정책.md, Sprint 1 schema.sql 작업 반영, WBS 본시트 1.x Owner/Reviewer/지원 담당 동기화

> 기준
>
> * 이 문서는 Google Sheets `WBS` 본시트 1.x row의 Owner, Reviewer, 지원 담당 값을 따른다.
> * WBS에서 지원 담당이 비어 있는 row는 `-`로 표기한다.
> * Owner가 반드시 지원 담당 안에 있어야 한다는 이전 배정 원칙은 폐기하고, WBS 본시트 L:N을 최신 기준으로 둔다.
> * DB, 인증, OAuth, JWT, API처럼 위험도가 높은 작업은 WBS에서 지정한 Reviewer를 기준으로 review한다.
---

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows다. DB schema는 `docs/03-design/ERD.md`와 `db/schema.sql`을 기준으로 관리하고, Spring Boot/JPA는 schema 생성이 아니라 mapping 검증에 사용한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.


## Day 1, 2026-07-01 — 개발 기반 구축

| WBS ID | 작업 | 지원 담당 | Owner | Reviewer | 비고 |
|---|---|---|---|---|---|
| 1.1.1 | Spring Boot 기본 프로젝트 생성 | 여도현 | 여도현 | 황스일 | Backend 초기 구조 |
| 1.1.2 | MySQL 8.4 연결 확인 | 여도현 | 여도현 | 황스일 | DB 연결 확인 |
| 1.1.2-A | ERD.md 기준 db/schema.sql 생성 | 김태준, 김민경 | 김민정 | 황스일 | ERD 기준 DB 생성 |
| 1.1.3 | Frontend 프로젝트 구조 생성 | 정철진 | 정철진 | 김민정 | Front 초기 구조 |
| 1.1.4 | CI 기준 확인 + AGENTS.md 등 규칙 확정 | 황스일 | 황스일 | 김태준 | 협업 규칙 / CI 기준 |
| 1.1.5 | PR Template 생성 확인 | 황스일 | 황스일 | 김민정 | PR 문서 확인 |
| 1.1.6 | Google OAuth 앱 등록 | 황스일 | 황스일 | 여도현 | OAuth / Secret 관련 |

---

## Day 2, 2026-07-02 — 인증 데이터/정책

| WBS ID | 작업 | 지원 담당 | Owner | Reviewer | 비고 |
|---|---|---|---|---|---|
| 1.2.1 | 인증 JPA Entity 구현 | 김민정 | 김태준 | 황스일 | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens`, `db/schema.sql` |
| 1.2.2 | 로그인/회원가입 화면 레이아웃 작업 | 정철진 | 정철진 | 김민정 | 화면 UI |
| 1.2.3 | JWT/Auth hardening 정책 반영 확인 | 여도현 | 여도현 | 김태준 | access 30분, refresh 14일, rotation/logout invalidation, email 정규화, OAuth blind auto-link 금지, non-ACTIVE 인증 차단 확인 |

---

## Day 3 AM, 2026-07-07 — Auth API/OAuth

| WBS ID | 작업 | 지원 담당 | Owner | Reviewer | 비고 |
|---|---|---|---|---|---|
| 1.3.1 | 이메일 회원가입/로그인 API 구현 | 김민정 | 김민경 | 황스일 | signup/login/logout/me |
| 1.3.2 | 소셜 로그인 API 구현 | 여도현 | 여도현 | 황스일 | Google OAuth |
| 1.3.3 | 로그인/회원가입 화면 마무리 작업 | 정철진 | 정철진 | 김태준 | API 연동 준비 |

---

## Day 3 PM, 2026-07-07 — Password Reset/연동

| WBS ID | 작업 | 지원 담당 | Owner | Reviewer | 비고 |
|---|---|---|---|---|---|
| 1.4.1 | 비밀번호 재설정 API 구현 | 여도현 | 김태준 | 황스일 | password reset |
| 1.4.2 | 로그인/회원가입 화면 API 연동 | - | 정철진 | 김민정 | Front ↔ Backend 연동 |
| 1.4.3 | Apidog(postman 대체) 컬렉션 정리 / API 문서화 | - | 김민정 | 김태준 | API 요청/응답 예시 |

---

## Day 3 QA, 2026-07-07 — 통합 테스트 + 버퍼 + 보고 준비

| WBS ID | 작업 | 지원 담당 | Owner | Reviewer | 비고 |
|---|---|---|---|---|---|
| 1.5.1 | 인증 흐름 end-to-end 테스트 | 김태준 | 여도현 | 정철진 | 전체 인증 흐름 검증 |
| 1.5.2 | 버그 수정 / 버퍼 | - | 김민경 | 여도현 | 인증 Backend 수정 |
| 1.5.3 | 6일차 보고 입력값 정리 | - | 김민정 | 황스일 | 보고용 문서 정리 |

---

## 역할 배정 메모

| 항목 | 내용 |
|---|---|
| 기준 변경 | 기존 역할 배정 원칙보다 WBS 본시트 L:N 값을 우선한다. |
| Day 3 압축 | 기존 Day 4~5 작업은 WBS 기준 Day 3 PM/QA로 압축해 관리한다. |
| 황스일 리뷰 | DB, OAuth, Auth API, password reset, 보고 입력값 등 WBS에서 지정된 위험도 높은 작업의 Reviewer로 유지한다. |
| 여도현 배치 | Spring Boot/MySQL, JWT/Auth hardening, OAuth, E2E Owner 또는 Reviewer로 배치한다. |
| 김태준 배치 | Auth Entity와 password reset Owner, API 문서화 Reviewer, E2E 지원 담당으로 배치한다. |
| 김민정 배치 | `db/schema.sql` 생성 Owner, API 문서화 Owner, Frontend/API 연동 Reviewer, 보고 입력값 Owner로 배치한다. |
| 김민경 배치 | 이메일 Auth API와 버그 수정 / 버퍼 Owner로 배치한다. |
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
