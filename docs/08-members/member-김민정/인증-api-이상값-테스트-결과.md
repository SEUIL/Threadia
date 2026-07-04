> **작성자**: 김민정 (WBS 1.5 Owner)
> **작성일**: 2026-07-04
> **최종 수정**: 2026-07-04
> **버전**: v1.0
> **상태**: 작성 중
> **태그**: auth, api, e2e, wbs-1.5

# 인증 API 이상값(edge case) 테스트 결과

> 기준 문서: `docs/04-api/API-명세서.md` 13.2절, `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md`
> 관련 요구사항: AUTH-01 ~ AUTH-04
> 관련 WBS: 1.5(통합 테스트 + 버퍼 + 보고 준비, Owner 김민정) 산출물 중 "E2E 검증 결과" 항목

**범위 안내**: 아래 E2E 시나리오는 WBS 1.5.1(인증 흐름 end-to-end 테스트, Owner 여도현, Reviewer 정철진)의 검증 범위와 겹친다. 1.5 진행 중 signup API 이상값 테스트를 먼저 진행하다 자연스럽게 나머지 endpoint까지 선반영하게 되어 이 문서로 남긴다. **1.5.1 최종 확인/승인은 여도현 담당**이며, 이 문서는 그 확인을 위한 참고 자료다.

---

## 1. 테스트 대상 및 방법

- 대상: `signup`, `login`, `refresh`, `logout`, `me` (email Auth API 전체)
- 방법: 로컬 backend(`localhost:8080`, `dev` 브랜치 기준)에 curl로 직접 요청
- 항목: SQL injection, XSS, null/빈 값, 잘못된 타입, 이모지, 존재하지 않는 필드 주입, 긴 문자열, 경계값, 깨진 JSON, 잘못된 Content-Type 등

---

## 2. Signup 테스트 결과 (11건)

| # | 시도 | 결과 | 평가 |
|---|---|---|---|
| 1 | SQL injection (`' OR '1'='1`, email) | 400 VALIDATION_ERROR | `@Email` 검증에서 차단 |
| 2 | XSS (`<script>alert(1)</script>`, nickname) | 201 성공, 원문 그대로 저장/응답 | 백엔드는 그대로 저장·반환. 프론트 렌더링 시 이스케이프 여부 별도 확인 필요(미검증) |
| 3 | null/빈 값 | 400 VALIDATION_ERROR | 정상 |
| 4 | password를 숫자 타입으로 | 400 (문자열 변환 후 형식 불일치로 거부) | 타입 혼동 없음 |
| 5 | 이모지/유니코드 nickname | 201 성공, 정상 저장 | 정상 |
| 6 | 존재하지 않는 필드 `role:"ADMIN"` 끼워넣기 | 201 성공, 필드는 조용히 무시됨 | 실제 role 미적용, 권한상승 없음 |
| 7 | nickname 10000자 | 400 VALIDATION_ERROR | 정상 |
| 8 | password 정확히 72자 (BCrypt 경계) | 201 성공 | 경계값 정확 |
| 9 | password 73자 | 400 VALIDATION_ERROR | 경계값 정확 |
| 10 | 깨진 JSON | 400 VALIDATION_ERROR | 정상 |
| 11 | `Content-Type: text/plain` | **500 INTERNAL_ERROR (발견 당시)** → PR #30으로 415 UNSUPPORTED_MEDIA_TYPE으로 수정 완료 | 버그. 아래 §5 참고 |

---

## 3. Login / Refresh / Logout / Me 테스트 결과

### 3.1 Login (11건)

| # | 시도 | 결과 | 평가 |
|---|---|---|---|
| 1 | SQL injection (email) | 400 | `@Email` 검증에서 차단 |
| 2 | null/빈 값 | 400 | 정상 |
| 3 | password 잘못된 타입(숫자) | 401 | 정상 거부 |
| 4 | 이모지 email | 401 | 정상 거부 |
| 5 | 존재하지 않는 필드 `role:"ADMIN"` 끼워넣기 (정상 자격증명) | 200 로그인 성공, role 무시 | 권한상승 없음 |
| 6 | password 10000자 | 401 | 정상 거부, 500/크래시 없음 |
| 7 | 깨진 JSON | 400 | 정상 |
| 8 | `Content-Type: text/plain` | 415 | signup의 415 fix가 공통 계층이라 login에도 정상 적용됨 확인 |
| 9 | 대문자 email (`E2E-ROUND2@EXAMPLE.COM`) | 200 로그인 성공 | email 정규화 정상 동작 |
| 10 | 틀린 password | 401 | 정상 |
| 11 | 존재하지 않는 email | 401 | 정상 |

### 3.2 Refresh (6건)

