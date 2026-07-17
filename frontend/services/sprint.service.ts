/*
 * Sprint API 클라이언트 — 계약: docs/04-api/API-명세서.md §6, §13.5 (WBS 4.4.1, PR #99).
 * apiClient가 Bearer access token 첨부와 401 refresh를 처리한다.
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  SprintCreateRequest,
  SprintData,
  SprintIssueAssignmentRequest,
  SprintUpdateRequest,
  SprintsData,
} from "@/types/sprint.types";

export const sprintService = {
  /** Sprint 목록 — A/P/C */
  list: (projectId: string | number) =>
    apiClient.get<ApiResponse<SprintsData>>(`/api/v1/projects/${projectId}/sprints`),

  /** Sprint 생성 — P/C. 생성 상태는 항상 PLANNED다. */
  create: (projectId: string | number, body: SprintCreateRequest) =>
    apiClient.post<ApiResponse<SprintData>>(`/api/v1/projects/${projectId}/sprints`, body),

  /**
   * Sprint metadata 상세 — 배정 이슈는 포함하지 않는다.
   * 배정 이슈는 issueService.list(projectId, { sprintId })로 따로 조회한다.
   */
  detail: (sprintId: string | number) =>
    apiClient.get<ApiResponse<SprintData>>(`/api/v1/sprints/${sprintId}`),

  /** Sprint 수정 — P/C, PLANNED 상태에서만 허용된다. */
  update: (sprintId: string | number, body: SprintUpdateRequest) =>
    apiClient.patch<ApiResponse<SprintData>>(`/api/v1/sprints/${sprintId}`, body),

  /** Sprint 시작 — P/C. 프로젝트당 ACTIVE는 하나뿐이라 이미 있으면 실패한다. */
  start: (sprintId: string | number) =>
    apiClient.post<ApiResponse<SprintData>>(`/api/v1/sprints/${sprintId}/start`),

  /**
   * Sprint 종료 — P/C. DONE이 아닌 배정 이슈는 **서버가 원자적으로** BACKLOG로
   * 되돌리고 Sprint를 clear한다(프론트에서 따로 정리하지 않는다).
   */
  complete: (sprintId: string | number, body?: { summary?: string | null }) =>
    apiClient.post<ApiResponse<SprintData>>(`/api/v1/sprints/${sprintId}/complete`, body ?? {}),

  /** 이슈 일괄 배정 — P/C. issueIds가 비어 있으면 400. status는 바뀌지 않는다. */
  assignIssues: (sprintId: string | number, body: SprintIssueAssignmentRequest) =>
    apiClient.post<ApiResponse<SprintData>>(`/api/v1/sprints/${sprintId}/issues`, body),
};
