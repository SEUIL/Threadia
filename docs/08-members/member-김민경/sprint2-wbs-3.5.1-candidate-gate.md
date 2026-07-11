> **작성자**: 김민경
> **검토자**: 황스일
> **작성일**: 2026-07-11
> **WBS**: 3.5.1 Candidate Gate
> **버전**: v1.0
> **상태**: 계획
> **우선순위**: P1
> **태그**: sprint-2, wbs-3.5.1, candidate-gate, issue, kanban, go-no-go

# Sprint 2 WBS 3.5.1 Candidate Gate

## 0. 목적

Sprint 2 Foundation 완료 상태와 팀 capacity를 기준으로 Issue/Kanban 후보 작업의 착수 여부와 최소 범위를 결정한다.

본 Gate는 `ISSUE-01~03`, `KANBAN-01~02`를 Sprint 2 **Candidate Scope**로 착수할 수 있는지 판단하기 위한 의사결정 문서다. 이 문서는 Candidate 착수 가능 여부를 판단하는 문서이며, 팀 합의 없이 해당 범위를 Sprint 2 **Committed Scope**로 승격하지 않는다.

---

## 1. Foundation 상태 검토

| 구분 | 상태 | 검토 결과 |
|---|---|---|
| WBS 3.1 | 완료 | Company/RBAC Backend Foundation 완료 |
| WBS 3.2 | 완료 | Company/RBAC Frontend Foundation 완료 |
| WBS 3.3 | 완료 | Project Backend Foundation 완료 |
| WBS 3.4 | QA 완료, PR 리뷰 진행 | Project Frontend 및 통합 QA 완료, PR 리뷰 진행 중 |

### 1.1 3.4.4 검증 결과

| 검증 항목 | 결과 |
|---|---|
| Backend `:8080`와 Frontend `:3000` 실연동 | 정상 |
| 프로젝트 목록/생성/카드 이동 | 정상 |
| 프로젝트 상세/수정/아카이빙 | 정상 |
| 멤버 역할 변경 | 정상 |
| 권한 제어 | 정상 |
| `npm run build` | 성공 |
| TypeScript 오류 | 0건 |

### 1.2 Foundation 검토 결론

Foundation 기능은 정상 동작한다. 3.4는 QA를 완료했고 PR 리뷰가 진행 중이나, Candidate 착수를 막는 Critical Issue는 확인되지 않았다.

3.4.4 검증 기준으로 Backend/Frontend 실연동, 프로젝트 주요 흐름, 멤버 역할 변경, 권한 제어, build/type 검증이 모두 통과했으므로 Candidate Gate 판단에 필요한 Foundation 기준은 충족된 것으로 본다.

---

## 2. 현재 구현 범위

현재 Sprint 2 Foundation 기준으로 구현 및 검증된 범위는 다음과 같다.

| 영역 | 구현/검증 범위 | 상태 |
|---|---|---|
| Company/RBAC Backend | 회사, 부서, 회사 구성원, RBAC API 및 검증 | 완료 |
| Company/RBAC Frontend | 회사 프로필, 진입 제어, 구성원/권한 UI 연동 | 완료 |
| Project Backend | 프로젝트 목록/생성/상세/수정/아카이빙, 프로젝트 멤버/역할 기반 제어 | 완료 |
| Project Frontend | 프로젝트 목록/생성/상세/수정/아카이빙, 멤버 역할 변경, 권한 UI 연동 | QA 완료, PR 리뷰 진행 |
| 통합 검증 | Backend `:8080` + Frontend `:3000` 실연동, build/type 검증 | 통과 |

현재 구현 범위는 Issue/Kanban 기능의 선행 조건인 회사, 프로젝트, 프로젝트 멤버, 권한 기반 동작을 제공한다. 따라서 `S05`, `issues`, `project_members` 기반 Candidate 착수에 필요한 최소 Foundation은 확보된 상태다.

---

## 3. Capacity 평가

### 3.1 Capacity 판단 기준

Candidate Gate의 capacity 평가는 다음 기준으로 수행한다.

