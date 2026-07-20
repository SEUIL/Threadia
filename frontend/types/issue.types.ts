/*
 * Issue 응답 타입 — backend IssueResponse record와 1:1 매핑.
 * 계약 기준: docs/04-api/API-명세서.md §7, §13.6 (WBS 4.4.1, PR #99)
 *
 * 이전에는 id/issueKey/title/status만 있었다(WBS 3.5 최소 Kanban 구현 시점).
 * 4.4.1이 full 계약을 열어주면서 나머지 필드를 채웠다.
 */
export type IssueStatus = "BACKLOG" | "TODO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE";

export type IssuePriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

/** 지금은 MANUAL만 생성된다(메시지 기반 생성은 계약 밖). */
export type IssueSourceType = "MANUAL";

export interface IssueResponse {
  id: number;
  /** 이슈 단독 참조(알림, 검색 등)에서 프로젝트 route를 복원하기 위한 필드. */
  projectId: number;
  issueKey: string;
  title: string;
  status: IssueStatus;
  description: string | null;
  priority: IssuePriority;
  assigneeUserId: number | null;
  /**
   * 등록자. 표시용이 아니라 **권한 판정용**이다 — trash/restore는 P/C 또는 등록자에게
   * 허용되므로(backend IssueService.trashOrRestore) 이 값과 현재 사용자를 비교해야
   * "내가 등록한 이슈" 경로를 판정할 수 있다. reporter는 immutable이다.
   */
  reporterUserId: number | null;
  sprintId: number | null;
  dueDate: string | null; // yyyy-MM-dd
  sourceType: IssueSourceType;
  trashedAt: string | null; // ISO-8601 UTC
  restoredAt: string | null; // ISO-8601 UTC
}

export interface IssuesData {
  issues: IssueResponse[];
}

export interface IssueData {
  issue: IssueResponse;
}

/**
 * 이슈 목록 필터. 페이지네이션은 없다.
 * ⚠️ unassignedOnly=true와 sprintId를 함께 보내면 400 VALIDATION_ERROR다.
 */
export interface IssueListFilters {
  status?: IssueStatus;
  sprintId?: number;
  assigneeId?: number;
  /** Sprint가 배정되지 않은 이슈만. sprintId와 동시 사용 불가. */
  unassignedOnly?: boolean;
  /** 기본 false — trashed 이슈를 함께 반환할지. */
  includeTrashed?: boolean;
}

/**
 * 이슈 생성. 상태는 항상 BACKLOG로 시작한다.
 * A(일반 멤버)는 title/description만 지정할 수 있고, priority/assigneeUserId/
 * dueDate/sprintId는 P/C만 지정할 수 있다.
 */
export interface IssueCreateRequest {
  title: string;
  description?: string | null;
  priority?: IssuePriority;
  assigneeUserId?: number | null;
  dueDate?: string | null;
  sprintId?: number | null;
}

/**
 * 이슈 수정. **보낸 필드만 반영된다** — 서버가 request body의 key 집합으로 판정하므로
 * 생략한 필드는 유지되고, 명시적 null은 description/assigneeUserId/dueDate/sprintId를 clear한다.
 * 허용되지 않은 필드를 담으면 400이라 바꿀 필드만 보내야 한다.
 * A는 title/description만 수정할 수 있다(그 외 필드를 담으면 403).
 */
export interface IssueUpdateRequest {
  title?: string;
  description?: string | null;
  priority?: IssuePriority;
  assigneeUserId?: number | null;
  dueDate?: string | null;
  /**
   * **해제 전용이라 null만 받는다.** 이 endpoint로는 배정할 수 없고, 값을 담아 보내면
   * 서버가 400으로 거절하며 `POST /sprints/{sprintId}/issues`를 쓰라고 안내한다
   * (IssueService.update 참고). 배정 경로를 하나로 유지하려는 계약이라, 타입도
   * `number | null`로 열어두지 않고 실제로 가능한 값만 남긴다.
   */
  sprintId?: null;
}
