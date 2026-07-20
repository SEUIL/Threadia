/*
 * Platform Audit(WBS 4.10.4) 타입 — redacted keyset 조회.
 * 근거: docs/04-api/API-명세서.md §10-C.1, backend com.threadia.backend.platform.audit.*
 */

export type AdminAction =
  | "SUPPORT_CASE_CREATED"
  | "PUBLIC_REPLY_CREATED"
  | "INTERNAL_NOTE_CREATED"
  | "STATUS_CHANGED"
  | "COMPANY_STATUS_CHANGED"
  | "SYSTEM_SETTING_REPLACED"
  | "PLATFORM_ADMIN_GRANTED"
  | "PLATFORM_ADMIN_REVOKED"
  | "PLATFORM_ADMIN_REACTIVATED"
  | "PLATFORM_ADMIN_ROLE_CHANGED";

export type AuditTargetType =
  | "SUPPORT_CASE"
  | "PUBLIC_REPLY"
  | "INTERNAL_NOTE"
  | "COMPANY"
  | "SYSTEM_SETTING"
  | "PLATFORM_ADMIN";

export type AuditAuthoritySource = "USER" | "PLATFORM_ADMIN";

export interface AuditResponse {
  id: number;
  supportCaseId: number | null;
  actorUserId: number;
  actorPlatformAdminId: number | null;
  authoritySource: AuditAuthoritySource;
  authorityRole: string | null;
  action: AdminAction;
  targetType: AuditTargetType;
  targetId: number | null;
  reason: string;
  beforeState: Record<string, unknown> | null;
  afterState: Record<string, unknown> | null;
  createdAt: string;
}

export interface AuditPageData {
  audits: AuditResponse[];
  nextCursor: string | null;
  hasNext: boolean;
  size: number;
}

/** GET /api/v1/platform/audits query filter — 전부 optional. */
export interface AuditFilters {
  actorUserId?: number;
  action?: AdminAction;
  targetType?: AuditTargetType;
  targetId?: number;
  supportCaseId?: number;
  createdFrom?: string;
  createdTo?: string;
  cursor?: string;
  size?: number;
}

export const ADMIN_ACTION_LABEL: Record<AdminAction, string> = {
  SUPPORT_CASE_CREATED: "문의/신고 접수",
  PUBLIC_REPLY_CREATED: "공개 답변 작성",
  INTERNAL_NOTE_CREATED: "내부 메모 작성",
  STATUS_CHANGED: "상태 변경",
  COMPANY_STATUS_CHANGED: "회사 상태 변경",
  SYSTEM_SETTING_REPLACED: "시스템 설정 변경",
  PLATFORM_ADMIN_GRANTED: "관리자 권한 부여",
  PLATFORM_ADMIN_REVOKED: "관리자 권한 회수",
  PLATFORM_ADMIN_REACTIVATED: "관리자 권한 재활성화",
  PLATFORM_ADMIN_ROLE_CHANGED: "관리자 역할 변경",
};
