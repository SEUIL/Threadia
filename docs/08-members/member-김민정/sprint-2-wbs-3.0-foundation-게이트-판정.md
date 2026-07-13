> **작성자**: 김민정 (WBS 3.0 Owner)
> **작성일**: 2026-07-07
> **최종 수정**: 2026-07-13
> **버전**: v1.6
> **상태**: 검토 요청
> **태그**: sprint-2, wbs-3.0, foundation, qa, gate

# Sprint 2 WBS 3.0 Foundation 게이트 판정

> 기준: 구글 시트 "WBS & 요구사항추적표"의 `WBS` 탭(3.0, 3.1~3.6 row). 저장소 `docs/00-project/sprint-운영/sprint-2-*.md`는 시트의 세부 sub-item(3.1.1~3.6.4) 분화를 아직 반영하지 못한 상태라, 이 문서는 시트 기준으로 작성한다.
> WBS 3.0(회사·프로젝트 기반 구축과 S03/S04 진입 skeleton, Owner 김민정)의 완료 조건은 "Foundation confirmed scope 검증 후 Issue/Kanban은 capacity candidate로 분리 기록"이다. 착수 가능 조건은 "3.1.1/3.1.2/3.2 skeleton은 선착수 가능; 완료/통합은 2(Sprint 1.5) 완료 후"이며, 이 문서는 그 통합 판정을 내린다.

---

## 0. WBS 3.0 진행 현황 (본인 self-report)

| WBS | 진척률 | 비고 |
|---|---|---|
| 3.0 | 95% (§2 4개 라인 전부 충족 확인, Reviewer 확인+merge 대기) | 2026-07-13 갱신: §2 Foundation Confirmed Scope 판정표를 4개 라인(3.1/3.2/3.3/3.4) 전부로 채웠고 **모두 "충족"**으로 확인했다. §3 Issue/Kanban capacity candidate 분리 기록(3.5.1)도 go 결정이 문서로 남아있어 충족. 세부는 §2·§3 참고. **100% 확정 조건: Reviewer(황스일)가 이 문서를 review하고 merge까지 완료.** 하위 구현 진행 상황(2026-07-08 시트 기준, 참고용 stale 값): 3.1.1/3.1.2 완료 100%, 3.1.3 60%, 3.1.4 0%, 3.2.1~3.2.4 전부 50%, 3.3.1~3.3.4 전부 0%, 3.4.1~3.4.3 전부 50% — 이후 3.1~3.4 전체가 dev에 머지 완료됐다(§2 근거 참고) |

진척률 갱신 규칙은 [`sprint-1.5-wbs-2.6-qa-gate-결과.md`](./sprint-1.5-wbs-2.6-qa-gate-결과.md)와 동일하게 적용한다: 진행중~99%는 본인(김민정)이, **100% 확정은 Reviewer(황스일)가 해당 PR을 review하고 merge까지 완료한 시점**에 이뤄진다(2026-07-08 규칙 변경).

---

## 1. 착수 조건 확인

| 선행 | 내용 | 상태 |
|---|---|---|
| WBS 2 (Sprint 1.5 전체) | company membership이 invitation accept로 생성되는 흐름 완료 | 완료(2026-07-08). 2.1~2.5 전부 merge 완료, QA gate 통과 — 상세는 [`sprint-1.5-wbs-2.6-qa-gate-결과.md`](./sprint-1.5-wbs-2.6-qa-gate-결과.md) 참고 |

3.0 자체의 "완료/통합" 판정은 WBS 2가 끝나야 가능하지만, 하위 skeleton 항목(3.1.1, 3.1.2, 3.2.1)은 시트 기준으로 선착수 가능하다고 명시되어 있다. 즉 그 사이 김민정은 아래 §2의 통합 판정 기준을 준비한다.

---

## 2. Foundation Confirmed Scope 판정 기준

3.0은 3.1~3.4(Backend/Frontend 4개 라인)의 완료 조건을 모두 충족해야 "Foundation confirmed"로 본다. 각 라인의 완료 조건을 시트 기준으로 옮겨 판정표를 만든다.

