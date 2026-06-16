> **작성자**: 정철진
> **작성일**: 2026-06-16
> **최종 수정**: 2026-06-16
> **버전**: v1.4
> **상태**: 작성 중

# Sprint 1 프론트엔드 의존성 / 기술 스택 / DoD

이 문서는 Sprint 1 프론트엔드 작업 착수 전 필요한 기술 스택, 의존성, blocker, 완료 기준을 정리한다.

기준 문서:
- `Sprint 1_detail.md` v0.2
- `Sprint1_Backlog.md`
- `Sprint1_담당그룹_역할분담 v1.1.md`
- `API 명세서 v2.3`
- `작업 유형별 완료 조건 v2.md`
- `test-build-API-화면 확인 기준 v2.md`

---

## 1. 문서 범위

이 문서는 **Frontend 자체 결정 사항**과 **Frontend 작업을 막는 Auth/API 의존성**을 함께 정리한다.

| 구분 | 이 문서에서 다루는 범위 |
|---|---|
| Frontend 직접 범위 | React + Next 기반 구조, route/layout, 로그인/회원가입 화면, 인증 API 연동, 브라우저 확인 |
| Auth/API 의존성 | 로그인/회원가입/logout/me API, Google OAuth, 비밀번호 재설정 API, JWT 정책, API 응답 형식 |
| Sprint 1 전체 인증 흐름 | 회원가입 -> 로그인 -> 로그아웃 -> Google OAuth -> 비밀번호 재설정 E2E 확인 |
| 제외/후순위 | 회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림 화면, 프로젝트 홈/대시보드, 목록 pagination 화면 구현 |

> Sprint 1 전체 범위에는 비밀번호 재설정 API와 인증 E2E가 포함된다. 다만 이 문서의 Frontend 직접 구현 범위는 로그인/회원가입 화면과 인증 연동 중심으로 둔다. 비밀번호 재설정 화면까지 포함할지는 별도 확인 항목으로 관리한다.

---

## 2. Sprint 1 프론트엔드 목표

Sprint 1 프론트엔드는 React + Next 기반 구조를 생성하고, 로그인·회원가입 화면과 인증 API 연동 준비 및 실제 연동까지 완료하는 것을 목표로 한다.

인증 관련 포함 항목은 우선순위가 아니라 Sprint 1 포함 여부 기준으로 정리한다.

| 인증 항목 | Sprint 1 포함 여부 | Frontend 영향 |
|---|---|---|
| 이메일 회원가입 | 포함 | 회원가입 화면, validation, API 연동 |
| 이메일 로그인 | 포함 | 로그인 화면, token 처리, error 처리 |
| 로그아웃 | 포함 | 로그인 상태 초기화, 보호 route 처리 |
| `/auth/me` | 포함 | 현재 사용자 context 조회, 로그인 유지 판단 |
| Google OAuth | 포함 | OAuth 시작 route/callback 처리 방식 확인 |
| 비밀번호 재설정 | 포함 | API는 Sprint 1 포함. 화면 포함 여부와 이메일 발송 방식은 확인 필요 |

---

## 3. 작업 일정

| Day | Frontend 관련 작업 | 완료 조건 |
|---|---|---|
| Day 1 | React + Next 프로젝트 구조 생성, routing/layout 기준 결정 | 브라우저에서 기본 route 진입 확인 |
| Day 2 | 로그인·회원가입 화면 layout 구현, JWT 정책 회의 결과 확인 | 화면 UI 초안 완료, token 저장/갱신 방향 확인 |
| Day 3 | 로그인·회원가입 화면 마무리, API 연동 준비 | API 응답 형식 기준으로 연동 준비 완료 |
| Day 4 | 로그인·회원가입 화면 API 실제 연동 | 브라우저에서 로그인 흐름 동작 확인 |
| Day 5 | 인증 흐름 E2E 확인 참여, 버그 수정, 6일차 보고 입력값 정리 | 회원가입 -> 로그인 -> 로그아웃 -> OAuth -> 비밀번호 재설정 흐름 확인 결과 기록 |