| # | 시도 | 결과 | 평가 |
|---|---|---|---|
| 1 | 쿠키 없음 | 401 | 정상 |
| 2 | 쓰레기 값 쿠키 | 401 | 정상 |
| 3 | SQL injection 쿠키 | 401 | 정상, 크래시 없음 |
| 4 | 쿠키 10000자 | 400 (Tomcat 자체 에러 페이지) | §4 참고 |
| 5 | 정상 refresh(토큰 회전) | 200 | 정상 |
| 6 | 회전 후 기존(구) refresh token 재사용 | 401 | rotation 정상 동작 |

### 3.3 Logout (4건)

| # | 시도 | 결과 | 평가 |
|---|---|---|---|
| 1 | Authorization 헤더 없음 | 401 | 정상 |
| 2 | 위조된 access token | 401 | 정상 |
| 3 | SQL injection Authorization | 401 | 정상, 크래시 없음 |
| 4 | 유효한 access token + 쿠키 없음 | 401 | 정상 |

> 참고: "이미 revoke된 refresh token 재사용" 시나리오는 본 테스트 이전에 이미 발견·수정 완료된 항목이라(단위 테스트로 커버) 이번 라운드에서는 재검증만 하고 별도 표에 포함하지 않았다.

### 3.4 Me (7건)

| # | 시도 | 결과 | 평가 |
|---|---|---|---|
| 1 | Authorization 없음 | 401 | 정상 |
| 2 | 위조 토큰 | 401 | 정상 |
| 3 | `Basic` 스킴 | 401 | 정상 |
| 4 | 소문자 `bearer` | 401 | 대소문자 구분 정상 |
| 5 | SQL injection | 401 | 정상, 크래시 없음 |
| 6 | 토큰 10000자 | 400 (Tomcat 자체 에러 페이지) | §4 참고 |
| 7 | 정상 토큰 (baseline) | 200 | 정상 |

---

## 4. Informational 발견 사항 (버그 아님)

### 4.1 과도하게 긴 쿠키/헤더 값 → Tomcat 자체 HTML 에러 페이지

`refresh` 쿠키 또는 `me`의 `Authorization` 헤더 값이 1만 자 수준으로 길면, 요청이 애플리케이션 코드(GlobalExceptionHandler)에 도달하기 전에 **Tomcat이 `maxHttpHeaderSize`(기본 8KB) 제한으로 요청 자체를 거부**한다. 상태 코드는 400으로 적절하지만, 응답 본문이 우리 JSON 에러 포맷이 아니라 Tomcat 기본 HTML 에러 페이지다.

- 실사용에서 정상 토큰/쿠키가 이 정도 길이가 될 일은 없어 심각도는 낮음
- 프론트가 모든 에러 응답을 JSON으로 파싱한다고 가정한 코드가 있다면 이 경우에만 파싱 실패 가능 — 별도 조치는 보류, 참고용으로만 기록

### 4.2 XSS payload 미이스케이프 저장

signup의 `nickname`에 `<script>` 태그를 넣으면 그대로 저장·응답된다(§2 항목 2). 백엔드에서 별도로 sanitize하지 않는 것이 의도된 설계인지, 프론트에서 렌더링 시 이스케이프 처리를 하는지는 **미검증** 상태다. 프론트 담당자 확인 필요.

---

## 5. 발견 후 수정한 버그

### Content-Type 불일치 요청이 500으로 응답되던 문제

- **증상**: `Content-Type: text/plain`으로 signup 요청 시 500 INTERNAL_ERROR 응답 (정상은 415)
- **원인**: `GlobalExceptionHandler`에 `HttpMediaTypeNotSupportedException` 전용 handler가 없어 catch-all(`Exception`)로 흘러가 클라이언트 요청 오류가 서버 오류로 응답됨
- **수정**: `HttpMediaTypeNotSupportedException` handler를 추가해 415 UNSUPPORTED_MEDIA_TYPE으로 응답하도록 수정
- **PR**: [#30](https://github.com/SEUIL/Threadia/pull/30) (Reviewer: 황스일, 리뷰 대기)
- **영향 범위**: `GlobalExceptionHandler`는 공통 계층이라 signup뿐 아니라 login 등 전체 API에 적용됨 (§3.1 항목 8에서 재검증 완료)
- WBS 1.5.2(버그 수정/버퍼, Owner 김민경) 범위와 겹치는 이슈이나, 발견 즉시 별도 PR로 처리 완료했다.

---

## 6. 결론

- signup/login/refresh/logout/me 전체에서 **새로 발견된 버그는 §5의 Content-Type 건 1개**이며 이미 수정·PR 반영 완료했다.
- email 정규화(대소문자), refresh token rotation, 이미 revoke된 토큰 재사용 차단 등 이전에 수정한 항목들은 모두 재검증되어 정상 동작을 확인했다.
- §4의 두 가지 informational 사항은 버그는 아니나 팀 공유가 필요하다(§4.2는 프론트 담당자 확인 필요).
