> **작성자**: Threadia 팀
> **작성일**: 2026-07-13
> **최종 수정**: 2026-07-13
> **버전**: v0.1
> **상태**: 검토 중
> **태그**: sprint-2, wbs, platform-admin, screen, authorization

# Sprint 2 WBS 3.7.3 S15 화면 권한 flow 초안

## 1. 목적과 초안 경계

이 문서는 S15 플랫폼 관리자 화면의 namespace, actor 경계, action flow를 초안으로 정리한다. 화면 layout, route, API 연동, 실제 접근 제어 구현을 확정하거나 수행하지 않는다.

S15는 기존 S03부터 S14까지의 회사와 프로젝트 workspace 흐름에 속하지 않는 platform-level console이다. 화면 상세와 API 매핑은 `4.10`의 선행 결정이 닫힌 뒤 canonical 문서에서 확정한다.

## 2. namespace와 actor 경계

| 항목 | 초안 기준 |
|---|---|
| 플랫폼 namespace | 플랫폼 console은 회사와 프로젝트 namespace와 분리한 별도 platform namespace로 둔다. 구체 route 문자열은 후속 API 및 화면 설계에서 결정한다. |
| 접근 actor | `PLATFORM_ADMIN`과 `SUPER_ADMIN`만 후보 actor다. |
| 비인가 actor | A, P, C와 비로그인 사용자는 플랫폼 action을 수행할 수 없다. |
| membership 경계 | `COMPANY_ADMIN`과 `PROJECT_ADMIN`은 platform action을 인가할 수 없다. 해당 role은 회사 또는 프로젝트 범위에서만 유효하다. |
| 권한 원천 | platform actor 여부는 별도 `platform_admins` 개념에서 판단한다. `users.role`을 사용하지 않는다. |

## 3. 정보 구조 초안

```text
S15 플랫폼 관리자 console
├── 운영 현황 요약 대시보드
├── support cases
│   ├── 문의
│   └── 신고
├── 사용자 enforcement
├── 회사 enforcement
├── 시스템 설정
└── 운영 audit log
```

운영 현황 요약 대시보드는 신규 문의, 처리 중 신고, 정지 사용자, 비활성 회사, 최근 관리자 조치를 표시하는 후보 영역이다. 집계 기준, 데이터 source, refresh 방식은 정해지지 않았으므로 구현 항목으로 확정하지 않는다.

## 4. 권한별 action 초안

| action | `PLATFORM_ADMIN` | `SUPER_ADMIN` | A/P/C |
|---|---:|---:|---:|
| support case 조회와 처리 | 가능 | 가능 | 불가 |
| 사용자 정지와 해제 | 가능 | 가능 | 불가 |
| 회사 비활성화와 해제 | 가능 | 가능 | 불가 |
| 운영 audit log 조회 | 가능 | 가능 | 불가 |
| 시스템 설정 조회 | 가능 | 가능 | 불가 |
| 시스템 설정 변경 | 불가 | 가능 | 불가 |
| platform admin grant/revoke | 불가 | 가능 | 불가 |
| 공지 또는 점검 배너 발행 | 불가 | 가능 | 불가 |

설정 변경, grant/revoke, 공지 또는 점검 배너는 권한 방향만 확인된 decision-needed 항목이다. 상세 화면, 승인 절차, 구현 API는 이 문서에서 확정하지 않는다.

## 5. 상태 전이와 감사 규칙

| 대상 | flow 초안 | 필수 규칙 |
|---|---|---|
| support case | 접수된 case를 `OPEN`에서 처리 중, 해결, 반려, 종료 상태로 전이한다. | 전이마다 처리 주체와 reason을 남기며, 답변 또는 처리 메모가 필요한지 상태별로 후속 결정한다. |
| 사용자 | 정책 위반 확인 후 `SUSPENDED` 상태로 전이하고, 필요 시 해제한다. | hard delete를 사용하지 않는다. reason과 immutable audit를 남긴다. |
| 회사 | 정책 위반 또는 요청 확인 후 비활성 상태로 전이하고, 필요 시 해제한다. | hard delete를 사용하지 않는다. 차단 범위와 해제 조건은 후속 결정한다. |
| 시스템 설정 | `SUPER_ADMIN`이 변경 요청을 검토하고 변경한다. | 변경 전후 값과 reason을 immutable audit에 남긴다. |
| platform admin 권한 | `SUPER_ADMIN`이 grant 또는 revoke를 수행한다. | 대상, 행위자, reason, 시각을 immutable audit에 남긴다. |

모든 상태 전이는 UI의 버튼 노출만으로 인가되지 않는다. platform actor 판정과 server-side 정책 검증이 필요하다는 원칙만 이 초안에서 확인한다.

## 6. UI flow 초안

1. 인증된 사용자가 platform namespace 진입을 요청한다.
2. 별도 platform actor 판정이 실패하면 platform console과 action을 허용하지 않는다.
3. 허용된 actor는 대시보드 또는 support case 목록으로 진입한다.
4. case 처리, 사용자 정지, 회사 비활성화, 설정 변경, 권한 변경의 변경 action은 reason 입력을 요구한다.
5. 변경 action이 승인되면 상태 전이와 immutable audit 기록을 함께 남긴다.
6. 처리 결과 notification은 수신자와 정책이 확정되기 전까지 flow에 포함하지 않는다.

이 flow는 구현 시나리오가 아니라 후속 설계에 전달할 화면 및 권한 경계다.

## 7. 보류와 제외

- 실제 dashboard 집계, 상세 UI, 플랫폼 route, 메뉴 노출 위치는 결정 전이다.
- 기존 S03, S13, S14의 A/P/C membership flow를 platform authorization에 재사용하지 않는다.
- 시스템 설정의 DB, 인프라, OAuth provider, 전역 feature flag 변경은 범위 밖이다.
- notification의 발송 UI와 채널은 후속 알림 결정 전까지 deferred다.

## 8. 기준 문서

- `docs/03-design/화면정의서.md`
- `docs/02-requirements/S15-플랫폼-관리자-1차-범위-정의.md`
- `docs/07-decisions/플랫폼-관리자-인가-모델-결정.md`
- `docs/00-project/sprint-운영/sprint-2-wbs-3.7.1-S15-기능-WBS-분해.md`
