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
  ProjectInvitableMemberData,
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
   * 프로젝트 초대 후보 exact-email 조회 — P/C 전용 (Issue #108).
   * 회사 관리자 전용인 lookup()과 달리 P도 쓸 수 있지만, 조회 범위가 이 프로젝트
   * 회사의 ACTIVE 구성원으로 한정된다. 후보가 아니면(미가입·비활성·다른 회사)
   * 구분 없이 404다 — 회사 밖 email의 가입 여부를 노출하지 않기 위한 계약이다.
   * 이미 프로젝트 멤버이거나 대기 중인 초대가 있으면 409.
   */
  findInvitableMember: (projectId: string | number, email: string) =>
    apiClient.get<ApiResponse<ProjectInvitableMemberData>>(
      `/api/v1/projects/${projectId}/invitable-members`,
      { params: { email } }
    ),

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
