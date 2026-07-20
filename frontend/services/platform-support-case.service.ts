/*
 * Platform Support Case API 클라이언트 — WBS 4.10.1 기준(platform 전용 endpoint만).
 * 일반 사용자용 본인 case 생성/조회(`/api/v1/support-cases`)는 이 서비스 범위 밖이다.
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  InternalNoteRequest,
  InternalNoteResponse,
  InternalNotesData,
  PublicReplyRequest,
  PublicReplyResponse,
  SupportCaseDetailData,
  SupportCaseStatusUpdateRequest,
  SupportCaseResponse,
  SupportCasesData,
} from "@/types/support-case.types";

export const platformSupportCaseService = {
  list: () => apiClient.get<ApiResponse<SupportCasesData>>("/api/v1/platform/support-cases"),

  get: (supportCaseId: number | string) =>
    apiClient.get<ApiResponse<SupportCaseDetailData>>(
      `/api/v1/platform/support-cases/${supportCaseId}`
    ),

  listInternalNotes: (supportCaseId: number | string) =>
    apiClient.get<ApiResponse<InternalNotesData>>(
      `/api/v1/platform/support-cases/${supportCaseId}/internal-notes`
    ),

  createPublicReply: (supportCaseId: number | string, body: PublicReplyRequest) =>
    apiClient.post<ApiResponse<PublicReplyResponse>>(
      `/api/v1/platform/support-cases/${supportCaseId}/public-replies`,
      body
    ),

  createInternalNote: (supportCaseId: number | string, body: InternalNoteRequest) =>
    apiClient.post<ApiResponse<InternalNoteResponse>>(
      `/api/v1/platform/support-cases/${supportCaseId}/internal-notes`,
      body
    ),

  updateStatus: (supportCaseId: number | string, body: SupportCaseStatusUpdateRequest) =>
    apiClient.patch<ApiResponse<SupportCaseResponse>>(
      `/api/v1/platform/support-cases/${supportCaseId}/status`,
      body
    ),
};
