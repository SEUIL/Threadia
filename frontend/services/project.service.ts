/*
 * Project API 클라이언트 — company.service.ts와 동일 패턴.
 * 계약: PR #61(목록/생성), PR #65(상세/수정/archive).
 *
 * ⚠️ update()는 partial patch가 아니라 full-replace다. status가 @NotNull이라
 * 호출마다 현재 status를 항상 같이 보내야 한다(ARCHIVED는 이 경로로 못 보냄 —
 * 반드시 archive()를 따로 호출). company-member.service.ts의 update()와 동일한
 * 이유의 제약이다.
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  ProjectArchiveData,
  ProjectCreateData,
  ProjectData,
  ProjectDashboardData,
  ProjectDetailData,
  ProjectStatus,
  ProjectsData,
} from "@/types/project.types";

export const projectService = {
  list: (companyId: string | number) =>
    apiClient.get<ApiResponse<ProjectsData>>(`/api/v1/companies/${companyId}/projects`),

  create: (
    companyId: string | number,
    body: {
      name: string;
      projectKey: string;
      description?: string | null;
      status?: ProjectStatus;
      startDate?: string | null;
      endDate?: string | null;
    }
  ) => apiClient.post<ApiResponse<ProjectCreateData>>(`/api/v1/companies/${companyId}/projects`, body),

  detail: (projectId: string | number) =>
    apiClient.get<ApiResponse<ProjectDetailData>>(`/api/v1/projects/${projectId}`),

  /** live-read 집계 — WBS 4.9.3.1. cache/snapshot 없이 매번 MySQL을 그대로 읽는다. */
  dashboard: (projectId: string | number) =>
    apiClient.get<ApiResponse<ProjectDashboardData>>(`/api/v1/projects/${projectId}/dashboard`),

  /** status를 항상 현재 값 그대로 같이 보낸다(full-replace, ARCHIVED 불가). */
  update: (
    projectId: string | number,
    body: {
      name: string;
      description?: string | null;
      status: Exclude<ProjectStatus, "ARCHIVED">;
      startDate?: string | null;
      endDate?: string | null;
    }
  ) => apiClient.patch<ApiResponse<ProjectData>>(`/api/v1/projects/${projectId}`, body),

  archive: (projectId: string | number, body?: { reason?: string | null }) =>
    apiClient.post<ApiResponse<ProjectArchiveData>>(`/api/v1/projects/${projectId}/archive`, body ?? {}),
};
