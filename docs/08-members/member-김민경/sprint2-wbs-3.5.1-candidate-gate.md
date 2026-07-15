> **작성자**: Threadia 팀
> **작성일**: 2026-07-13
> **최종 수정**: 2026-07-13
> **버전**: v1.1
> **상태**: 검토 중
> **태그**: sprint-2, wbs-3.5.1, issue, kanban, candidate-gate

# Sprint 2 WBS 3.5.1 Issue/Kanban Candidate Gate

> 기준: WBS 내보내기 `WBS` 탭 3.5.1 행, `docs/00-project/sprint-운영/sprint-2-상세계획.md`, `docs/08-members/member-김민정/sprint-2-wbs-3.4.4-project-frontend-검증기준.md`

## 1. 결정

WBS 3.5는 Sprint 2 committed scope가 아닌 capacity candidate다. 다만 현재 후보 실행은 팀장 승인에 따라 진행한다. 이 결정은 Foundation의 WBS 현황과 남은 수행 가능 인력을 함께 고려한 실행 결정이며, WBS 범위나 외부 WBS 담당 귀속을 변경하지 않는다.

WBS 3.5.1의 외부 WBS Owner는 김민정이고 Reviewer는 황스일이다. 실제 구현은 김민정과 김민경이 수행할 수 없으므로, 팀장 승인에 따라 지정된 책임 실행자 1인이 단독으로 맡는다. 이 문서는 실제 실행 책임과 WBS Owner/Reviewer 역할을 구분해 기록하며, 외부 WBS의 Owner 또는 Reviewer를 변경하지 않는다.

## 2. Foundation 및 finding 사실관계

WBS 내보내기 기준으로 3.1 Company/RBAC Backend, 3.3 Project Backend, 3.4 Project Frontend는 `완료` 상태다. 3.2 Company Frontend/Entry는 `진행중` 상태이며 진척률은 100%로 기록되어 있다. 이 문서는 해당 WBS 현황을 후보 실행 판단의 배경으로만 사용하며, Foundation 전체의 추가 검증·review·완료를 선언하지 않는다.

3.4.4 검증 기준 문서의 finding 상태는 다음과 같다.

- F-1은 프로젝트 카드의 멤버 수·이슈 수·PM 미표시에 관한 후속 채움 사항으로 남아 있다.
- F-2는 S04에서 P 역할을 A로 근사하는 한계이나, 해당 문서에서 기능 버그가 아닌 것으로 종결했다.
- F-3은 멤버 목록 API 실패가 프로젝트 설정 전체 오류로 번지던 문제이며, 2026-07-12 재검증에서 멤버 섹션 국소 오류 처리로 해소를 확인했다.

## 3. 후보 실행 범위

팀장 승인으로 실행하는 범위는 아래 항목으로 고정한다.

| WBS | 포함 범위 |
|---|---|
| 3.5.2 | 프로젝트 하위 이슈 목록·생성의 `GET`/`POST` |
| 3.5.3 | `BACKLOG`, `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE` 고정 5컬럼과 이슈 상태 `PATCH`; A 역할의 `DONE` 이동은 금지 |
| 3.5.4 | 후보 실행 결과의 evidence 정리 |

후보 API 계약은 프로젝트 이슈 `GET`, title-only `POST`, 이슈 상태 `PATCH`로 제한한다. `POST`는 `title`만 받고 `BACKLOG` 상태로 생성한다. 세 endpoint의 이슈 응답 객체는 `id`, `issueKey`, `title`, `status`만 포함한다.

`description`, `priority`, `assigneeUserId`, `sprintId`, `dueDate`, `sourceType`을 포함하는 full Issue contract와 이슈 상세 수정은 이번 후보 실행에서 제외한다. canonical 계약 변경 묶음은 [2026-07-13 WBS 3.5 candidate Issue/Kanban 최소 API 계약 동기화](../../07-decisions/기획문서-변경이력.md#change-2026-07-13-wbs-3-5-candidate-issue-kanban-api-contract)를 따른다.

## 4. 명시적 제외 범위

다음 항목은 이번 후보 실행 범위에 포함하지 않는다.

- 이슈 상세 수정
- 담당자 지정
- 우선순위
- 마감일
- 휴지통 및 복원
- Sprint 연결
- from-message 연결
- 문서 연결
- thread 연결
- notification 연결

## 5. capacity 근거와 기록 원칙

후보 실행은 Foundation 완료 후 남는 capacity가 있을 때만 검토하는 기존 WBS 원칙과 달리, 현재는 팀장 승인에 따른 단일 책임 실행으로 진행한다. 김민정과 김민경은 이 작업을 수행할 수 없으므로 둘을 구현 인력 또는 병렬 담당으로 기록하지 않는다. 제공된 근거에 없는 작업 시간, 추정치, 일정, 승인 시점, merge, test 결과는 기록하지 않는다.

문서 상태가 `검토 중`인 동안에는 구현, validation, review, PR 상태 또는 merge가 완료되었다고 표기하지 않는다. 3.5.4에는 실제로 생성된 evidence 또는 미실행 사실만 후속으로 기록한다.
