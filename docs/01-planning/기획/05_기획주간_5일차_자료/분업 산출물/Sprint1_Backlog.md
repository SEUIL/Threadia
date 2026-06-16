## 작업 묶음 1. Sprint 1 Backlog 초안

| 작업 | 순서 | 권장 담당 그룹 | 완료 조건 | 검증 방법 | 의존성 |
| --- | --- | --- | --- | --- | --- |
| Spring Boot 기본 프로젝트 생성 | 1 | 여도현 + 김태준 | local에서 서버 실행 가능 | `./gradlew bootRun` | repository clone, Java/Gradle 환경 |
| MySQL 8.4 연결 확인 | 2 | 여도현 | test DB 연결 성공 | 연결 test 또는 간단 query | DB 실행 방식 결정, Spring Boot 기반 |
| Frontend 프로젝트 구조 생성 | 3 | 정철진 | 라우팅, 기본 layout 구성 | 브라우저 실행 | React + Next.js App Router vs Pages Router 결정 |
| 인증 JPA Entity 구현 | 4 | 여도현 + 김태준 + 김민경 | ERD v2.0 기준 `users`, `oauth_accounts`, `password_reset_tokens` JPA Entity 클래스 구현. `users.role` 필드 없이 구현, `users.password_hash`는 소셜 전용 계정 NULL 허용 | `./gradlew build` 통과 | ERD v2.0, MySQL 연결 확인 |
| 이메일 회원가입/로그인 API 구현 | 5 | 여도현 + 김태준 + 김민경 | API 명세서 v2.0 기준 `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me` 구현 | Postman 또는 curl | 인증 Entity, JWT 정책 결정 |
| 소셜 로그인 API 구현 (Google OAuth) | 6 | 여도현 + 김태준 + 김민경 | API 명세서 v2.0 기준 `GET /oauth2/authorization/{provider}`, `GET /api/v1/auth/oauth2/callback/{provider}` 구현 | Postman 또는 브라우저 | 인증 Entity, Google OAuth Client ID/Secret 발급 완료 |
| 비밀번호 재설정 API 구현 | 7 | 여도현 + 김태준 + 김민경 | API 명세서 v2.0 기준 `POST /api/v1/auth/password-reset/request`, `POST /api/v1/auth/password-reset/confirm` 구현. 이메일 발송은 Sprint 1에서 로그 출력으로 대체 가능 여부 팀 합의 필요 | Postman 또는 curl | password_reset_tokens Entity |
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
| (3) Frontend 프로젝트 구조 생성 | 정철진 | 라우팅, 기본 layout 구성 | App Router vs Pages Router 결정 |
| (9)(10) CI 기준 확인 + AGENTS.md 등 규칙 확정 | 황스일 + 김민정 | CI workflow 기준 합의, root/docs 규칙 검토 완료 | 4일차 협업 하네스 |
| (11) PR Template 생성 확인 | 황스일 | `.github/PULL_REQUEST_TEMPLATE.md` 생성 완료 | PR Template v1.0 |
| Google OAuth 앱 등록 | 황스일 + 김민정 | Google Cloud Console Client ID/Secret 발급, callback URL 등록 | Sprint 1 착수 전 필수 선결 조건 |

### Day 2 — 인증 Entity 구현

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| (4) 인증 JPA Entity 구현 | 여도현 + 김태준 + 김민경 | `users`, `oauth_accounts`, `password_reset_tokens` 구현, `./gradlew build` 통과 | Day 1 MySQL 연결 확인 |
| 로그인/회원가입 화면 레이아웃 작업 | 정철진 | 화면 UI만 우선 구현 (API 연동 전) | 화면정의서_v3, API 없이 진행 가능 |
| JWT 정책 결정 회의 | 황스일 + 여도현 | access/refresh token 만료 시간, 갱신 전략, 저장 위치 결정 | Day 3 API 구현 전 확정 필요 |

### Day 3 — 이메일 로그인 API + 소셜 로그인 API

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| (5) 이메일 회원가입/로그인 API 구현 | 여도현 + 김태준 + 김민경 | signup/login/logout/me 구현, Postman 검증 | 인증 Entity, JWT 정책 결정 (Day 2 완료분) |
| (6) 소셜 로그인 API 구현 (Google OAuth) | 여도현 + 김태준 + 김민경 | OAuth 시작/callback 구현, 브라우저로 redirect 확인 | Google OAuth Client ID/Secret (Day 1 완료분) |
| 로그인/회원가입 화면 마무리 작업 | 정철진 | 화면 UI 완성, API 연동 준비 | API 응답 형식 확인 (황스일 + 김민정 지원) |

### Day 4 — 비밀번호 재설정 API + 화면 연동

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| (7) 비밀번호 재설정 API 구현 | 여도현 + 김태준 + 김민경 | reset 요청/확인 흐름 구현, 이메일은 로그 출력으로 대체 | password_reset_tokens Entity |
| (8) 로그인/회원가입 화면 API 연동 | 정철진 | 로그인/회원가입 화면에서 실제 API 호출, 브라우저에서 동작 확인 | Day 3 인증 API 완료 |
| Postman 컬렉션 정리 / API 문서화 | 황스일 + 김민정 | 인증 API 전체 요청/응답 예시 정리 | API 명세서 v2.0 |

### Day 5 — 통합 테스트 + 버퍼 + 6일차 준비

| 작업 | 담당 | 완료 조건 | 의존성/비고 |
| --- | --- | --- | --- |
| 인증 흐름 end-to-end 테스트 | 전체 | 회원가입 → 로그인 → 로그아웃 → 소셜 로그인 → 비밀번호 재설정 전체 시나리오 통과 | Day 1~4 전체 완료 |
| 버그 수정 / 버퍼 | 여도현 + 김태준 + 김민경 | 통합 테스트에서 발견된 이슈 수정 | 일정 여유 확보용 |
| 6일차 보고 입력값 정리 | 황스일 | Sprint 1 목표/Backlog/DoD/검증/blocker 요약 | 6일차 보고용 |

> 작업량이 줄어든 만큼 Day 5는 의도적으로 통합 테스트와 버퍼로 비워뒀다. 일정이 당겨지면 다음 스프린트 도메인 Entity(회사/프로젝트/이슈) 설계를 미리 검토하는 시간으로 써도 된다.
