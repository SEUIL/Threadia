> **작성자**: Threadia 팀
> **작성일**: 2026-07-13
> **최종 수정**: 2026-07-13
> **버전**: v1.1 WBS 3.6.3 문서 추적성 QA finding과 사용자 승인 disposition 기록
> **상태**: 검토 중
> **태그**: sprint-2, wbs, qa, traceability, findings

# Sprint 2 WBS 3.6.3 문서 추적성 QA finding

## 1. 목적과 판정 범위

이 문서는 WBS 3.6.3의 WBS·Sprint 운영 문서·canonical 문서 간 추적성 충돌과 미확정 사항을 reviewer가 판정할 수 있도록 기록한다. 이 문서는 Sprint 2 완료, 배포 준비 완료, 실제 AWS 배포를 선언하지 않는다.

WBS 본시트는 read-only 근거로만 사용한다. 이 문서 작성으로 WBS 본시트, canonical 문서, 멤버 소유 문서, 코드, workflow, AWS 산출물은 수정하지 않았다. finding에 대한 canonical-document correction은 reviewer 확인과 별도 승인 전까지 수행하지 않는다.

## 2. 증빙 범위와 재검증 한계

| 구분 | 근거와 제한 |
|---|---|
| evidence source | repo-local `docs/00-project/sprint-운영/sprint-2-backlog.md`, `sprint-2-상세계획.md`, `sprint-2-기능-통합-QA-gate.md`와 해당 canonical 요구사항·설계·API 문서를 비교 근거로 사용한다. WBS 본시트는 read-only 참조로 한정한다. |
| revision limit | 이 finding은 2026-07-13에 확인 가능한 repo-local 문서와 현 세션 검증 결과만 반영한다. 외부 서비스, AWS, GitHub Actions, 운영 환경, 사용자 mailbox는 재검증하지 않았다. |
| current local verification | frontend `lint`와 `build`는 현 세션에서 PASS다. browser/smoke 재검증은 Chromium 부재와 기존에 실행 중인 unrelated dev server 때문에 BLOCKED다. 따라서 현 세션 browser/smoke PASS를 주장하지 않는다. |
| historical QA evidence | `sprint-2-기능-통합-QA-gate.md`의 QA-05는 managed Chromium smoke 3 tests PASS라는 과거 기록을 보존한다. 이는 현 세션 재검증 결과가 아니라 historical evidence다. |

## 3. Finding ledger

| ID | finding | 분류 | owner / next gate | evidence path | 권고 처분 |
|---|---|---|---|---|---|
| F-001 | S15 관련 WBS 참조가 과거 `4.9`로 남아 있으나 현재 기준은 `4.10`이다. Sprint 2 backlog의 3.7 완료 조건도 `4.9` 이동 상태를 언급하므로 WBS와 연결 문서의 기준 번호를 reviewer가 확정해야 한다. | approval-needed | Owner: 황스일 / Next gate: 3.6.4 및 3.7.5 | `docs/00-project/sprint-운영/sprint-2-backlog.md`, `docs/00-project/sprint-운영/sprint-2-상세계획.md`, WBS 본시트 read-only | WBS 현재 행을 기준으로 번호를 확인한 뒤 승인된 문서 정합화 묶음에서만 canonical 및 운영 문서를 정정한다. 현재는 수정하지 않는다. |
| F-002 | `project-member`와 `project-invitation`의 범위가 Sprint 2 project foundation의 reference인지 Sprint 3 `PROJECT-MEMBER-01~02` candidate인지 문서별로 혼재되어 해석 여지가 있다. 회사 invitation과 프로젝트 invitation의 경계도 분리해 판정해야 한다. | approval-needed | Owner: 황스일, Project owner / Next gate: 3.6.4 | `docs/00-project/Definition-of-Done.md`, `docs/00-project/검증-방법.md`, `docs/00-project/sprint-운영/sprint-2-backlog.md`, `docs/02-requirements/기능-시나리오-API-ERD-추적표.md` | Sprint 2에는 project foundation의 관계 reference만 유지하고, project membership 생성·project invitation 구현은 Sprint 3 candidate인지 reviewer가 확정한다. 확정 전 Sprint 2 완료 조건에 포함하지 않는다. |
| F-003 | S04 프로젝트 카드가 요구하는 상태, 멤버 수, PM 등 통계 표시 항목과 project list API response contract의 제공 필드·집계 기준이 하나의 검증 가능한 계약으로 연결되어 있지 않다. | blocker | Owner: 김태준, 정철진 / Next gate: 3.6.4 | `docs/00-project/sprint-운영/sprint-2-backlog.md`, `docs/03-design/화면정의서.md`, `docs/04-api/API-명세서.md` | S04 카드 필드별 source, 집계 주체, empty/error 처리와 API response field를 정합화한 뒤 API·화면 evidence를 제출한다. 이 계약 공백이 남으면 S04 통합 검증을 PASS로 판정하지 않는다. |
| F-004 | WBS 3.6.3의 dedicated finding artifact와 3.6.4가 소비할 reviewable result artifact가 없었다. 이 문서는 3.6.3 초안 증빙을 제공하지만 reviewer 확인 전 완료 증빙은 아니다. | non-blocker | Owner: 황스일 / Next gate: reviewer confirmation 후 3.6.4 | `docs/00-project/sprint-운영/sprint-2-wbs-3.6.3-문서-추적성-QA.md`, `docs/00-project/sprint-운영/sprint-2-기능-통합-QA-gate.md` | 본 문서의 reviewer 판정, finding disposition, 미확인 검증 상태를 3.6.4 입력값으로 사용한다. 3.6.4 결과 문서는 reviewer가 재확인 가능한 결과와 잔여 조치를 별도로 남긴다. |

