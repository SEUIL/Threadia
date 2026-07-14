> **작성자**: Threadia 팀
> **작성일**: 2026-07-13
> **최종 수정**: 2026-07-13
> **버전**: v0.1
> **상태**: 검토 중
> **태그**: sprint-2, wbs, platform-admin, handoff, go-no-go

# Sprint 2 WBS 3.7.4 S15 Go No-Go handoff 초안

## 1. handoff 판정

| 구분 | 판정 | 의미 |
|---|---|---|
| planning | Go | `3.7.1`부터 `3.7.3`의 범위, 영향도, 권한 flow 초안을 후속 검토 입력으로 넘길 수 있다. |
| S15 implementation | No-Go | 선행 foundation, security 적용 방향, 상세 ERD/API/화면 결정, audit 및 운영 정책 evidence가 없으므로 구현에 착수할 수 없다. |
| canonical alignment | No-Go | `3.7.5`의 승인 전에는 canonical 문서를 수정하지 않는다. |
| reviewer approval | 미확정 | 이 문서는 최종 reviewer 승인이나 구현 commitment를 기록하지 않는다. |
| deployment readiness | 미확정 | 이 문서는 배포 준비 또는 운영 배포 가능 상태를 선언하지 않는다. |

## 2. 확인된 planning 결과

1. S15의 공식 WBS ID는 `4.10`이며, 실행 row는 `4.10.1` support cases, `4.10.2` user/company enforcement, `4.10.3` system settings decision, `4.10.4` platform authorization 및 audit/operation logs다.
2. 플랫폼 권한은 `platform_admins`로 분리하며 `users.role`을 만들지 않는다.
3. 문의와 신고는 `support_cases`에서 `INQUIRY`와 `REPORT`로 구분한다.
4. 사용자 정지와 회사 비활성화는 hard delete가 아니라 상태 전이로 처리한다.
5. 모든 platform action은 reason과 immutable `admin_action_audits` 기록을 필요로 한다.
6. `COMPANY_ADMIN`과 `PROJECT_ADMIN`은 platform action의 인가 근거가 될 수 없다.

## 3. implementation No-Go 사유

| blocker | 현재 상태 | 구현 전 필요 조치 |
|---|---|---|
| Company/RBAC/Project foundation | S15의 선행 WBS `3.1~3.4` 기준이 필요하다. | 실제 선행 foundation의 완료 evidence와 platform 경계 호환성을 확인한다. |
| Security 적용 방향 | 현재 backend의 `SecurityConfig`는 permitAll 상태이며, platform-admin table, package, route, audit log가 없다. | 이는 gating concern이며 구현 authorization이 아니다. 실제 인증과 platform authorization 적용 방향을 설계 및 검증한다. |
| ERD | 신규 분리 domain의 영향만 식별됐다. | `platform_admins`, `support_cases`, immutable `admin_action_audits`의 상세 설계를 승인한다. |
| API | full endpoint, request/response, error code가 없다. | 권한, 상태 전이, reason, audit, 실패 처리 기준을 계약으로 결정한다. |
| 화면 | S15 정보 구조와 flow는 초안이다. | namespace, entry, 상태 표현, forbidden 처리, action confirmation을 설계한다. |
| 시스템 설정 | 설정 범위와 변경 절차가 확정되지 않았다. | 허용 목록, 권한, 변경 전후 기록, 보존 정책을 결정한다. |
| 알림 | 처리 결과 notification의 정책이 없다. | 수신자, 발송 조건, 채널, 실패 처리 여부를 결정한다. |
| 운영 감사 | 보존 기간, 조회 권한, 민감정보 처리 기준이 없다. | immutable audit의 보존 및 접근 정책을 결정한다. |

## 4. implementation Go 최소 evidence

아래 evidence는 구현 Go를 검토하기 위한 최소 입력이며, 모두 충족해도 자동 구현 승인이나 배포 승인을 뜻하지 않는다.

- [ ] `3.1~3.4` 선행 foundation의 검증 가능한 완료 또는 명시적 compatibility 판단이 있다.
- [ ] platform actor를 `platform_admins`에서 판정하는 security 설계와 negative authorization 사례가 있다.
- [ ] `users.role` 및 company/project membership platform permission 금지 검증 기준이 있다.
- [ ] support case 상태 전이, reason 요구, 답변 또는 처리 메모 규칙이 결정됐다.
- [ ] 사용자 정지와 회사 비활성화의 영향 범위, 해제 기준, 실패 처리가 결정됐다.
- [ ] immutable `admin_action_audits`의 보존, 조회 권한, 민감정보 처리와 변경 전후 기록 기준이 결정됐다.
- [ ] system settings, grant/revoke, dashboard, notification의 포함 또는 deferred 판단이 명시됐다.
- [ ] ERD, API, 화면 초안에 대한 승인 경로와 이후 검증 계획이 있다.

## 5. canonical alignment 전송 규칙

`3.7.5`는 위 No-Go 판단과 미결 결정을 승인 절차로 검토한 뒤에만 시작한다. 승인된 canonical-document alignment의 대상은 기능 분해표, 요구사항 정의서, 사용자 시나리오, 추적표, 화면정의서, ERD, API 명세서, 보류 기능 표, 기획문서 변경 이력이 될 수 있다. 실제 수정 대상과 미수정 사유는 승인 후에 결정한다.

공식 WBS는 `4.10`이다. 과거 `4.9` 표현은 `3.7.5` 승인 alignment가 완료될 때까지 그대로 둔다. 이 handoff는 WBS Sheets, canonical 문서, 코드, DB, workflow를 변경하지 않는다.

## 6. AWS, OIDC, CodeDeploy hold

AWS, OIDC, CodeDeploy는 S15 implementation Go와 별개의 hold 항목이다. 이 handoff는 cloud resource, credential, workflow, deployment, AWS/OIDC/CodeDeploy 설계 또는 설정 변경을 승인하거나 수행하지 않는다. 별도 승인과 검증 기준이 생길 때까지 hold 상태를 유지한다.

## 7. 다음 순서

1. reviewer는 planning Go와 implementation No-Go의 구분을 검토한다.
2. 미결 결정과 최소 evidence의 충족 여부를 확인한다.
3. 승인된 경우에만 `3.7.5` canonical alignment 대상과 변경 이력을 결정한다.
4. `4.10` implementation은 별도 구현 Go 판단 후에만 착수한다.

## 8. 기준 문서

- `docs/00-project/sprint-운영/sprint-2-wbs-3.6.4-sprint-2-handoff.md`
- `docs/00-project/sprint-운영/sprint-2-backlog.md`
- `docs/00-project/sprint-운영/sprint-3-backlog.md`
- `docs/07-decisions/sprint-3-범위-결정.md`
- `docs/07-decisions/플랫폼-관리자-인가-모델-결정.md`
