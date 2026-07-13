> **작성자**: 김민정 (WBS 3.6.2 Owner)
> **작성일**: 2026-07-07
> **최종 수정**: 2026-07-13
> **버전**: v1.3
> **상태**: 검토 요청
> **태그**: sprint-2, wbs-3.6.2, frontend, qa, ci

# Sprint 2 WBS 3.6.2 Frontend route/build/browser QA Evidence 정리

> 기준: 구글 시트 WBS 탭 3.6.2 row, [`황스일/sprint-2-wbs-3.2.5-company-frontend-qa-결과.md`](../황스일/sprint-2-wbs-3.2.5-company-frontend-qa-결과.md), [`sprint-2-wbs-3.4.4-project-frontend-검증기준.md`](./sprint-2-wbs-3.4.4-project-frontend-검증기준.md)
> WBS 3.6.2(Frontend route/build/browser QA evidence 정리, Owner 김민정)의 착수 가능 조건은 "frontend QA 케이스 선작성 가능; 최종 evidence는 화면 구현 결과 후"다. 선행 WBS는 3.2, 3.4 전체다. 완료 조건(시트 원문): "route skeleton이 아니라 사용자 흐름, 권한별 UI, 상태 처리가 확인됨".
> 3.6.2는 새 검증 케이스를 만드는 문서가 아니라, 이미 만든 **3.2.5(Company Frontend)**와 **3.4.4(Project Frontend)** 결과를 취합하는 상위 roll-up이다. 개별 케이스는 두 문서에서 관리하고, 여기서는 전체 lint/typecheck/build 상태와 두 문서의 종합 판정만 다룬다.

---

## 0. WBS 3.6.2 진행 현황 (본인 self-report)

| WBS | 진척률 | 비고 |
|---|---|---|
| 3.6.2 | 100% (재검증 완료, Reviewer 확인 대기) | 취합 완료(2026-07-13). 두 blocking 이슈 모두 해소 확인: **PR #72**(3.4.4) Finding F-3가 커밋 [`1ca0e97`](https://github.com/SEUIL/Threadia/commit/1ca0e97)로 해소되고 dev 머지 완료(`dd61591`) — 브라우저 재검증(2026-07-12) 완료, 3.4.4 문서 95%로 갱신됨. F-1/F-2는 owner 스코프 판단으로 보류/종결. **`/hub` WebGL fallback**(3.2.5)도 커밋 [`1c36c8a`](https://github.com/SEUIL/Threadia/commit/1c36c8a)가 dev에 반영되어 있음을 확인하고, `member1@test.com`(A 역할)으로 브라우저 재검증(2026-07-13) — WebGL 정상 지원 시 `<canvas>` 정상 렌더(에러 없음), WebGL 미지원 재현(`HTMLCanvasElement.getContext` 패치) 시 "3D 허브를 표시할 수 없습니다" fallback이 정확히 노출되고 좌측 레일(HubOverlay)도 유지됨을 확인. §2.2 두 라인 모두 **통과**로 갱신, 종합 판정 **통과** 확정. **남은 것: Reviewer(정철진) 확인 요청** |

---

## 1. 착수 조건 확인