| 라인 | WBS | 완료 조건 (시트 원문) | 확인 결과 | 비고 |
|---|---|---|---|---|
| Company/RBAC Backend | 3.1 | 회사/부서/구성원 API와 membership 기반 권한 검사 기준이 동작 | **충족** | [`sprint-2-wbs-3.1.5-company-rbac-검증케이스.md`](./sprint-2-wbs-3.1.5-company-rbac-검증케이스.md) self-report 100%(케이스 전부 통과, #4/#7 버그 PR #66로 수정·재검증). QA 문서 PR #71이 dev에 머지됨(`5a20d65`) |
| Company Frontend | 3.2 | 로그인 후 S03/S04 진입, 권한별 메뉴 노출, 회사 관리 route 확인 가능 | **충족** | [`황스일/sprint-2-wbs-3.2.5-company-frontend-qa-결과.md`](../황스일/sprint-2-wbs-3.2.5-company-frontend-qa-결과.md)는 2026-07-08 결론 그대로 `검토 중` 상태이며, `/hub` WebGL context 생성 실패를 이유로 3.0 확정을 보류시켜 두고 있다(해당 문서는 황스일 소유라 이 PR에서 직접 수정하지 않음). 이 판정은 그 문서를 근거로 삼지 않고, **아래는 이번 PR 작성자(김민정)가 2026-07-13에 직접 실행한 재검증 evidence**다: fallback 처리 커밋 `1c36c8a`가 dev에 반영돼 있음을 `git log`로 확인 → 프론트 dev 서버(`localhost:3000`) 기동, `member1@test.com`(A 역할)으로 로그인 후 `/hub` 진입 → (1) WebGL 정상 지원 환경: `<canvas>` 정상 렌더, 브라우저 콘솔 에러 0건(THREE.WebGLRenderer 관련 에러 없음). (2) `HTMLCanvasElement.prototype.getContext`를 패치해 `webgl`/`webgl2` 컨텍스트가 `null`을 반환하도록 강제한 뒤 `/hub` 재진입: `<canvas>`가 렌더되지 않고 "3D 허브를 표시할 수 없습니다. 이 브라우저 또는 환경에서 WebGL을 사용할 수 없습니다..." fallback 문구가 노출되며, 좌측 레일(회사/프로젝트/DM/알림 메뉴)은 그대로 유지됨을 확인. 두 경로 모두 에러로 화면이 깨지지 않는다. **후속 조치 제안**: 황스일이 3.2.5 문서를 이 evidence로 갱신하거나, 별도 evidence 문서(PR)로 남겨줄 것을 요청함 |
| Project Backend | 3.3 | 프로젝트 생성/조회/수정/archive와 멤버 조회 skeleton 동작 | **충족** | [`sprint-2-wbs-3.3.5-project-backend-검증케이스.md`](./sprint-2-wbs-3.3.5-project-backend-검증케이스.md) self-report 100%(케이스 20개 전부 통과, Finding F-1은 PR #76으로 해결·재검증). QA 문서 PR #74가 Reviewer(황스일) review+merge로 dev에 머지됨(`4c24f86`, `gh pr view 74`로 확인) |
| Project Frontend | 3.4 | 프로젝트 목록 진입, 생성/수정/아카이빙 UI 확인 가능 | **충족** | [`sprint-2-wbs-3.4.4-project-frontend-검증기준.md`](./sprint-2-wbs-3.4.4-project-frontend-검증기준.md) self-report 100%(§2 22개 중 정상 통과 17, Finding F-3는 PR #72 커밋 `1ca0e97`로 해소·재검증 완료). 구현 PR #72(`dd61591`)와 QA 문서 PR #75(`80db72f`, 머지자 `cjfwls39`=정철진) 둘 다 dev에 머지됨 |

`확인 결과`는 `충족 / 부분 충족 / 미충족`으로 표기하고, `부분 충족`·`미충족`이면 비고에 근거를 남긴다. 4개 라인이 모두 `충족`이어야 3.0을 "Foundation confirmed"로 본다. **2026-07-13 기준 4개 라인 전부 충족.**

---

## 3. Issue/Kanban Capacity Candidate 분리 기록

3.0의 완료 조건 후반부("Issue/Kanban은 capacity candidate로 분리 기록")는 WBS 3.5.1(Issue/Kanban 착수 여부 결정, Owner 김민정)의 산출물을 그대로 인용한다. 이 문서에서 중복 작성하지 않고, 3.0 완료 판정 시 3.5.1 결과 문서를 링크로 참조한다.

- 3.5.1 착수 조건: 3.1~3.4 Foundation 통과 여부 확인, 2026-07-20 기준 capacity 확인
- 3.0에는 "3.5.1에서 go/no-go와 사유가 기록되었는가"만 확인하고, 실제 판단 로직은 3.5.1 문서에서 다룬다.

**확인 결과(2026-07-13): 충족.** [`member-김민경/sprint2-wbs-3.5.1-candidate-gate.md`](../member-김민경/sprint2-wbs-3.5.1-candidate-gate.md)에 go 결정과 실행 범위(3.5.2/3.5.3/3.5.4)가 기록돼 있다. 해당 문서는 "Foundation 전체의 추가 검증·review·완료를 선언하지 않는다"고 명시하므로, 3.0의 Foundation confirmed 판정은 여전히 이 문서(§2)에서 별도로 내린다.

---

## 4. 검증 방법 (시트 기준: API/DB/브라우저/CI 확인)

> Backend API 검증(3.1.5, 3.3.5)은 팀 방향에 따라 Apidog collection 실행으로 진행한다(canonical DoD/검증-방법 문서 미반영, 상세는 각 하위 문서 참고). Frontend 검증(3.2.5, 3.4.4)은 기존대로 브라우저/lint/build 확인이다.

1. §2의 4개 라인 완료 조건을 3.1.5/3.2.5/3.3.5/3.4.4(하위 QA 항목) 문서의 확인 결과로 채운다.
2. 4개 라인이 모두 충족되면 3.5.1 결과를 확인해 Issue/Kanban 분리 기록 완료 여부를 판정한다.
3. 전체를 종합해 3.0을 완료 처리하고, Reviewer(황스일)에게 review + merge를 요청한다.
4. 하나라도 미충족이면 3.0을 보류하고 해당 라인 owner에게 후속 조치를 요청한다.