---

## 4. 프론트엔드 기술 스택

### 4.1 확정 / 기준 있음

| 항목 | 결정 | 상태 | 비고 |
|---|---|---|---|
| Framework | React + Next.js | 확정 | Sprint 1 Frontend 구조 기준 |
| 인증 방식 | JWT Bearer Token | 확정 | API 명세서 v2.3 기준 |
| 인증 헤더 | `Authorization: Bearer <token>` | 확정 | API 요청 시 사용 |
| 소셜 로그인 | Google OAuth | Sprint 1 포함 | 앱 등록과 callback 기준 확인 필요 |
| API 단일 응답 | `{ data, meta }` | 확정 | API 명세서 v2.3 기준 |
| API 에러 응답 | `{ error: { code, message, details }, meta }` | 확정 | API 명세서 v2.3 기준 |

### 4.2 미정 / 확인 필요

| 항목 | 현재 상태 | 필요한 결정 | Owner / 확인자 |
|---|---|---|---|
| Routing | 미정 | App Router vs Pages Router 결정 | 정철진, 팀 전체 |
| Package manager | 미정 | `npm`, `pnpm`, `yarn` 중 하나로 통일 | 정철진, 팀 전체 |
| TypeScript | 확인 필요 | Sprint 1 FE 구조에서 필수 적용 여부 결정 | 정철진 |
| Tailwind CSS | 확인 필요 | Sprint 1 UI styling 기준으로 채택할지 결정 | 정철진 |
| Three.js | 후순위 | 인증 중심 Sprint 1의 필수 blocker로 두지 않음 | 정철진 |
| Zustand | 확인 필요 | 로그인 상태 등 client state 관리 방식 결정 | 정철진 |
| TanStack Query | 확인 필요 | server state/cache 적용 여부 결정 | 정철진 |
| refresh token | 미정 | 사용 여부, 만료 시간, 갱신 전략, 저장 위치 결정 | 황스일, 여도현 |

> App Router, refresh token, package manager, Zustand, TanStack Query는 현재 문서 기준으로 임의 확정하지 않는다. Sprint 1 시작 전 또는 Day 2 JWT 정책 회의에서 결정된 내용만 확정 항목으로 올린다.

---

## 5. API 연동 기준

기준 문서: `API 명세서 v2.3`

이 문서에서는 API v2.3 전체가 아니라 **Auth 섹션과 인증 연동에 필요한 공통 응답 형식**을 우선 적용한다.

| 항목 | 내용 | 상태 |
|---|---|---|
| Base URL (dev) | `http://localhost:8080/api/v1` | 확정 |
| 단일 응답 형식 | `{ data, meta }` | 확정 |
| 목록 응답 형식 | `{ data, page, meta }` | API 기준 존재, Sprint 1 FE 필수 blocker 아님 |
| error response | `{ error: { code, message, details }, meta }` | 확정 |
| `/auth/me` 응답 | user, companyMemberships, projectMemberships, unread count 예시 존재 | Sprint 1 FE에서 필요한 최소 context 확인 필요 |

에러 응답 예시:

```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "프로젝트 접근 권한이 없습니다.",
    "details": {}
  },
  "meta": {
    "requestId": "req_..."
  }
}
```

---

## 6. 의존성 / Blocker 정리

### 6.1 영역별 의존성 / Blocker