| WBS | 상태(2026-07-13 기준) | 비고 |
|---|---|---|
| 3.2 (Company Frontend, 3.2.1~3.2.4) | **완료** (dev merge, PR #62) | 상세는 [`황스일/sprint-2-wbs-3.2.5-company-frontend-qa-결과.md`](../황스일/sprint-2-wbs-3.2.5-company-frontend-qa-결과.md) 참고. QA(3.2.5)는 황스일이 수행, PR #77로 dev 반영. `/hub` WebGL 실패 finding은 fallback 처리(`1c36c8a`)가 dev에 반영돼 있음을 확인하고, 2026-07-13 브라우저로 재검증 완료(§2.2) |
| 3.4 (Project Frontend, 3.4.1~3.4.3) | **완료** (PR #72 dev 머지, `dd61591`) | 상세는 [`sprint-2-wbs-3.4.4-project-frontend-검증기준.md`](./sprint-2-wbs-3.4.4-project-frontend-검증기준.md) §1 참고. 선행 3.3.x는 dev merge 완료. PR #72 Finding F-3는 커밋 `1ca0e97`로 해소·재검증 완료(2026-07-12), F-1/F-2는 owner 스코프 판단으로 보류/종결. PR #72는 Reviewer(김민정) 승인 후 dev 머지됨 |

3.2·3.4 모두 dev 머지 완료 상태로, §2 종합 판정을 두 라인 모두 통과로 채운다. Company(`/hub` WebGL fallback)·Project(PR #72 F-3) 두 blocking 이슈가 모두 해소됐음을 브라우저로 재확인해 최종 종합 "통과"를 확정한다.

---

## 2. Frontend 전체 QA 판정 기준

### 2.1 빌드/스크립트 공통 확인

| 항목 | 확인 방법 | 확인 결과 | 비고 |
|---|---|---|---|
| lint | `npm run lint` | **실패(PR 밖 원인)** | error 2 + warning 1, 전부 PR #72/#62 변경 파일 밖 기존 이슈(`graph/page.tsx` set-state-in-effect, `notifications/page.tsx` unused var, `design-sample/.../layouts` refs). 3.4.4·3.2.5 검증에서 동일 확인. repo "error 0" 미충족이나 이번 프론트 PR 책임 아님 |
| typecheck | `npm run typecheck` (script 있으면) | **script 없음 → build로 대체** | `package.json`에 typecheck script 부재. `next build`가 타입체크 포함(통과), 황스일 3.2.5 검증에서도 `npx tsc --noEmit` PASS 확인 |
| build | `npm run build` | **정상 통과** | 2026-07-11(3.4.4 검증 시) `next build` 성공, TS 에러 0, 전 route 빌드(PR #72의 `projects`·`settings` 포함), `backlog` TS 에러 재발 없음. 황스일 3.2.5 검증에서도 `npm run build` PASS |

### 2.2 화면별 케이스 종합 판정

| 라인 | 문서 | 케이스 수 | 종합 결과 | 비고 |
|---|---|---|---|---|
| Company Frontend (3.2.5) | [`황스일/sprint-2-wbs-3.2.5-company-frontend-qa-결과.md`](../황스일/sprint-2-wbs-3.2.5-company-frontend-qa-결과.md) | 브라우저 11 + 명령/API 검증 (황스일 문서 구조) | **통과(재검증, 2026-07-13)** | 1차(2026-07-11): 브라우저 11개 중 10 PASS, `/hub` S03 허브 진입만 WebGL context 생성 실패로 FAIL(황스일 §4 #3). **재검증(2026-07-13)**: `/hub` WebGL fallback 커밋 `1c36c8a`가 dev에 반영된 것을 확인 후, `member1@test.com`으로 브라우저 재현 — WebGL 정상 지원 시 `<canvas>` 정상 렌더(콘솔 에러 0건), `HTMLCanvasElement.getContext`를 패치해 WebGL 미지원을 재현하면 "3D 허브를 표시할 수 없습니다" fallback이 정확히 노출되고 좌측 레일(HubOverlay)도 그대로 유지됨을 확인. API/RBAC·build·tsc는 1차와 동일 PASS, lint는 기존 파일 원인 FAIL(3.4.4와 동일 사유, 이 PR 밖) |
| Project Frontend (3.4.4) | [`sprint-2-wbs-3.4.4-project-frontend-검증기준.md`](./sprint-2-wbs-3.4.4-project-frontend-검증기준.md) | 22개 | **통과(재검증, 2026-07-12)** | 1차(2026-07-10): 정상 통과 16 / 부분·Finding 4(#1/#5/#8/#19) / 확인 불가 2(#4/#18). **재검증(2026-07-12)**: Finding F-3(#19, 멤버목록 Promise.all 결합)가 커밋 `1ca0e97`로 해소되고 dev 머지 완료(`dd61591`) — 브라우저로 before/after 대비 확인, 정상 통과 17로 상향. F-1(#1, 카드 데이터 축소)·F-2(#8, P 역할 부재)는 owner가 스코프 판단으로 보류/종결(기능 버그 아님). 확인 불가 2건(#4/#18)은 그대로 |

`종합 결과`는 `통과 / 부분 통과(실패 케이스 있음) / 확인 불가` 중 하나로 표기한다. `부분 통과`면 실패 케이스 번호와 관련 PR을 비고에 남긴다.

### 2.3 권한별 UI 교차 확인

3.2.5와 3.4.4는 각각 회사/프로젝트 범위에서 권한별 UI(A/P/C)를 다루므로, 두 문서의 권한별 케이스가 서로 일관되는지 여기서 한 번 더 확인한다.

| 확인 항목 | 기대 결과 | 확인 결과 | 비고 |
|---|---|---|---|
| A 역할 사용자가 회사/프로젝트 관리 메뉴 전반에서 일관되게 제한되는가 | 3.2.5·3.4.4의 forbidden 케이스가 모두 통과 | **통과** | 3.2.5: 일반 멤버가 회사 stats/부서/구성원 API 403(황스일 §3). 3.4.4: A 역할이 프로젝트 생성 버튼 미노출(#7)·S12 직접 접근 시 차단(#12/#22). 회사·프로젝트 양쪽에서 A 제한 일관 |
| C 역할 사용자의 회사 관리 권한과 프로젝트 생성 권한이 두 화면에서 일관되는가 | 두 문서의 "C만 가능" 케이스가 모두 통과 | **통과** | 3.2.5: 관리자 회사 관리 API 200(황스일 §3). 3.4.4: C만 `+ 새 프로젝트` 버튼 노출(#6)·프로젝트 생성 성공(#3/#10). C 권한 일관 |

---

## 3. 검증 방법

1. 3.2, 3.4가 완료된 시점에 §1을 갱신한다.
2. 3.2.5, 3.4.4 문서의 §2 케이스를 브라우저에서 역할별로 확인하고 각 문서의 `확인 결과`를 채운다.
3. §2.1 lint/typecheck/build를 실행한다.
4. §2.2, §2.3 종합 판정표를 채운다.
5. 결과를 Reviewer(정철진)에게 재확인 요청한다.
6. 실패 케이스가 있으면 3.6 전체 완료 처리를 보류하고 owner(정철진)에게 후속 조치를 요청한다.

---

## 4. 남은 작업

- Reviewer(정철진)에게 재확인 요청(§2.2 두 라인 통과 확정, 종합 판정 통과)
