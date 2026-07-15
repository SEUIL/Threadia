/*
 * Invitation API 클라이언트 — auth.service.ts와 동일 패턴.
 * apiClient가 Bearer access token 첨부와 401 refresh를 처리한다.
 * 계약: docs/04-api/API-명세서.md §6-A, §13.5-A (WBS 2.2/2.3/2.4 backend)
 */
import apiClient from "@/lib/axios";
import type { ApiResponse, UserSummary } from "@/types/auth.types";
import type {
  CompanyInvitationData,
  CompanyInvitationsData,
  InvitationActionData,
  InvitationRole,
} from "@/types/invitation.types";

export const invitationService = {
  /** 내가 받은 회사 초대 목록 (S14 수신, /invitations) */
  listMine: () =>
    apiClient.get<ApiResponse<CompanyInvitationsData>>("/api/v1/users/me/invitations"),

  accept: (invitationId: number) =>
    apiClient.post<ApiResponse<InvitationActionData>>(`/api/v1/invitations/${invitationId}/accept`),

  decline: (invitationId: number) =>
    apiClient.post<ApiResponse<InvitationActionData>>(`/api/v1/invitations/${invitationId}/decline`),

  /** 보낸 초대 취소 — COMPANY_ADMIN 전용 */
  cancel: (invitationId: number) =>
    apiClient.post<ApiResponse<InvitationActionData>>(`/api/v1/invitations/${invitationId}/cancel`),

  /** 회사가 보낸 초대 목록 (S14 발신, /{companyId}/members) — COMPANY_ADMIN 전용 */
  listCompany: (companyId: string | number) =>
    apiClient.get<ApiResponse<CompanyInvitationsData>>(
      `/api/v1/companies/${companyId}/invitations`
    ),

  /** 회사 초대 생성 — COMPANY_ADMIN 전용. 대상은 lookup으로 확인한 등록 사용자만 */
  create: (companyId: string | number, body: { inviteeUserId: number; role: InvitationRole }) =>
    apiClient.post<ApiResponse<CompanyInvitationData>>(
      `/api/v1/companies/${companyId}/invitations`,
      body
    ),

  /** 등록 사용자 exact email lookup. 서버가 trim+lowercase 정규화 후 조회한다 */
  lookup: (email: string) =>
    apiClient.get<ApiResponse<{ user: UserSummary }>>("/api/v1/users/lookup", {
      params: { email },
    }),
};
