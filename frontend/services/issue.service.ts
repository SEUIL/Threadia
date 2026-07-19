/*
 * Issue API 클라이언트 — 계약: docs/04-api/API-명세서.md §7, §13.6 (WBS 4.4.1, PR #99).
 * apiClient가 Bearer access token 첨부와 401 refresh를 처리한다.
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  IssueCreateRequest,
  IssueData,
  IssueListFilters,
  IssuesData,
  IssueStatus,
  IssueUpdateRequest,
} from "@/types/issue.types";

export const issueService = {
  /** 이슈 목록. ⚠️ unassignedOnly와 sprintId를 함께 넘기면 서버가 400을 준다. */
  list: (projectId: string | number, filters?: IssueListFilters) =>
    apiClient.get<ApiResponse<IssuesData>>(`/api/v1/projects/${projectId}/issues`, {
      params: filters,
    }),

  /** 이슈 생성. 상태는 항상 BACKLOG. 관리 전용 필드(priority/assignee/dueDate/sprintId)는 P/C만. */
  create: (projectId: string | number, body: IssueCreateRequest) =>
    apiClient.post<ApiResponse<IssueData>>(`/api/v1/projects/${projectId}/issues`, body),

  /** 바꿀 필드만 담아 보낸다 — 보낸 key만 반영되고 허용되지 않은 필드는 400이다. */
  update: (issueId: string | number, body: IssueUpdateRequest) =>
    apiClient.patch<ApiResponse<IssueData>>(`/api/v1/issues/${issueId}`, body),

  /** A는 DONE으로 바꿀 수 없다(403). 같은 상태로 바꾸면 200 no-op. */
  updateStatus: (issueId: string | number, body: { status: IssueStatus }) =>
    apiClient.patch<ApiResponse<IssueData>>(`/api/v1/issues/${issueId}/status`, body),

  /** 휴지통 이동 — P/C 또는 등록자. idempotent. */
  trash: (issueId: string | number) =>
    apiClient.post<ApiResponse<IssueData>>(`/api/v1/issues/${issueId}/trash`),

  restore: (issueId: string | number) =>
    apiClient.post<ApiResponse<IssueData>>(`/api/v1/issues/${issueId}/restore`),

  detail: (issueId: string | number) =>
    apiClient.get<ApiResponse<IssueData>>(`/api/v1/issues/${issueId}`),
};