| 기준 | 평가 |
|---|---|
| Foundation 선행 작업 완료 여부 | 3.1~3.3 완료, 3.4 QA 완료 |
| 통합 동작 안정성 | Backend/Frontend 실연동 정상 |
| build/type 안정성 | `npm run build` 성공, TypeScript 오류 0건 |
| Blocking Finding 존재 여부 | 없음 |
| Candidate 착수 시 영향 범위 | S05, issues, project_members 중심으로 제한 가능 |
| Committed Scope 승격 여부 | 팀 합의 전 승격 금지 |

### 3.2 Capacity 평가 결과

Foundation이 정상 동작하고 build/type 검증이 통과했으므로, Issue/Kanban Candidate의 최소 범위 착수는 가능하다.

다만 3.4 PR 리뷰가 진행 중이고 Finding 후속 개선이 남아 있으므로, 착수 범위는 `ISSUE-01~03`, `KANBAN-01~02`의 최소 구현 검토와 기반 설계/착수 준비로 제한한다. Candidate Scope는 용량 기반 후보이며, 팀 합의 없이 Sprint 2 Committed Scope로 간주하지 않는다.

---

## 4. Candidate 범위

### 4.1 착수 후보

| 후보 ID | 기능 | 최소 범위 |
|---|---|---|
| ISSUE-01 | 이슈 생성 | 프로젝트 하위 이슈 생성, 기본 제목/설명/status 저장 |
| ISSUE-02 | 이슈 상세 조회/수정 | 이슈 상세 조회, 제한된 필드 수정 |
| ISSUE-03 | 담당자/우선순위/마감일 지정 | 프로젝트 멤버 기반 담당자 검증, 우선순위/마감일 필드 관리 |
| KANBAN-01 | 5컬럼 칸반 보드 | `BACKLOG`, `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE` 고정 컬럼 표시 |
| KANBAN-02 | 카드 상태 변경 | 칸반 카드 이동 및 상태 변경, 권한 기준 적용 |

### 4.2 최소 착수 범위

Candidate 착수 시 최소 범위는 다음으로 제한한다.

| 범위 | 내용 |
|---|---|
| 화면 | S05 칸반 보드 중심 |
| 데이터 | `issues`, `project_members` 기반 |
| 기능 | 이슈 생성/조회/수정, 담당자/우선순위/마감일, 5컬럼 보드, 카드 상태 변경 |
| 권한 | 기존 Foundation의 회사/프로젝트 멤버 권한 정책을 재사용 |
| 제외 | 칸반 컬럼 커스터마이징, 고급 필터/검색, 복잡한 알림 연동, Thread/Document 추가 연결 |

---

## 5. Finding 검토

아래 Finding은 모두 Candidate 착수를 막는 Blocker가 아니라 Minor Finding으로 판단한다. 따라서 Go 판단의 제한 조건이나 No-Go 사유로 사용하지 않는다.

| ID | 내용 | 영향 | 판단 | 후속 처리 |
|---|---|---|---|---|
| F-1 | S04 카드의 멤버 수/이슈 수/PM 미표시 | API 필드 미구현으로 표시 보완 필요 | Minor | 후속 API 필드 확장 및 UI 표시 개선 |
| F-2 | P 역할 표시가 A로 근사됨 | 권한 동작은 정상이나 UI 표현이 부정확 | Minor | 역할 label/display mapping 개선 |
| F-3 | 멤버 API 실패 시 페이지 전체 Error | Error Handling 개선 필요 | Minor | 부분 실패 처리 및 fallback UI 개선 |

### 5.1 Finding 판단 근거

- F-1은 표시 데이터 보완 사항이며 Foundation 핵심 기능과 권한 동작을 차단하지 않는다.
- F-2는 UI 표현 문제로, 실제 권한 제어는 정상 동작한다.
- F-3은 실패 상황의 Error Handling 개선 사항이며 정상 동작 흐름에는 영향이 없다.
- 세 Finding 모두 Critical Issue가 아니며 Candidate 착수 판단의 Blocker가 아니다.

---

## 6. Go / No-Go 판단 및 근거

## 6.1 판단 결과

**Go**

Sprint 2 Candidate Gate는 Go로 판단한다.

### 6.2 Go 판단 근거

