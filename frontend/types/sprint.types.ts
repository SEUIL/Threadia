/*
 * Sprint 응답 타입 — backend SprintResponse record와 1:1 매핑.
 * 계약 기준: docs/04-api/API-명세서.md §6, §13.5 (WBS 4.4.1, PR #99)
 *
 * 상태는 PLANNED → ACTIVE → COMPLETED 단방향이고 생성은 항상 PLANNED다.
 * 프로젝트별 ACTIVE Sprint는 하나뿐이며, 수정은 PLANNED에만 허용된다.
 * 생성·수정·시작·완료·일괄배정은 전부 P/C 전용이다.
 */
export type SprintStatus = "PLANNED" | "ACTIVE" | "COMPLETED";

export interface SprintResponse {
  id: number;
  projectId: number;
  name: string;
  goal: string | null;
  status: SprintStatus;
  startDate: string | null; // yyyy-MM-dd
  endDate: string | null;
  startedAt: string | null; // ISO-8601 UTC, 시작 시 서버가 기록
  completedAt: string | null;
  summary: string | null; // 완료 시 입력한 회고 요약
}

export interface SprintsData {
  sprints: SprintResponse[];
}

export interface SprintData {
  sprint: SprintResponse;
}

/** name은 필수(최대 150자). 나머지는 선택. */
export interface SprintCreateRequest {
  name: string;
  goal?: string | null;
  startDate?: string | null;
  endDate?: string | null;
}

/** PLANNED 상태에서만 허용된다. */
export interface SprintUpdateRequest {
  name?: string;
  goal?: string | null;
  startDate?: string | null;
  endDate?: string | null;
}

/**
 * Sprint에 이슈 일괄 배정.
 * ⚠️ issueIds는 null이거나 빈 배열일 수 없다(400). 배정은 status를 바꾸지 않는다.
 */
export interface SprintIssueAssignmentRequest {
  issueIds: number[];
}