| 영역 | 의존성 | Blocker | 영향 | 해결 담당 | 결정 시점 | 상태 |
|---|---|---|---|---|---|---|
| Frontend 구조 | React + Next, route/layout 기준, package manager | App Router vs Pages Router 미결, package manager 미정 | Day 1 프로젝트 세팅 지연 가능 | 정철진, 팀 전체 | Sprint 1 시작 전 | 미결 |
| Auth 정책 | JWT Bearer Token, access/refresh token 정책 | token 저장 위치, access/refresh token 만료/갱신 방식 미정 | 로그인 API 구현 및 FE 상태 처리 기준 충돌 | 황스일 Owner, 여도현 지원, 김태준 Reviewer | Day 2 | 미결 |
| OAuth 앱 등록 | Google Cloud Console, Client ID/Secret, callback URL | Client ID/Secret 준비와 redirect URI 등록 필요 | Day 3 소셜 로그인 API 지연 | 황스일 Owner, 김민정 지원, 여도현 Reviewer | Day 1 | 일부 미결 |
| Auth API 구현 | signup/login/logout/me, OAuth, password reset API | 실제 구현 응답과 API v2.3 일치 여부 확인 필요 | FE-BE 병렬 작업 지연 | 김민경, 여도현, 김태준, 정철진 | Day 3~4 | 확인 필요 |
| 비밀번호 재설정 | password_reset_tokens, reset request/confirm API | 이메일 발송을 Sprint 1에서 로그/Mock으로 대체할지 합의 필요 | 인증 E2E 검증 범위 불명확 | 김태준 Owner, 황스일 Reviewer | Day 4 전 | 미결 |

### 6.2 프론트엔드 기능별 의존성 / Blocker

| 기능 | 의존성 | Blocker | 영향 | 해결 담당 | 상태 |
|---|---|---|---|---|---|
| Frontend 프로젝트 구조 | React + Next, routing/layout, package manager | App Router vs Pages Router 미결, package manager 미정 | Day 1 route/layout 작업 지연 | 정철진 Owner, 김민정 Reviewer | 미결 |
| 로그인/회원가입 화면 | route, 로그인/회원가입 API, error response 형식 | API 실제 응답 형식과 error JSON 처리 기준 확인 필요 | 화면 연동 지연 | 정철진 Owner, 황스일 Reviewer | 확인 필요 |
| Frontend 상태 관리 | JWT 정책, `/auth/me`, FE state/cache 기준 | token 저장 위치와 로그인 상태 유지 기준 미정 | 인증 상태 처리 충돌 | 정철진, 황스일, 여도현 | 일부 미결 |
| 로그아웃 | logout API, token/session 초기화 기준 | logout 후 client state 정리 방식 확인 필요 | 로그인 상태가 화면에 남을 수 있음 | 정철진, 김민경 | 확인 필요 |
| `/auth/me` | JWT claim, user context DTO | API 예시는 있으나 Sprint 1 FE 최소 context 범위 확인 필요 | 로그인 유지, 보호 route 처리 기준 불명확 | 정철진, 김민경 | 확인 필요 |
| 소셜 로그인 | Google OAuth provider, redirect URI, callback API | Client ID/Secret, redirect URI, callback 처리 방식 확인 필요 | 1순위 인증 기능 지연 | 황스일, 김민정, 여도현, 정철진 | 일부 미결 |
| 비밀번호 재설정 | reset request/confirm API, email/log 대체 기준 | FE 화면 포함 여부와 이메일 발송 대체 방식 확인 필요 | E2E 검증 범위 불명확 | 김태준, 정철진, 황스일 | 미결 |
| Pagination | `{ data, page, meta }`, cursor page 응답 | Sprint 1 인증 FE의 직접 blocker 아님 | 후속 목록 화면 구현 시 영향 | 정철진, API 담당 | 후순위 |

---

## 7. Blocker 상세

### B-01. App Router vs Pages Router / package manager 미정

- **현재 정리된 내용**: React + Next 기반 Frontend 구조는 Sprint 1 범위다.
- **남은 Blocker**: App Router vs Pages Router, package manager가 확정되지 않았다.
- **영향**: 프로젝트 생성, route/layout 구조, dependency install, 실행 명령 불일치 가능
- **해결 담당**: 정철진 Owner, 김민정 Reviewer, 팀 전체 합의
- **결정 시점**: Sprint 1 시작 전
- **필요 결정**:
  - `app/` 기반 App Router와 Pages Router 중 하나 선택
  - `npm`, `pnpm`, `yarn` 중 하나로 통일

### B-02. JWT 저장 위치 / refresh token 정책 미정

