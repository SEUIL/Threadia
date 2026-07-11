> **작성자**: 황스일
> **작성일**: 2026-07-08
> **최종 수정**: 2026-07-08
> **버전**: v1.0
> **상태**: 검토 중
> **태그**: sprint-2, wbs-3.2.5, company-frontend, rbac, qa

# Sprint 2 WBS 3.2.5 Company Frontend QA 결과

이 문서는 WBS 3.2.5("Company frontend 브라우저/빌드 검증")의 PR #62 기준 QA 결과다. WBS 3.2.5는 기능 구현이 아니라 WBS 3.2.1~3.2.4 구현 결과를 브라우저와 build 관점에서 검증하는 QA-only row다.

**검증 대상**: PR #62 `feat(company): 회사 프론트 진입점·설정·구성원·RBAC 실API 연동`  
**검증 기준 ref**: GitHub PR #62 merge ref, local branch `qa/pr-62-merge`, commit `e8612b4`  
**기준 문서**: `docs/00-project/sprint-운영/sprint-2-backlog.md`, `docs/00-project/Definition-of-Done.md`, `docs/00-project/검증-방법.md`

---

## 결론

S03/S04/S13/S14 관련 주요 route는 로그인, 권한별 진입, 관리자/일반 멤버 UI 확인 기준으로 대부분 정상 동작했다. 다만 `/hub`에서 WebGL context 생성 실패로 런타임 에러가 발생했으므로 WBS 3.2.5를 무조건 완료 처리하기보다는 **S03 허브 WebGL fallback 또는 환경 제약 명시가 필요**하다.

정적 검증에서는 backend test, TypeScript check, frontend build는 통과했다. `npm run lint`는 PR #62 변경 파일 외 기존 lint 이슈로 실패했다.

---

## 1. 실행 환경과 seed

| 항목 | 값 |
|---|---|
| Frontend | Next.js dev server, `http://localhost:3000` |
| Backend | Spring Boot bootRun, `http://localhost:8080` |
| DB | Docker MySQL 8.4, `localhost:3307`, `db/schema.sql` 적용 |
| 회사 | `Threadia QA Company`, `companyId=1` |
| 부서 | `QA Department`, `departmentId=1` |
| 프로젝트 | `QA Browser Project`, `projectId=1` |
| 계정 | 회사 관리자 1명, 일반 멤버 1명 |

QA 계정의 raw password, token, cookie 값은 문서에 남기지 않는다.

---

## 2. 명령 기반 검증 결과

| 검증 | 결과 | 메모 |
|---|---|---|
| `./gradlew test` | PASS | backend test 전체 통과 |
| `npm ci` | PASS | frontend dependency 설치 완료, npm audit moderate 2건 표시 |
| `npx tsc --noEmit` | PASS | TypeScript type error 없음 |
| `npm run build` | PASS | Next production build 성공 |
| `npm run lint` | FAIL | PR #62 변경 파일 외 기존 lint 이슈로 실패 |
| backend bootRun | PASS | `/actuator/health`는 SMTP password 미설정으로 503이지만 Tomcat/API는 정상 동작 |
| login API | PASS | admin/member 계정 모두 `POST /api/v1/auth/login` 200 확인 |

### lint 실패 항목

| 파일 | 내용 |
|---|---|
| `frontend/app/(app)/[companyId]/[projectId]/graph/page.tsx` | `react-hooks/set-state-in-effect` |
| `frontend/app/(app)/[companyId]/notifications/page.tsx` | unused `companyId` warning |
| `frontend/design-sample/src/app/layouts/page.tsx` | `react-hooks/refs` |

위 lint 항목은 PR #62 변경 파일 목록에 직접 포함되지 않은 기존 이슈로 판단한다.

---

## 3. API/RBAC 확인 결과

