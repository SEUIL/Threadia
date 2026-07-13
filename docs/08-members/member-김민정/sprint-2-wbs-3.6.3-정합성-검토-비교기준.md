> **작성자**: 김민정 (WBS 3.6.3 Owner)
> **작성일**: 2026-07-07
> **최종 수정**: 2026-07-13
> **버전**: v1.3
> **상태**: 검토 요청
> **태그**: sprint-2, wbs-3.6.3, wbs-sync, docs, qa

# Sprint 2 WBS 3.6.3 WBS·Docs·요구사항추적표 정합성 검토 비교 기준

> 기준: 구글 시트 WBS 탭 3.6.3 row, `feedback_wbs_sheet_source_of_truth`(구글 시트가 canonical, 저장소 docs는 참고용)
> WBS 3.6.3(WBS·docs·요구사항추적표 정합성 검토, Owner 김민정)의 착수 가능 조건은 "비교 기준 선확인 가능; 최종 finding은 WBS 확정/구현 결과 후"다. 선행 WBS는 **"3"(3.0~3.5 전체)**이며, **2026-07-13 PR [#93](https://github.com/SEUIL/Threadia/pull/93) 머지로 WBS 3.0(Foundation)이 확정**돼 선행 조건이 충족됐다. 이 문서는 비교 방법론(§2~4), 잠정 관찰 이력(§5), 최종 finding(§6)을 다룬다.

---

## 0. WBS 3.6.3 진행 현황 (본인 self-report)

| WBS | 진척률 | 비고 |
|---|---|---|
| 3.6.3 | 95% (최종 finding 확정, Reviewer 확인+merge 대기) | 비교 기준(§2~4) 확정 + 잠정 비교 실행(2026-07-11 §5 #1~5, 2026-07-13 §5 #6~7 확장). **2026-07-13 PR #93 머지로 WBS "3"(3.0~3.5) 확정** → 선행 조건 충족돼 §5 관찰 7건을 §6 최종 finding으로 승격했다. **100% 확정 조건: Reviewer(황스일)가 이 문서를 review하고 merge까지 완료.** |

---

## 1. 착수 조건 확인

| 선행 | 상태 | 비고 |
|---|---|---|
| WBS "3" (3.0~3.5 전체) | **완료(2026-07-13)** | 3.0(Foundation 게이트) §2 4개 라인(3.1~3.4) 전부 충족 확인 + PR [#93](https://github.com/SEUIL/Threadia/pull/93) dev 머지(`6e1c87d`)로 확정. 3.5도 시트 기준 완료(3.5, 3.5.1~3.5.4 전부 상태=완료·진척률 100% — [`member-김민경/sprint2-wbs-3.5.1-candidate-gate.md`](../member-김민경/sprint2-wbs-3.5.1-candidate-gate.md) go 결정 참고). 상세는 [`sprint-2-wbs-3.0-foundation-게이트-판정.md`](./sprint-2-wbs-3.0-foundation-게이트-판정.md) 참고 |
| 요구사항추적표 | 최신 상태로 참조 가능 | 시트 "요구사항추적표" 탭 기준 (COMPANY/DEPT/MEMBER/RBAC/PROJECT/PROJECT-MEMBER 등) |

WBS "3"이 확정됐으므로, 이 문서의 §5 관찰 사항을 §6에서 최종 finding으로 승격한다.

---

## 2. 비교 대상

| 구분 | 소스 | 성격 |
|---|---|---|
| WBS 시트 (canonical) | 구글 시트 "WBS & 요구사항추적표" → `WBS` 탭 | 진실 소스. 계속 수정됨 |
| 요구사항추적표 (canonical) | 같은 파일 → `요구사항추적표` 탭 | 기능 ID(COMPANY-01 등) 기준, WBS ID와 별개 체계 |
| WBS-배정변경안 | 같은 파일 → `WBS-배정변경안` 탭 | 배정 변경 초안, 참고용. **2026-07-13 확인: 시트 다운로드(`workbook.xml`)에 이 탭이 없음 — §5 #7 참고** |
| WBS-1.4-1.5-변경안 | 같은 파일 → `WBS-1.4-1.5-변경안` 탭 | Sprint 1.4/1.5 변경 초안, 참고용. **2026-07-13 확인: 시트 다운로드(`workbook.xml`)에 이 탭이 없음 — §5 #7 참고** |
| 저장소 docs | `docs/00-project/sprint-운영/sprint-2-상세계획.md`, `sprint-2-backlog.md` | 시트를 뒤늦게 반영하는 파생 문서 |
| 팀원 개인 작업 문서 | `docs/08-members/member-*/` | 개별 owner가 시트 항목을 참조해 작성 |

---

## 3. 비교 기준 (필드별)

WBS 시트의 각 row와 저장소 docs/요구사항추적표를 아래 필드 기준으로 대조한다.

| 필드 | 시트 컬럼 | 대조 대상 | 불일치 판정 기준 |
|---|---|---|---|
| WBS ID 세분화 | A (WBS ID) | `sprint-2-backlog.md`의 WBS 번호 표기 | 시트에 있는 sub-item(예: 3.1.1~3.1.5)이 저장소 docs에 없으면 불일치 |
| Owner/Reviewer | L, M | 저장소 docs의 Owner/Reviewer 표기 | 인물이 다르면 불일치 (특히 QA sub-item은 dev owner와 QA owner가 반대로 배정되는 경우가 있어 주의) |
| 선행 WBS ID | O | 저장소 docs의 의존성 서술 | 시트 기준 선행이 저장소에 없거나 다르면 불일치 |
| 완료 조건 | T | 저장소 docs의 완료 조건 서술 | 문구가 실질적으로 다른 기준을 말하면 불일치 (표현 차이는 제외) |
| 요구사항 ID 매핑 | I | 요구사항추적표의 "비고" 컬럼(`WBS 3.x` 표기) | 상호 참조가 어긋나면 불일치 |
| 상태/진척률 | V, W | 저장소 docs 또는 팀원 개인 문서의 self-report | 시트가 최신이므로 저장소가 오래된 값이면 "지연 반영"으로 별도 분류(불일치와는 다르게 취급) |

---

## 4. 비교 방법

1. 구글 시트 `WBS` 탭 전체를 다운로드해 WBS ID, Owner, Reviewer, 선행, 완료 조건을 표로 정리한다 (`download_file_content` + xlsx 파싱, 이번 세션에서 이미 1회 수행함).
2. 저장소 `sprint-2-상세계획.md`, `sprint-2-backlog.md`의 WBS 표를 같은 필드로 정리한다.
3. §3 필드별로 두 표를 대조하고 불일치를 finding으로 기록한다.
4. 요구사항추적표의 "비고" 컬럼(`WBS 3.x` 표기)과 시트 WBS 탭의 요구사항 ID 컬럼(I)을 상호 대조한다.
5. finding은 "저장소 반영 필요" / "시트 오기재 의심(팀 확인 필요)" / "의도된 차이(문서화만 누락)"로 분류한다.
6. 3.0 확정 이후 이 문서를 갱신하며 최종 finding으로 확정하고 Reviewer(황스일)에게 요청한다. — **완료(2026-07-13), §6 참고**

---

## 5. 잠정 관찰 이력 (2026-07-11/13 실행, §6에서 최종 finding으로 승격됨)

> #1~5는 시트 WBS 탭 ↔ `sprint-2-backlog.md`(2026-07-07 이후 미수정) 대조로 확인했다(2026-07-11). #6은 §4 절차 4(요구사항추적표 "비고" 컬럼 ↔ WBS 탭 "요구사항 ID" 컬럼) 대조, #7은 §2 대조 대상 탭의 실존 여부 확인이다(2026-07-13). 아래는 원 관찰 시점 그대로 보존한 기록이고, WBS "3" 확정에 따른 최종 판정은 §6을 본다. 대조 시점 backlog top-level: 3.1 여도현/황스일, 3.2 정철진/김민정, 3.3 김태준/여도현, 3.4 정철진/김민정, 3.5 김민경/여도현, 3.6 황스일/김민정.

| # | 관찰 내용 | 분류(잠정) |
|---|---|---|
| 1 | `sprint-2-backlog.md`가 3.1을 여도현 단일 owner로만 표기하지만, 시트는 3.1.1~3.1.5로 세분화되어 있고 3.1.5는 Owner 김민정(QA)이다 | 저장소 반영 필요 |
| 2 | `sprint-2-backlog.md`가 3.3/3.4를 김태준·정철진 owner로만 표기하지만, 시트는 3.3.5/3.4.4에서 Owner=김민정·Reviewer=정철진(부모 항목과 반대)으로 뒤집힌 QA sub-item을 별도로 둔다. (3.2.5는 시트 최신본(2026-07-11)에서 Owner=황스일로 갱신됨 — 황스일이 실제 QA 수행, PR #77 반영. 즉 3.2.5는 더 이상 김민정 owner 아님) | 저장소 반영 필요 |
| 3 | `sprint-2-backlog.md`에는 3.6 하위 sub-item(3.6.1~3.6.4)이 WBS 항목으로 없다(3.6.3은 line 275에 "문서/추적성 QA 결과가 필요" 서술로만 1회 언급) | 저장소 반영 필요 |
| 4 | 3.6 parent의 Owner/Reviewer가 backlog와 시트에서 정반대다: backlog는 **Owner 황스일·Reviewer 김민정**, 시트 WBS 탭은 **Owner 김민정·Reviewer 황스일**. **2026-07-11 시트 재확인 + 팀 확인 결과 시트가 정답(Owner 김민정·Reviewer 황스일)** → backlog가 오기재. 세분화 이전에 3.6 parent row의 담당부터 어긋나 있으니 backlog를 시트 기준으로 정정해야 함 | 저장소(backlog) 반영 필요 |
| 5 | **(신규)** 3.5도 backlog는 3.5(김민경) 단일이지만 시트는 3.5.1~3.5.4로 세분화되어 있고 3.5.1(착수 go/no-go)·3.5.4(evidence)는 Owner 김민정이다 | 저장소 반영 필요 |
| 6 | **(신규, 2026-07-13)** §4 절차 4 실행: 요구사항추적표 "비고" 컬럼(WBS 3.x 표기) ↔ WBS 탭 "요구사항 ID" 컬럼(I)을 시트 재다운로드해 상호 대조했다. RBAC-01/COMPANY-01·02·04/DEPT-01~04/MEMBER-01·03·04/PROJECT-01~04 등 Sprint 2 항목은 양쪽이 "WBS 3.1~3.4" 계열로 정확히 일치한다. 그런데 **PROJECT-MEMBER-01/02, ISSUE-01~04, KANBAN-01/02는 요구사항추적표에서 아직 "구현 단계: Sprint 3 후보" / "WBS ID: 4.3~4.4"로 표기**돼 있다. 실제로는 WBS 탭에서 PROJECT-MEMBER-01/02가 **3.3.4(Sprint 2, 완료 100%, PR #70 dev merge)**로, ISSUE-01~04/KANBAN-01/02가 **3.5.1~3.5.4(Sprint 2 capacity candidate, 완료 100%, [`member-김민경/sprint2-wbs-3.5.1-candidate-gate.md`](../member-김민경/sprint2-wbs-3.5.1-candidate-gate.md) go 결정)**로 이미 구현·완료됐다. 요구사항추적표의 해당 비고("WBS 4.x reference는 WBS rewrite 후 backfill")가 아직 backfill되지 않은 것으로 보인다 | 의도된 차이(문서화만 누락) — 요구사항추적표 backfill 대상. 최종 finding 승격 시 시트 owner에게 backfill 요청 필요 |
| 7 | **(신규, 2026-07-13)** §2에 대조 대상으로 등록된 `WBS-배정변경안`·`WBS-1.4-1.5-변경안` 탭을 재대조하려 시트를 다시 다운로드했으나, `workbook.xml`에 `요구사항추적표`(sheetId 1)·`WBS`(sheetId 2) 두 탭만 존재하고 두 변경안 탭은 없다. 삭제·병합됐는지 팀 확인 필요 — WBS 탭이 이미 3.1.1~3.1.5 같은 세분화 구조를 갖추고 있어, 변경안이 검토·반영된 뒤 탭이 정리됐을 가능성이 있다 | 시트 확인 필요(팀 확인 필요) |

---

## 6. 최종 Finding (2026-07-13 확정)

WBS "3"(3.0~3.5) 확정으로 §5의 잠정 관찰 7건을 최종 finding으로 승격한다. 분류와 근거는 §5 원문과 동일하며, 여기서는 조치 방향만 확정한다.

| # | Finding | 분류 | 조치 방향 |
|---|---|---|---|
| F-1 | `sprint-2-backlog.md`가 3.1을 여도현 단일 owner로 표기하지만 시트는 3.1.1~3.1.5로 세분화, 3.1.5 Owner는 김민정(QA) | 저장소 반영 필요 | `sprint-2-backlog.md`를 시트 기준 sub-item 구조로 정정 |
| F-2 | `sprint-2-backlog.md`가 3.3/3.4를 김태준·정철진 단일 owner로 표기하지만 시트는 3.3.5/3.4.4에서 Owner=김민정·Reviewer=정철진으로 뒤집힌 QA sub-item을 별도로 둔다 | 저장소 반영 필요 | 동일하게 `sprint-2-backlog.md` 정정 |
| F-3 | `sprint-2-backlog.md`에 3.6 하위 sub-item(3.6.1~3.6.4)이 WBS 항목으로 없음 | 저장소 반영 필요 | `sprint-2-backlog.md`에 3.6.1~3.6.5 항목 추가 |
| F-4 | 3.6 parent의 Owner/Reviewer가 backlog(Owner 황스일·Reviewer 김민정)와 시트(Owner 김민정·Reviewer 황스일)에서 정반대. **시트가 정답**(2026-07-11 팀 확인) | 저장소(backlog) 반영 필요 | `sprint-2-backlog.md`의 3.6 Owner/Reviewer를 시트 기준으로 뒤집어 정정 |
| F-5 | `sprint-2-backlog.md`가 3.5를 김민경 단일로 표기하지만 시트는 3.5.1~3.5.4로 세분화, 3.5.1·3.5.4 Owner는 김민정 | 저장소 반영 필요 | `sprint-2-backlog.md`를 시트 기준 sub-item 구조로 정정 |
| F-6 | 요구사항추적표에서 PROJECT-MEMBER-01/02·ISSUE-01~04·KANBAN-01/02가 아직 "Sprint 3 후보"/WBS 4.x로 남아있으나, 실제로는 WBS 3.3.4·3.5.1~3.5.4(Sprint 2)로 완료됨 | 의도된 차이(문서화만 누락) | 요구사항추적표 owner에게 backfill 요청(구현 단계 → "Sprint 2 완료", WBS ID → 3.3.4/3.5.x) |
| F-7 | `WBS-배정변경안`·`WBS-1.4-1.5-변경안` 탭이 현재 시트에 존재하지 않음(`workbook.xml` 확인) | 시트 확인 필요(팀 확인 필요) | 시트 관리자에게 탭 삭제/병합 여부 확인 요청 — 삭제가 의도된 것이면 §2 대조 대상에서 이 두 탭을 제외 |

`저장소 반영 필요`(F-1~F-5)는 총 5건 모두 `sprint-2-backlog.md`가 시트를 뒤늦게 반영하는 파생 문서이기 때문에 생기는 동일 유형 finding이다. 4개 라인 전부가 이미 완료·확정된 지금, backlog 정정은 정보 정확성 문제이며 실행을 막는 blocker는 아니다.

---

## 7. 남은 작업

- Reviewer(황스일)에게 이 문서 확인+merge 요청 (100% 확정 조건)
- F-1~F-5: `sprint-2-backlog.md`/`sprint-2-상세계획.md` 갱신 여부와 담당자를 팀에 확인
- F-6: 요구사항추적표 owner에게 backfill 요청
- F-7: 시트 관리자에게 `WBS-배정변경안`·`WBS-1.4-1.5-변경안` 탭 소재 확인 요청