- **현재 정리된 내용**: JWT Bearer Token 방식과 인증 헤더는 확정이다.
- **남은 Blocker**: refresh token 사용 여부, token 저장 위치, access token 만료 처리, refresh token 갱신 전략이 미정이다.
- **영향**: 로그인 API 연동, 인증 상태 유지, FE 상태 관리 기준 결정 불가
- **해결 담당**: 황스일 Owner, 여도현 지원, 김태준 Reviewer, 정철진 FE 영향 확인
- **결정 시점**: Day 2 JWT 정책 결정 회의

검토 선택지:

| 방식 | 장점 | 리스크 |
|---|---|---|
| `httpOnly Cookie` | XSS 방어에 유리 | CORS / cookie 설정 필요 |
| `localStorage` | 구현 단순 | XSS 위험 |
| memory + refresh token Cookie | 보안 우선 | 구현 복잡도 증가 |

### B-03. Google OAuth 세부 설정 확인 필요

- **현재 정리된 내용**: Google OAuth는 Sprint 1 포함이다.
- **남은 Blocker**: Google Client ID/Secret 준비, redirect URI 등록, callback 처리 방식 확인이 필요하다.
- **영향**: 소셜 로그인 API와 로그인 화면 연동 지연
- **해결 담당**:
  - OAuth 앱 등록: 황스일 Owner, 김민정 지원, 여도현 Reviewer
  - 소셜 로그인 API: 여도현 Owner, 황스일 Reviewer
  - Frontend 연동: 정철진 Owner
- **결정 시점**: Day 1~Day 3
- **확인 필요**:
  - Google Cloud Console 앱 등록 여부
  - Client ID/Secret 관리 방식
  - local/dev redirect URI
  - backend callback endpoint
  - frontend callback route 필요 여부
  - callback 성공 후 token 직접 반환 vs token 교환 code 전달 방식

### B-04. 로그인/회원가입/logout/me API 실제 응답 확인 필요

- **현재 정리된 내용**: API 명세 v2.3 기준 응답 형식은 존재한다.
- **남은 Blocker**: 실제 backend 구현 응답이 명세와 일치하는지 확인해야 한다.
- **영향**: 로그인·회원가입 화면 API 연동 지연
- **해결 담당**:
  - 이메일 회원가입/로그인/logout/me API: 김민경 Owner, 황스일 Reviewer
  - Frontend API 연동: 정철진 Owner, 황스일 Reviewer
  - Postman/API 문서화: 김민정 Owner, 김태준 Reviewer
- **결정 시점**: Day 4 전
- **기준**:
  - 성공: `{ data, meta }`
  - 실패: `{ error: { code, message, details }, meta }`

### B-05. `/auth/me` 사용자 context 적용 범위 확인 필요

- **현재 정리된 내용**: API v2.3에는 `/auth/me` 응답 예시가 있다.
- **남은 Blocker**: Sprint 1 축소 범위에서 Frontend가 실제로 사용할 최소 context를 확인해야 한다.
- **영향**: 로그인 유지, 보호 route, user/company/project context 처리 기준 불명확
- **해결 담당**: 김민경 Owner, 정철진 Frontend 확인, 황스일 Reviewer
- **결정 시점**: 인증 상태 처리 전
- **확인 필요**:
  - user 기본 정보
  - companyMemberships 사용 여부
  - projectMemberships 사용 여부
  - unreadNotificationCount / dmUnreadCount 사용 여부

### B-06. 비밀번호 재설정 이메일 발송 방식 확인 필요

- **현재 정리된 내용**: 비밀번호 재설정 API는 Sprint 1 포함이다.
- **남은 Blocker**: Sprint 1에서 실제 이메일 발송 없이 로그/Mock으로 대체할지 팀 합의가 필요하다.
- **영향**: Day 5 인증 E2E 검증 범위와 증빙 방식 불명확
- **해결 담당**: 김태준 Owner, 황스일 Reviewer, 정철진 Frontend 영향 확인
- **결정 시점**: Day 4 전
- **확인 필요**:
  - reset token 발급 방식
  - 이메일 발송 로그 출력 대체 여부
  - reset request/confirm API 요청/응답 예시
  - Frontend 비밀번호 재설정 화면 포함 여부