| 계정 | API | 결과 | 판정 |
|---|---|---:|---|
| 회사 관리자 | `GET /companies/1` | 200 | PASS |
| 회사 관리자 | `GET /companies/1/stats` | 200 | PASS |
| 회사 관리자 | `GET /companies/1/departments` | 200 | PASS |
| 회사 관리자 | `GET /companies/1/company-members` | 200 | PASS |
| 회사 관리자 | `GET /companies/1/projects` | 200 | PASS |
| 일반 멤버 | `GET /companies/1` | 200 | PASS |
| 일반 멤버 | `GET /companies/1/stats` | 403 | PASS |
| 일반 멤버 | `GET /companies/1/departments` | 403 | PASS |
| 일반 멤버 | `GET /companies/1/company-members` | 403 | PASS |

회사 관리 통계, 부서, 구성원 API는 일반 멤버에게 403을 반환해 backend RBAC 기준과 충돌하지 않는다.

---

## 4. 브라우저 확인 결과

| 순서 | 화면/route | 확인 항목 | 결과 | 판정 |
|---:|---|---|---|---|
| 1 | `/login` | 로그인 화면 접속 | 정상 동작 | PASS |
| 2 | 관리자 로그인 | 회사 관리자 계정 로그인 | 정상 동작 | PASS |
| 3 | `/hub` | S03 허브 진입 | WebGL context 생성 실패 런타임 에러 | FAIL |
| 4 | `/1/projects` | 프로젝트 목록, `+ 새 프로젝트` 버튼 | 정상 동작 | PASS |
| 5 | `/1/settings` | 회사 정보, 통계, 부서 목록, 수정 버튼, 구성원 관리 진입 | 정상 동작 | PASS |
| 6 | `/1/members` | 구성원 목록, 구성원 추가 UI, role/department select | 정상 동작 | PASS |
| 7 | `/1/1/backlog` | 백로그/스프린트 렌더링, 관리자 기준 `+ 새 스프린트` | 정상 동작 | PASS |
| 8 | `/1/1/board` | 칸반 보드, 카드 상세, 상태 변경 UI | 정상 동작 | PASS |
| 9 | `/1/1/settings` | 프로젝트 설정, 멤버 관리, archive UI | 정상 동작 | PASS |
| 10 | 일반 멤버 로그인 | 로그아웃 후 일반 멤버 계정 로그인 | 정상 동작 | PASS |
| 11 | 동일 route 재확인 | 일반 멤버 기준 route 재확인 | 정상 동작 | PASS |

### `/hub` WebGL 에러

`/hub` 진입 시 브라우저 console에 다음 계열의 에러가 발생했다.

```text
THREE.WebGLRenderer: A WebGL context could not be created.
THREE.WebGLRenderer: Error creating WebGL context.
```

관찰된 세부 메시지는 ANGLE/NVIDIA Direct3D9Ex 경로에서 `BindToCurrentSequence failed`로 WebGL context 생성이 실패했다는 내용이다. 현재 `/hub`는 `@react-three/fiber` `Canvas`를 바로 렌더링하므로 WebGL을 사용할 수 없는 환경에서 fallback 없이 runtime error로 이어진다.

---

## 5. 완료 조건별 판단

| WBS 3.2.5 완료 조건 | 결과 | 판단 |
|---|---|---|
| route skeleton만이 아니라 사용자 흐름 확인 | 로그인 후 주요 route 직접 확인 완료 | 부분 충족 |
| loading/empty/error/forbidden 상태 확인 | backend 403과 route별 error 성격 확인, 다만 모든 상태 조합은 별도 자동화 없음 | 부분 충족 |
| 권한별 메뉴/action 제한 확인 | 관리자/일반 멤버 계정으로 주요 route 재확인 | 충족 |
| lint/typecheck/build 가능 범위 확인 | typecheck/build PASS, lint 기존 이슈로 FAIL | 부분 충족 |
| 브라우저 route 확인 | `/hub` 제외 주요 route PASS | 부분 충족 |

---

## 6. 후속 조치 제안

1. `/hub`에 WebGL context 생성 실패 fallback을 추가하거나, WebGL 미지원 환경에서는 3D Canvas 대신 안내/대체 entry UI를 표시한다.
2. `npm run lint` 실패 항목은 PR #62와 별도 이슈 또는 후속 fix PR로 정리한다.
3. `3.2.5` 완료 처리 전, `/hub` WebGL 에러를 수정하거나 팀 합의로 환경 제약을 명시해야 한다.
