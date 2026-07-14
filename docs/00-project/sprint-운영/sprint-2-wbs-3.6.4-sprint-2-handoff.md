> **작성자**: Threadia 팀
> **작성일**: 2026-07-13
> **최종 수정**: 2026-07-14
> **버전**: v0.2 WBS 3.6.4 Sprint 2 QA handoff draft
> **상태**: 검토 중
> **태그**: sprint-2, wbs, handoff, qa, traceability

# Sprint 2 WBS 3.6.4 handoff 초안

## 1. 목적과 상태 경계

이 문서는 WBS 3.6.1부터 3.6.3까지의 evidence를 reviewer가 추적할 수 있도록 정리한 QA handoff 초안이다. 3.6.4는 완료로 선언하지 않으며, 실제 reviewer 승인도 기록하지 않는다.

이 문서는 Sprint 2 전체 완료, 배포 준비 완료, 실제 배포를 선언하지 않는다. WBS 본시트, canonical 문서, 멤버 소유 evidence, 코드, workflow, AWS 산출물, Git configuration은 수정하지 않는다.

## 2. 입력 evidence 요약

| WBS | evidence 상태 | reviewer-auditable 근거 | handoff 판정 |
|---|---|---|---|
| 3.6.1 | historical/self-report backend evidence가 있다. | `sprint-2-기능-통합-QA-gate.md`의 QA-01부터 QA-04까지 backend test, build, auth/API preflight, DB schema/JPA 검증 PASS 기록 | 과거 self-report 기록으로만 참조한다. 현 handoff의 재검증 또는 reviewer-confirmed closure가 아니다. |
| 3.6.2 | 현재 worktree lint/build는 PASS, 현재 worktree Playwright smoke는 BLOCKED다. isolated worktree smoke와 QA-05는 별도 과거 기록이다. | `sprint-2-wbs-3.6.3-문서-추적성-QA.md`, `frontend-smoke-QA-재현성-blocker.md`, `sprint-2-기능-통합-QA-gate.md` | current worktree lint/build PASS는 현 세션 결과다. current worktree Playwright smoke는 BLOCKED다. isolated worktree smoke PASS는 3 tests, 26.9s의 별도 결과로 current-worktree PASS와 동등하지 않고 browser console error 상태도 판정하지 않았다. QA-05 PASS는 3 tests, 18.7s의 historical evidence이며 현 세션 검증이 아니다. |
| 3.6.3 | finding F-001부터 F-004와 F-001부터 F-003의 사용자 승인 disposition이 있다. | `sprint-2-wbs-3.6.3-문서-추적성-QA.md` | disposition은 후속 gate 기준이다. 실제 reviewer 승인이나 canonical 수정 완료를 뜻하지 않는다. |

## 3. 3.6.3 disposition과 실행 경계

| finding | 승인된 처분 | 다음 gate에서의 처리 | 현재 금지 경계 |
|---|---|---|---|
| F-001 | S15 official WBS ID는 `4.10`이다. | 사용자 승인 scope boundary다. 과거 `4.9` 참조의 정합화는 별도 승인 gate에서만 다룬다. | 지금 WBS 본시트 또는 canonical 문서를 수정하지 않는다. |
| F-002 | project invitation, project-member 생성과 mutation은 `4.3` 아래 Sprint 3 candidate scope다. Sprint 2에는 기존 project foundation reference만 남긴다. | 사용자 승인 scope boundary다. | invitation 또는 project-member 생성·mutation을 Sprint 2 완료 조건으로 넣거나 구현하지 않는다. |
| F-003 | S04 list-card member count, PM, statistics의 API contract expansion은 `4.1` committed scope 이후에만 검토한다. | 현재 S04 integrated PASS blocker로 유지한다. | 현재 API 계약이나 구현을 수정하지 않으며, blocker를 해제하지 않는다. |
| F-004 | 3.6.4 dedicated result artifact가 필요하다. | dedicated artifact와 reviewer 확인이 필요한 follow-up으로 유지한다. | 이 초안은 reviewer 승인 또는 완료 증빙이 아니다. |

## 4. WBS 3.5 candidate 상태

WBS 3.5 candidate는 시작하지 않았다. 이는 scope 결정이 아니라 evidence-limited 상태 기록이다. F-003이 남아 있어 reviewable Foundation closure가 입증되지 않았고, capacity-gate evidence도 없다.

`3.7.1`부터 `3.7.5`, `4.1`은 이 handoff의 실행 지시가 아닌 별도 governance를 따르는 미래 gate로만 참조한다.

## 5. 7/22 Backlog 진입 조건

**NOT READY**. repository-local canonical Backlog 진입 기준을 식별하지 못했다. 따라서 7/22 Backlog 진입 가능 여부를 판정할 evidence가 없다.

이 상태는 일정, 우선순위 또는 Sprint 3 scope 결정이 아니다.

## 6. 범위 밖 승인 경계

AWS, OIDC, CodeDeploy는 이 QA handoff의 범위 밖이며, 별도 승인 없이는 설계 확정, credential 설정, workflow 또는 cloud resource 변경, deployment를 다루지 않는다.

## 7. Reviewer 확인 항목

- [ ] 3.6.1 historical/self-report와 3.6.2의 current worktree, isolated worktree, historical QA-05 evidence provenance를 구분한다.
- [ ] current worktree Playwright smoke가 BLOCKED이며, isolated worktree smoke PASS 3 tests, 26.9s가 current-worktree PASS나 browser console error 판정이 아님을 확인한다.
- [ ] F-001과 F-002가 사용자 승인 scope boundary이며, F-003이 현재 S04 integrated PASS blocker이고 F-004가 dedicated artifact/reviewer follow-up임을 확인한다.
- [ ] WBS 3.5 candidate가 시작하지 않았으며, F-003 잔존으로 reviewable Foundation closure가 입증되지 않고 capacity-gate evidence도 없다는 근거를 확인한다.
- [ ] 7/22 Backlog 진입 조건이 repository-local canonical criteria 미식별로 `NOT READY`이고, 일정·우선순위·Sprint 3 scope 결정이 아님을 확인한다.

## 8. 입력 문서

- `docs/00-project/sprint-운영/sprint-2-기능-통합-QA-gate.md`
- `docs/00-project/sprint-운영/sprint-2-wbs-3.6.3-문서-추적성-QA.md`
- `docs/00-project/sprint-운영/frontend-smoke-QA-재현성-blocker.md`
