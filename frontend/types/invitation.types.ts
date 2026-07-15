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

/**
 * Sprint 1.5 회사 초대 role. 프로젝트 초대 role(PROJECT_ADMIN)은 Sprint 3
 * `PROJECT-MEMBER-01~02` 후보로 재분류됐다(API-명세서 v1.3 Sprint 3 기준선).
 */
export type InvitationRole = "COMPANY_ADMIN" | "MEMBER";

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

/** accept / decline / cancel 공통 응답. membershipCreated는 accept에서만 의미 있음 */
export interface InvitationActionData {
  invitationId: number;
  status: InvitationStatus;
  membershipCreated: boolean;
}
