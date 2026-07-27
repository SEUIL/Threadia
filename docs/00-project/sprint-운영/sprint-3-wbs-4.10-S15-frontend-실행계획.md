> **작성자**: Threadia 팀
> **작성일**: 2026-07-16
> **최종 수정**: 2026-07-16
> **버전**: v0.2
> **상태**: 검토 중
> **태그**: sprint-3, wbs, frontend, platform-admin, s15

# Sprint 3 WBS 4.10 S15 Frontend 실행계획

## 1. 목적과 적용 범위

이 문서는 단일 Frontend 개발자가 S15 플랫폼 관리자 1차 범위를 구현할 때 사용할 coarse-grained 실행 WBS다. 기존 Sprint 3 Backlog의 parent `4.10` 및 실행 row `4.10.1~4.10.4`를 Frontend 관점에서 연결하며, component 단위로 분할하지 않는다.

현재 확정된 Backend API 계약은 WBS `4.10.1` support case, WBS `4.10.2` 회사 `INACTIVE` lifecycle/enforcement, WBS `4.10.3` system settings catalog다. `4.10.3`은 정확히 8개의 code-owned settings catalog에 대해 `GET /api/v1/platform/system-settings`, `GET /api/v1/platform/system-settings/{key}`, `PUT /api/v1/platform/system-settings/{key}`를 제공한다. 조회는 `PLATFORM_ADMIN`과 `SUPER_ADMIN`, whole-document replacement PUT은 `SUPER_ADMIN`만 수행하며 strict typed JSON, `expectedVersion` 불일치와 no-op 변경의 409 code, audit 변경값 redaction을 계약에 포함한다. `4.10.4` audit/operation logs 및 platform admin lifecycle/grant/revoke는 정책만 확정됐고 endpoint, DTO, error code, 화면 계약은 미정이다. `4.10.2`의 사용자 정지/해제 API와 platform actor 전달 계약도 아직 Frontend 연동 기준으로 확정되지 않았다. 따라서 이 문서는 미정 계약을 새로 만들지 않으며, 해당 항목은 계약 확정을 착수 조건으로 둔다.

## 2. 현재 Frontend 기준과 공통 경계

| 항목 | 현재 기준 | WBS 적용 원칙 |
|---|---|---|
| 인증 진입 | `frontend/app/(app)/layout.tsx`가 인증되지 않은 사용자를 `/login`으로 이동시킨다. | platform console도 기존 인증 session 복원과 app-level gate를 재사용한다. |
| 회사 workspace | `frontend/app/(app)/[companyId]/layout.tsx`가 회사 경로에만 `RailShell`을 적용한다. | S15는 회사/프로젝트 workspace와 분리된 platform-level console이므로 회사 route 또는 membership role을 재사용하지 않는다. |
| 권한 원천 | 현재 Frontend는 `companyService.getMyMemberships()`와 `useCurrentCompanyRole()`로 회사 role을 다룬다. | `COMPANY_ADMIN`, `PROJECT_ADMIN` 또는 `users.role`을 platform 권한 판정에 사용하지 않는다. platform actor 정보의 제공 계약이 필요하다. |
| API 계층 | `frontend/services/`의 service와 `frontend/types/`의 response type, `ApiResponse` envelope를 사용한다. | 승인된 Backend 계약에만 service/type를 추가하고, 미정 endpoint는 추측해 구현하지 않는다. |
| 브라우저 검증 | `frontend/tests/smoke/`의 Playwright route mock 기반 smoke 패턴을 사용한다. | 계약된 API는 mock smoke와 실제 통합/e2e 후보를 분리해 검증한다. |

## 3. 단일 개발자 실행 WBS