## 4. 사용자 승인 disposition

아래 처분은 2026-07-13 사용자 승인 결정을 기록한 것이다. 이는 3.6.3 finding의 처리 기준이며, reviewer가 3.6.3 또는 3.6.4를 실제로 승인했다는 기록은 아니다. canonical 문서, WBS 본시트, 멤버 소유 evidence, 코드, workflow, AWS 산출물은 이 처분 기록으로 수정하지 않는다.

| ID | 사용자 승인 disposition | 후속 경계 | 현재 영향 |
|---|---|---|---|
| F-001 | S15의 official WBS ID는 `4.10`이다. 과거 `4.9` 참조는 3.7.5의 승인된 canonical-document alignment에서 처리한다. | 3.7.1부터 3.7.4까지는 S15 planning gate 결과와 정합화 대상만 식별한다. canonical 문서의 실제 정정은 3.7.5 승인 경로에서만 수행한다. | 현재 문서와 WBS 본시트는 수정하지 않는다. `4.9` 참조의 존재는 Sprint 2 완료 또는 배포 판단의 근거가 아니다. |
| F-002 | project invitation, project-member 생성과 mutation은 `4.3` 아래 Sprint 3 candidate scope다. Sprint 2는 기존 project foundation reference만 유지한다. | Sprint 2 scope, 완료 조건, 통합 PASS에 invitation 또는 project-member 생성·mutation을 넣지 않는다. | canonical 문서와 API를 지금 수정하지 않는다. project foundation reference만 Sprint 2 evidence로 읽는다. |
| F-003 | S04 list-card의 member count, PM, statistics는 `4.1` committed scope 이후에만 API contract expansion을 검토한다. | `4.1` committed scope 전에는 API field, 집계 기준, endpoint를 수정하지 않는다. | contract 공백은 Sprint 2 S04 integrated PASS의 blocker로 유지한다. 이 처분은 현재 API 수정 권한이 아니다. |
| F-004 | 3.6.4 dedicated result artifact가 필요하다는 finding은 유지한다. | 3.6.4 handoff 초안은 reviewer-auditable input과 잔여 검증을 기록한다. | 3.6.4의 완료 또는 reviewer 실제 승인을 선언하지 않는다. |

## 5. 3.6.4 handoff와 완료 조건

3.6.4는 3.6.1~3.6.3의 reviewable result를 함께 받아야 한다. 이 문서가 제공하는 입력은 F-001~F-004의 분류, owner, next gate, evidence path, 권고 처분과 현 세션 재검증 제한이다.

3.6.3은 reviewer가 F-001과 F-002의 scope·기준 번호 결정을 확인하고, F-003의 blocker 처리 계획과 F-004의 evidence handoff를 확인하기 전에는 완료로 판정하지 않는다. 현재 상태는 `검토 중`으로 유지한다.

## 6. Reviewer 확인 항목

- [ ] F-001의 S15 WBS 기준 번호를 현재 WBS read-only 참조로 확인하고, 정정 대상 문서와 승인 경로를 결정한다.
- [ ] F-002의 project-member/project-invitation을 Sprint 2 foundation reference와 Sprint 3 candidate로 구분할지 확인한다.
- [ ] F-003의 S04 카드 통계/API contract 정합화 owner와 제출 evidence를 확인한다.
- [ ] F-004의 3.6.3 결과를 3.6.4 입력으로 수용하고, 3.6.4 dedicated result artifact를 남길 책임자를 확인한다.
- [ ] 현 세션 lint/build PASS와 browser/smoke BLOCKED, historical QA-05 PASS를 서로 다른 검증 상태로 수용한다.
- [ ] 사용자 승인 disposition이 canonical 문서 또는 API의 즉시 수정 권한으로 오해되지 않도록 3.7.5와 `4.1` gate 경계를 확인한다.

## 7. 관련 문서

- `docs/00-project/sprint-운영/sprint-2-상세계획.md`
- `docs/00-project/sprint-운영/sprint-2-backlog.md`
- `docs/00-project/sprint-운영/sprint-2-기능-통합-QA-gate.md`
- `docs/00-project/Definition-of-Done.md`
- `docs/00-project/검증-방법.md`
