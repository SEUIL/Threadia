> **작성자**: Threadia 팀
> **작성일**: 2026-07-13
> **최종 수정**: 2026-07-18
> **버전**: v0.4 WBS 4.10.4 Backend 완료 주석
> **상태**: 검토 중
> **태그**: sprint-2, wbs, platform-admin, s15, planning

# Sprint 2 WBS 3.7.1 S15 기능 WBS 분해 초안

## 1. 목적과 경계

이 문서는 `DEFER-01`과 S15의 1차 범위를 Sprint 3 공식 WBS `4.10`의 실행 후보로 분해한 **historical planning gate 산출물**이다. 아래 planning 당시 미착수 표현은 당시 상태를 보존하며, 현재 `4.10.4` Backend lifecycle/audit-read는 구현됐다.

S15의 공식 WBS ID는 `4.10`이다. 과거 문서의 `4.9` S15 표현은 `3.7.5`의 승인된 canonical-document alignment 전까지 수정하지 않는다.

## 2. 4.10 분해 기준

| WBS ID | 작업명 | 1차 범위 | planning gate 판정 |
|---|---|---|---|
| `4.10.1` | support cases 구현 범위 확정/구현 | 문의와 신고를 `support_cases`로 통합하고 `INQUIRY`와 `REPORT`를 구분한다. 접수, 처리 중, 해결, 반려, 종료의 상태 전이와 답변 또는 처리 메모를 다룬다. | 구현 후보이나 상세 API, 데이터 구조, 알림 정책은 미정이다. |
| `4.10.2` | user/company enforcement 구현 | 플랫폼 관리자가 사용자 정지와 회사 비활성화를 상태 변경으로 수행하는 범위를 다룬다. | 정책은 확정됐지만 구현은 미착수다. 회사는 `INACTIVE`이며, 차단의 구현 방식과 해제 절차는 후속 구현 설계 대상이다. |
| `4.10.3` | system settings 구현 범위 확정 | 운영 설정의 전체 catalog 범위와 저장 정책을 확정한다. | 정책 결정은 확정됐지만 구현은 미착수다. 설정 key 이름, schema 저장 구조, API와 변경 절차는 후속 구현 설계 대상이다. |
| `4.10.4` | audit/operation logs 및 platform admin lifecycle/grant/revoke 구현 | 플랫폼 관리자 조치의 행위자, 대상, 사유, 변경 전후, 가능한 `support_case` 연결과 platform admin lifecycle/grant/revoke를 다룬다. | planning 당시에는 미착수였으나 현재 Backend lifecycle/audit-read는 구현됐다. S15 Frontend, 보존 종료 cleanup/purge, migration은 후속 범위다. |

## 3. 기능별 범위와 보류 경계

### 3.1 `4.10.1` support cases

문의와 신고는 별도 기능으로 분기하지 않고 `support_cases`라는 하나의 case 개념으로 관리한다. 유형은 `INQUIRY`와 `REPORT`로 구분한다. 처리 담당자는 플랫폼 관리자이며, 상태 변경과 답변 또는 처리 메모는 조치 이력과 연결되어야 한다.

처리 결과 알림은 후속 알림 설계에서 결정한다. 알림 발송 여부, 수신자, 채널, 실패 처리 방식은 이번 분해에서 확정하지 않는다.

### 3.2 `4.10.2` 사용자 정지와 회사 비활성화

사용자 정지는 기존 `users.status = SUSPENDED` 방향을 사용하며, 물리 삭제로 처리하지 않는다. 정지된 사용자는 로그인, token refresh, `/auth/me`의 기존 `ACTIVE` 조건에서 제외되는 기준과 연결된다.

회사 조치는 `INACTIVE` 상태 전환으로 다룬다. `INACTIVE` 회사는 모든 회사 및 하위 프로젝트 read와 mutation 접근을 차단하고 사용자 회사 선택기에서 숨긴다. 플랫폼 관리자의 운영 접근은 유지한다. 해제 절차와 차단의 구현 방식은 후속 설계 대상이다. 두 조치 모두 reason과 관리자 audit 기록이 필요하다.