| ID | 작업 | 결과물 | Backend 의존성 | 수용 기준 |
|---|---|---|---|---|
| `4.10-FE-1` | Platform console shell, route, 접근 경계 | 회사 workspace와 분리된 platform console 진입 구조와 공통 loading/unauthorized/error 상태를 추가한다. 메뉴 노출은 platform actor 확인 이후에만 수행한다. | platform actor를 판정할 응답 필드 또는 전용 조회 계약이 필요하다. 현재 `/auth/refresh`, `/auth/me`, 회사 membership 응답에는 이 계약이 없다. 구체 platform route 문자열도 미정이다. | 비로그인 사용자는 기존 인증 흐름으로 처리한다. A/P/C actor는 platform 화면 및 변경 action에 진입할 수 없다. `PLATFORM_ADMIN`과 `SUPER_ADMIN`의 UI capability 차이는 승인된 권한표만 반영한다. UI 노출은 보조 수단이며 server-side 인가를 대체하지 않는다. |
| `4.10-FE-2` | Support case 사용자/관리자 vertical slice | 승인된 `4.10.1` 계약을 사용해 일반 사용자의 case 생성·본인 목록/상세와 platform 관리자의 전체 목록·상세, public reply, internal note, 상태 전이 화면을 하나의 흐름으로 연결한다. | `4.10.1` API 계약: `/api/v1/support-cases`와 `/api/v1/platform/support-cases` namespace, response envelope, 상태 전이, reason 규칙. Backend가 해당 authorization 및 audit 원자성을 제공해야 한다. | `INQUIRY`와 `REPORT` 입력 경계를 계약대로 구분하고, 허용 상태 전이만 선택 가능하다. 모든 mutation은 별도 reason(공백 제거 후 3~500자)을 전송한다. 일반 사용자 화면에는 internal note와 타 사용자 case가 노출되지 않는다. 처리 결과 notification은 표시·발송 범위에 포함하지 않는다. |
| `4.10-FE-3` | User/company enforcement UI | 플랫폼 관리자가 사용자 `SUSPENDED` 및 회사 `INACTIVE` 상태를 조회·변경하는 운영 흐름을 구현한다. 조치 사유, 성공/실패 상태, 영향 안내를 해당 정책 범위에서만 제공한다. | `4.10.2`는 회사 `INACTIVE` 전환 API와 Backend enforcement가 구현됐다. 다만 사용자 목록·상세, suspend/activate, 회사 restore, platform actor 조회, Frontend 연동용 DTO/error code 계약은 미정이다. 회사 `INACTIVE` 차단과 selector 숨김은 Backend가 강제한다. | 승인된 계약 전에는 가짜 endpoint, 임시 성공 상태, mock 완료 선언을 추가하지 않는다. 계약 후에는 hard delete 없이 상태 전이만 요청하고 reason을 필수로 처리한다. `INACTIVE` 회사가 사용자 회사 선택기에서 숨겨지고 회사·하위 프로젝트 read/mutation이 차단되는 실제 Backend 결과를 통합 검증한다. platform 운영 접근은 유지되는 계약을 확인한다. |
| `4.10-FE-4` | System settings 관리 UI | `SUPER_ADMIN` 전용 운영 settings catalog의 조회·변경 UI를 구현한다. catalog의 key별 typed value와 versioning은 Backend 계약을 그대로 표현한다. | `4.10.3` API 계약은 확정됐다. 정확히 8개의 code-owned catalog에 대해 `GET /api/v1/platform/system-settings`, `GET /api/v1/platform/system-settings/{key}` 조회와 `PUT /api/v1/platform/system-settings/{key}` whole-document replacement를 사용한다. 조회는 `PLATFORM_ADMIN`과 `SUPER_ADMIN`, PUT은 `SUPER_ADMIN`만 가능하다. strict typed JSON, `expectedVersion` 불일치와 no-op 변경의 409 code를 처리하며, Backend의 redacted audit snapshot 내부 기록 보장에 의존한다. platform actor 조회와 console route 계약은 별도 `4.10-FE-1` 의존성으로 남는다. | `PLATFORM_ADMIN`에게 settings 변경 control을 제공하지 않는다. `SUPER_ADMIN`은 승인된 8개 catalog key만 조회·변경하며, PUT은 전체 문서 replacement와 현재 version을 사용한다. typed JSON 오류, `expectedVersion` 불일치와 no-op 변경의 409 code를 성공으로 표시하지 않는다. settings frontend는 Backend의 redacted audit snapshot 내부 기록 보장에 의존하며, audit 표시 검증은 `4.10.4` audit read API 계약 뒤 `4.10-FE-5`로 유보한다. DB/인프라 runtime, OAuth provider on/off, 전역 feature flag, 가입/domain 제한은 UI 범위 밖이다. |
| `4.10-FE-5` | Audit/operation logs 및 platform admin lifecycle UI | platform audit 조회와 `SUPER_ADMIN`의 platform admin grant/revoke UI를 정책 범위 내에서 연결한다. audit은 운영 감사 원장으로만 표시하고 활동 피드·알림과 합치지 않는다. | `4.10.4`는 policy-only다. audit keyset pagination response/cursor, payload masking, retention 종료 처리, grant/revoke 및 lifecycle endpoint·DTO·error code, 승인 workflow 계약이 확정돼야 한다. | audit 조회는 keyset pagination 계약을 사용하고 password, token, secret, message/case body 원문을 표시하지 않는다. grant/revoke는 `SUPER_ADMIN`만 실행하며 reason을 요구한다. 마지막 활성 `SUPER_ADMIN` revoke/demote 금지 응답을 UI가 정확히 처리한다. 승인 workflow가 미정인 동안 이를 UI로 가정하지 않는다. |
| `4.10-FE-6` | API client/type 정합성 및 smoke/e2e coverage | 위 vertical slice의 service/type, 공통 API 오류 처리, Playwright smoke와 Backend 연동 e2e evidence를 정리한다. 기능별 component test로 과분할하지 않고 권한·가시성·변경 흐름을 우선 검증한다. | `4.10.1`과 `4.10.3`은 확정 API를 즉시 대상으로 한다. `4.10.2`와 `4.10.4`는 각각의 계약과 test fixture가 확정된 뒤 추가한다. `4.10.3` UI는 별도 platform actor 조회와 console route 계약도 필요하다. Frontend test 환경은 `frontend/playwright.config.ts`와 기존 route mock 패턴을 따른다. | service/type은 승인된 endpoint와 DTO에만 대응하고 기존 `ApiResponse` envelope를 유지한다. support case smoke는 인증, 일반 사용자 소유권, internal note 비노출, platform 처리, invalid reason/상태 전이를 다룬다. system settings smoke는 8개 catalog의 list/single-key 조회, `SUPER_ADMIN` PUT whole-document replacement, `PLATFORM_ADMIN` 변경 control 비노출, strict typed JSON, `expectedVersion` 및 no-op 409 code를 다룬다. audit snapshot은 Backend의 redacted 내부 기록 보장에 의존하며, audit payload 표시·검증은 `4.10.4` audit read API 계약 뒤 `4.10-FE-5`로 유보한다. `4.10.2`와 `4.10.4` 후속 정책 항목은 A/P/C 거부, `PLATFORM_ADMIN`/`SUPER_ADMIN` capability 차이, reason, audit 민감정보 비노출, contract error를 포함한다. 실제 e2e는 Backend 준비 상태와 별도로 mock smoke 통과를 대체하지 않는다. |

