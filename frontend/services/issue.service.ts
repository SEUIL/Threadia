import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type { IssueData, IssuesData, IssueStatus } from "@/types/issue.types";

export const issueService = {
  list: (projectId: string | number) =>
    apiClient.get<ApiResponse<IssuesData>>(`/api/v1/projects/${projectId}/issues`),

  create: (projectId: string | number, body: { title: string }) =>
    apiClient.post<ApiResponse<IssueData>>(`/api/v1/projects/${projectId}/issues`, body),

  updateStatus: (issueId: string | number, body: { status: IssueStatus }) =>
    apiClient.patch<ApiResponse<IssueData>>(`/api/v1/issues/${issueId}/status`, body),

  unassignFromSprint: (issueId: string | number) =>
    apiClient.patch<ApiResponse<IssueData>>(`/api/v1/issues/${issueId}`, { sprintId: null }),
};