### 3.3 `4.10.3` 시스템 설정

시스템 설정은 문의/신고 카테고리, 제재 사유 및 기간 preset, 답변 템플릿, 공지 또는 점검 배너, 고객센터 운영 상태, 처리 결과 알림 on/off를 포함하는 기존 운영 설정 전체 catalog로 확정한다. 각 설정은 code-owned per-key schema/validator, JSON value, versioning을 사용하는 typed catalog로 저장한다. DB/인프라 runtime 설정, OAuth provider on/off, 전역 feature flag, 가입 또는 domain 제한은 제외한다. 구체 key 이름, schema 저장 구조, version 충돌 처리, 변경 절차는 후속 구현 설계 대상이다.

대시보드 요약, 시스템 설정의 실제 변경 UI, 공지 또는 점검 배너 발행은 이 planning gate에서 확정하지 않는다. grant/revoke는 `SUPER_ADMIN`만 수행하고 reason과 immutable audit을 필수로 하며 마지막 활성 `SUPER_ADMIN`의 revoke/demote를 금지하는 정책은 확정했다. 현재 grant/revoke/reactivate/role change Backend는 구현됐고 등록 절차, 승인 workflow, S15 Frontend는 후속 설계 대상이다.

### 3.4 `4.10.4` audit/operation logs 및 platform admin lifecycle/grant/revoke

`PLATFORM_ADMIN`은 문의와 신고 처리, 사용자 정지, 회사 비활성화, 운영 로그 조회를 수행하는 역할이다. `SUPER_ADMIN`은 플랫폼 관리자 권한 부여와 회수, 시스템 설정 변경, 공지 또는 점검 배너, 제재 preset 관리를 수행하는 역할이다.

모든 플랫폼 관리자 조치는 reason과 audit log를 남긴다. audit log는 3년간 보존하고, payload에서 password, token, secret, message/case body 원문을 제외하거나 마스킹한다. platform admin은 keyset audit-read를 사용한다. audit log는 사용자 알림, 활동 피드, 시스템 로그와 다른 운영 감사 원장으로 분리한다. read-side allowlist redaction은 Backend에 구현됐고 S15 Frontend, 보존 종료 cleanup/purge, legacy migration은 후속 범위다.

플랫폼 관리자 권한의 grant/revoke, reactivate, role change는 `SUPER_ADMIN`만 수행할 수 있으며 reason과 immutable audit을 필수로 한다. 마지막 활성 `SUPER_ADMIN`의 revoke 또는 demote는 금지한다. lifecycle Backend는 구현됐고 승인 workflow와 S15 Frontend는 후속 설계 대상이다.

## 4. 권한 모델 불변식

| 항목 | 기준 |
|---|---|
| 플랫폼 권한 저장소 | 별도 `platform_admins` 개념을 사용한다. |
| 금지 모델 | `users.role`을 만들지 않는다. |
| 회사와 프로젝트 역할 | `company_members.role`, `project_members.role`은 기존 A/P/C 범위에만 사용한다. |
| 권한 분리 | `COMPANY_ADMIN`, `PROJECT_ADMIN`은 플랫폼 action을 인가할 수 없다. |
| 변경 방식 | 사용자 정지와 회사 비활성화는 hard delete가 아니라 상태 전이로 다룬다. |

## 5. 다음 gate 입력

`3.7.2`는 이 기능 분해를 기준으로 ERD와 API 영향만 초안으로 기록한다. `3.7.3`은 플랫폼 namespace와 actor 경계를 화면 flow 초안으로 정리한다. `3.7.4`는 미결 결정과 최소 evidence를 확인해 planning Go와 implementation No-Go를 handoff한다.

## 6. 기준 문서

- `docs/00-project/sprint-운영/sprint-2-backlog.md`
- `docs/00-project/sprint-운영/sprint-3-backlog.md`
- `docs/02-requirements/S15-플랫폼-관리자-1차-범위-정의.md`
- `docs/07-decisions/플랫폼-관리자-인가-모델-결정.md`
- `docs/07-decisions/sprint-3-범위-결정.md`
