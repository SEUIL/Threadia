/*
 * Invitation API 클라이언트 — auth.service.ts와 동일 패턴.
 * apiClient가 Bearer access token 첨부와 401 refresh를 처리한다.
 * 계약: docs/04-api/API-명세서.md §6-A, §13.5-A (WBS 2.2/2.3/2.4 backend)
 */
import apiClient from "@/lib/axios";
import type { ApiResponse, UserSummary } from "@/types/auth.types";
import type {
  CompanyInvitationData,
  CompanyInvitationRole,
  CompanyInvitationsData,
  InvitationActionData,
  ProjectInvitationData,
  ProjectInvitationRole,
  ProjectInvitationsData,
} from "@/types/invitation.types";

export const invitationService = {
  /** 내가 받은 회사 초대 목록 (S14 수신, /invitations) */
  listMine: () =>
    apiClient.get<ApiResponse<CompanyInvitationsData>>("/api/v1/users/me/invitations"),

  accept: (invitationId: number) =>
    apiClient.post<ApiResponse<InvitationActionData>>(`/api/v1/invitations/${invitationId}/accept`),

  decline: (invitationId: number) =>
    apiClient.post<ApiResponse<InvitationActionData>>(`/api/v1/invitations/${invitationId}/decline`),

  /** 보낸 초대 취소 — 저장된 scope 기준으로 COMPANY는 C, PROJECT는 P/C가 취소한다 */
  cancel: (invitationId: number) =>
    apiClient.post<ApiResponse<InvitationActionData>>(`/api/v1/invitations/${invitationId}/cancel`),

  /** 회사가 보낸 초대 목록 (S14 발신, /{companyId}/members) — COMPANY_ADMIN 전용 */
  listCompany: (companyId: string | number) =>
    apiClient.get<ApiResponse<CompanyInvitationsData>>(
      `/api/v1/companies/${companyId}/invitations`
    ),

  /** 회사 초대 생성 — COMPANY_ADMIN 전용. 대상은 lookup으로 확인한 등록 사용자만 */
  create: (
    companyId: string | number,
    body: { inviteeUserId: number; role: CompanyInvitationRole }
  ) =>
    apiClient.post<ApiResponse<CompanyInvitationData>>(
      `/api/v1/companies/${companyId}/invitations`,
      body
    ),

  /** 프로젝트가 보낸 초대 목록 (S12) — P/C 전용 */
  listProject: (projectId: string | number) =>
    apiClient.get<ApiResponse<ProjectInvitationsData>>(`/api/v1/projects/${projectId}/invitations`),

  /**
   * 프로젝트 초대 생성 — P/C 전용. 대상은 같은 회사의 ACTIVE 구성원만 가능하고
   * (서버가 TARGET_NOT_ACTIVE_COMPANY_MEMBER로 거절), 이미 프로젝트 멤버이거나
   * 본인이면 409다. scopeType은 보내지 않는다 — scope는 endpoint가 결정한다.
   */
  createProject: (
    projectId: string | number,
    body: { inviteeUserId: number; role: ProjectInvitationRole }
  ) =>
    apiClient.post<ApiResponse<ProjectInvitationData>>(
      `/api/v1/projects/${projectId}/invitations`,
      body
    ),

  /** 등록 사용자 exact email lookup. 서버가 trim+lowercase 정규화 후 조회한다 */
  lookup: (email: string) =>
    apiClient.get<ApiResponse<{ user: UserSummary }>>("/api/v1/users/lookup", {
      params: { email },
    }),
};
