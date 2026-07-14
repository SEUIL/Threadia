> **작성자**: Threadia 팀
> **작성일**: 2026-07-13
> **최종 수정**: 2026-07-13
> **버전**: v0.1
> **상태**: 검토 중
> **태그**: sprint-2, wbs, platform-admin, s15, planning

# Sprint 2 WBS 3.7.1 S15 기능 WBS 분해 초안

## 1. 목적과 경계

이 문서는 `DEFER-01`과 S15의 1차 범위를 Sprint 3 공식 WBS `4.10`의 실행 후보로 분해하는 planning gate 산출물이다. 구현 범위, API 계약, DB schema, 화면 계약을 확정하거나 구현 착수를 승인하지 않는다.

S15의 공식 WBS ID는 `4.10`이다. 과거 문서의 `4.9` S15 표현은 `3.7.5`의 승인된 canonical-document alignment 전까지 수정하지 않는다.

## 2. 4.10 분해 기준

| WBS ID | 작업명 | 1차 범위 | planning gate 판정 |
|---|---|---|---|
| `4.10.1` | support cases 구현 범위 확정/구현 | 문의와 신고를 `support_cases`로 통합하고 `INQUIRY`와 `REPORT`를 구분한다. 접수, 처리 중, 해결, 반려, 종료의 상태 전이와 답변 또는 처리 메모를 다룬다. | 구현 후보이나 상세 API, 데이터 구조, 알림 정책은 미정이다. |
| `4.10.2` | user/company enforcement 구현 | 플랫폼 관리자가 사용자 정지와 회사 비활성화를 상태 변경으로 수행하는 범위를 다룬다. | 구현 후보이나 회사 비활성 상태의 정확한 표현, 차단 범위, 해제 정책은 미정이다. |
| `4.10.3` | system settings 구현 범위 확정 | 문의 카테고리, 신고 사유, 제재 preset, 답변 템플릿, 공지 또는 점검 배너, 고객센터 운영 상태를 검토한다. | decision-needed다. 설정 항목별 허용 범위, 변경 절차, 보존 정책을 확정해야 한다. |
| `4.10.4` | audit/operation logs 구현 | 플랫폼 관리자 조치의 행위자, 대상, 사유, 변경 전후, 가능한 `support_case` 연결을 남기는 범위를 다룬다. | 구현 후보이나 audit 보존, 조회 범위, 상세 데이터 구조는 미정이다. |

## 3. 기능별 범위와 보류 경계

### 3.1 `4.10.1` support cases

문의와 신고는 별도 기능으로 분기하지 않고 `support_cases`라는 하나의 case 개념으로 관리한다. 유형은 `INQUIRY`와 `REPORT`로 구분한다. 처리 담당자는 플랫폼 관리자이며, 상태 변경과 답변 또는 처리 메모는 조치 이력과 연결되어야 한다.

처리 결과 알림은 후속 알림 설계에서 결정한다. 알림 발송 여부, 수신자, 채널, 실패 처리 방식은 이번 분해에서 확정하지 않는다.

### 3.2 `4.10.2` 사용자 정지와 회사 비활성화

사용자 정지는 기존 `users.status = SUSPENDED` 방향을 사용하며, 물리 삭제로 처리하지 않는다. 정지된 사용자는 로그인, token refresh, `/auth/me`의 기존 `ACTIVE` 조건에서 제외되는 기준과 연결된다.

회사 조치는 비활성화 상태 전환으로 다룬다. 회사 workspace 진입과 회사 하위 프로젝트 action을 차단하는 방향은 확인됐지만, 정확한 상태값, 차단 시점, 복구 절차는 후속 결정 대상이다. 두 조치 모두 reason과 관리자 audit 기록이 필요하다.

### 3.3 `4.10.3` 시스템 설정

시스템 설정은 DB, 인프라, OAuth provider, 전역 feature flag 같은 runtime 설정을 포함하지 않는다. 운영 콘솔에 직접 필요한 고객센터 및 제재 운영 설정만 후보로 둔다.

대시보드 요약, 시스템 설정의 실제 변경 UI, 공지 또는 점검 배너 발행, 플랫폼 관리자 권한 부여와 회수는 이번 planning gate에서 구현 범위로 확정하지 않는다. 특히 플랫폼 관리자 grant/revoke는 `SUPER_ADMIN` 권한 범위로만 확인하며, 등록 절차와 승인 흐름은 decision-needed로 남긴다.

### 3.4 `4.10.4` 플랫폼 인가와 운영 감사

`PLATFORM_ADMIN`은 문의와 신고 처리, 사용자 정지, 회사 비활성화, 운영 로그 조회를 수행하는 역할이다. `SUPER_ADMIN`은 플랫폼 관리자 권한 부여와 회수, 시스템 설정 변경, 공지 또는 점검 배너, 제재 preset 관리를 수행하는 역할이다.

모든 플랫폼 관리자 조치는 reason과 audit log를 남긴다. audit log는 사용자 알림, 활동 피드, 시스템 로그와 다른 운영 감사 원장으로 분리한다. notification은 처리 결과를 누구에게 어떤 방식으로 보낼지 결정 전까지 deferred로 둔다.

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
