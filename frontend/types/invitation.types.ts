/*
 * Invitation 응답 타입 — backend InvitationResponse record와 1:1 매핑.
 * 계약 기준: docs/04-api/API-명세서.md §13.5-A,
 * docs/08-members/member-정철진/기술-연구/s14-invitation-프론트-연동-설계.md §2
 *
 * companyName/inviterNickname/inviteeEmail/inviteeNickname은 화면 표시용,
 * id/companyId/projectId/inviterUserId/inviteeUserId는 action·권한 처리용이다.
 */

export type InvitationStatus = "PENDING" | "ACCEPTED" | "DECLINED" | "CANCELED" | "EXPIRED";

export type InvitationScopeType = "COMPANY" | "PROJECT";

/** 회사 초대 role — POST /companies/{id}/invitations */
export type CompanyInvitationRole = "COMPANY_ADMIN" | "MEMBER";

/** 프로젝트 초대 role — POST /projects/{id}/invitations (WBS 4.3.1) */
export type ProjectInvitationRole = "PROJECT_ADMIN" | "MEMBER";

export interface InvitationResponse {
  id: number;
  scopeType: InvitationScopeType;
  companyId: number;
  companyName: string;
  projectId: number | null;
  inviterUserId: number;
  inviterNickname: string;
  inviteeUserId: number;
  inviteeEmail: string;
  inviteeNickname: string;
  /**
   * scope에 따라 값이 다르다 — COMPANY면 CompanyInvitationRole, PROJECT면
   * ProjectInvitationRole. 서버가 String으로 내려주므로 표시할 때 scope를 함께 본다.
   */
  role: string;
  status: InvitationStatus;
  expiresAt: string; // ISO-8601 UTC, 생성 +7일
}

/** GET /users/me/invitations, GET /companies/{id}/invitations — 페이지네이션 없는 scoped wrapper */
export interface CompanyInvitationsData {
  invitations: InvitationResponse[];
}

/** POST /companies/{id}/invitations 201 응답 */
export interface CompanyInvitationData {
  invitation: InvitationResponse;
}

/** GET /projects/{id}/invitations — PROJECT scope만 반환, 생성 시각 내림차순 */
export interface ProjectInvitationsData {
  invitations: InvitationResponse[];
}

/** POST /projects/{id}/invitations 201 응답 */
export interface ProjectInvitationData {
  invitation: InvitationResponse;
}

/** accept / decline / cancel 공통 응답. membershipCreated는 accept에서만 의미 있음 */
export interface InvitationActionData {
  invitationId: number;
  status: InvitationStatus;
  membershipCreated: boolean;
}