## 4. 순서와 Backend 의존성

1. `4.10-FE-1`은 platform actor 조회 계약과 route 결정 후 시작한다. `4.10-FE-2`는 확정된 `4.10.1` support case 계약을 기준으로 독립 착수할 수 있지만, 관리 console 진입은 `4.10-FE-1`의 actor 경계를 따른다.
2. `4.10-FE-3`과 `4.10-FE-5`는 각각 `4.10.2`와 `4.10.4`의 API/DTO/error contract가 승인된 뒤 착수한다. `4.10-FE-4`는 확정된 `4.10.3` settings API를 대상으로 service/type과 화면 흐름을 준비할 수 있으나, platform actor 조회와 console route 계약은 `4.10-FE-1`의 착수 조건으로 유지한다. 정책 문서만으로 미정 service, type, route, action을 선구현하지 않는다.
3. `4.10-FE-6`은 각 승인 계약 및 구현 결과에 맞춰 누적한다. 최종 e2e는 target Backend와 test account/fixture가 준비된 뒤 실행하며, smoke mock은 그 전에도 회귀 검증으로 유지한다.

## 5. 명시적 제외 범위

- 새로운 Backend route, schema, API request/response, error code, platform actor 전달 방식의 제안 또는 구현
- 정책 미확정 상태의 enforcement 해제 절차, 확정된 8개 system settings catalog 밖의 key·schema·version 충돌 방식, audit payload masking·보존 종료 방식, platform admin 승인 workflow/lifecycle 세부 설계
- 운영 현황 dashboard 집계, 집계 source, refresh 정책
- support case 처리 결과 notification의 수신자, 채널, 발송 UI
- DB/인프라 runtime 설정, OAuth provider on/off, 전역 feature flag, 가입/domain 제한
- 결제/구독, 고급 통계/BI, 실시간 상담 채팅, 관리자 조직/부서 관리, 자동 제재 rule engine
- 회사/프로젝트 workspace의 기존 A/P/C membership 화면을 platform console로 재활용하거나 `users.role`을 추가하는 변경

## 6. 기준 문서

- `docs/00-project/sprint-운영/sprint-3-backlog.md`
- `docs/00-project/sprint-운영/sprint-2-wbs-3.7.1-S15-기능-WBS-분해.md`
- `docs/00-project/sprint-운영/sprint-2-wbs-3.7.3-S15-화면-권한-flow.md`
- `docs/02-requirements/S15-플랫폼-관리자-1차-범위-정의.md`
- `docs/04-api/API-명세서.md`
- `docs/07-decisions/플랫폼-관리자-인가-모델-결정.md`
- `docs/07-decisions/sprint-3-범위-결정.md`

## 7. 변경 이력

| 일자 | 버전 | 변경 내용 |
|---|---|---|
| 2026-07-16 | v0.2 | `4.10.3`을 policy-only 의존성에서 확정된 8개 code-owned system settings catalog API 의존성으로 갱신했다. GET list/single key, `SUPER_ADMIN` PUT whole-document replacement, strict typed JSON, `expectedVersion` 및 no-op 409 code, audit redaction과 Frontend 범위를 반영했다. |
| 2026-07-16 | v0.1 | S15 WBS `4.10`의 단일 Frontend 개발자용 coarse-grained 실행 계획을 최초 작성했다. `4.10.1` 확정 API와 `4.10.2~4.10.4` 정책-only 의존성을 분리했다. |
