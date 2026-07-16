> **작성자**: Threadia 팀
> **작성일**: 2026-07-13
> **최종 수정**: 2026-07-16
> **버전**: v0.3
> **상태**: 검토 중
> **태그**: sprint-2, wbs, platform-admin, erd, api

# Sprint 2 WBS 3.7.2 S15 ERD API 영향도 초안

## 1. 목적과 비확정 경계

이 문서는 S15의 ERD와 API 영향 영역을 식별하는 planning gate 초안이다. 새로운 DB schema, column, FK, index, endpoint, request/response body, error code를 확정하지 않는다.

S15는 `3.7~3.7.5` planning gate 이후 Sprint 3 공식 WBS `4.10`에서 추적한다. 현재 canonical ERD와 API 명세서는 S15의 concrete schema 및 full endpoint contract를 decision-needed로 둔다.

## 2. 신규 분리 domain

| domain | 필요성 | 현재 경계 |
|---|---|---|
| `platform_admins` | 플랫폼 운영 권한을 회사와 프로젝트 membership에서 분리한다. | `SUPER_ADMIN`만 grant/revoke할 수 있고 reason 및 immutable audit이 필수다. 마지막 활성 `SUPER_ADMIN` revoke/demote 금지의 구현 방식과 상세 lifecycle은 후속 설계 대상이다. |
| `support_cases` | 문의와 신고를 `INQUIRY`와 `REPORT`로 구분하는 하나의 case 개념으로 다룬다. | case 상세 필드, 답변 표현, 대상 참조 방식, 담당자 배정 규칙은 확정하지 않는다. |
| `admin_action_audits` | 관리자 조치의 reason, 행위자, 대상, 변경 전후, 가능한 case 연결을 불변 감사 원장으로 남긴다. | 3년 보존, 민감 payload 제외 또는 마스킹, platform admin keyset 조회를 정책으로 둔다. payload 형식, masking 방식, 보존 종료 후 삭제 방식, index는 후속 설계 대상이다. |

`admin_action_audits`는 planning gate에서 요구되는 불변 audit domain 이름이다. 기존 `activity_events`, `notification_events`, 사용자-facing 알림, 시스템 로그와 통합하지 않는다.

## 3. 기존 domain 영향

| 기존 영역 | S15 영향 | 확정하지 않는 사항 |
|---|---|---|
| `users` | 사용자 정지는 기존 `SUSPENDED` 상태 전환 및 `ACTIVE` 인증 조건과 연결된다. | `users.role` 추가, 신규 사용자 권한 field, 정지 해제 API 계약을 정하지 않는다. |
| `companies` | 회사 비활성화는 `INACTIVE` 상태 전환이며 모든 회사 및 하위 project read/mutation을 차단하고 사용자 회사 선택기에서 숨긴다. platform admin 운영 접근은 유지한다. | 구체 상태 저장, 차단 구현, 복구 절차를 정하지 않는다. |
| `company_members` | 회사 역할은 기존 회사 범위의 권한 판단에만 남긴다. | platform permission 추가를 금지한다. |
| `project_members` | 프로젝트 역할은 기존 프로젝트 범위의 권한 판단에만 남긴다. | platform permission 추가를 금지한다. |
| `notification_events`, `notification_recipients` | support case 처리 결과 알림은 향후 연계 후보가 될 수 있다. | 알림 생성 조건, 수신자, 채널, UI를 정하지 않는다. |
| `activity_events` | platform audit와 역할을 분리한다. | platform action을 project activity 원장에 기록하는 계약을 정하지 않는다. |

`users.role`과 membership 기반 platform permission은 모두 금지한다. `COMPANY_ADMIN` 또는 `PROJECT_ADMIN` 여부만으로 플랫폼 API와 화면 action을 허용해서는 안 된다.

## 4. API 영향 초안

| `4.10` 실행 row | 필요 API 범주 | planning gate 결론 |
|---|---|---|
| `4.10.1` | support case 생성, 목록, 상세, 답변 또는 처리 메모, 상태 전이 | endpoint와 body는 미정이다. case 상태 전이와 reason/audit 연계를 먼저 설계한다. |
| `4.10.2` | 사용자 정지 또는 해제, 회사 비활성화 또는 해제 | `INACTIVE` 회사의 read/mutation 차단, selector 숨김, platform admin 운영 접근 유지까지 정책으로 확정했다. 해제 조건과 에러 계약은 미정이다. |
| `4.10.3` | 설정 조회와 변경 | full operating-settings catalog를 per-key code-owned schema/validator, JSON value, versioning typed catalog로 저장하는 정책을 확정했다. key 이름, endpoint, 저장 구조는 미정이며 DB와 인프라 runtime 설정 API는 범위 밖이다. |
| `4.10.4` | audit/operation logs 및 platform admin lifecycle/grant/revoke | 3년 보존, 민감 payload 제외 또는 마스킹, platform admin keyset 조회와 `SUPER_ADMIN` 전용 grant/revoke를 정책으로 확정했다. API, 승인 workflow, masking 및 보존 종료 처리 방식은 미정이다. |

플랫폼 API는 회사 또는 프로젝트 resource path에 종속되지 않는 별도 namespace를 후속 화면 및 API 설계에서 정한다. 현 단계에서 구체 endpoint를 만들지 않는다.

## 5. 선행 결정 목록

1. `platform_admins`의 `SUPER_ADMIN` 전용 grant/revoke와 마지막 활성 `SUPER_ADMIN` 보호 정책은 확정됐다. 역할 lifecycle 구현, 활성 상태 표현과 API 계약을 결정해야 한다.
2. `support_cases`의 상태 전이 허용표, 답변과 내부 처리 메모의 구분, 신고 대상 표현을 결정해야 한다.
3. 사용자 정지와 회사 비활성화의 해제 권한, 실패 및 복구 기준을 결정해야 한다.
4. `admin_action_audits`의 immutable 보존 구현, payload masking 방식, 보존 종료 처리와 조회 API를 결정해야 한다.
5. 시스템 설정의 구체 key 이름, version 충돌 처리, 변경 전후 값의 audit 표현과 API 계약을 결정해야 한다.
6. 처리 결과 notification의 수신자와 발송 정책을 결정해야 한다.

## 6. canonical 반영 전제

이 문서는 영향 목록일 뿐 canonical ERD와 API의 변경 요청이 아니다. `3.7.4`의 implementation No-Go가 해제되고 승인된 `3.7.5` alignment가 시작된 뒤에만 실제 수정 대상과 변경 이력을 결정한다.

## 7. 기준 문서

- `docs/03-design/ERD.md`
- `docs/04-api/API-명세서.md`
- `docs/02-requirements/기능-시나리오-API-ERD-추적표.md`
- `docs/02-requirements/S15-플랫폼-관리자-1차-범위-정의.md`
- `docs/07-decisions/플랫폼-관리자-인가-모델-결정.md`