### B-07. Cursor pagination은 Sprint 1 인증 FE blocker가 아님

- **현재 정리된 내용**: API v2.3에는 `{ data, page, meta }`와 `page.cursor`, `hasNext`, `size` 예시가 있다.
- **정리 방향**: 로그인/회원가입 중심 Sprint 1 FE의 직접 blocker로 두지 않는다.
- **후속 처리**: 다음 Sprint에서 목록 화면을 구현할 때 TanStack Query, `useInfiniteQuery`, cursor field mapping을 확인한다.

---

## 8. 확정 항목 / 미정 항목

### 8.1 확정 항목

| 항목 | 내용 |
|---|---|
| Frontend 기반 | React + Next |
| 인증 방식 | JWT Bearer Token |
| 인증 헤더 | `Authorization: Bearer <token>` |
| 소셜 로그인 | Google OAuth Sprint 1 포함 |
| 단일 응답 형식 | `{ data, meta }` |
| error response | `{ error: { code, message, details }, meta }` |
| Frontend 초기 검증 | `npm run dev` 또는 합의된 실행 명령으로 route/화면 브라우저 확인 |

### 8.2 미결 / 확인 필요 항목

| 구분 | 항목 | 현재 상태 | Owner / 확인자 | 결정/확인 시점 |
|---|---|---|---|---|
| 미결 | App Router vs Pages Router | 미정 | 정철진, 팀 전체 | Sprint 1 시작 전 |
| 미결 | package manager | 미정 | 정철진, 팀 전체 | Sprint 1 시작 전 |
| 확인 필요 | TypeScript 적용 여부 | 확인 필요 | 정철진 | Sprint 1 시작 전 |
| 확인 필요 | Tailwind CSS 적용 여부 | 확인 필요 | 정철진 | UI 구현 전 |
| 확인 필요 | Zustand/TanStack Query 적용 여부 | 확인 필요 | 정철진 | API 연동 전 |
| 미결 | JWT 저장 위치 | 미정 | 황스일, 여도현, 김태준 | Day 2 |
| 미결 | refresh token 사용 여부/갱신 전략 | 미정 | 황스일, 여도현, 김태준 | Day 2 |
| 미결 | Google OAuth Client ID/Secret | 준비 필요 | 황스일, 김민정, 여도현 | Day 1 |
| 미결 | Google OAuth redirect URI | 확인 필요 | 황스일, 김민정, 여도현 | Day 1~3 |
| 확인 필요 | `/auth/me` Sprint 1 최소 context | API 예시 존재, 적용 범위 확인 필요 | 김민경, 정철진, 황스일 | 인증 상태 처리 전 |
| 확인 필요 | 로그인/회원가입/logout/me API 실제 응답 | 명세 기준 존재, 구현 확인 필요 | 김민경, 정철진, 황스일 | Day 4 전 |
| 미결 | 비밀번호 재설정 이메일 발송 방식 | 로그/Mock 대체 여부 확인 필요 | 김태준, 황스일 | Day 4 전 |
| 후순위 | pagination `page` 세부 필드 | API 기준 존재, Sprint 1 인증 FE 직접 blocker 아님 | 정철진, API 담당 | 목록 화면 구현 전 |
| 확인 필요 | query key 기준 | Frontend 내부 기준 정리 필요 | 정철진 | API 연동 전 |

---

## 9. 프론트엔드 Definition of Done

### 9.1 공통 DoD