| 근거 | 판단 |
|---|---|
| Foundation 기능 정상 동작 | 충족 |
| 3.1~3.3 완료 | 충족 |
| 3.4 QA 완료 | 충족 |
| 3.4 PR 리뷰 진행 중 | 착수 차단 아님 |
| Backend/Frontend 실연동 정상 | 충족 |
| 프로젝트 목록/생성/카드 이동 정상 | 충족 |
| 프로젝트 상세/수정/아카이빙 정상 | 충족 |
| 멤버 역할 변경 정상 | 충족 |
| 권한 제어 정상 | 충족 |
| `npm run build` 성공 | 충족 |
| TypeScript 오류 0건 | 충족 |
| Candidate 착수를 막는 Critical Issue | 없음 |
| Finding | 모두 Minor 후속 개선 사항 |

### 6.3 No-Go로 판단하지 않는 이유

현재 확인된 Finding은 모두 후속 개선 사항이며, Issue/Kanban Candidate 착수 자체를 차단하지 않는다. Foundation의 핵심 기능, 실연동, 권한 제어, build/type 검증이 모두 통과했으므로 No-Go 판단 근거가 없다.

---

## 7. 3.5.2 착수 가능 여부

**3.5.2 착수 가능**

WBS 3.5.2는 Candidate Scope의 최소 범위를 기준으로 착수 가능하다.

단, 3.5.2 착수는 `ISSUE-01~03`, `KANBAN-01~02`를 Sprint 2 Candidate Scope로 다루는 것을 의미한다. 이는 Sprint 2 Committed Scope 확정이 아니며, 팀 합의와 capacity 재확인 없이 committed scope로 승격하지 않는다.

---

## 8. 착수/미착수 사유

### 8.1 착수 사유

| 항목 | 사유 |
|---|---|
| Foundation 통과 | 3.1~3.3 완료, 3.4 QA 완료로 Candidate 선행 조건 충족 |
| 통합 안정성 | Backend/Frontend 실연동 정상 |
| 핵심 Project 흐름 정상 | 프로젝트 목록/생성/상세/수정/아카이빙 및 카드 이동 정상 |
| 권한 기반 동작 가능 | 멤버 역할 변경과 권한 제어 정상 |
| Build/Test 통과 | `npm run build` 성공, TypeScript 오류 0건 |
| Critical Issue 없음 | Candidate 착수를 막는 Blocker 없음 |
| 범위 제한 가능 | S05, issues, project_members 중심의 최소 범위로 착수 가능 |

### 8.2 미착수 사유 해당 여부

| 미착수 기준 | 해당 여부 | 비고 |
|---|---|---|
| Foundation 미완료 | 해당 없음 | Foundation 기능 정상 동작 |
| Build/Test 실패 | 해당 없음 | build 성공, TypeScript 오류 0 |
| Critical Issue 존재 | 해당 없음 | Finding은 모두 Minor |
| 권한 제어 불안정 | 해당 없음 | 권한 제어 정상 |
| 실연동 실패 | 해당 없음 | Backend/Frontend 실연동 정상 |
| Candidate 범위 과다 | 부분 관리 필요 | 최소 범위로 제한하여 착수 |

현재 기준에서 미착수 사유는 없다. 다만 Candidate Scope는 용량 기반 후보이므로, 착수 후에도 팀 capacity와 PR 리뷰 결과를 계속 확인한다.

---

## 9. 최종 결론

Sprint 2 WBS 3.5.1 Candidate Gate 판단 결과는 **Go**다.

Foundation은 정상 동작하며, 3.1~3.3은 완료되었고 3.4는 QA 완료 및 PR 리뷰 진행 상태다. Backend `:8080`와 Frontend `:3000` 실연동, 프로젝트 주요 흐름, 멤버 역할 변경, 권한 제어, build/type 검증이 모두 통과하였다.

확인된 F-1, F-2, F-3은 모두 Minor Finding이며 후속 개선 사항으로 기록한다. 이 Finding들은 Candidate 착수를 막는 Blocker 또는 No-Go 사유가 아니다.

따라서 `ISSUE-01~03`, `KANBAN-01~02`는 Sprint 2 **Candidate Scope**로 3.5.2에서 착수 가능하다. 단, 이는 Sprint 2 **Committed Scope** 확정이 아니며, 팀 합의 없이 committed scope로 승격하지 않는다.

