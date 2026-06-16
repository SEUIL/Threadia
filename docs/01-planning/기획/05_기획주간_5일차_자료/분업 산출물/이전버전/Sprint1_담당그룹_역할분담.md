> **작성자**: Threadia 팀
> **작성일**: 2026-06-16
> **최종 수정**: 2026-06-16
> **버전**: v1.0
> **상태**: 검토 중

# Sprint 1 담당 그룹/역할 분담

## 1. 목적

이 문서는 Sprint 1 Backlog 확정 이후 각 작업의 담당 그룹, owner, reviewer, 검증 책임을 정리하기 위한 역할 분담표다.

현재 단계에서는 owner를 비워두고, 기존 Backlog에 적힌 담당 그룹 인원을 `지원 담당`에 모아 둔다. 이후 회의에서 각 작업별 owner 1명과 reviewer 1명을 확정한다.

## 2. 작성 기준

| 기준 | 내용 |
|---|---|
| 기준 문서 | `Sprint 1_detail.md`, `Sprint1_Backlog.md`, `ai-assisted-분업-기준.md` |
| Sprint 1 범위 | 개발 기반 구축, 인증 JPA Entity, 이메일 로그인, Google OAuth, 비밀번호 재설정, 로그인/회원가입 화면 |
| Owner | 아직 미확정이므로 빈칸으로 둔다 |
| 지원 담당 | 기존 Backlog의 담당 그룹 인원을 모두 적는다 |
| Reviewer | owner 확정 후 owner와 다른 사람으로 지정한다 |
| 승인 필요 여부 | DB schema, 인증/인가, CI, AWS, secret 변경이 있으면 `필요`로 표시한다 |

## 3. Sprint 1 역할 분담표

| 순서 | Day | Backlog | Owner | 지원 담당 | Reviewer | 검증 책임 | 주요 변경 범위 | 승인 필요 여부 |
|---:|---|---|---|---|---|---|---|---|
| 1 | Day 1 | Spring Boot 기본 프로젝트 생성 |  | 여도현, 김태준, 김민경 |  | `./gradlew bootRun` 실행 확인 | Backend skeleton, Gradle 설정 | 필요 |
| 2 | Day 1 | MySQL 8.4 연결 확인 |  | 여도현 |  | local DB 연결 test 또는 간단 query 확인 | DB 설정, application 설정 | 필요 |
| 3 | Day 1 | Frontend 프로젝트 구조 생성 |  | 정철진 |  | 브라우저에서 기본 route 확인 | Frontend skeleton, route, layout | 보통 |
| 4 | Day 1 | CI 기준 확인 + AGENTS.md 등 규칙 확정 |  | 황스일, 김민정 |  | CI 기준 문서와 root/docs 규칙 확인 | CI 기준, 협업 규칙 문서 | 필요 |
| 5 | Day 1 | PR Template 생성 확인 |  | 황스일 |  | `.github/PULL_REQUEST_TEMPLATE.md` 존재 확인 | PR Template | 보통 |
| 6 | Day 1 | Google OAuth 앱 등록 |  | 황스일, 김민정 |  | Google OAuth Client ID/Secret 발급과 callback URL 등록 확인 | Google Cloud Console 설정, OAuth callback 기준 | 필요 |
| 7 | Day 2 | 인증 JPA Entity 구현 |  | 여도현, 김태준, 김민경 |  | `./gradlew build` 또는 `./gradlew test` 통과 | `users`, `oauth_accounts`, `password_reset_tokens` Entity | 필요 |
| 8 | Day 2 | 로그인/회원가입 화면 레이아웃 작업 |  | 정철진 |  | 브라우저에서 로그인/회원가입 route 확인 | 로그인 화면, 회원가입 화면 | 보통 |
| 9 | Day 2 | JWT 정책 결정 회의 |  | 황스일, 여도현 |  | access/refresh token 정책 문서화 확인 | 인증 정책, token 만료/갱신 기준 | 필요 |
| 10 | Day 3 | 이메일 회원가입/로그인 API 구현 |  | 여도현, 김태준, 김민경 |  | Postman 또는 curl로 signup/login/logout/me 확인 | 인증 API, DTO, service, controller | 필요 |
| 11 | Day 3 | 소셜 로그인 API 구현 (Google OAuth) |  | 여도현, 김태준, 김민경 |  | OAuth 시작/callback 흐름을 Postman 또는 브라우저로 확인 | OAuth API, security 설정, `oauth_accounts` 연동 | 필요 |
| 12 | Day 3 | 로그인/회원가입 화면 마무리 작업 |  | 정철진 |  | 화면 UI 완성 및 API 연동 준비 상태 확인 | 로그인/회원가입 UI, API 응답 처리 후보 | 보통 |
| 13 | Day 4 | 비밀번호 재설정 API 구현 |  | 여도현, 김태준, 김민경 |  | reset 요청/확인 흐름을 Postman 또는 curl로 확인 | password reset API, `password_reset_tokens` 연동 | 필요 |
| 14 | Day 4 | 로그인/회원가입 화면 API 연동 |  | 정철진 |  | 브라우저에서 로그인/회원가입 API 연동 확인 | Frontend auth API 연동, 화면 상태 처리 | 보통 |
| 15 | Day 4 | Postman 컬렉션 정리 / API 문서화 |  | 황스일, 김민정 |  | 인증 API 요청/응답 예시 확인 | Postman collection, API 문서 | 보통 |
| 16 | Day 5 | 인증 흐름 end-to-end 테스트 |  | 황스일, 여도현, 정철진, 김민정, 김민경, 김태준 |  | 회원가입 -> 로그인 -> 로그아웃 -> 소셜 로그인 -> 비밀번호 재설정 시나리오 확인 | Backend API, Frontend 화면, DB 연동 | 필요 |
| 17 | Day 5 | 버그 수정 / 버퍼 |  | 여도현, 김태준, 김민경 |  | 수정 항목별 관련 test/API/화면 재확인 | 인증 Backend, DB, API | 변경 범위에 따라 판단 |
| 18 | Day 5 | 6일차 보고 입력값 정리 |  | 황스일 |  | Sprint 1 목표/Backlog/DoD/검증/blocker 요약 확인 | 6일차 보고용 문서 | 보통 |

## 4. Owner 확정 시 채울 항목

회의에서 각 작업별로 아래 항목을 확정한다.

| 항목 | 확정 기준 |
|---|---|
| Owner | 작업 결과를 최종 설명하고 PR을 책임질 사람 1명 |
| Reviewer | owner와 다른 사람 1명 이상 |
| 검증 책임 | test, curl, 브라우저 확인, 문서 검토를 실제 수행할 사람 |
| 주요 변경 범위 | 동시에 수정하면 충돌이 날 파일 또는 폴더 |
| 승인 필요 여부 | DB schema, 인증/인가, CI, AWS, secret 변경 여부 |

## 5. 회의 확인 체크리스트

- [ ] 모든 Backlog에 owner 1명이 지정되었다.
- [ ] owner와 reviewer가 같은 사람으로 지정되지 않았다.
- [ ] 인증, DB, CI, AWS, secret 관련 작업은 승인 필요로 표시되었다.
- [ ] 같은 파일을 여러 명이 동시에 수정하지 않도록 주요 변경 범위를 확인했다.
- [ ] 각 작업의 검증 책임자가 test, API, 화면, 문서 검토 중 무엇을 확인할지 알고 있다.