| 기준 | 완료 조건 |
|---|---|
| 범위 | Sprint 1 Backlog 또는 합의된 Frontend 작업 범위만 반영한다 |
| 정상 케이스 | 대표 성공 흐름을 브라우저에서 확인한다 |
| 예외 케이스 | 필수값 누락, 인증 실패, token 없음/만료 등 주요 실패 조건을 확인한다 |
| API 일치 | API 명세서 v2.3의 Auth request / response / error 형식과 일치한다 |
| 보안 | token, secret, password, 개인정보가 코드, 문서, 로그에 노출되지 않는다 |
| 정리 | 임시 로그, debug code, 불필요한 주석 처리 코드가 남아 있지 않다 |
| 증빙 | 확인 route, 브라우저 확인 기록, API 요청/응답 예시 중 필요한 증빙을 남긴다 |
| 후속 조치 | 미완료 또는 미확인 항목은 사유, owner, 처리 시점을 기록한다 |

### 9.2 프론트엔드 작업 유형별 DoD

| 작업 유형 | 완료 조건 | 최소 증빙 |
|---|---|---|
| 프로젝트 구조 | 합의된 Next routing 방식 기준으로 route와 layout이 생성되어 브라우저에서 접근 가능하다 | 확인 route, 브라우저 확인 기록 |
| UI 구현 | 로그인·회원가입 화면의 주요 layout과 입력 흐름이 동작한다 | 화면 확인 기록 |
| API 연동 | 성공, 실패, loading 상태를 구분 처리한다 | API 응답 예시, 브라우저 확인 기록 |
| 인증 상태 | 로그인 후 token 처리, `/auth/me`, 로그아웃 흐름이 기준에 맞게 동작한다 | token 발급/차단 확인, `/auth/me` 확인 |
| Google OAuth | OAuth 시작과 callback 처리 방식이 API 기준과 맞는다 | OAuth redirect/callback 확인 |
| 비밀번호 재설정 | Sprint 1 FE 범위에 포함하기로 한 경우 reset request/confirm 흐름이 동작한다 | API 요청/응답 예시, 화면 확인 기록 |
| 문서 | 팀원이 문서만 보고 다음 행동을 할 수 있고 관련 문서와 충돌하지 않는다 | 문서 링크, 검토 결과 |
| PR | 변경 내용, 검증 결과, 영향 범위, 보안 체크가 작성된다 | PR 본문 또는 작업 기록 |

### 9.3 완료로 보지 않는 경우

| 경우 | 이유 |
|---|---|
| 구현만 있고 브라우저 확인 기록이 없다 | 사용자 관점 동작을 확인할 수 없다 |
| 정상 케이스만 확인했다 | 실패 케이스에서 실제 사용자가 막힐 수 있다 |
| API 변경 또는 연동인데 요청/응답 예시가 없다 | API 명세와 실제 동작 일치 여부를 확인할 수 없다 |
| build 또는 test 실패가 남아 있는데 사유가 없다 | merge 가능 상태로 판단할 수 없다 |
| token 저장 위치가 불명확한 상태로 인증 구현을 완료 처리했다 | 보안 및 유지보수 리스크가 남는다 |
| 미확인 항목의 owner와 처리 시점이 없다 | 후속 작업으로 추적되지 않는다 |
| 미정 항목을 확정처럼 문서화했다 | 팀 합의 전 결정으로 오해될 수 있다 |

---

## 10. Sprint 1 착수 전 확인 체크리스트

- [ ] App Router vs Pages Router 결정
- [ ] package manager 확정
- [ ] JWT 저장 위치 결정
- [ ] refresh token 사용 여부와 갱신 전략 결정
- [ ] Google OAuth Client ID/Secret 준비
- [ ] Google OAuth redirect URI 확정
- [ ] Google OAuth callback 처리 방식 확인
- [ ] `/auth/me` Sprint 1 최소 context 확인
- [ ] 로그인/회원가입/logout/me API 성공 응답 예시 확인
- [ ] error response JSON 실제 구현 확인
- [ ] 비밀번호 재설정 이메일 발송 로그/Mock 대체 여부 확인
- [ ] 비밀번호 재설정 화면을 Sprint 1 FE 범위에 포함할지 확인
- [ ] Zustand/TanStack Query 등 상태 관리 도입 여부 확인
- [ ] TanStack Query query key 기준 정리
- [ ] pagination은 후속 목록 화면 blocker로 이동 처리
